// User settings, kept in the browser's storage next to the data.

export const SETTINGS_KEY = 'expensiarmus.settings';
const OLD_REMINDER_KEY = 'expensiarmus.reminder';

export const DEFAULTS = { reminderOn: true, reminderTime: '21:30', wizard: true, money: 'en', theme: 'system' };

/** "21:30" style, 00:00–23:59. */
export const validTime = (t) => /^([01]\d|2[0-3]):[0-5]\d$/.test(String(t));

export function loadSettings(storage) {
  let saved = {};
  try {
    saved = JSON.parse(storage.getItem(SETTINGS_KEY) ?? '{}') ?? {};
  } catch {
    saved = {};
  }
  const s = { ...DEFAULTS, ...saved };
  // Before settings existed, the reminder switch was stored on its own.
  if (!('reminderOn' in saved) && storage.getItem(OLD_REMINDER_KEY) === 'off') s.reminderOn = false;
  if (!validTime(s.reminderTime)) s.reminderTime = DEFAULTS.reminderTime;
  if (!['en', 'el'].includes(s.money)) s.money = 'en';
  if (!['system', 'light', 'dark'].includes(s.theme)) s.theme = 'system';
  s.reminderOn = s.reminderOn !== false;
  s.wizard = s.wizard !== false;
  return s;
}

export const saveSettings = (storage, s) => storage.setItem(SETTINGS_KEY, JSON.stringify(s));
