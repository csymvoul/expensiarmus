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
  assert.equal(csv, `${HEADER}\n2026-11-01,Rent,Home,,600.00,100,600.00\n2026-11-05,"Pizza, beers",Entertainment,Delivery,24.00,50,12.00\n`);
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
  // Data saved before favourites existed loads with none.
  assert.deepEqual(load(s), { ...data, favorites: [] });
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
  assert.equal(CATEGORIES.find((c) => c.id === 'FUEL').emoji, '🚆');
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
  assert.match(css, /\.frame \{[^}]*background: var\(--frame-line\)/);
  // Every main button carries the same frame as the Record button.
  const buttons = app.match(/<button class="(btn|fab)[^"]*"[^>]*>/g);
  const framed = app.match(/<button class="(btn|fab)[^"]*"[^>]*><span class="frame"><span class="frame-inner[^"]*">/g);
  assert.ok(buttons.length >= 8);
  assert.equal(framed.length, buttons.length);
  assert.match(app, /\['settings', '#\/settings', W\.settings\]/);
  assert.doesNotMatch(app, /test-owl/);
  assert.equal(wordsFor(true).exportButton, 'Geminio · export CSV');
  assert.equal(wordsFor(false).exportButton, 'Export CSV');
});

test('ledger entry page has one way to amend and one to delete', () => {
  const app = readFileSync(new URL('../js/app.js', import.meta.url), 'utf8');
  const view = app.slice(app.indexOf('function viewScreen('), app.indexOf('// ---------- category ----------'));
  assert.equal(view.match(/data-action="amend"/g).length, 1);
  assert.equal(view.match(/data-action="ask-delete"/g).length, 1);
  assert.match(view, /header\(W\.entry, \{ backBtn: true, icon: TAB_ICONS\.month \}\)/);
});

test('the expense form saves only with the gold ✓ in its top bar and has no delete', () => {
  const app = readFileSync(new URL('../js/app.js', import.meta.url), 'utf8');
  const edit = app.slice(app.indexOf('function editScreen('), app.indexOf('/** Fills the form from a suggestion'));
  assert.equal(edit.match(/data-action="save"/g).length, 1);
  assert.match(edit, /<button class="icon-btn" data-action="save"[^>]*><span class="check-glyph"><\/span><\/button>/);
  assert.match(edit, /header\(d\.id \? W\.editExpense : W\.newExpense, \{ backBtn: true, actions: check \}\)/);
  assert.doesNotMatch(edit, /class="btn wide" data-action="save"/);
  assert.doesNotMatch(edit, /ask-delete|deleteModal/);
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
  assert.equal(defaultTitle('FUEL', null), 'Transport');
});

// --- painted emblems ---
import { existsSync } from 'node:fs';
import { TAB_ICONS } from '../js/model.js';

const webFile = (path) => new URL(`../${path}`, import.meta.url);

test('every category, food type and tab has its own emblem image on disk', () => {
  const icons = [...CATEGORIES, ...FOOD_TYPES].map((t) => t.icon);
  const tabs = Object.values(TAB_ICONS);
  assert.equal(new Set([...icons, ...tabs]).size, CATEGORIES.length + FOOD_TYPES.length + 5);
  assert.deepEqual(Object.keys(TAB_ICONS), ['month', 'calendar', 'annals', 'goals', 'settings']);
  for (const path of [...icons, ...tabs]) assert.ok(existsSync(webFile(path)), `${path} is missing`);
  assert.equal(CATEGORIES.find((c) => c.id === 'FUEL').icon, 'icons/emblems/cat_fuel.png');
  assert.equal(FOOD_TYPES.find((f) => f.id === 'DELIVERY').icon, 'icons/emblems/food_delivery.png');
});

test('the service worker caches every emblem for offline use', () => {
  const sw = readFileSync(webFile('sw.js'), 'utf8');
  for (const path of [...CATEGORIES, ...FOOD_TYPES].map((t) => t.icon).concat(Object.values(TAB_ICONS))) {
    assert.ok(sw.includes(`'${path}'`), `${path} is not cached`);
  }
});

// --- emblems in headers and Settings, dark button lines ---
import { EMBLEMS, SEARCH_GLYPH, withoutEmoji } from '../js/model.js';

test('withoutEmoji drops a leading emoji and keeps plain labels', () => {
  assert.equal(withoutEmoji('⚙️ Settings'), 'Settings');
  assert.equal(withoutEmoji('📊 Annals'), 'Annals');
  assert.equal(withoutEmoji('🦉 Owl reminder'), 'Owl reminder');
  assert.equal(withoutEmoji('Daily reminder'), 'Daily reminder');
  assert.equal(withoutEmoji('Protego · November'), 'Protego · November');
});

