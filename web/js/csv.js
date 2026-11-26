import { CATEGORIES, FOOD_TYPES, category, foodType, myShareCents, makeExpense } from './model.js';
import { plain, parseToCents } from './money.js';

export const HEADER = 'date,title,category,food_type,amount_eur,my_share_percent,my_share_eur,trip';

export const escape = (v) => (/[",\n\r]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

/** Oldest first, one line per expense, ending with the trip's name (if any). Same columns as the Android export. */
export function toCsv(expenses, trips = []) {
  const tripName = new Map(trips.map((t) => [t.id, t.name]));
  const rows = [...expenses].sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0)).map((e) =>
    [e.date, escape(e.title), escape(category(e.category).label), e.foodType ? foodType(e.foodType).label : '',
      plain(e.amountCents), String(e.myPercent), plain(myShareCents(e)), escape(tripName.get(e.tripId) ?? '')].join(','));
  return [HEADER, ...rows].join('\n') + '\n';
}

// ---------- Apparate: import (same rules as the Android app's CsvImporter) ----------

/** Splits CSV text into rows of fields: quoted fields, doubled quotes and CRLF are handled. */
export function parseCsv(text, delimiter = ',') {
  const rows = [];
  let row = [];
  let field = '';
  let quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') { field += '"'; i++; } else if (c === '"') quoted = false; else field += c;
    } else if (c === '"' && field === '') quoted = true;
    else if (c === delimiter) { row.push(field); field = ''; }
    else if (c === '\n' || c === '\r') {
      if (c === '\r' && text[i + 1] === '\n') i++;
      row.push(field); rows.push(row); row = []; field = '';
    } else field += c;
  }
  if (field !== '' || row.length) { row.push(field); rows.push(row); }
  return rows;
}

const squash = (v) => String(v ?? '').toLowerCase().replace(/[\s_-]/g, '');
/** Earlier names still read, so old backups come back in: Home was Rent before web 1.11, Transport was Fuel before web 1.13. */
const OLD_NAMES = { rent: 'RENT', fuel: 'FUEL' };
const findCategory = (v) => CATEGORIES.find((c) => squash(c.label) === squash(v) || squash(c.id) === squash(v)) ?? category(OLD_NAMES[squash(v)]);
const findFood = (v) => FOOD_TYPES.find((f) => squash(f.label) === squash(v) || squash(f.id) === squash(v));

