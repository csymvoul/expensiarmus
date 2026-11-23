// Euro amounts are whole cents to avoid floating point rounding errors.

/** 'en' shows €1,234.56; 'el' (Greek) shows 1.234,56 €. Set from the settings. */
let style = 'en';
export const setMoneyStyle = (s) => { style = s === 'el' ? 'el' : 'en'; };
export const moneyStyle = () => style;

const MAX_EUROS = 100_000_000_000;

/**
 * Parses what the user typed into cents. Accepts a comma or a dot as the decimal separator
 * ("12,50", "12.5") and ignores thousands separators ("1.234,56", "1,234.56"). A separator
 * followed by exactly three digits is a thousands separator. Returns null when it is not an amount.
 */
export function parseToCents(input) {
  let s = String(input).trim();
  if (s.startsWith('€')) s = s.slice(1);
  if (s.endsWith('€')) s = s.slice(0, -1);
  s = s.trim();
  if (!/^[0-9.,]+$/.test(s)) return null;
  const lastSep = Math.max(s.lastIndexOf('.'), s.lastIndexOf(','));
  const decimals = lastSep >= 0 ? s.length - lastSep - 1 : 0;
  let intRaw;
  let fracRaw;
  if (lastSep >= 0 && decimals >= 1 && decimals <= 2) {
    intRaw = s.slice(0, lastSep);
    fracRaw = s.slice(lastSep + 1);
  } else if (lastSep >= 0 && decimals === 0) {
    return null;
  } else {
    intRaw = s;
    fracRaw = '';
  }
  const intDigits = intRaw.replace(/\D/g, '');
  if (intDigits === '' && fracRaw === '') return null;
  const euros = Number(intDigits || '0');
  if (euros > MAX_EUROS) return null;
  return euros * 100 + Number(fracRaw.padEnd(2, '0'));
}

/** "€1,234.56", or "1.234,56 €" in the Greek style. */
export function format(cents, s = style) {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  const group = s === 'el' ? '.' : ',';
  const euros = Math.floor(abs / 100).toString().replace(/\B(?=(\d{3})+(?!\d))/g, group);
  const dec = String(abs % 100).padStart(2, '0');
  return s === 'el' ? `${sign}${euros},${dec} €` : `${sign}€${euros}.${dec}`;
}

/** "1234.56", for CSV files. */
export function plain(cents) {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  return `${sign}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

/** Text to pre-fill an amount field: "1234" for whole euros, otherwise "1234.56" ("1234,56" in Greek style). */
export const forInput = (cents, s = style) => {
  if (cents % 100 === 0) return String(cents / 100);
  return s === 'el' ? plain(cents).replace('.', ',') : plain(cents);
};

/** Short form for calendar cells: "€45", "€1.2k", "<€1" (Greek style: "45€", "1,2k€", "<1€"). */
export function compact(cents, s = style) {
  const wrap = (n) => (s === 'el' ? `${n}€` : `€${n}`);
  if (cents >= 1 && cents <= 49) return `<${wrap(1)}`;
  const euros = Math.floor((cents + 50) / 100);
  if (euros < 1000) return wrap(euros);
  if (euros < 10000) return wrap(`${Math.floor(euros / 1000)}${s === 'el' ? ',' : '.'}${Math.floor((euros % 1000) / 100)}k`);
  return wrap(`${Math.floor(euros / 1000)}k`);
}