test('logo and Settings emblems exist on disk and are cached offline', () => {
  const sw = readFileSync(webFile('sw.js'), 'utf8');
  assert.deepEqual(Object.keys(EMBLEMS), ['logo', 'geminio', 'apparate', 'reminder', 'themeSystem', 'themeLight', 'themeDark']);
  for (const path of Object.values(EMBLEMS)) {
    assert.ok(existsSync(webFile(path)), `${path} is missing`);
    assert.ok(sw.includes(`'${path}'`), `${path} is not cached`);
  }
});

test('headers, Settings and Annals use the painted emblems, not emoji', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /header\('Expensiarmus', \{ actions, icon: EMBLEMS\.logo \}\)/);
  for (const tab of ['settings', 'annals', 'goals']) assert.match(app, new RegExp(`icon: TAB_ICONS\\.${tab}[ ,]`));
  assert.match(app, /header\(cat\.label, \{ backBtn: true, icon: /);
  assert.doesNotMatch(app, /\$\{cat\.emoji\} \$\{cat\.label\}`, \{ backBtn/);
  assert.match(app, /TAB_ICONS\.calendar \}\)\}By year/);
  for (const e of ['themeSystem', 'themeLight', 'themeDark', 'geminio', 'reminder']) assert.match(app, new RegExp(`EMBLEMS\\.${e}`));
  assert.doesNotMatch(app, /📱 Like the phone|☀️ Light|🌙 Dark|📅 By year/);
});

test('in dark mode the gold buttons get burgundy frame lines', () => {
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  const dark = css.slice(css.indexOf(":root[data-theme='dark']"));
  assert.match(dark.slice(0, dark.indexOf('}')), /--frame-line: #7B1E1E;/);
  const media = css.slice(css.indexOf('@media (prefers-color-scheme: dark)'));
  assert.match(media.slice(0, media.indexOf('}\n}')), /--frame-line: #7B1E1E;/);
  assert.match(css, /\.frame \{[^}]*background: var\(--frame-line\)/);
});

test('Quick add date uses the calendar emblem; Accio uses a magnifier with no badge, per theme', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  const sw = readFileSync(webFile('sw.js'), 'utf8');
  assert.match(app, /<label class="quick-date">\$\{emblem\(TAB_ICONS\.calendar, 24\)\}<input/);
  assert.doesNotMatch(app, /quick-date">📅/);
  assert.match(app, /data-to="#\/search"[^`]*<span class="search-glyph"><\/span>/);
  assert.match(app, /header\(W\.search, \{ backBtn: true, glyph: '<span class="search-glyph"><\/span>' \}\)/);
  assert.doesNotMatch(app, /emblem\([^)]*search/i);
  for (const path of Object.values(SEARCH_GLYPH)) {
    assert.ok(existsSync(webFile(path)), `${path} is missing`);
    assert.ok(sw.includes(`'${path}'`), `${path} is not cached`);
  }
  // Bronze glass on the light theme, gold glass in both dark-theme blocks.
  assert.equal(css.match(/--search-glyph: url\('\.\.\/icons\/emblems\/search_light\.png'\)/g).length, 1);
  assert.equal(css.match(/--search-glyph: url\('\.\.\/icons\/emblems\/search_dark\.png'\)/g).length, 2);
});

test('Cast a new ward shows the painted Protego shield, not the emoji', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /data-action="ward-add"><span class="frame"><span class="frame-inner with-emblem">\$\{emblem\(TAB_ICONS\.goals, 26\)\}\$\{esc\(withoutEmoji\(W\.newWard\)\)\}/);
  assert.equal(withoutEmoji('🛡 Cast a new ward'), 'Cast a new ward');
  assert.equal(withoutEmoji('🎯 Add a goal'), 'Add a goal');
});

test('the header and tab-bar rules have no sparkle clusters at their ends', () => {
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  assert.doesNotMatch(css, /\.bar::before|\.tabs::before/);
});

// --- latest expenses on the Ledger, month at a glance on the Calendar ---
import { ledgerPreview, monthGlance, LATEST_COUNT } from '../js/queries.js';

test('the Ledger lists the latest 5 expenses until Show all is tapped', () => {
  const list = Array.from({ length: 8 }, (_, i) => e(`2026-11-${String(20 - i).padStart(2, '0')}`, 100, 'OTHER'));
  assert.equal(LATEST_COUNT, 5);
  assert.deepEqual(ledgerPreview(list, false).map((x) => x.date), ['2026-11-20', '2026-11-19', '2026-11-18', '2026-11-17', '2026-11-16']);
  assert.equal(ledgerPreview(list, true).length, 8);
  assert.equal(ledgerPreview(list.slice(0, 3), false).length, 3);
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /Show all \$\{expenses\.length\} ›/);
});

