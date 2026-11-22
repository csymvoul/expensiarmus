// Euro amounts are whole cents to avoid floating point rounding errors.

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

/** "€1,234.56" */
export function format(cents) {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  const euros = Math.floor(abs / 100).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${sign}€${euros}.${String(abs % 100).padStart(2, '0')}`;
}

/** "1234.56", for CSV files. */
export function plain(cents) {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  return `${sign}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

/** Text to pre-fill an amount field: "1234" for whole euros, otherwise "1234.56". */
export const forInput = (cents) => (cents % 100 === 0 ? String(cents / 100) : plain(cents));

/** Short form for calendar cells: "€45", "€1.2k", "<€1". */
export function compact(cents) {
  if (cents >= 1 && cents <= 49) return '<€1';
  const euros = Math.floor((cents + 50) / 100);
  if (euros < 1000) return `€${euros}`;
  if (euros < 10000) return `€${Math.floor(euros / 1000)}.${Math.floor((euros % 1000) / 100)}k`;
  return `€${Math.floor(euros / 1000)}k`;
}
