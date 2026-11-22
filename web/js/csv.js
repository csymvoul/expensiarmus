import { category, foodType, myShareCents } from './model.js';
import { plain } from './money.js';

export const HEADER = 'date,title,category,food_type,amount_eur,my_share_percent,my_share_eur';

export const escape = (v) => (/[",\n\r]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

/** Oldest first, one line per expense. Same columns as the Android export. */
export function toCsv(expenses) {
  const rows = [...expenses].sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0)).map((e) =>
    [e.date, escape(e.title), escape(category(e.category).label), e.foodType ? foodType(e.foodType).label : '',
      plain(e.amountCents), String(e.myPercent), plain(myShareCents(e))].join(','));
  return [HEADER, ...rows].join('\n') + '\n';
}
