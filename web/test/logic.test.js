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
  assert.deepEqual(CATEGORIES.map((c) => c.id), ['RENT', 'SUPERMARKET', 'ENTERTAINMENT', 'BILLS', 'SHOPPING', 'FUEL', 'OTHER']);
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
  goals: { entries: [{ category: 'ENTERTAINMENT', month: '2026-11', cents: 2000, onwards: false }] },
  alertsSent: [],
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
  assert.equal(s.overall, undefined); // the monthly total goal is gone
  const ent = s.categories.find((c) => c.category === 'ENTERTAINMENT').goal;
  assert.equal(ent.percent, 210);
  assert.equal(ent.isOver, true);
  assert.equal(s.categories.find((c) => c.category === 'RENT').goal, null);
  assert.equal(monthlySummary(data, '2026-12').categories.find((c) => c.category === 'ENTERTAINMENT').goal, null);
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
  s.setItem(KEY, JSON.stringify({ expenses: [{ id: 'x', date: '2026-11-01', title: 'Cinema', amountCents: 900, category: 'ENTERTAINMENT', myPercent: 100 }], goals: { monthlyCents: 500, perCategoryCents: { RENT: 1, ENTERTAINMENT: 6000 } } }));
  const d = load(s);
  assert.equal(d.expenses[0].foodType, null);
  // Pre-1.5 goals: Entertainment becomes an every-month goal, the rest is dropped.
  assert.deepEqual(d.goals, { entries: [{ category: 'ENTERTAINMENT', month: '2000-01', cents: 6000, onwards: true }] });
  assert.deepEqual(d.alertsSent, []);
});

// --- fuel and annals ---

import { yearSummary, allTimeSummary, monthsBetween } from '../js/summary.js';

test('fuel category sits between shopping and other', () => {
  const ids = CATEGORIES.map((c) => c.id);
  assert.deepEqual(ids.slice(-3), ['SHOPPING', 'FUEL', 'OTHER']);
  assert.equal(CATEGORIES.find((c) => c.id === 'FUEL').emoji, '⛽');
  assert.throws(() => makeExpense({ id: 'f', date: '2026-11-01', title: 'Diesel', amountCents: 6000, category: 'FUEL', foodType: 'DELIVERY' }));
});

const today = '2026-11-22';
const annals = {
  expenses: [
    e('2025-03-10', 10000, 'RENT'),
    e('2025-12-01', 50000, 'RENT'),
    e('2026-01-05', 60000, 'RENT'),
    e('2026-01-20', 4000, 'FUEL'),
    e('2026-03-02', 6000, 'FUEL', 50),
    e('2026-11-21', 2000, 'ENTERTAINMENT', 100, 'DINE_OUT'),
  ],
  goals: {},
};

test('months between', () => {
  assert.equal(monthsBetween('2025-03', '2026-11'), 21);
  assert.equal(monthsBetween('2026-01', '2026-01'), 1);
  assert.equal(monthsBetween('2026-05', '2026-01'), 0);
});

test('year summary matches the Android app', () => {
  const y = yearSummary(annals, 2026, today);
  assert.equal(y.totalCents, 69000);
  assert.equal(y.paidTotalCents, 72000);
  assert.equal(y.expenseCount, 4);
  assert.equal(y.bars.length, 12);
  assert.deepEqual(y.bars.map((b) => b.label).join(''), 'JFMAMJJASOND');
  assert.equal(y.bars[0].cents, 64000);
  assert.equal(y.bars[2].cents, 3000);
  assert.equal(y.bars.reduce((s, b) => s + b.cents, 0), y.totalCents);
  assert.equal(y.monthsCounted, 11);
  assert.equal(y.averagePerMonthCents, Math.floor((69000 + 5) / 11));
  assert.equal(y.previousTotalCents, 10000);
  assert.equal(y.changePercent, 590);
  assert.equal(y.biggest.amountCents, 60000);
  assert.equal(y.categories.find((c) => c.category === 'FUEL').spentCents, 7000);
  assert.equal(y.food.totalCents, 2000);
});

test('first, past, future and empty years', () => {
  assert.equal(yearSummary(annals, 2025, today).monthsCounted, 10);
  assert.equal(yearSummary(annals, 2025, today).changePercent, null);
  const withOld = { ...annals, expenses: [...annals.expenses, e('2024-12-31', 30000, 'OTHER')] };
  assert.equal(yearSummary(withOld, 2025, today).previousTotalCents, 30000);
  assert.equal(yearSummary(withOld, 2025, today).changePercent, 100);
  const future = yearSummary(annals, 2027, today);
  assert.equal(future.totalCents, 0);
  assert.equal(future.monthsCounted, 0);
  assert.equal(future.biggest, null);
  assert.equal(yearSummary({ expenses: [], goals: {} }, 2026, today).monthsCounted, 0);
});

