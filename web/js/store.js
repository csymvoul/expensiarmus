import { emptyData, makeExpense } from './model.js';

const KEY = 'expensiarmus.data.v1';

/**
 * Reads the saved data. Anything unreadable is kept under a backup key instead of being lost,
 * and invalid expenses are skipped. `storage` is localStorage in the app and a fake in tests.
 */
export function load(storage) {
  const raw = storage.getItem(KEY);
  if (raw == null) return emptyData();
  try {
    const parsed = JSON.parse(raw);
    const expenses = [];
    for (const e of parsed.expenses ?? []) {
      try {
        expenses.push(makeExpense(e));
      } catch {
        // skip a broken entry rather than losing everything
      }
    }
    const g = parsed.goals ?? {};
    return {
      expenses,
      goals: { monthlyCents: g.monthlyCents ?? null, perCategoryCents: { ...(g.perCategoryCents ?? {}) } },
    };
  } catch {
    storage.setItem(`${KEY}.corrupt-${Date.now()}`, raw);
    return emptyData();
  }
}

export function save(storage, data) {
  storage.setItem(KEY, JSON.stringify(data));
}

export { KEY };
