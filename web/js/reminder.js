// The 21:30 owl: what it says, and which evenings to schedule. Same wording as the Android app.
import { myShareCents } from './model.js';
import { format } from './money.js';
import { wordsFor } from './words.js';

export const HOUR = 21;
export const MINUTE = 30;

const pad = (n) => String(n).padStart(2, '0');
const isoDay = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

/** Title and body of the reminder for one day ("YYYY-MM-DD"), based on what was recorded that day. */
export function reminderMessage(expenses, day, words = wordsFor(true)) {
  const todays = expenses.filter((e) => e.date === day);
  if (todays.length === 0) {
    return { title: words.owlNothing, body: 'No expenses recorded today. Tap to record what you spent.' };
  }
  const count = todays.length === 1 ? '1 expense' : `${todays.length} expenses`;
  const total = format(todays.reduce((s, e) => s + myShareCents(e), 0));
  return { title: words.owlSome, body: `Today: ${count}, ${total}. Tap to add anything you missed.` };
}

/**
 * Reminders for the coming evenings, starting today if 21:30 is still ahead of `now`, `days`
 * calendar days in all. The iPhone app re-schedules these whenever the data changes or the app
 * is opened, so each evening's text matches what has been recorded by then.
 */
export function upcomingReminders(expenses, now, days = 30, time = `${HOUR}:${MINUTE}`, words = wordsFor(true)) {
  const [hour, minute] = time.split(':').map(Number);
  const out = [];
  for (let i = 0; i < days; i++) {
    const at = new Date(now.getFullYear(), now.getMonth(), now.getDate() + i, hour, minute);
    if (at <= now) continue;
    const date = isoDay(at);
    out.push({ date, hour, minute, ...reminderMessage(expenses, date, words) });
  }
  return out;
}
