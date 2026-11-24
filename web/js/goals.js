// Per-month goals ("wards") for a category, or for the whole month (category null), and the
// 80% / over-the-goal warnings. Same rules as the
// Android app (model/Expense.kt Goals, domain/GoalAlerts.kt).
import { CATEGORIES, category } from './model.js';
import { format } from './money.js';
import { monthlySummary } from './summary.js';
import { wordsFor } from './words.js';

/** Start month given to converted pre-1.5 goals, so they keep covering past months too. */
export const LEGACY_START = '2000-01';

const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const monthName = (m) => MONTHS[Number(m.slice(5, 7)) - 1];
const monthYear = (m) => `${monthName(m)} ${m.slice(0, 4)}`;

/**
 * The goal that applies to a category in a month ("2026-11"), or null. A category of null is the
 * whole month: a goal on everything spent that month. A goal for one specific
 * month wins over an "onwards" goal; among "onwards" goals the latest that started by then wins.
 * An entry of 0 cents switches an earlier goal off.
 */
export function goalFor(goals, cat, month) {
  const entries = goals?.entries ?? [];
  const single = entries.find((e) => e.category === cat && !e.onwards && e.month === month);
  const effective = single ?? entries
    .filter((e) => e.category === cat && e.onwards && e.month <= month)
    .sort((a, b) => (a.month < b.month ? 1 : -1))[0];
  return effective && effective.cents > 0 ? effective : null;
}

/** Sets a goal for one month, or from that month onwards. */
export function setGoal(goals, cat, month, cents, onwards) {
  const kept = (goals.entries ?? []).filter((e) =>
    !(e.category === cat && ((!e.onwards && e.month === month) || (onwards && e.onwards && e.month >= month))));
  return { entries: [...kept, { category: cat, month, cents, onwards }] };
}

/** Removes a goal for one month, or from that month onwards. */
export function removeGoal(goals, cat, month, onwards) {
  let kept = (goals.entries ?? []).filter((e) => !(e.category === cat && !e.onwards && e.month === month));
  if (onwards) kept = kept.filter((e) => !(e.category === cat && e.month >= month));
  // An earlier "onwards" goal may still reach this month: switch it off explicitly.
  if (goalFor({ entries: kept }, cat, month)) kept = [...kept, { category: cat, month, cents: 0, onwards }];
  return { entries: kept };
}

/**
 * Converts pre-1.5 goals ({ monthlyCents, perCategoryCents }): the Entertainment goal becomes an
 * "every month" goal; the monthly total and other categories are dropped.
 */
export function migrateGoals(goals) {
  if (!goals) return { entries: [] };
  const entries = Array.isArray(goals.entries) ? goals.entries.filter((e) => (e.category === null || category(e.category)) && e.month) : [];
  const legacy = goals.perCategoryCents?.ENTERTAINMENT;
  if (legacy > 0 && !entries.length) entries.push({ category: 'ENTERTAINMENT', month: LEGACY_START, cents: legacy, onwards: true });
  return { entries };
}

/** How far a goal reaches, as shown on the Protego screen. */
export function scopeText(entry, month) {
  if (!entry.onwards) return `Only ${monthYear(month)}`;
  if (entry.month === LEGACY_START) return 'Every month';
  if (entry.month === month) return `From ${monthYear(month)} onwards`;
  return `Every month since ${monthYear(entry.month)}`;
}

export const alertKey = (cat, month, level) => `${cat ?? 'TOTAL'}|${month}|${level}`;

/** Warnings not yet sent for a month: at 80% of a goal, and on reaching it (100%, which wins). */
export function alertsDue(data, month, words = wordsFor(true)) {
  const sent = new Set(data.alertsSent ?? []);
  const s = monthlySummary(data, month);
  const watched = [{ category: null, goal: s.totalGoal }, ...s.categories];
  return watched.flatMap((c) => {
    const g = c.goal;
    if (!g) return [];
    const level = g.spentCents >= g.goalCents ? 100 : g.spentCents * 100 >= g.goalCents * 80 ? 80 : 0;
    if (!level || sent.has(alertKey(c.category, month, level))) return [];
    return [alertMessage(c.category, month, level, g, words)];
  });
}

export function alertMessage(cat, month, level, g, words = wordsFor(true)) {
  const label = cat ? category(cat).label : 'monthly'; // "80% of your monthly ward is spent"
  const head = `${format(g.spentCents)} of ${format(g.goalCents)} in ${monthName(month)}`;
  return {
    category: cat,
    month,
    level,
    title: level >= 100 ? words.alert100(label) : words.alert80(label),
    body: g.spentCents > g.goalCents
      ? `${head} · ${format(g.spentCents - g.goalCents)} over.`
      : level >= 100 ? `${head} · nothing left.` : `${head} · ${format(g.goalCents - g.spentCents)} left. Time to slow down.`,
  };
}

/** Records warnings as sent. Reaching 100% also counts as the 80% warning. */
export function markAlertsSent(data, alerts) {
  const keys = new Set(data.alertsSent ?? []);
  for (const a of alerts) for (const l of a.level >= 100 ? [80, 100] : [80]) keys.add(alertKey(a.category, a.month, l));
  return { ...data, alertsSent: [...keys] };
}

export { CATEGORIES };