test('all-time summary', () => {
  const all = allTimeSummary(annals, today);
  assert.deepEqual(all.bars.map((b) => b.label), ['2025', '2026']);
  assert.deepEqual(all.bars.map((b) => b.cents), [60000, 69000]);
  assert.equal(all.totalCents, 129000);
  assert.equal(all.monthsCounted, 21);
  assert.equal(all.changePercent, null);
  const old = { ...annals, expenses: [...annals.expenses, e('2019-06-01', 100, 'OTHER')] };
  assert.equal(allTimeSummary(old, today).bars[0].label, "'19");
  assert.equal(allTimeSummary(old, today).bars.length, 8);
  const empty = allTimeSummary({ expenses: [], goals: {} }, today);
  assert.deepEqual(empty.bars, []);
  assert.equal(empty.totalCents, 0);
});

// --- 21:30 owl reminder (iPhone app) ---

import { reminderMessage, upcomingReminders } from '../js/reminder.js';

test('reminder wording matches the Android app', () => {
  const day = '2026-11-22';
  const none = reminderMessage([e('2026-11-21', 500, 'OTHER')], day);
  assert.equal(none.title, '🦉 Owl post: did you forget the ledger?');
  assert.equal(none.body, 'No expenses recorded today. Tap to record what you spent.');
  const some = reminderMessage([e(day, 1000, 'OTHER'), e(day, 3000, 'OTHER', 50), e('2026-11-21', 9999, 'OTHER')], day);
  assert.equal(some.title, '🦉 Owl post: anything else for the ledger?');
  assert.equal(some.body, 'Today: 2 expenses, €25.00. Tap to add anything you missed.');
  assert.equal(reminderMessage([e(day, 450, 'OTHER')], day).body, 'Today: 1 expense, €4.50. Tap to add anything you missed.');
});

test('upcoming reminders start today before 21:30', () => {
  const list = upcomingReminders([e('2026-11-22', 1200, 'OTHER')], new Date(2026, 10, 22, 9, 0), 30);
  assert.equal(list.length, 30);
  assert.equal(list[0].date, '2026-11-22');
  assert.equal(list[0].body, 'Today: 1 expense, €12.00. Tap to add anything you missed.');
  assert.equal(list[1].date, '2026-11-23');
  assert.equal(list[1].title, '🦉 Owl post: did you forget the ledger?');
  assert.equal(list.at(-1).date, '2026-12-21');
  assert.ok(list.every((r) => r.hour === 21 && r.minute === 30));
});

test('upcoming reminders skip today once 21:30 has passed', () => {
  const list = upcomingReminders([], new Date(2026, 10, 22, 21, 30), 30);
  assert.equal(list.length, 29);
  assert.equal(list[0].date, '2026-11-23');
});

test('upcoming reminders cross month and year ends', () => {
  const list = upcomingReminders([], new Date(2026, 11, 31, 8, 0), 3);
  assert.deepEqual(list.map((r) => r.date), ['2026-12-31', '2027-01-01', '2027-01-02']);
});

// --- split wording ---

import { splitLine } from '../js/model.js';

test('split wording for the read-only view', () => {
  assert.equal(splitLine(e('2026-11-22', 3600, 'OTHER'), format), 'Not split · all yours');
  assert.equal(splitLine(e('2026-11-22', 3600, 'OTHER', 50), format), 'Your share 50% (€18.00) · theirs 50% (€18.00)');
  assert.equal(splitLine(e('2026-11-22', 1001, 'OTHER', 33), format), 'Your share 33% (€3.30) · theirs 67% (€6.71)');
});

// --- page setup: no zoom, fixed top bar ---

import { readFileSync } from 'node:fs';

