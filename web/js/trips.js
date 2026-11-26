// Trips: a name, dates and an optional total budget. Expenses keep their category and can also belong to
// a trip; they still count in their month, category and wards. Same rules as Android (domain/Trips.kt).
import { myShareCents, category, foodType } from './model.js';
import { goalProgress } from './summary.js';

const isDay = (v) => typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v) && !Number.isNaN(Date.parse(`${v}T00:00:00Z`))
  && new Date(`${v}T00:00:00Z`).toISOString().slice(0, 10) === v;
const daysBetween = (a, b) => Math.round((Date.parse(`${b}T00:00:00Z`) - Date.parse(`${a}T00:00:00Z`)) / 86400000);
const fold = (s) => String(s).normalize('NFD').replace(/\p{Mn}+/gu, '').toLowerCase().trim();

/** A trip, or an error message saying what to fix. */
export function checkTrip(t) {
  const name = String(t.name ?? '').trim();
  if (!name) return { error: 'Give it a name.' };
  if (!isDay(t.start) || !isDay(t.end)) return { error: 'Pick the first and last day.' };
  if (t.end < t.start) return { error: 'The last day is before the first.' };
  const budget = t.budgetCents ?? null;
  if (budget != null && (!Number.isInteger(budget) || budget < 0)) return { error: 'The budget must be an amount, or left empty.' };
  return { trip: { id: t.id, name, start: t.start, end: t.end, budgetCents: budget || null } };
}

/** Adds or replaces (same id) a trip. Newest first. Two trips can't share a name. */
export function saveTrip(trips, t) {
  const checked = checkTrip(t);
  if (checked.error) return checked;
  const others = (trips ?? []).filter((x) => x.id !== t.id);
  if (others.some((x) => fold(x.name) === fold(checked.trip.name))) return { error: 'You already have a trip with that name.' };
  return { list: [...others, checked.trip].sort((a, b) => (a.start < b.start ? 1 : a.start > b.start ? -1 : a.name.localeCompare(b.name))) };
}

/** Removes a trip; its expenses stay, no longer tied to it. */
export function deleteTrip(data, id) {
  return {
    ...data,
    trips: (data.trips ?? []).filter((t) => t.id !== id),
    expenses: data.expenses.map((e) => (e.tripId === id ? (({ tripId, ...rest }) => rest)(e) : e)),
  };
}

/** Before, during (day x of n) or after the trip, on [today]. */
export function tripStatus(t, today) {
  const days = daysBetween(t.start, t.end) + 1;
  if (today < t.start) return { state: 'upcoming', days, day: 0, inDays: daysBetween(today, t.start) };
  if (today > t.end) return { state: 'done', days, day: days };
  return { state: 'ongoing', days, day: daysBetween(t.start, today) + 1 };
}

/** Trips you can still record for: the ones not over yet, the soonest first. */
export const tripsToOffer = (trips, today) => (trips ?? []).filter((t) => t.end >= today).sort((a, b) => (a.start < b.start ? -1 : a.start > b.start ? 1 : 0));

/** The trip on today, if any (the one that started last when they overlap): ticked by default when recording. */
export const ongoingTrip = (trips, today) => tripsToOffer(trips, today).filter((t) => t.start <= today).at(-1) ?? null;

export const withoutTrips = (expenses) => expenses.filter((e) => !e.tripId);

/**
 * Everything about one trip: its expenses (newest first), total, budget progress, average per day
 * (over the days so far while it is on), the spend per category (and food type) and per day.
 */
export function tripSummary(data, id, today) {
  const trip = (data.trips ?? []).find((t) => t.id === id);
  if (!trip) return null;
  const expenses = data.expenses.filter((e) => e.tripId === id)
    .sort((a, b) => (a.date === b.date ? a.title.localeCompare(b.title) : a.date < b.date ? 1 : -1));
  const totalCents = expenses.reduce((s, e) => s + myShareCents(e), 0);
  const status = tripStatus(trip, today);
  const counted = status.state === 'ongoing' ? status.day : status.days;
  const groups = new Map();
  for (const e of expenses) {
    const key = `${e.category}|${e.foodType ?? ''}`;
    groups.set(key, (groups.get(key) ?? 0) + myShareCents(e));
  }
  const byCategory = [...groups.entries()].map(([key, cents]) => {
    const [cat, food] = key.split('|');
    return { category: cat, foodType: food || null, label: food ? foodType(food).label : category(cat).label, cents };
  }).sort((a, b) => b.cents - a.cents || a.label.localeCompare(b.label));
  const days = new Map();
  for (const e of expenses) days.set(e.date, (days.get(e.date) ?? 0) + myShareCents(e));
  return {
    trip,
    status,
    expenses,
    count: expenses.length,
    totalCents,
    budget: trip.budgetCents ? goalProgress(trip.budgetCents, totalCents) : null,
    perDayCents: counted > 0 ? Math.floor((totalCents * 2 + counted) / (counted * 2)) : 0,
    byCategory,
    byDay: [...days.entries()].sort((a, b) => (a[0] < b[0] ? 1 : -1)).map(([date, cents]) => ({ date, cents })),
  };
}

/** Cleans saved trips on load: valid ones only, unique ids. */
export function loadTrips(raw) {
  const seen = new Set();
  return (Array.isArray(raw) ? raw : []).flatMap((t) => {
    if (!t || typeof t.id !== 'string' || !t.id || seen.has(t.id)) return [];
    const c = checkTrip(t);
    if (c.error) return [];
    seen.add(t.id);
    return [c.trip];
  });
}