/** "2026-11-22", or a spreadsheet's "22/11/2026" or "22.11.2026" (day first), as YYYY-MM-DD; null if not a real date. */
export function parseDate(v) {
  const s = String(v ?? '').trim();
  let m = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
  let y; let mo; let d;
  if (m) [, y, mo, d] = m.map(Number);
  else if ((m = s.match(/^(\d{1,2})[/.](\d{1,2})[/.](\d{4})$/))) [, d, mo, y] = m.map(Number);
  else return null;
  const date = new Date(Date.UTC(y, mo - 1, d));
  if (date.getUTCFullYear() !== y || date.getUTCMonth() !== mo - 1 || date.getUTCDate() !== d) return null;
  return `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}

/** Same date, title, amount, category, food type and split: the same expense for import purposes. */
const sameness = (e) => [e.date, e.title.trim().toLowerCase(), e.amountCents, e.category, e.foodType ?? '', e.myPercent].join('|');

/**
 * Apparate: reads a CSV (the one Geminio exports, or a spreadsheet with the same columns, separated by commas
 * or semicolons) against the expenses already in the ledger. Nothing is changed here: it returns the new
 * expenses, how many rows are already in the ledger, and the rows it could not read with the reason.
 * Rows match existing expenses one for one, so re-importing a backup adds nothing and two identical
 * coffees stay two coffees.
 */
export function importCsv(text, existing, newId, trips = []) {
  const clean = String(text).replace(/^﻿/, '');
  const firstLine = clean.split(/\r?\n/, 1)[0] ?? '';
  const delimiter = firstLine.includes(';') && !firstLine.includes(',') ? ';' : ',';
  const rows = parseCsv(clean, delimiter);
  const result = { added: [], duplicates: 0, problems: [], rows: 0, newTrips: [] };
  if (!rows.length) {
    result.problems.push({ line: 1, reason: 'The file is empty.' });
    return result;
  }
  const head = rows[0].map(squash);
  const col = (name) => head.indexOf(squash(name));
  const missing = ['date', 'title', 'category', 'amount_eur'].filter((n) => col(n) < 0);
  if (missing.length) {
    result.problems.push({ line: 1, reason: `Not an Expensiarmus CSV: no ${missing.join(', ')} column.` });
    return result;
  }
  const at = { date: col('date'), title: col('title'), category: col('category'), food: col('food_type'), amount: col('amount_eur'), percent: col('my_share_percent'), trip: col('trip') };
  // Trips by name: existing ones are matched (case and accents ignored); unknown names become new trips
  // spanning the dates of their expenses.
  const tripKey = (v) => squash(String(v).normalize('NFD').replace(/\p{Mn}+/gu, ''));
  const tripsByName = new Map((trips ?? []).map((t) => [tripKey(t.name), t.id]));
  const created = new Map();
  const left = new Map();
  for (const e of existing) left.set(sameness(e), (left.get(sameness(e)) ?? 0) + 1);
  rows.slice(1).forEach((cells, i) => {
    const line = i + 2;
    if (cells.every((c) => c.trim() === '')) return;
    result.rows += 1;
    const get = (k) => (at[k] >= 0 ? (cells[at[k]] ?? '').trim() : '');
    const problem = (reason) => { result.problems.push({ line, reason }); };
    const date = parseDate(get('date'));
    if (!date) return problem(`"${get('date')}" is not a date.`);
    const title = get('title');
    if (!title) return problem('The title is empty.');
    const cat = findCategory(get('category'));
    if (!cat) return problem(`"${get('category')}" is not a category.`);
    const foodText = get('food');
    const food = foodText ? findFood(foodText) : null;
    if (foodText && !food) return problem(`"${foodText}" is not a food type.`);
    if (food && cat.id !== 'ENTERTAINMENT') return problem('Only Entertainment can have a food type.');
    const cents = parseToCents(get('amount'));
    if (cents == null) return problem(`"${get('amount')}" is not an amount.`);
    const pctText = get('percent');
    const pct = pctText === '' ? 100 : Number(pctText);
    if (!/^\d*$/.test(pctText) || !Number.isInteger(pct) || pct < 1 || pct > 100) return problem(`"${pctText}" is not a share between 1 and 100.`);
    const e = makeExpense({ id: newId(), date, title, amountCents: cents, category: cat.id, myPercent: pct, foodType: food ? food.id : null });
    const key = sameness(e);
    if ((left.get(key) ?? 0) > 0) {
      left.set(key, left.get(key) - 1);
      result.duplicates += 1;
      return undefined;
    }
    const tripName = get('trip');
    if (tripName) {
      let id = tripsByName.get(tripKey(tripName));
      if (!id) {
        const t = created.get(tripKey(tripName)) ?? { id: newId(), name: tripName, start: date, end: date, budgetCents: null };
        if (date < t.start) t.start = date;
        if (date > t.end) t.end = date;
        created.set(tripKey(tripName), t);
        id = t.id;
      }
      e.tripId = id;
    }
    result.added.push(e);
    return undefined;
  });
  result.newTrips = [...created.values()];
  return result;
}

/** What the confirmation shows: counts, then up to [maxProblems] unreadable rows ("Line 4: …"). Same as Android. */
export function importSummary(r, maxProblems = 5) {
  const lines = [r.rows === 1 ? 'Found 1 expense in the file.' : `Found ${r.rows} expenses in the file.`];
  if (r.rows > 0) lines.push(`${r.added.length} new, ${r.duplicates} already in your ledger.`);
  if (r.problems.length) {
    lines.push(r.problems.length === 1 ? '1 row could not be read:' : `${r.problems.length} rows could not be read:`);
    for (const p of r.problems.slice(0, maxProblems)) lines.push(`Line ${p.line}: ${p.reason}`);
    if (r.problems.length > maxProblems) lines.push(`…and ${r.problems.length - maxProblems} more.`);
  }
  const nt = r.newTrips ?? [];
  if (nt.length) lines.push(`${nt.length === 1 ? 'Also adds 1 trip' : `Also adds ${nt.length} trips`}: ${nt.map((t) => t.name).join(', ')}.`);
  return lines;
}
