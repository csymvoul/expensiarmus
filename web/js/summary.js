import { CATEGORIES, FOOD_TYPES, monthOf, myShareCents } from './model.js';
import { format } from './money.js';
import { goalFor } from './goals.js';
import { wordsFor } from './words.js';

const byNewest = (a, b) => (a.date === b.date ? a.title.toLowerCase().localeCompare(b.title.toLowerCase()) : a.date < b.date ? 1 : -1);
export { byNewest };

export function goalProgress(goalCents, spentCents) {
  const percent = goalCents > 0 ? Math.floor((spentCents * 100 + Math.floor(goalCents / 2)) / goalCents) : 0;
  return {
    goalCents,
    spentCents,
    fraction: goalCents > 0 ? spentCents / goalCents : 0,
    percent,
    remainingCents: goalCents - spentCents,
    isOver: spentCents > goalCents,
  };
}

/** "63% of €1,200.00 · €444.00 left", or the overspend with a broken ward. */
export function describeGoal(g, words = wordsFor(true)) {
  const head = `${g.percent}% of ${format(g.goalCents)}`;
  return g.isOver
    ? `${head} · ${format(-g.remainingCents)} over. ${words.overGoal}`
    : `${head} · ${format(g.remainingCents)} left`;
}

/** Food spending inside Entertainment. */
export function foodSummary(expenses) {
  const ent = expenses.filter((e) => e.category === 'ENTERTAINMENT');
  const food = ent.filter((e) => e.foodType);
  const total = food.reduce((s, e) => s + myShareCents(e), 0);
  return {
    totalCents: total,
    types: FOOD_TYPES.map((t) => {
      const of = food.filter((e) => e.foodType === t.id);
      const spent = of.reduce((s, e) => s + myShareCents(e), 0);
      return { type: t.id, spentCents: spent, count: of.length, shareOfFood: total > 0 ? spent / total : 0 };
    }),
    otherEntertainmentCents: ent.filter((e) => !e.foodType).reduce((s, e) => s + myShareCents(e), 0),
  };
}

/** Totals for one month ("2026-11"). All amounts are my share of each expense. */
export function monthlySummary(data, month) {
  const inMonth = data.expenses.filter((e) => monthOf(e.date) === month).sort(byNewest);
  const total = inMonth.reduce((s, e) => s + myShareCents(e), 0);
  const goals = data.goals ?? {};
  return {
    month,
    totalCents: total,
    paidTotalCents: inMonth.reduce((s, e) => s + e.amountCents, 0),
    expenses: inMonth,
    categories: CATEGORIES.map((c) => {
      const spent = inMonth.filter((e) => e.category === c.id).reduce((s, e) => s + myShareCents(e), 0);
      const entry = goalFor(goals, c.id, month);
      return {
        category: c.id,
        spentCents: spent,
        shareOfMonth: total > 0 ? spent / total : 0,
        goal: entry ? goalProgress(entry.cents, spent) : null,
      };
    }),
    food: foodSummary(inMonth),
  };
}

const MONTH_LETTERS = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D'];
const ym = (date) => date.slice(0, 7);
const year = (date) => Number(date.slice(0, 4));
/** Months from start to end inclusive ("2025-03" .. "2026-11" is 21); 0 when end is before start. */
export function monthsBetween(start, end) {
  const [sy, sm] = start.split('-').map(Number);
  const [ey, em] = end.split('-').map(Number);
  const n = (ey - sy) * 12 + (em - sm) + 1;
  return n > 0 ? n : 0;
}
const maxStr = (...xs) => xs.filter(Boolean).sort().at(-1);
const minStr = (...xs) => xs.filter(Boolean).sort()[0];

function periodBuild(expenses, monthsCounted, bars, previousTotalCents) {
  const total = expenses.reduce((s, e) => s + myShareCents(e), 0);
  let biggest = null;
  for (const e of expenses) {
    if (!biggest || myShareCents(e) > myShareCents(biggest) || (myShareCents(e) === myShareCents(biggest) && e.date > biggest.date)) biggest = e;
  }
  return {
    totalCents: total,
    paidTotalCents: expenses.reduce((s, e) => s + e.amountCents, 0),
    expenseCount: expenses.length,
    monthsCounted,
    averagePerMonthCents: monthsCounted > 0 ? Math.floor((total + Math.floor(monthsCounted / 2)) / monthsCounted) : 0,
    bars,
    categories: CATEGORIES.map((c) => {
      const spent = expenses.filter((e) => e.category === c.id).reduce((s, e) => s + myShareCents(e), 0);
      return { category: c.id, spentCents: spent, shareOfMonth: total > 0 ? spent / total : 0, goal: null };
    }),
    food: foodSummary(expenses),
    biggest,
    previousTotalCents,
    changePercent: previousTotalCents > 0 ? Math.round(((total - previousTotalCents) * 100) / previousTotalCents) : null,
  };
}

/**
 * One calendar year. The average counts the months from January (or the first recorded month)
 * up to the current month (December for past years). The current year is compared with the same
 * months of the previous year. `today` is "YYYY-MM-DD".
 */
export function yearSummary(data, y, today) {
  const inYear = data.expenses.filter((e) => year(e.date) === y);
  const first = data.expenses.length ? minStr(...data.expenses.map((e) => ym(e.date))) : null;
  const lastInYear = inYear.length ? maxStr(...inYear.map((e) => ym(e.date))) : null;
  const start = maxStr(`${y}-01`, first);
  const end = maxStr(minStr(`${y}-12`, ym(today)), lastInYear);
  const through = y === year(today) ? Number(today.slice(5, 7)) : 12;
  const hasPrevious = data.expenses.some((e) => year(e.date) === y - 1);
  const previous = data.expenses
    .filter((e) => year(e.date) === y - 1 && Number(e.date.slice(5, 7)) <= through)
    .reduce((s, e) => s + myShareCents(e), 0);
  const bars = MONTH_LETTERS.map((label, i) => {
    const month = `${y}-${String(i + 1).padStart(2, '0')}`;
    return { label, month, cents: inYear.filter((e) => ym(e.date) === month).reduce((s, e) => s + myShareCents(e), 0) };
  });
  return periodBuild(inYear, first ? monthsBetween(start, end) : 0, bars, hasPrevious ? previous : null);
}

/** Everything recorded, with one bar per year from the first year to this year. */
export function allTimeSummary(data, today) {
  if (!data.expenses.length) return periodBuild([], 0, [], null);
  const first = minStr(...data.expenses.map((e) => ym(e.date)));
  const last = maxStr(...data.expenses.map((e) => ym(e.date)));
  const years = [];
  for (let y = year(first); y <= Math.max(year(today), year(last)); y++) years.push(y);
  const bars = years.map((y) => ({
    label: years.length > 5 ? `'${String(y % 100).padStart(2, '0')}` : String(y),
    year: y,
    cents: data.expenses.filter((e) => year(e.date) === y).reduce((s, e) => s + myShareCents(e), 0),
  }));
  return periodBuild(data.expenses, monthsBetween(first, maxStr(ym(today), last)), bars, null);
}
