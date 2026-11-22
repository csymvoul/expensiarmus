// Categories, food types and expense helpers. Same data shape as the Android app.

export const CATEGORIES = [
  { id: 'RENT', label: 'Rent', hint: 'Monthly rent', emoji: '🏰', nickname: 'The Castle Keep', color: '#A33A3A' },
  { id: 'SUPERMARKET', label: 'Super market', hint: 'Groceries and household items', emoji: '🛒', nickname: 'Potion Ingredients', color: '#2F7D4E' },
  { id: 'ENTERTAINMENT', label: 'Entertainment', hint: 'Food, cinema, theatre, etc.', emoji: '🍽️', nickname: 'Revelry & Feasts', color: '#C8962E' },
  { id: 'BILLS', label: 'Bills', hint: 'Water, electricity, gas, etc.', emoji: '⚡', nickname: 'Lumos, Aguamenti, Incendio', color: '#3A63A8' },
  { id: 'SHOPPING', label: 'Shopping', hint: 'Shoes, clothes, jewellery, etc.', emoji: '🛍️', nickname: 'Diagon Alley', color: '#B5487A' },
  { id: 'OTHER', label: 'Other', hint: 'Presents and anything else', emoji: '🎁', nickname: 'Curiosities', color: '#7E5AA6' },
];

/** Optional subgroup for food expenses, which belong to Entertainment. */
export const FOOD_TYPES = [
  { id: 'DELIVERY', label: 'Delivery', hint: 'Food ordered in', emoji: '🦉', nickname: 'Brought by owl', color: '#8C6416' },
  { id: 'DINE_OUT', label: 'Dine out', hint: 'Restaurants, tavernas, bars', emoji: '🍷', nickname: 'Feasting out', color: '#C8962E' },
  { id: 'DAY_TO_DAY', label: 'Day-to-day', hint: 'Coffee at work, snacks, quick bites', emoji: '☕', nickname: 'Daily potions', color: '#E6C878' },
];

export const category = (id) => CATEGORIES.find((c) => c.id === id);
export const foodType = (id) => FOOD_TYPES.find((f) => f.id === id);

/** My part of the amount, rounded half up to the nearest cent. */
export const myShareCents = (e) => Math.floor((e.amountCents * e.myPercent + 50) / 100);
export const isSplit = (e) => e.myPercent < 100;

/**
 * Checks an expense and returns a clean copy. Throws when it is invalid, like the Android
 * model does: negative amount, share outside 1..100, unknown category, or food type on a
 * category other than Entertainment.
 */
export function makeExpense({ id, date, title, amountCents, category: cat, myPercent = 100, foodType: food = null }) {
  if (!id) throw new Error('id is required');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error('date must be YYYY-MM-DD');
  if (!Number.isInteger(amountCents) || amountCents < 0) throw new Error('amount must not be negative');
  if (!Number.isInteger(myPercent) || myPercent < 1 || myPercent > 100) throw new Error('myPercent must be 1..100');
  if (!category(cat)) throw new Error(`unknown category ${cat}`);
  if (food != null && cat !== 'ENTERTAINMENT') throw new Error('only entertainment can be food');
  if (food != null && !foodType(food)) throw new Error(`unknown food type ${food}`);
  return { id, date, title, amountCents, category: cat, myPercent, foodType: food ?? null };
}

export const emptyData = () => ({ expenses: [], goals: { monthlyCents: null, perCategoryCents: {} } });

export const upsert = (data, e) => ({ ...data, expenses: [...data.expenses.filter((x) => x.id !== e.id), e] });
export const remove = (data, id) => ({ ...data, expenses: data.expenses.filter((x) => x.id !== id) });
export const find = (data, id) => data.expenses.find((x) => x.id === id);

/** "2026-11" for a "2026-11-22" date. */
export const monthOf = (date) => date.slice(0, 7);
