import { CATEGORIES, category, foodType, monthOf, myShareCents } from './model.js';
import { byNewest } from './summary.js';

const pad = (n) => String(n).padStart(2, '0');
export const daysInMonth = (month) => {
  const [y, m] = month.split('-').map(Number);
  return new Date(Date.UTC(y, m, 0)).getUTCDate();
};
export const shiftMonth = (month, delta) => {
  const [y, m] = month.split('-').map(Number);
  const d = new Date(Date.UTC(y, m - 1 + delta, 1));
  return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}`;
};

/** My share spent on each day of the month that has expenses: { "2026-11-03": 4250 }. */
export function dailyTotals(expenses, month) {
  const totals = {};
  for (const e of expenses) {
    if (monthOf(e.date) === month) totals[e.date] = (totals[e.date] ?? 0) + myShareCents(e);
  }
  return totals;
}

export const onDay = (expenses, day) => expenses.filter((e) => e.date === day).sort(byNewest);

/** Calendar cells for the month in whole weeks starting on Monday; null pads before and after. */
export function calendarCells(month) {
  const [y, m] = month.split('-').map(Number);
  const firstDow = new Date(Date.UTC(y, m - 1, 1)).getUTCDay(); // 0 = Sunday
  const lead = (firstDow + 6) % 7;
  const days = Array.from({ length: daysInMonth(month) }, (_, i) => `${month}-${pad(i + 1)}`);
  const cells = [...Array(lead).fill(null), ...days];
  return [...cells, ...Array((7 - (cells.length % 7)) % 7).fill(null)];
}

/** All expenses of a category grouped by month, newest month first; optionally one food type. */
export function byCategory(expenses, categoryId, foodTypeId = null) {
  const groups = new Map();
  for (const e of expenses) {
    if (e.category !== categoryId || (foodTypeId && e.foodType !== foodTypeId)) continue;
    const m = monthOf(e.date);
    if (!groups.has(m)) groups.set(m, []);
    groups.get(m).push(e);
  }
  return [...groups.entries()]
    .sort((a, b) => (a[0] < b[0] ? 1 : -1))
    .map(([month, list]) => ({ month, totalCents: list.reduce((s, e) => s + myShareCents(e), 0), expenses: list.sort(byNewest) }));
}

/** Lower case without accents, so "Σουβλάκι" matches "σουβλακι" and "Café" matches "cafe". */
export const normalize = (text) =>
  String(text).normalize('NFD').replace(/\p{Mn}+/gu, '').toLowerCase().replace(/ς/g, 'σ').trim();

/**
 * Expenses whose title, category or food type contain every word of the query, limited to the
 * chosen categories when any are chosen. With no query and no categories the result is empty.
 */
export function search(expenses, query, categories = []) {
  const words = normalize(query).split(/\s+/).filter(Boolean);
  if (words.length === 0 && categories.length === 0) return [];
  return expenses
    .filter((e) => {
      if (categories.length && !categories.includes(e.category)) return false;
      const hay = [e.title, category(e.category).label, e.foodType ? foodType(e.foodType).label : ''].map(normalize);
      return words.every((w) => hay.some((h) => h.includes(w)));
    })
    .sort(byNewest);
}

/**
 * Earlier expenses whose title starts with (or has a word starting with) what was typed, most used
 * first. Each suggestion carries the latest amount, category, food type and split for that title.
 */
export function suggestions(expenses, typed, limit = 4) {
  const q = normalize(typed);
  if (!q) return [];
  const groups = new Map();
  expenses.forEach((e, i) => {
    const key = normalize(e.title);
    const g = groups.get(key) ?? { uses: 0, latest: null, at: -1 };
    g.uses += 1;
    if (!g.latest || e.date > g.latest.date || (e.date === g.latest.date && i > g.at)) {
      g.latest = e;
      g.at = i;
    }
    groups.set(key, g);
  });
  return [...groups.entries()]
    .filter(([key]) => key.startsWith(q) || key.split(/\s+/).some((w) => w.startsWith(q)))
    .sort(([ka, a], [kb, b]) => b.uses - a.uses || (a.latest.date < b.latest.date ? 1 : a.latest.date > b.latest.date ? -1 : ka.localeCompare(kb)))
    .slice(0, limit)
    .map(([, g]) => ({
      title: g.latest.title,
      amountCents: g.latest.amountCents,
      category: g.latest.category,
      foodType: g.latest.foodType ?? null,
      myPercent: g.latest.myPercent,
      uses: g.uses,
    }));
}

/** Title used when Quick add saves without one: the food type, or else the category. */
export const defaultTitle = (cat, food) => (food ? foodType(food).label : category(cat).label);

export { CATEGORIES };

/** How many of the month's expenses the Ledger lists before "Show all". */
export const LATEST_COUNT = 5;

/** The Ledger's list: the newest few, or all of them once "Show all" is tapped. Expects newest first. */
export const ledgerPreview = (expenses, showAll, n = LATEST_COUNT) => (showAll ? expenses : expenses.slice(0, n));

/**
 * The calendar's month at a glance: my-share total, number of expenses, average per day, the busiest day
 * and the change from the month before. While [month] is still running (it holds [today]) the average is
 * over the days so far, and the previous month is counted only up to the same day so the comparison is fair.
 */
export function monthGlance(expenses, month, today) {
  const mine = expenses.filter((e) => monthOf(e.date) === month);
  const totalCents = mine.reduce((s, e) => s + myShareCents(e), 0);
  const running = monthOf(today) === month;
  const daysCounted = running ? Number(today.slice(8, 10)) : daysInMonth(month);
  const dailyAverageCents = Math.floor((totalCents * 2 + daysCounted) / (daysCounted * 2)); // half up
  let busiest = null;
  for (const [date, cents] of Object.entries(dailyTotals(expenses, month))) {
    if (!busiest || cents > busiest.cents || (cents === busiest.cents && date < busiest.date)) busiest = { date, cents };
  }
  const previousMonth = shiftMonth(month, -1);
  const upToDay = running ? Math.min(daysCounted, daysInMonth(previousMonth)) : null;
  const previousCents = expenses
    .filter((e) => monthOf(e.date) === previousMonth && (upToDay == null || Number(e.date.slice(8, 10)) <= upToDay))
    .reduce((s, e) => s + myShareCents(e), 0);
  const changePercent = previousCents > 0 ? Math.round(((totalCents - previousCents) * 100) / previousCents) : null;
  return { totalCents, count: mine.length, daysCounted, dailyAverageCents, busiest, previousMonth, previousCents, upToDay, changePercent };
}
