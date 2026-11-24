import { emptyData, makeExpense, category, foodType } from './model.js';
import { migrateGoals } from './goals.js';

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
    return {
      expenses,
      goals: migrateGoals(parsed.goals),
      alertsSent: Array.isArray(parsed.alertsSent) ? parsed.alertsSent : [],
      // Favourites with a known category (and, for Entertainment only, a known food type) and a title.
      favorites: (Array.isArray(parsed.favorites) ? parsed.favorites : []).filter((f) => f && typeof f.title === 'string' && f.title.trim()
        && category(f.category) && (f.foodType == null || (f.category === 'ENTERTAINMENT' && foodType(f.foodType)))
        && Number.isInteger(f.myPercent ?? 100) && (f.myPercent ?? 100) >= 1 && (f.myPercent ?? 100) <= 100)
        .map((f) => ({ title: f.title.trim(), category: f.category, foodType: f.foodType ?? null, myPercent: f.myPercent ?? 100 }))
        .slice(0, 5),
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
