import { test } from 'node:test';
import assert from 'node:assert/strict';
import { makeExpense, myShareCents, upsert, remove, find, emptyData, CATEGORIES, FOOD_TYPES } from '../js/model.js';
import { parseToCents, format, plain, forInput, compact } from '../js/money.js';
import { monthlySummary, goalProgress, describeGoal } from '../js/summary.js';
import { dailyTotals, onDay, calendarCells, byCategory, search, normalize, shiftMonth, daysInMonth } from '../js/queries.js';
import { toCsv, escape, HEADER } from '../js/csv.js';
import { load, save, KEY } from '../js/store.js';

let n = 0;
const e = (date, cents, cat, percent = 100, food = null, title = `t${n}`) =>
  makeExpense({ id: `id${n++}`, date, title, amountCents: cents, category: cat, myPercent: percent, foodType: food });

// --- model ---

test('split share rounds half up to the cent', () => {
  assert.equal(myShareCents(e('2026-11-01', 4250, 'OTHER')), 4250);
  assert.equal(myShareCents(e('2026-11-01', 4250, 'OTHER', 50)), 2125);
  assert.equal(myShareCents(e('2026-11-01', 101, 'OTHER', 33)), 33);
  assert.equal(myShareCents(e('2026-11-01', 1, 'OTHER', 50)), 1);
  assert.equal(myShareCents(e('2026-11-01', 10001, 'OTHER', 50)), 5001);
});

test('invalid expenses are rejected', () => {
  assert.throws(() => e('2026-11-01', -1, 'OTHER'));
  assert.throws(() => e('2026-11-01', 100, 'OTHER', 0));
  assert.throws(() => e('2026-11-01', 100, 'OTHER', 101));
  assert.throws(() => e('2026-11-01', 100, 'NOPE'));
  assert.throws(() => e('2026-11-01', 100, 'SUPERMARKET', 100, 'DELIVERY'));
  assert.throws(() => e('22/11/2026', 100, 'OTHER'));
});

test('categories and food types match the Android app', () => {
  assert.deepEqual(CATEGORIES.map((c) => c.id), ['RENT', 'SUPERMARKET', 'ENTERTAINMENT', 'BILLS', 'SHOPPING', 'OTHER']);
  assert.deepEqual(FOOD_TYPES.map((f) => f.id), ['DELIVERY', 'DINE_OUT', 'DAY_TO_DAY']);
  assert.equal(new Set(CATEGORIES.map((c) => c.emoji)).size, CATEGORIES.length);
  assert.equal(CATEGORIES.find((c) => c.id === 'SHOPPING').emoji, '🛍️');
});

test('upsert adds then replaces by id, remove deletes', () => {
  let d = emptyData();
  d = upsert(d, makeExpense({ id: 'x', date: '2026-11-01', title: 'a', amountCents: 100, category: 'OTHER' }));
  d = upsert(d, makeExpense({ id: 'x', date: '2026-11-01', title: 'a', amountCents: 300, category: 'OTHER' }));
  assert.equal(d.expenses.length, 1);
  assert.equal(find(d, 'x').amountCents, 300);
  assert.equal(find(remove(d, 'x'), 'x'), undefined);
});

// --- money ---

test('parses amounts like the Android app', () => {
  const cases = { '12': 1200, '12.50': 1250, '12,50': 1250, '12.5': 1250, ',50': 50, '1.234,56': 123456, '1,234.56': 123456, '1.234': 123400, ' €9,90 ': 990 };
  for (const [input, cents] of Object.entries(cases)) assert.equal(parseToCents(input), cents, input);
  for (const bad of ['', 'abc', '12a', '-5', '12.', '.', '99999999999999999999']) assert.equal(parseToCents(bad), null, bad);
});

test('formats amounts', () => {
  assert.equal(format(0), '€0.00');
  assert.equal(format(123456), '€1,234.56');
  assert.equal(format(100000000), '€1,000,000.00');
  assert.equal(format(-310), '-€3.10');
  assert.equal(plain(7), '0.07');
  assert.equal(forInput(120000), '1200');
  assert.equal(forInput(1250), '12.50');
  for (const c of [0, 7, 1250, 120000, 123456]) assert.equal(parseToCents(forInput(c)), c);
});