test('month at a glance: total, count, average a day so far, busiest day, fair comparison', () => {
  const list = [
    e('2026-11-01', 45000, 'RENT'), e('2026-11-03', 1000, 'OTHER'), e('2026-11-03', 2000, 'OTHER', 50),
    e('2026-11-20', 9000, 'SHOPPING'), // after "today": still in the month total
    e('2026-10-02', 30000, 'RENT'), e('2026-10-05', 500, 'OTHER'), e('2026-10-25', 99999, 'OTHER'),
    e('2026-12-01', 7000, 'OTHER'),
  ];
  const g = monthGlance(list, '2026-11', '2026-11-10');
  assert.equal(g.totalCents, 45000 + 1000 + 1000 + 9000);
  assert.equal(g.count, 4);
  assert.equal(g.daysCounted, 10);
  assert.equal(g.dailyAverageCents, 5600);
  assert.deepEqual(g.busiest, { date: '2026-11-01', cents: 45000 });
  // October only up to the 10th: the 25th is left out.
  assert.equal(g.upToDay, 10);
  assert.equal(g.previousMonth, '2026-10');
  assert.equal(g.previousCents, 30500);
  assert.equal(g.changePercent, Math.round((56000 - 30500) * 100 / 30500));
});

test('a finished month averages over all its days and compares with the whole month before', () => {
  const list = [e('2026-10-02', 3100, 'OTHER'), e('2026-09-30', 6200, 'OTHER'), e('2026-09-01', 0, 'OTHER')];
  const g = monthGlance(list, '2026-10', '2026-11-23');
  assert.equal(g.daysCounted, 31);
  assert.equal(g.dailyAverageCents, 100);
  assert.equal(g.upToDay, null);
  assert.equal(g.changePercent, -50);
});

test('an empty month, or one with nothing the month before, has no comparison', () => {
  const empty = monthGlance([], '2026-11', '2026-11-05');
  assert.deepEqual([empty.totalCents, empty.count, empty.dailyAverageCents, empty.busiest, empty.changePercent], [0, 0, 0, null, null]);
  const first = monthGlance([e('2026-11-02', 500, 'OTHER')], '2026-11', '2026-11-05');
  assert.equal(first.changePercent, null);
  // Ties for the busiest day go to the earlier date; half a cent rounds up.
  const tie = monthGlance([e('2026-02-09', 300, 'OTHER'), e('2026-02-04', 300, 'OTHER'), e('2026-02-28', 14, 'OTHER')], '2026-02', '2026-03-01');
  assert.equal(tie.busiest.date, '2026-02-04');
  assert.equal(tie.dailyAverageCents, 22); // 614 / 28 = 21.93
  // March 31st running month: February has only 28 days, so it is counted in full.
  assert.equal(monthGlance([], '2026-03', '2026-03-31').upToDay, 28);
});

test('Record expense and Add expense carry the same pen', () => {
  assert.equal(wordsFor(true).fab.split(' ')[0], '✒️');
  assert.equal(wordsFor(false).fab.split(' ')[0], '✒️');
  assert.equal(wordsFor(false).fab, '✒️ Add expense');
});

// --- Apparate: import (mirrors CsvImporterTest on Android) ---
import { importCsv, importSummary, parseDate } from '../js/csv.js';

let imp = 0;
const ids = () => `new${imp++}`;

test('a Geminio export comes back in exactly, and importing it again adds nothing', () => {
  const ledger = [
    e('2026-11-01', 45000, 'RENT', 100, null, 'Rent, Nov'),
    e('2026-11-22', 270, 'ENTERTAINMENT', 50, 'DAY_TO_DAY', 'Coffee "Freddo"'),
    e('2026-11-03', 6230, 'SUPERMARKET', 100, null, 'Lidl'),
  ];
  const csv = '﻿' + toCsv(ledger);
  const fresh = importCsv(csv, [], ids);
  assert.equal(fresh.rows, 3);
  assert.deepEqual(fresh.problems, []);
  const strip = (x) => ({ ...x, id: '' });
  assert.deepEqual(fresh.added.map(strip), [...ledger].sort((a, b) => a.date.localeCompare(b.date)).map(strip));
  const again = importCsv(csv, ledger, ids);
  assert.equal(again.added.length, 0);
  assert.equal(again.duplicates, 3);
});

test('identical rows are matched one for one', () => {
  const coffee = e('2026-11-05', 250, 'ENTERTAINMENT', 100, 'DAY_TO_DAY', 'Coffee');
  const csv = toCsv([coffee, { ...coffee, id: '2' }, { ...coffee, id: '3' }]);
  const r = importCsv(csv, [coffee], ids);
  assert.equal(r.duplicates, 1);
  assert.equal(r.added.length, 2);
  assert.ok(r.added.every((x) => x.id.startsWith('new')));
});