test('page blocks zooming and keeps the top bar fixed', () => {
  const html = readFileSync(new URL('../index.html', import.meta.url), 'utf8');
  const css = readFileSync(new URL('../css/style.css', import.meta.url), 'utf8');
  const app = readFileSync(new URL('../js/app.js', import.meta.url), 'utf8');
  assert.match(html, /maximum-scale=1, user-scalable=no/);
  assert.match(css, /html \{[^}]*touch-action: manipulation/);
  assert.match(css, /\.bar \{\s*position: fixed;/);
  assert.match(app, /'gesturestart', 'gesturechange', 'gestureend'/);
});

test('record button is framed and floats on a shadow; settings is a tab', () => {
  const css = readFileSync(new URL('../css/style.css', import.meta.url), 'utf8');
  const app = readFileSync(new URL('../js/app.js', import.meta.url), 'utf8');
  assert.match(css, /\.fab \{[^}]*filter: drop-shadow/);
  assert.match(css, /\.frame \{[^}]*background: var\(--gold\)/);
  // Every main button carries the same frame as the Record button.
  const buttons = app.match(/<button class="(btn|fab)[^"]*"[^>]*>/g);
  const framed = app.match(/<button class="(btn|fab)[^"]*"[^>]*><span class="frame"><span class="frame-inner">/g);
  assert.ok(buttons.length >= 8);
  assert.equal(framed.length, buttons.length);
  assert.match(app, /\['settings', '#\/settings', W\.settings\]/);
  assert.doesNotMatch(app, /test-owl/);
  assert.equal(wordsFor(true).exportButton, 'Geminio · export CSV');
  assert.equal(wordsFor(false).exportButton, 'Export CSV');
});

test('number pad keys are centred', () => {
  const css = readFileSync(new URL('../css/style.css', import.meta.url), 'utf8');
  const rule = css.match(/\.numpad button \{([^}]*)\}/)[1];
  assert.match(rule, /display: flex/);
  assert.match(rule, /align-items: center/);
  assert.match(rule, /justify-content: center/);
});

// --- per-month goals and warnings (same cases as the Android tests) ---

import { goalFor, setGoal, removeGoal, migrateGoals, scopeText, alertsDue, markAlertsSent, LEGACY_START } from '../js/goals.js';

const cents = (g, cat, m) => goalFor(g, cat, m)?.cents ?? null;
const E = 'ENTERTAINMENT';

test('goals for one month or from a month onwards', () => {
  assert.equal(cents({ entries: [] }, E, '2026-11'), null);
  const one = setGoal({ entries: [] }, E, '2026-11', 6000, false);
  assert.deepEqual([cents(one, E, '2026-10'), cents(one, E, '2026-11'), cents(one, E, '2026-12')], [null, 6000, null]);
  const on = setGoal({ entries: [] }, E, '2026-11', 6000, true);
  assert.deepEqual([cents(on, E, '2026-10'), cents(on, E, '2026-11'), cents(on, E, '2027-01')], [null, 6000, 6000]);
  assert.equal(cents(on, 'FUEL', '2026-11'), null);
});

test('single-month goals win; later onwards goals take over', () => {
  let g = setGoal(setGoal({ entries: [] }, E, '2026-11', 6000, true), E, '2026-12', 9000, false);
  assert.deepEqual([cents(g, E, '2026-11'), cents(g, E, '2026-12'), cents(g, E, '2027-01')], [6000, 9000, 6000]);
  g = setGoal(setGoal({ entries: [] }, E, '2026-10', 5000, true), E, '2026-12', 7000, true);
  assert.deepEqual([cents(g, E, '2026-11'), cents(g, E, '2026-12'), cents(g, E, '2027-01')], [5000, 7000, 7000]);
  g = setGoal(setGoal(setGoal({ entries: [] }, E, '2026-12', 7000, true), E, '2026-11', 4000, false), E, '2026-11', 6000, true);
  assert.deepEqual([cents(g, E, '2026-11'), cents(g, E, '2026-12')], [6000, 6000]);
  assert.equal(g.entries.length, 1);
});

test('removing for one month or from a month onwards', () => {
  const base = setGoal({ entries: [] }, E, '2026-10', 5000, true);
  const one = removeGoal(base, E, '2026-11', false);
  assert.deepEqual([cents(one, E, '2026-10'), cents(one, E, '2026-11'), cents(one, E, '2026-12')], [5000, null, 5000]);
  const on = removeGoal(setGoal(base, E, '2027-01', 9000, false), E, '2026-11', true);
  assert.deepEqual([cents(on, E, '2026-10'), cents(on, E, '2026-11'), cents(on, E, '2026-12'), cents(on, E, '2027-01')], [5000, null, null, null]);
  assert.deepEqual(removeGoal(setGoal({ entries: [] }, E, '2026-11', 6000, false), E, '2026-11', false).entries, []);
});