test('compact amounts for calendar cells', () => {
  assert.equal(compact(49), '<€1');
  assert.equal(compact(50), '€1');
  assert.equal(compact(4520), '€45');
  assert.equal(compact(123456), '€1.2k');
  assert.equal(compact(1234500), '€12k');
});

// --- monthly summary ---

const data = {
  expenses: [
    e('2026-11-01', 60000, 'RENT'),
    e('2026-11-03', 8000, 'SUPERMARKET'),
    e('2026-11-20', 6000, 'ENTERTAINMENT', 50, 'DINE_OUT'),
    e('2026-11-21', 1200, 'ENTERTAINMENT'),
    e('2026-11-30', 9000, 'BILLS', 50),
    e('2026-10-31', 99999, 'OTHER'),
    e('2026-12-01', 99999, 'OTHER'),
  ],
  goals: { monthlyCents: 100000, perCategoryCents: { ENTERTAINMENT: 2000 } },
};

test('only the month counts, using my share', () => {
  const s = monthlySummary(data, '2026-11');
  assert.equal(s.expenses.length, 5);
  assert.equal(s.totalCents, 60000 + 8000 + 3000 + 1200 + 4500);
  assert.equal(s.paidTotalCents, 60000 + 8000 + 6000 + 1200 + 9000);
  assert.deepEqual(s.expenses.map((x) => x.date.slice(8)), ['30', '21', '20', '03', '01']);
  assert.equal(s.categories.reduce((a, c) => a + c.spentCents, 0), s.totalCents);
});

test('goals report progress', () => {
  const s = monthlySummary(data, '2026-11');
  assert.equal(s.overall.percent, 77);
  assert.equal(s.overall.isOver, false);
  const ent = s.categories.find((c) => c.category === 'ENTERTAINMENT').goal;
  assert.equal(ent.percent, 210);
  assert.equal(ent.isOver, true);
  assert.equal(s.categories.find((c) => c.category === 'RENT').goal, null);
  assert.equal(monthlySummary({ ...data, goals: { monthlyCents: 0 } }, '2026-11').overall, null);
});

test('goal descriptions', () => {
  assert.equal(describeGoal(goalProgress(100000, 50000)), '50% of €1,000.00 · €500.00 left');
  assert.equal(describeGoal(goalProgress(2000, 3000)), '150% of €20.00 · €10.00 over. The Protego ward is broken!');
});

test('food summary inside entertainment', () => {
  const f = monthlySummary(data, '2026-11').food;
  assert.equal(f.totalCents, 3000);
  assert.equal(f.otherEntertainmentCents, 1200);
  assert.deepEqual(f.types.map((t) => t.count), [0, 1, 0]);
});

// --- calendar, category, search ---

test('month helpers', () => {
  assert.equal(shiftMonth('2026-12', 1), '2027-01');
  assert.equal(shiftMonth('2026-01', -1), '2025-12');
  assert.equal(daysInMonth('2028-02'), 29);
});

test('calendar grid starts on Monday and covers whole weeks', () => {
  const nov = calendarCells('2026-11'); // 1 Nov 2026 is a Sunday, the 30th a Monday
  assert.equal(nov.length, 42);
  assert.equal(nov[6], '2026-11-01');
  assert.equal(nov[35], '2026-11-30');
  assert.equal(calendarCells('2027-02').length, 28);
  for (let m = 1; m <= 12; m++) {
    const month = `2026-${String(m).padStart(2, '0')}`;
    const cells = calendarCells(month);
    assert.equal(cells.length % 7, 0);
    assert.equal(cells.filter(Boolean).length, daysInMonth(month));
  }
});

const list = [
  e('2026-11-03', 4000, 'SUPERMARKET', 100, null, 'Lidl'),
  e('2026-11-03', 250, 'ENTERTAINMENT', 100, 'DAY_TO_DAY', 'Freddo espresso'),
  e('2026-11-14', 2000, 'ENTERTAINMENT', 50, 'DINE_OUT', 'Σουβλάκι με φίλους'),
  e('2026-11-20', 9000, 'SHOPPING', 100, null, 'Running shoes'),
  e('2026-10-31', 6000, 'SHOPPING', 100, null, 'Earrings'),
  e('2025-12-24', 3000, 'OTHER', 100, null, 'Gift for mum'),
];