test('spreadsheet CSVs work too: semicolons, day-first dates, Greek amounts, names in any case', () => {
  const csv = '﻿Date;Title;Category;Food type;Amount EUR;My share percent\r\n'
    + '22/11/2026;Souvlaki;entertainment;delivery;"1.234,56";\r\n'
    + '3.11.2026;Diesel;FUEL;;60;50\r\n\r\n';
  const r = importCsv(csv, [], ids);
  assert.deepEqual(r.problems, []);
  assert.equal(r.rows, 2);
  const [souvlaki, diesel] = r.added;
  assert.deepEqual([souvlaki.date, souvlaki.foodType, souvlaki.amountCents, souvlaki.myPercent], ['2026-11-22', 'DELIVERY', 123456, 100]);
  assert.deepEqual([diesel.category, diesel.myPercent, diesel.date], ['FUEL', 50, '2026-11-03']);
});

test('unreadable rows are listed with their line and reason, the rest still come in', () => {
  const csv = [
    'date,title,category,food_type,amount_eur,my_share_percent',
    '2026-11-31,Ghost,Rent,,10,100',
    '2026-11-02,,Rent,,10,100',
    '2026-11-02,Wands,Potions,,10,100',
    '2026-11-02,Wine,Bills,Dine out,10,100',
    '2026-11-02,Socks,Shopping,,ten,100',
    '2026-11-02,Split,Other,,10,0',
    '2026-11-02,Good,Other,,10,',
  ].join('\n');
  const r = importCsv(csv, [], ids);
  assert.deepEqual(r.problems.map((p) => p.line), [2, 3, 4, 5, 6, 7]);
  assert.equal(r.problems[0].reason, '"2026-11-31" is not a date.');
  assert.equal(r.problems[3].reason, 'Only Entertainment can have a food type.');
  assert.deepEqual(r.added.map((x) => x.title), ['Good']);
  assert.equal(r.rows, 7);
});

test('a file without the needed columns is refused; dates must be real', () => {
  assert.equal(importCsv('name,price\nx,1\n', [], ids).problems[0].reason, 'Not an Expensiarmus CSV: no date, title, category, amount_eur column.');
  assert.equal(importCsv('', [], ids).problems[0].reason, 'The file is empty.');
  assert.equal(parseDate('2026-2-3'), '2026-02-03');
  assert.equal(parseDate('03/02/2026'), '2026-02-03');
  assert.equal(parseDate('2026-02-30'), null);
  assert.equal(parseDate('Feb 3'), null);
});

test('the confirmation sums it up and lists at most five problems, as on Android', () => {
  const problems = Array.from({ length: 7 }, (_, i) => ({ line: i + 2, reason: `bad ${i + 1}` }));
  assert.deepEqual(importSummary({ added: [{}], duplicates: 2, problems, rows: 10 }), [
    'Found 10 expenses in the file.', '1 new, 2 already in your ledger.', '7 rows could not be read:',
    'Line 2: bad 1', 'Line 3: bad 2', 'Line 4: bad 3', 'Line 5: bad 4', 'Line 6: bad 5', '…and 2 more.',
  ]);
  assert.deepEqual(importSummary({ added: [], duplicates: 1, problems: [], rows: 1 }), ['Found 1 expense in the file.', '0 new, 1 already in your ledger.']);
});

