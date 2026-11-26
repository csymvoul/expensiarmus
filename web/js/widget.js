// What the iPhone home-screen widget shows, worked out here (same rules as Android's WidgetContent)
// and handed to the Swift shell, which saves it where the widget can read it.
import { category } from './model.js';
import { format } from './money.js';
import { monthlySummary } from './summary.js';
import { tripsToOffer, tripSummary } from './trips.js';

const MONTH = (month) => new Date(`${month}-01T00:00:00Z`).toLocaleDateString('en-GB', { month: 'long', timeZone: 'UTC' });

/**
 * This month's total, up to three wards (the whole-month ward first, then the category wards closest
 * to or furthest over their goal) and the trips not over yet. The widget works out which trip is on
 * each day itself, so its day count stays right while the app is closed.
 */
export function widgetContent(data, month, words, today) {
  const s = monthlySummary(data, month);
  const line = (name, g) => ({ text: `${name} · ${format(g.spentCents)} of ${format(g.goalCents)}`, percent: Math.min(100, Math.max(0, g.percent)), over: g.isOver });
  const whole = s.totalGoal ? [line(`✦ ${words.wholeMonth}`, s.totalGoal)] : [];
  const wards = [...whole, ...s.categories
    .filter((c) => c.goal)
    .sort((a, b) => b.goal.fraction - a.goal.fraction)
    .slice(0, 3 - whole.length)
    .map((c) => line(`${category(c.category).emoji} ${category(c.category).label}`, c.goal))];
  return {
    month,
    heading: `Expensiarmus · ${MONTH(month)}`,
    total: format(s.totalCents),
    label: words.spentThisMonth,
    wards,
    empty: wards.length ? null : words.noGoals,
    trips: tripsToOffer(data.trips, today).slice(0, 5).map((t) => {
      const sum = tripSummary(data, t.id, today);
      return { name: t.name, start: t.start, end: t.end, spent: format(sum.totalCents), budget: t.budgetCents ? format(t.budgetCents) : null };
    }),
  };
}
