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

export { CATEGORIES };
