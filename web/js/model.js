// Categories, food types and expense helpers. Same data shape as the Android app.

export const CATEGORIES = [
  { id: 'RENT', label: 'Rent', hint: 'Monthly rent', emoji: '🏰', nickname: 'The Castle Keep', color: '#A33A3A', icon: 'icons/emblems/cat_rent.png' },
  { id: 'SUPERMARKET', label: 'Super market', hint: 'Groceries and household items', emoji: '🛒', nickname: 'Potion Ingredients', color: '#2F7D4E', icon: 'icons/emblems/cat_supermarket.png' },
  { id: 'ENTERTAINMENT', label: 'Entertainment', hint: 'Food, cinema, theatre, etc.', emoji: '🍽️', nickname: 'Revelry & Feasts', color: '#C8962E', icon: 'icons/emblems/cat_entertainment.png' },
  { id: 'BILLS', label: 'Bills', hint: 'Water, electricity, gas, etc.', emoji: '⚡', nickname: 'Lumos, Aguamenti, Incendio', color: '#3A63A8', icon: 'icons/emblems/cat_bills.png' },
  { id: 'SHOPPING', label: 'Shopping', hint: 'Shoes, clothes, jewellery, etc.', emoji: '🛍️', nickname: 'Diagon Alley', color: '#B5487A', icon: 'icons/emblems/cat_shopping.png' },
  { id: 'FUEL', label: 'Fuel', hint: 'Petrol (gas), diesel, LPG', emoji: '⛽', nickname: 'Floo Powder', color: '#2E8B8B', icon: 'icons/emblems/cat_fuel.png' },
  { id: 'OTHER', label: 'Other', hint: 'Presents and anything else', emoji: '🎁', nickname: 'Curiosities', color: '#7E5AA6', icon: 'icons/emblems/cat_other.png' },
];

/** Optional subgroup for food expenses, which belong to Entertainment. */
export const FOOD_TYPES = [
  { id: 'DELIVERY', label: 'Delivery', hint: 'Food ordered in', emoji: '🦉', nickname: 'Brought by owl', color: '#8C6416', icon: 'icons/emblems/food_delivery.png' },
  { id: 'DINE_OUT', label: 'Dine out', hint: 'Restaurants, tavernas, bars', emoji: '🍷', nickname: 'Feasting out', color: '#C8962E', icon: 'icons/emblems/food_dine_out.png' },
  { id: 'DAY_TO_DAY', label: 'Day-to-day', hint: 'Coffee at work, snacks, quick bites', emoji: '☕', nickname: 'Daily potions', color: '#E6C878', icon: 'icons/emblems/food_day_to_day.png' },
];

/** Painted emblem for each bottom tab. */
export const TAB_ICONS = {
  month: 'icons/emblems/tab_ledger.png',
  calendar: 'icons/emblems/tab_calendar.png',
  annals: 'icons/emblems/tab_annals.png',
  goals: 'icons/emblems/tab_goals.png',
  settings: 'icons/emblems/tab_settings.png',
};

/** Painted emblems for the app's own title and the Settings cards. */
export const EMBLEMS = {
  logo: 'icons/emblems/logo_pouch.png',
  geminio: 'icons/emblems/set_geminio.png',
  reminder: 'icons/emblems/set_reminder.png',
  themeSystem: 'icons/emblems/set_theme_system.png',
  themeLight: 'icons/emblems/set_theme_light.png',
  themeDark: 'icons/emblems/set_theme_dark.png',
};

/** Accio's magnifying glass, drawn without a badge: bronze on parchment, gold on the dark theme. CSS picks one. */
export const SEARCH_GLYPH = { light: 'icons/emblems/search_light.png', dark: 'icons/emblems/search_dark.png' };

/** A label without its leading emoji ("⚙️ Settings" → "Settings"), for places that show a painted emblem instead. */
export const withoutEmoji = (label) => label.replace(/^[^\p{L}\p{N}]+\s*/u, '');

export const category = (id) => CATEGORIES.find((c) => c.id === id);
export const foodType = (id) => FOOD_TYPES.find((f) => f.id === id);

/** My part of the amount, rounded half up to the nearest cent. */
export const myShareCents = (e) => Math.floor((e.amountCents * e.myPercent + 50) / 100);
export const isSplit = (e) => e.myPercent < 100;

/** "Not split · all yours", or "Your share 50% (€18.00) · theirs 50% (€18.00)". */
export function splitLine(e, fmt) {
  if (!isSplit(e)) return 'Not split · all yours';
  const mine = myShareCents(e);
  return `Your share ${e.myPercent}% (${fmt(mine)}) · theirs ${100 - e.myPercent}% (${fmt(e.amountCents - mine)})`;
}

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

export const emptyData = () => ({ expenses: [], goals: { entries: [] }, alertsSent: [] });

export const upsert = (data, e) => ({ ...data, expenses: [...data.expenses.filter((x) => x.id !== e.id), e] });
export const remove = (data, id) => ({ ...data, expenses: data.expenses.filter((x) => x.id !== id) });
export const find = (data, id) => data.expenses.find((x) => x.id === id);

/** "2026-11" for a "2026-11-22" date. */
export const monthOf = (date) => date.slice(0, 7);