test('Settings has the Apparate card: a CSV picker, a confirmation, and nothing added before it', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /data-action="import"><span class="frame"><span class="frame-inner">\$\{esc\(W\.importButton\)\}/);
  assert.match(app, /<input type="file" id="import-file" accept="\.csv,text\/csv[^"]*" data-input="import-file" hidden>/);
  assert.match(app, /const ON_CHANGE = \['import-file',/);
  assert.match(app, /card-title">\$\{emblem\(EMBLEMS\.apparate, 36\)\}\$\{esc\(W\.importTitle\)\}/);
  // Reading the file only opens the confirmation; the expenses are committed on "Add".
  const read = app.slice(app.indexOf('async function readImport'), app.indexOf('function deleteModal'));
  assert.doesNotMatch(read, /commit\(/);
  assert.match(app, /'import-confirm': \(\) => \{[\s\S]*?commit\(\{ \.\.\.data, expenses: \[\.\.\.data\.expenses, \.\.\.added\] \}\)/);
  assert.equal(wordsFor(true).importButton, 'Apparate · import CSV');
  assert.equal(wordsFor(true).imported(12), 'Apparate! 12 expenses appeared in the ledger.');
  assert.equal(wordsFor(false).imported(1), '1 expense imported.');
});

test('Home was called Rent, and old backups still come back in', () => {
  assert.equal(CATEGORIES.find((c) => c.id === 'RENT').label, 'Home');
  assert.equal(CATEGORIES.find((c) => c.id === 'RENT').hint, 'Rent and shared building costs');
  const r = importCsv('date,title,category,amount_eur\n2026-10-01,October rent,Rent,450\n2026-11-01,Κοινόχρηστα,Home,35\n', [], ids);
  assert.deepEqual(r.problems, []);
  assert.deepEqual(r.added.map((x) => x.category), ['RENT', 'RENT']);
});

test('Evanesco is red by day and gold at night, like the other buttons', () => {
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  assert.match(css, /\.btn\.danger \.frame-inner \{ background-color: var\(--danger-bg\); color: var\(--on-danger\); \}/);
  const light = css.slice(css.indexOf(':root {'), css.indexOf('@media (prefers-color-scheme: dark)'));
  assert.match(light, /--danger-bg: var\(--error\);/);
  const media = css.slice(css.indexOf('@media (prefers-color-scheme: dark)'), css.indexOf(":root[data-theme='dark']"));
  const forced = css.slice(css.indexOf(":root[data-theme='dark']"));
  for (const dark of [media, forced.slice(0, forced.indexOf('}'))]) {
    assert.match(dark, /--danger-bg: var\(--primary\);/);
    assert.match(dark, /--on-danger: var\(--on-primary\);/);
  }
});

// --- category pages follow where they were opened from ---
import { inPeriod } from '../js/queries.js';

test('a category page lists the month, the year or all time it was opened for', () => {
  const list = [
    e('2026-11-03', 100, 'OTHER'), e('2026-11-28', 200, 'OTHER'), e('2026-10-30', 400, 'OTHER'),
    e('2025-11-03', 800, 'OTHER'), e('2026-01-01', 1600, 'RENT'),
  ];
  assert.equal(inPeriod(list, '').length, 5);
  assert.deepEqual(inPeriod(list, '2026-11').map((x) => x.amountCents), [100, 200]);
  assert.deepEqual(inPeriod(list, '2026').map((x) => x.amountCents), [100, 200, 400, 1600]);
  assert.deepEqual(inPeriod(list, '2025').map((x) => x.amountCents), [800]);
  // November of this year only, grouped by month: one group.
  assert.deepEqual(byCategory(inPeriod(list, '2026-11'), 'OTHER').map((g) => [g.month, g.totalCents]), [['2026-11', 300]]);
  // The year: its months only, newest first.
  assert.deepEqual(byCategory(inPeriod(list, '2026'), 'OTHER').map((g) => g.month), ['2026-11', '2026-10']);
});

test('the Ledger opens categories for its month, Annals for the year or all time', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /\$\{breakdownCard\(s, ui\.month\)\}\n\s*\$\{s\.food\.totalCents > 0 \? foodCard\(s\.food, ui\.month\)/);
  assert.match(app, /breakdownCard\(s, a\.view === 'year' \? String\(a\.year\) : ''\)/);
  assert.match(app, /foodCard\(s\.food, a\.view === 'year' \? String\(a\.year\) : ''\)/);
  assert.match(app, /data-cat="\$\{cat\.id\}" data-period="\$\{period\}"/);
  assert.match(app, /categoryScreen\(r\.a, r\.b, r\.c \?\? ''\)/);
  assert.match(app, /'cat-food': \(el\) => location\.replace\(categoryPath\(route\(\)\.a, el\.dataset\.food, route\(\)\.c\)\)/);
  assert.match(app, /byCategory\(inPeriod\(data\.expenses, period\), cat\.id, foodId\)/);
});

test('Transport was called Fuel, and old backups still come back in', () => {
  const fuel = CATEGORIES.find((c) => c.id === 'FUEL');
  assert.equal(fuel.label, 'Transport');
  assert.equal(fuel.nickname, 'Floo Powder');
  assert.equal(fuel.hint, 'Fuel, tickets (plane, train, metro, bus), taxis, parking');
  const r = importCsv('date,title,category,amount_eur\n2026-10-01,Diesel,Fuel,60\n2026-11-02,Metro card,Transport,30\n', [], ids);
  assert.deepEqual(r.problems, []);
  assert.deepEqual(r.added.map((x) => x.category), ['FUEL', 'FUEL']);
});

// --- whole-month ward and the way back to this month ---
import { WHOLE_MONTH, goalKey } from '../js/model.js';

test('a whole-month ward watches everything spent, next to the category wards', () => {
  let goals = setGoal({ entries: [] }, null, '2026-11', 100000, true);
  goals = setGoal(goals, 'ENTERTAINMENT', '2026-11', 20000, false);
  assert.equal(goalFor(goals, null, '2026-11').cents, 100000);
  assert.equal(goalFor(goals, null, '2027-02').cents, 100000); // onwards
  assert.equal(goalFor(goals, null, '2026-10'), null);
  assert.equal(goalFor(goals, 'ENTERTAINMENT', '2026-11').cents, 20000);
  assert.equal(goalFor(goals, 'RENT', '2026-11'), null); // a whole-month ward is not a category ward
  const data = { expenses: [e('2026-11-01', 45000, 'RENT'), e('2026-11-05', 40000, 'ENTERTAINMENT')], goals, alertsSent: [] };
  const s = monthlySummary(data, '2026-11');
  assert.deepEqual([s.totalGoal.goalCents, s.totalGoal.spentCents, s.totalGoal.percent], [100000, 85000, 85]);
  assert.equal(monthlySummary({ ...data, goals: { entries: [] } }, '2026-11').totalGoal, null);
  // Removing it for November only switches the onwards ward off there, December keeps it.
  const off = removeGoal(goals, null, '2026-11', false);
  assert.equal(goalFor(off, null, '2026-11'), null);
  assert.equal(goalFor(off, null, '2026-12').cents, 100000);
  // Saved whole-month wards survive loading.
  assert.equal(migrateGoals(goals).entries.filter((x) => x.category === null).length, 1);
  assert.equal(goalKey('TOTAL'), null);
  assert.equal(goalKey('RENT'), 'RENT');
  assert.equal(WHOLE_MONTH.id, 'TOTAL');
});

test('the whole-month ward warns at 80% and when broken, once each', () => {
  const goals = setGoal({ entries: [] }, null, '2026-11', 10000, false);
  const at = (cents, sent = []) => alertsDue({ expenses: [e('2026-11-02', cents, 'OTHER')], goals, alertsSent: sent }, '2026-11');
  assert.deepEqual(at(7000), []);
  const eighty = at(8500);
  assert.equal(eighty.length, 1);
  assert.equal(eighty[0].category, null);
  assert.equal(eighty[0].title, wordsFor(true).alert80('monthly'));
  assert.match(eighty[0].body, /€85\.00 of €100\.00 in November/);
  assert.equal(at(8500, ['TOTAL|2026-11|80']).length, 0);
  const broken = at(12000, ['TOTAL|2026-11|80']);
  assert.equal(broken[0].level, 100);
  assert.equal(broken[0].title, wordsFor(true).alert100('monthly'));
  assert.deepEqual(markAlertsSent({ alertsSent: [] }, broken).alertsSent.sort(), ['TOTAL|2026-11|100', 'TOTAL|2026-11|80']);
});

test('the Ledger shows the whole-month ward under its total, and a way back to this month', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /\$\{s\.totalGoal \? `\$\{goalBar\(s\.totalGoal\)\}/);
  assert.match(app, /if \(ui\.month === thisMonth\) return '';/);
  assert.doesNotMatch(app, /class="link this-month"/);
  assert.match(app, /'this-month': \(\) => \{ ui\.month = today\(\)\.slice\(0, 7\);/);
  assert.equal(wordsFor(true).thisMonth('November 2026'), 'Back to November 2026');
  assert.match(app, /const vaults = \[whole, \.\.\.CATEGORIES\]/);
  assert.match(app, /setGoal\(data\.goals, goalKey\(cat\)/);
  assert.match(app, /removeGoal\(data\.goals, goalKey\(cat\)/);
});

test('the Time-Turner sits in the top bar, bronze by day and gold at night, without a badge', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  const sw = readFileSync(webFile('sw.js'), 'utf8');
  assert.match(app, /const actions = `\$\{timeTurner\(\)\}<button class="icon-btn" data-action="go" data-to="#\/search"/);
  assert.match(app, /header\(`\$\{W\.goals\} · \$\{name\}`, \{ icon: TAB_ICONS\.goals, actions: timeTurner\(\) \}\)/);
  assert.match(app, /data-action="this-month" aria-label="\$\{label\}"[^>]*><span class="today-glyph"><\/span>/);
  assert.equal(css.match(/--today-glyph: url\('\.\.\/icons\/emblems\/today_light\.png'\)/g).length, 1);
  assert.equal(css.match(/--today-glyph: url\('\.\.\/icons\/emblems\/today_dark\.png'\)/g).length, 2);
  for (const f of ['today_light.png', 'today_dark.png']) {
    assert.ok(existsSync(webFile(`icons/emblems/${f}`)));
    assert.ok(sw.includes(`'icons/emblems/${f}'`));
  }
});