test('daily totals and day lists', () => {
  const t = dailyTotals(list, '2026-11');
  assert.equal(t['2026-11-03'], 4250);
  assert.equal(t['2026-11-14'], 1000);
  assert.equal(t['2026-10-31'], undefined);
  assert.deepEqual(onDay(list, '2026-11-03').map((x) => x.title), ['Freddo espresso', 'Lidl']);
});

test('category pages group by month, newest first', () => {
  const g = byCategory(list, 'SHOPPING');
  assert.deepEqual(g.map((x) => x.month), ['2026-11', '2026-10']);
  assert.deepEqual(g.map((x) => x.totalCents), [9000, 6000]);
  assert.deepEqual(byCategory(list, 'ENTERTAINMENT', 'DINE_OUT').flatMap((x) => x.expenses).map((x) => x.title), ['Σουβλάκι με φίλους']);
  assert.deepEqual(byCategory(list, 'RENT'), []);
});

test('Accio search', () => {
  const titles = (q, cats = []) => search(list, q, cats).map((x) => x.title);
  assert.deepEqual(titles('SHOE'), ['Running shoes']);
  assert.deepEqual(titles('σουβλακι'), ['Σουβλάκι με φίλους']);
  assert.deepEqual(titles('shopping'), ['Running shoes', 'Earrings']);
  assert.deepEqual(titles('dine'), ['Σουβλάκι με φίλους']);
  assert.deepEqual(titles('espresso day-to-day'), ['Freddo espresso']);
  assert.deepEqual(titles('gift', ['OTHER', 'SHOPPING']), ['Gift for mum']);
  assert.deepEqual(titles(''), []);
  assert.equal(normalize('λογαριασμός'), 'λογαριασμοσ');
});

// --- CSV and storage ---

test('CSV has the same columns as the Android export', () => {
  const csv = toCsv([
    makeExpense({ id: '2', date: '2026-11-05', title: 'Pizza, beers', amountCents: 2400, category: 'ENTERTAINMENT', myPercent: 50, foodType: 'DELIVERY' }),
    makeExpense({ id: '1', date: '2026-11-01', title: 'Rent', amountCents: 60000, category: 'RENT' }),
  ]);
  assert.equal(csv, `${HEADER}\n2026-11-01,Rent,Rent,,600.00,100,600.00\n2026-11-05,"Pizza, beers",Entertainment,Delivery,24.00,50,12.00\n`);
  assert.equal(escape('say "hi"'), '"say ""hi"""');
});

const fakeStorage = () => {
  const m = new Map();
  return { getItem: (k) => (m.has(k) ? m.get(k) : null), setItem: (k, v) => m.set(k, String(v)), keys: () => [...m.keys()] };
};

test('storage round trip', () => {
  const s = fakeStorage();
  assert.deepEqual(load(s), emptyData());
  save(s, data);
  assert.deepEqual(load(s), data);
});

test('corrupt storage is backed up, broken entries are skipped', () => {
  const s = fakeStorage();
  s.setItem(KEY, '{not json');
  assert.deepEqual(load(s), emptyData());
  assert.equal(s.keys().filter((k) => k.startsWith(`${KEY}.corrupt-`)).length, 1);
  s.setItem(KEY, JSON.stringify({ expenses: [{ id: 'ok', date: '2026-11-01', title: 'a', amountCents: 5, category: 'OTHER', myPercent: 100 }, { id: 'bad', amountCents: -1 }] }));
  assert.deepEqual(load(s).expenses.map((x) => x.id), ['ok']);
});

test('Android export JSON shape loads (foodType missing)', () => {
  const s = fakeStorage();
  s.setItem(KEY, JSON.stringify({ expenses: [{ id: 'x', date: '2026-11-01', title: 'Cinema', amountCents: 900, category: 'ENTERTAINMENT', myPercent: 100 }], goals: { monthlyCents: 500, perCategoryCents: { RENT: 1 } } }));
  const d = load(s);
  assert.equal(d.expenses[0].foodType, null);
  assert.equal(d.goals.perCategoryCents.RENT, 1);
});