test('old goals convert, new ones are kept', () => {
  const g = migrateGoals({ monthlyCents: 120000, perCategoryCents: { ENTERTAINMENT: 6000, RENT: 50000 } });
  assert.deepEqual([cents(g, E, '2024-03'), cents(g, 'RENT', '2026-11')], [6000, null]);
  const fresh = setGoal({ entries: [] }, E, '2026-11', 6000, false);
  assert.deepEqual(migrateGoals(fresh), fresh);
  assert.deepEqual(migrateGoals(undefined), { entries: [] });
});

test('scope wording', () => {
  assert.equal(scopeText({ month: '2026-11', onwards: false }, '2026-11'), 'Only November 2026');
  assert.equal(scopeText({ month: '2026-11', onwards: true }, '2026-11'), 'From November 2026 onwards');
  assert.equal(scopeText({ month: '2026-09', onwards: true }, '2026-11'), 'Every month since September 2026');
  assert.equal(scopeText({ month: LEGACY_START, onwards: true }, '2026-11'), 'Every month');
});

const wardData = (...exps) => ({ expenses: exps, goals: setGoal({ entries: [] }, E, '2026-11', 6000, false), alertsSent: [] });
const fun = (c, p = 100, date = '2026-11-10') => e(date, c, E, p);

test('warnings at 80% and at the goal, with the Android wording', () => {
  assert.deepEqual(alertsDue(wardData(fun(4700)), '2026-11'), []);
  const at80 = alertsDue(wardData(fun(4800)), '2026-11');
  assert.deepEqual(at80.map((a) => a.level), [80]);
  assert.equal(at80[0].title, '🦉 80% of your Entertainment ward is spent');
  assert.equal(at80[0].body, '€48.00 of €60.00 in November · €12.00 left. Time to slow down.');
  assert.deepEqual(alertsDue(wardData(fun(9600, 50)), '2026-11').map((a) => a.level), [80]);
  const exact = alertsDue(wardData(fun(6000)), '2026-11')[0];
  assert.equal(exact.title, '⚡ Your Entertainment ward is broken');
  assert.equal(exact.body, '€60.00 of €60.00 in November · nothing left.');
  assert.equal(alertsDue(wardData(fun(6500)), '2026-11')[0].body, '€65.00 of €60.00 in November · €5.00 over.');
  assert.deepEqual(alertsDue(wardData(e('2026-11-10', 99999, 'RENT')), '2026-11'), []);
});

test('each warning only once per month; a jump past the goal covers 80% too', () => {
  const d = wardData(fun(5000));
  const sent = markAlertsSent(d, alertsDue(d, '2026-11'));
  assert.deepEqual(alertsDue({ ...sent, expenses: [...sent.expenses, fun(100)] }, '2026-11'), []);
  const over = { ...sent, expenses: [...sent.expenses, fun(2000)] };
  assert.deepEqual(alertsDue(over, '2026-11').map((a) => a.level), [100]);
  assert.deepEqual(alertsDue(markAlertsSent(over, alertsDue(over, '2026-11')), '2026-11'), []);
  const jump = wardData(fun(7000));
  assert.deepEqual(markAlertsSent(jump, alertsDue(jump, '2026-11')).alertsSent.sort(), ['ENTERTAINMENT|2026-11|100', 'ENTERTAINMENT|2026-11|80']);
});

// --- settings, number styles, words, suggestions (1.6) ---

import { setMoneyStyle } from '../js/money.js';
import { wordsFor } from '../js/words.js';
import { loadSettings, saveSettings, validTime, DEFAULTS, SETTINGS_KEY } from '../js/settings.js';
import { suggestions, defaultTitle } from '../js/queries.js';

test('Greek number style', () => {
  assert.equal(format(123456, 'el'), '1.234,56 €');
  assert.equal(format(-310, 'el'), '-3,10 €');
  assert.equal(format(5, 'el'), '0,05 €');
  assert.equal(compact(4520, 'el'), '45€');
  assert.equal(compact(123456, 'el'), '1,2k€');
  assert.equal(compact(30, 'el'), '<1€');
  assert.equal(forInput(1250, 'el'), '12,50');
  assert.equal(parseToCents(forInput(123456, 'el')), 123456);
  setMoneyStyle('el');
  assert.equal(format(1250), '12,50 €');
  setMoneyStyle('en');
  assert.equal(format(1250), '€12.50');
});