test('Annals by year has its own Time-Turner back to the current year', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /header\(W\.annals, \{ icon: TAB_ICONS\.annals, actions: yearTurner\(\) \}\)/);
  assert.match(app, /if \(ui\.annals\.view !== 'year' \|\| ui\.annals\.year === thisYear\) return '';/);
  assert.match(app, /'this-year': \(\) => \{ ui\.annals = \{ \.\.\.ui\.annals, year: Number\(today\(\)\.slice\(0, 4\)\)/);
  assert.equal(wordsFor(true).thisMonth('2026'), 'Back to 2026');
});

// --- favourites, and suggestions that only fill the title ---
import { favoriteOf, favoriteKey, isFavorite, toggleFavorite, MAX_FAVORITES } from '../js/queries.js';

test('a favourite keeps the title, category, food type and split, never the amount', () => {
  const coffee = e('2026-11-05', 270, 'ENTERTAINMENT', 50, 'DAY_TO_DAY', ' Freddo espresso ');
  assert.deepEqual(favoriteOf(coffee), { title: 'Freddo espresso', category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY', myPercent: 50 });
  let favs = toggleFavorite([], coffee);
  assert.equal(favs.length, 1);
  assert.ok(isFavorite(favs, { ...coffee, title: 'FREDDO ESPRESSO', amountCents: 999 })); // case and amount don't matter
  assert.ok(!isFavorite(favs, { ...coffee, foodType: 'DINE_OUT' })); // another food type is another favourite
  favs = toggleFavorite(favs, e('2026-11-01', 45000, 'RENT', 100, null, 'Apartment'));
  assert.deepEqual(favs.map((f) => f.title), ['Apartment', 'Freddo espresso']); // sorted by title
  favs = toggleFavorite(favs, coffee); // tapping the star again removes it
  assert.deepEqual(favs.map((f) => f.title), ['Apartment']);
  assert.equal(favoriteKey({ title: 'Καφές', category: 'OTHER' }), favoriteKey({ title: 'καφες', category: 'OTHER', foodType: null }));
});

test('favourites are saved and loaded; broken ones are dropped', () => {
  const mem = new Map();
  const storage = { getItem: (k) => mem.get(k) ?? null, setItem: (k, v) => mem.set(k, v) };
  const favorites = [{ title: 'Freddo', category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY', myPercent: 100 },
    { title: 'Ghost', category: 'POTIONS', foodType: null, myPercent: 100 }, { title: 'Wine', category: 'BILLS', foodType: 'DINE_OUT' },
    { title: '  ', category: 'OTHER' }, { title: 'Old', category: 'OTHER' }];
  save(storage, { ...emptyData(), favorites });
  assert.deepEqual(load(storage).favorites, [
    { title: 'Freddo', category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY', myPercent: 100 },
    { title: 'Old', category: 'OTHER', foodType: null, myPercent: 100 },
  ]);
  assert.deepEqual(emptyData().favorites, []);
});

test('there can be at most five favourites', () => {
  assert.equal(MAX_FAVORITES, 5);
  let favs = [];
  for (const t of ['A', 'B', 'C', 'D', 'E']) favs = toggleFavorite(favs, e('2026-11-01', 100, 'OTHER', 100, null, t));
  assert.equal(favs.length, 5);
  const sixth = e('2026-11-01', 100, 'OTHER', 100, null, 'F');
  assert.equal(toggleFavorite(favs, sixth), favs); // full: nothing changes
  const fewer = toggleFavorite(favs, e('2026-11-01', 100, 'OTHER', 100, null, 'C'));
  assert.equal(toggleFavorite(fewer, sixth).length, 5); // after removing one, there is room again
});

test('no title suggestions when recording; favourites live in Quick add and on the expense view', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  // You type a title or pick a favourite: earlier titles are not suggested in Quick add or the form.
  assert.doesNotMatch(app, /suggestionList|quick-sugg|edit-sugg|title-sugg|suggestions\(/);
  assert.doesNotMatch(app, /function applySuggestion/);
  assert.match(app, /\$\{favStar\(e\)\}<\/div>/);
  assert.match(app, /\$\{favRow\(q\)\}/);
  assert.match(app, /prefill = \{ title: q\.title, amount: q\.amount, date: q\.date, category: q\.cat, foodType: q\.food, myPercent: q\.pct \}/);
});

test('Quick add is compact: smaller amount, keys and tiles', () => {
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(css, /\.quick-amount \{[^}]*font-size: 32px/);
  assert.match(css, /\.numpad button \{[^}]*height: 40px/);
  assert.match(css, /\.sheet \{[^}]*gap: 7px/);
  assert.match(app, /\$\{emblem\(t\.icon, 32\)\}<span>/);
});

test('the favourite star is a drawn glyph like the others, bronze by day and gold at night', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  const sw = readFileSync(webFile('sw.js'), 'utf8');
  assert.doesNotMatch(app, /[★☆]/); // no text stars
  for (const kind of ['on', 'off']) {
    assert.equal(css.match(new RegExp(`--star-${kind}: url\\('\\.\\./icons/emblems/star_${kind}_light\\.png'\\)`, 'g')).length, 1);
    assert.equal(css.match(new RegExp(`--star-${kind}: url\\('\\.\\./icons/emblems/star_${kind}_dark\\.png'\\)`, 'g')).length, 2);
    for (const theme of ['light', 'dark']) {
      assert.ok(existsSync(webFile(`icons/emblems/star_${kind}_${theme}.png`)));
      assert.ok(sw.includes(`'icons/emblems/star_${kind}_${theme}.png'`));
    }
  }
});

import { saveFavorite, removeFavoriteAt } from '../js/queries.js';

test('the Favourites page edits, adds and removes favourites with the same rules as Android', () => {
  const coffee = { title: 'Freddo', category: 'ENTERTAINMENT', foodType: 'DAY_TO_DAY', myPercent: 100 };
  const rent = { title: 'Apartment', category: 'RENT', foodType: null, myPercent: 100 };
  let r = saveFavorite([], null, coffee);
  assert.deepEqual(r.list, [coffee]);
  r = saveFavorite(r.list, null, rent);
  assert.deepEqual(r.list.map((f) => f.title), ['Apartment', 'Freddo']);
  // Editing: change the title, split and category; a food type only stays under Entertainment.
  r = saveFavorite(r.list, 1, { title: ' Espresso ', category: 'OTHER', foodType: 'DAY_TO_DAY', myPercent: 50 });
  assert.deepEqual(r.list[1], { title: 'Espresso', category: 'OTHER', foodType: null, myPercent: 50 });
  assert.equal(saveFavorite(r.list, 0, rent).list.length, 2); // saving unchanged is fine
  assert.equal(saveFavorite(r.list, 0, { ...rent, title: ' ' }).error, 'Give it a title.');
  assert.equal(saveFavorite(r.list, 0, { ...rent, myPercent: 0 }).error, 'Your share must be between 1 and 100%.');
  assert.equal(saveFavorite(r.list, null, { ...rent, title: 'APARTMENT' }).error, 'You already have this favourite.');
  let five = [];
  for (const t of ['A', 'B', 'C', 'D', 'E']) five = saveFavorite(five, null, { ...rent, title: t }).list;
  assert.equal(saveFavorite(five, null, { ...rent, title: 'F' }).error, 'You can keep up to 5 favourites.');
  assert.equal(saveFavorite(five, 2, { ...rent, title: 'F' }).list.length, 5); // editing one of five is fine
  assert.deepEqual(removeFavoriteAt(five, 0).map((f) => f.title), ['B', 'C', 'D', 'E']);
});

test('Quick add records with the ✓ at the top; tiles and favourites only select', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  const css = readFileSync(webFile('css/style.css'), 'utf8');
  const sw = readFileSync(webFile('sw.js'), 'utf8');
  const sheet = app.slice(app.indexOf('function quickSheet()'), app.indexOf('function quickKey('));
  assert.match(sheet, /<div class="sheet-head">[\s\S]*data-action="quick-record"[^>]*><span class="check-glyph"><\/span><\/button>\s*<\/div>/);
  assert.match(sheet, /data-action="quick-pick"/);
  assert.doesNotMatch(app, /data-action="quick-save"|'quick-save'|fav-record/);
  const pick = app.slice(app.indexOf("  'quick-pick':"), app.indexOf("  'quick-record':"));
  assert.doesNotMatch(pick, /quickRecord|commit/); // a tile only selects
  const record = app.slice(app.indexOf("  'quick-record':"), app.indexOf("  'fav-pick':"));
  assert.match(record, /'Type an amount first' : !q\.cat \? 'Pick a category first'/);
  assert.match(record, /category: q\.cat, foodType: q\.food, myPercent: q\.pct \?\? 100/);
  assert.equal(css.match(/--check-glyph: url\('\.\.\/icons\/emblems\/check_light\.png'\)/g).length, 1);
  assert.equal(css.match(/--check-glyph: url\('\.\.\/icons\/emblems\/check_dark\.png'\)/g).length, 2);
  for (const v of ['light', 'dark']) assert.ok(existsSync(webFile(`icons/emblems/check_${v}.png`)) && sw.includes(`'icons/emblems/check_${v}.png'`));
});

test('favourites have their own page, a Settings card, and show in the full form for new expenses', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  assert.match(app, /case 'favorites': app\.innerHTML = favoritesScreen\(\); break;/);
  assert.doesNotMatch(app, /fav-page/); // not editable from Quick add
  assert.match(app, /data-action="go" data-to="#\/favorites"/);
  assert.match(app, /const r = saveFavorite\(data\.favorites, ed\.index, ed\);/);
  assert.match(app, /\$\{!d\.id && data\.favorites\?\.length \?/); // only for a new expense
  assert.match(app, /'edit-fav': \(el\) => \{[\s\S]*?category: f\.category, foodType: f\.foodType \?\? null, split:/);
});

test('Settings cards come in the chosen order: Favourites, Owl reminder, Wizard words, Number format, Theme, Geminio, Apparate', () => {
  const app = readFileSync(webFile('js/app.js'), 'utf8');
  const screen = app.slice(app.indexOf('function settingsScreen()'), app.indexOf('// ---------- annals'));
  const at = ['Favourites</h3>', 'EMBLEMS.reminder', 'Wizard words</h3>', 'Number format</h3>', 'Theme</h3>', 'EMBLEMS.geminio', 'EMBLEMS.apparate'].map((m) => screen.indexOf(m));
  assert.ok(at.every((i) => i > 0), 'every card is there');
  assert.deepEqual([...at].sort((a, b) => a - b), at);
});
