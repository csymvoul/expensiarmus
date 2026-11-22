import { CATEGORIES, FOOD_TYPES, monthOf, myShareCents } from './model.js';
import { format } from './money.js';

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
export function describeGoal(g) {
  const head = `${g.percent}% of ${format(g.goalCents)}`;
  return g.isOver
    ? `${head} · ${format(-g.remainingCents)} over. The Protego ward is broken!`
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
      const goal = goals.perCategoryCents?.[c.id];
      return {
        category: c.id,
        spentCents: spent,
        shareOfMonth: total > 0 ? spent / total : 0,
        goal: goal > 0 ? goalProgress(goal, spent) : null,
      };
    }),
    overall: goals.monthlyCents > 0 ? goalProgress(goals.monthlyCents, total) : null,
    food: foodSummary(inMonth),
  };
}