test('plain and wizard words', () => {
  const w = wordsFor(true);
  const p = wordsFor(false);
  assert.equal(w.goalsTab, '🛡 Protego');
  assert.equal(p.goalsTab, '🎯 Goals');
  assert.equal(p.search, 'Search');
  assert.equal(p.deleteBtn, 'Delete');
  assert.equal(p.alert80('Fuel'), '80% of your Fuel goal is spent');
  // Every wording exists in both sets.
  assert.deepEqual(Object.keys(w).sort(), Object.keys(p).sort());
});

test('plain words reach reminders and goal warnings', () => {
  const plain = wordsFor(false);
  assert.equal(reminderMessage([], '2026-11-22', plain).title, "Did you record today's expenses?");
  const d = { expenses: [e('2026-11-10', 6500, 'ENTERTAINMENT')], goals: setGoal({ entries: [] }, 'ENTERTAINMENT', '2026-11', 6000, false), alertsSent: [] };
  assert.equal(alertsDue(d, '2026-11', plain)[0].title, "You've reached your Entertainment goal");
  assert.equal(describeGoal(goalProgress(2000, 3000), plain), '150% of €20.00 · €10.00 over. Over the goal!');
});

test('reminders at a chosen time', () => {
  const list = upcomingReminders([], new Date(2026, 10, 22, 20, 0), 3, '19:45');
  assert.deepEqual(list.map((r) => `${r.date} ${r.hour}:${r.minute}`), ['2026-11-23 19:45', '2026-11-24 19:45']);
  assert.equal(upcomingReminders([], new Date(2026, 10, 22, 8, 0), 1, '07:05').length, 0);
  assert.equal(upcomingReminders([], new Date(2026, 10, 22, 7, 0), 1, '07:05')[0].hour, 7);
});

test('settings load with defaults, keep valid values and migrate the old reminder switch', () => {
  const s = fakeStorage();
  assert.deepEqual(loadSettings(s), DEFAULTS);
  s.setItem('expensiarmus.reminder', 'off');
  assert.equal(loadSettings(s).reminderOn, false);
  saveSettings(s, { ...DEFAULTS, reminderTime: '07:15', wizard: false, money: 'el', theme: 'dark' });
  assert.deepEqual(loadSettings(s), { reminderOn: true, reminderTime: '07:15', wizard: false, money: 'el', theme: 'dark' });
  s.setItem(SETTINGS_KEY, JSON.stringify({ reminderTime: '25:00', money: 'xx', theme: 'purple' }));
  assert.deepEqual(loadSettings(s), { ...DEFAULTS, reminderOn: false });
  s.setItem(SETTINGS_KEY, '{oops');
  assert.equal(loadSettings(s).reminderTime, '21:30');
  assert.ok(validTime('00:00') && validTime('23:59') && !validTime('24:00') && !validTime('9:30'));
});

const history = [
  makeExpense({ id: 'a', date: '2026-11-01', title: 'Freddo espresso', amountCents: 250, category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY' }),
  makeExpense({ id: 'b', date: '2026-11-03', title: 'Freddo espresso', amountCents: 270, category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY' }),
  makeExpense({ id: 'c', date: '2026-11-02', title: 'Fresh juice', amountCents: 400, category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY' }),
  makeExpense({ id: 'd', date: '2026-11-04', title: 'Diesel', amountCents: 6000, category: 'FUEL' }),
  makeExpense({ id: 'e', date: '2026-11-05', title: 'Σουβλάκι με Μαρία', amountCents: 3600, category: 'ENTERTAINMENT', myPercent: 50, foodType: 'DINE_OUT' }),
];

test('suggestions from history: most used first, latest amount', () => {
  const s = suggestions(history, 'fre');
  assert.deepEqual(s.map((x) => x.title), ['Freddo espresso', 'Fresh juice']);
  assert.deepEqual(s[0], { title: 'Freddo espresso', amountCents: 270, category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY', myPercent: 100, uses: 2 });
  assert.deepEqual(suggestions(history, 'espr').map((x) => x.title), ['Freddo espresso']);
  assert.deepEqual(suggestions(history, 'μαρια').map((x) => [x.title, x.myPercent]), [['Σουβλάκι με Μαρία', 50]]);
  assert.deepEqual(suggestions(history, ''), []);
  assert.deepEqual(suggestions(history, 'xyz'), []);
  assert.equal(suggestions(history, 'f', 1).length, 1);
});

test('quick add default titles', () => {
  assert.equal(defaultTitle('SUPERMARKET', null), 'Super market');
  assert.equal(defaultTitle('ENTERTAINMENT', 'DAY_TO_DAY'), 'Day-to-day');
  assert.equal(defaultTitle('FUEL', null), 'Fuel');
});
