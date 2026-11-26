// Expensiarmus web app: screens, navigation and event handling. Logic lives in the other modules.
import { CATEGORIES, FOOD_TYPES, TAB_ICONS, EMBLEMS, WHOLE_MONTH, goalKey, withoutEmoji, category, foodType, makeExpense, myShareCents, isSplit, splitLine, upsert, remove, find, monthOf } from './model.js';
import { parseToCents, format, forInput, compact, setMoneyStyle } from './money.js';
import { monthlySummary, describeGoal, goalProgress, yearSummary, allTimeSummary, monthRecap, recapText, recapMonthFor } from './summary.js';
import { goalFor, setGoal, removeGoal, scopeText, alertsDue, markAlertsSent } from './goals.js';
import { categoryTotals, dailyTotals, onDay, calendarCells, byCategory, byDay, inPeriod, isFavorite, toggleFavorite, MAX_FAVORITES, saveFavorite, removeFavoriteAt, search, shiftMonth, defaultTitle, ledgerPreview, monthGlance, LATEST_COUNT } from './queries.js';
import { toCsv, importCsv, importSummary } from './csv.js';
import { saveTrip, deleteTrip, tripStatus, tripsToOffer, ongoingTrip, withoutTrips, tripSummary } from './trips.js';
import { load, save, KEY } from './store.js';
import { upcomingReminders } from './reminder.js';
import { wordsFor } from './words.js';
import { widgetContent } from './widget.js';
import { loadSettings, saveSettings, validTime } from './settings.js';

const app = document.getElementById('app');
const storage = window.localStorage;
let data = load(storage);
let settings = loadSettings(storage);
let W = wordsFor(settings.wizard);

const pad = (n) => String(n).padStart(2, '0');
const today = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};
const asDate = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d || 1);
};
const fmt = (iso, opts) => new Intl.DateTimeFormat('en-GB', opts).format(asDate(iso));
const monthTitle = (month) => fmt(`${month}-01`, { month: 'long', year: 'numeric' });
const monthName = (m) => fmt(`${m}-01`, { month: 'long' });
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const newId = () => (crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`);

const ui = {
  month: today().slice(0, 7),
  day: today(),
  highlighted: null,
  modal: false,
  quick: null, // the Quick add sheet, when open
  monthListTrips: null, // on a month's full list: 'without' leaves trip expenses out
  openDays: new Set(), // on a month's full list: the days opened to show their expenses (all closed at first)
  listCatsOpen: true, // on a month's full list: the By vault card open (tap its heading to fold it)
  importing: null, // Apparate: what the picked CSV would add, waiting for the user to confirm
  annals: { view: 'year', year: Number(today().slice(0, 4)), bar: null },
};
let draft = null; // the expense being added or edited
let prefill = null; // what Quick add hands to the full form ("More details")
const finder = { q: '', cats: [] };
let navDepth = 0;

// Inside the iPhone app the page can talk to Swift through this handler; on the web it is absent.
const nativeBridge = window.webkit?.messageHandlers?.expensiarmus ?? null;
const post = (message) => nativeBridge?.postMessage(message);

/** Applies the settings: number style, wording, theme (also for the iPhone app's own colours). */
function applySettings() {
  setMoneyStyle(settings.money);
  W = wordsFor(settings.wizard);
  if (settings.theme === 'system') delete document.documentElement.dataset.theme;
  else document.documentElement.dataset.theme = settings.theme;
  post({ type: 'theme', value: settings.theme });
}

function updateSettings(change) {
  settings = { ...settings, ...change };
  saveSettings(storage, settings);
  applySettings();
  scheduleOwls();
  updateWidget();
  render();
}

/** Hands the iPhone app what its home-screen widget shows: this month, its wards and the trips to come. */
function updateWidget() {
  post({ type: 'widget', content: widgetContent(data, today().slice(0, 7), W, today()) });
}

/** Asks the iPhone app to (re)schedule the evening owls for the coming days, at the chosen time. */
function scheduleOwls() {
  const evenings = settings.reminderOn ? upcomingReminders(data.expenses, new Date(), 30, settings.reminderTime, W) : [];
  // On the 1st, a second owl sums up the month just finished (opens the Ledger, not Record expense).
  const recaps = evenings.filter((o) => o.date.endsWith('-01')).flatMap((o) => {
    const r = monthRecap(data, recapMonthFor(o.date));
    return r ? [{ ...o, id: `recap-${r.month}`, route: '#/', title: W.recapTitle(fmt(`${r.month}-01`, { month: 'long' })), body: recapText(r, W) }] : [];
  });
  post({ type: 'schedule', enabled: settings.reminderOn, reminders: [...evenings, ...recaps] });
}

function commit(next) {
  // Warn once when a category of the current month reaches 80% of its goal, or goes over.
  const alerts = alertsDue(next, today().slice(0, 7), W);
  data = alerts.length ? markAlertsSent(next, alerts) : next;
  save(storage, data);
  // The iPhone app keeps a copy outside the web view's storage, as a backup.
  post({ type: 'save', key: KEY, json: storage.getItem(KEY) });
  scheduleOwls();
  updateWidget();
  alerts.forEach(showGoalAlert);
}

/** A real notification inside the iPhone app; on the web (no notifications on iPhone) an in-app card. */
function showGoalAlert(a) {
  if (nativeBridge) {
    post({ type: 'notify', id: `goal-${a.category ?? 'TOTAL'}-${a.month}`, title: a.title, body: a.body });
    return;
  }
  const el = document.createElement('div');
  el.className = 'toast alert';
  el.innerHTML = `<b>${esc(a.title)}</b><span>${esc(a.body)}</span>`;
  document.body.append(el);
  setTimeout(() => el.remove(), 6500);
}

// ---------- navigation ----------

const TABS = ['month', 'calendar', 'annals', 'goals', 'settings'];

function route() {
  const [name, a, b, c] = location.hash.replace(/^#\/?/, '').split('/');
  const part = (x) => (x ? decodeURIComponent(x) : null);
  return { name: name || 'month', a: part(a), b: part(b), c: part(c) };
}
function go(path) {
  navDepth++;
  location.hash = path;
}
function back() {
  if (navDepth > 0) {
    navDepth--;
    history.back();
  } else {
    location.hash = '#/';
  }
}

// ---------- small pieces ----------

const ICON = {
  back: '<svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg>',
  left: '<svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg>',
  right: '<svg viewBox="0 0 24 24"><path d="M9 5l7 7-7 7"/></svg>',
  search: '<svg viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6"/><path d="M15 15l5 5"/></svg>',
};

/** Top bar. With an icon, the title's own emoji gives way to the painted emblem. */
function header(title, { backBtn = false, actions = '', icon = '', glyph = '' } = {}) {
  return `<header class="bar"><div class="bar-row">
    ${backBtn ? `<button class="icon-btn" data-action="back" aria-label="Back">${ICON.back}</button>` : ''}
    <h1>${glyph || (icon ? `<img class="bar-icon" src="${icon}" alt="">` : '')}<span>${esc(icon ? withoutEmoji(title) : title)}</span></h1>
    <div class="bar-actions">${actions}</div>
  </div></header>`;
}

/** The bottom tab bar: Ledger · Calendar · Annals · Protego (or the plain names). */
function tabBar(active) {
  const tabs = [['month', '#/', W.ledgerTab], ['calendar', '#/calendar', W.calendarTab], ['annals', '#/annals', W.annalsTab], ['goals', '#/goals', W.goalsTab], ['settings', '#/settings', W.settings]];
  return `<nav class="tabs">${tabs.map(([key, to, label]) => {
    const [, ...words] = label.split(' ');
    return `<button class="tab ${active === key ? 'on' : ''}" data-action="tab" data-to="${to}"><img class="tab-icon" src="${TAB_ICONS[key]}" alt=""><span>${esc(words.join(' '))}</span></button>`;
  }).join('')}</nav>`;
}

const card = (inner, cls = '') => `<section class="card ${cls}"><div class="paper">${inner}</div></section>`;
/** Round gold-rimmed badge with a painted emblem. */
const emblem = (icon, size = 30) => `<img class="emblem" src="${icon}" alt="" style="--s:${size}px">`;
/** Small emblem at the front of a chip. */
const chipIcon = (thing) => `<img class="chip-icon" src="${thing.icon}" alt="">`;
const ornament = (text) => `<h2 class="ornament"><span>${esc(text)}</span></h2>`;
const hint = (text) => `<p class="hint">${esc(text)}</p>`;
/** Category hint, with its wizard nickname in front when wizard words are on. */
const withNickname = (thing) => (W.nicknames ? `${thing.nickname}: ${thing.hint}` : thing.hint);

function goalBar(g, thin = false) {
  const cls = g.isOver ? 'over' : g.percent >= 80 ? 'warn' : '';
  return `<div class="goal-track ${thin ? 'thin' : ''}"><div class="goal-fill ${cls}" style="width:${Math.min(100, g.fraction * 100)}%"></div></div>`;
}

function expenseRow(e, showYear = false) {
  const c = category(e.category);
  const f = e.foodType ? foodType(e.foodType) : null;
  const when = fmt(e.date, showYear ? { day: 'numeric', month: 'short', year: 'numeric' } : { weekday: 'short', day: 'numeric', month: 'short' });
  const trip = e.tripId ? (data.trips ?? []).find((t) => t.id === e.tripId) : null;
  const details = [when, c.label + (f ? ` › ${f.label}` : ''), isSplit(e) ? `my share ${e.myPercent}%` : '', trip ? trip.name : ''].filter(Boolean).join(' · ');
  return `<button class="row" data-action="open" data-id="${esc(e.id)}">
    ${emblem(f ? f.icon : c.icon)}
    <span class="row-main"><span class="row-title">${esc(e.title)}</span><span class="row-sub">${esc(details)}</span></span>
    <span class="row-amount"><b>${format(myShareCents(e))}</b>${isSplit(e) ? `<small>of ${format(e.amountCents)}</small>` : ''}</span>
  </button>`;
}

// ---------- ledger and calendar ----------

/** SVG donut; slices are stroked arcs clockwise from 12 o'clock. Tapping one highlights it. */
function donut(summary) {
  const size = 200;
  const thick = 26;
  const r = (size - thick * 1.4) / 2;
  const cx = size / 2;
  const total = summary.totalCents;
  const visible = summary.categories.filter((c) => c.spentCents > 0);
  const gap = visible.length > 1 ? 2 : 0;
  const pt = (deg) => [cx + r * Math.sin((deg * Math.PI) / 180), cx - r * Math.cos((deg * Math.PI) / 180)];
  let start = 0;
  const arcs = summary.categories.map((c) => {
    if (c.spentCents <= 0) return '';
    const sweep = (c.spentCents / total) * 360;
    const sel = ui.highlighted === c.category;
    const style = `stroke="${category(c.category).color}" stroke-width="${sel ? thick * 1.4 : thick}" opacity="${ui.highlighted && !sel ? 0.3 : 1}"`;
    let shape;
    if (visible.length === 1) {
      shape = `<circle cx="${cx}" cy="${cx}" r="${r}" fill="none" ${style} data-action="slice" data-cat="${c.category}"/>`;
    } else {
      const a0 = start + gap / 2;
      const a1 = start + Math.max(sweep - gap / 2, gap / 2 + 0.5);
      const [x0, y0] = pt(a0);
      const [x1, y1] = pt(a1);
      shape = `<path d="M${x0.toFixed(2)} ${y0.toFixed(2)} A${r} ${r} 0 ${a1 - a0 > 180 ? 1 : 0} 1 ${x1.toFixed(2)} ${y1.toFixed(2)}" fill="none" ${style} data-action="slice" data-cat="${c.category}"/>`;
    }
    start += sweep;
    return shape;
  });
  const pick = summary.categories.find((c) => c.category === ui.highlighted);
  const center = pick
    ? `${emblem(category(pick.category).icon, 30)}<span class="h-small">${esc(category(pick.category).label)}</span><b>${format(pick.spentCents)}</b><small>${Math.round(pick.shareOfMonth * 100)}%</small>`
    : `<span class="label gold">✦ Total ✦</span><b class="h-medium">${format(total)}</b>`;
  return `<div class="donut">
    <svg viewBox="0 0 ${size} ${size}" aria-label="Spending by category">
      <circle cx="${cx}" cy="${cx}" r="${r - thick}" fill="transparent" stroke="none" data-action="slice-clear"/>
      ${arcs.join('')}
    </svg>
    <div class="donut-center">${center}</div>
  </div>`;
}

function totalCard(s) {
  return card(`
    <h3 class="h-small">${W.spentThisMonth}</h3>
    <p class="total">${format(s.totalCents)}</p>
    ${s.paidTotalCents !== s.totalCents ? `<p class="small soft">Your share. Full bills came to ${format(s.paidTotalCents)}.</p>` : ''}
    ${s.totalGoal ? `${goalBar(s.totalGoal)}<span class="small">${esc(describeGoal(s.totalGoal, W))}</span>` : ''}`);
}

/** [period] is what a tapped category opens for: the month, the year, or "" for all time. */
function breakdownCard(s, period = '') {
  const rows = s.categories.map((c) => {
    const cat = category(c.category);
    const dim = (c.spentCents === 0 && !c.goal) || (ui.highlighted && ui.highlighted !== c.category);
    return `<button class="legend ${ui.highlighted === c.category ? 'glow' : ''} ${dim ? 'dim' : ''}" data-action="category" data-cat="${cat.id}" data-period="${period}">
      <span class="legend-row">
        ${emblem(cat.icon)}
        <span class="row-main"><span class="h-small">${esc(cat.label)}</span>${W.nicknames ? `<span class="row-sub italic">${esc(cat.nickname)}</span>` : ''}</span>
        <span class="row-amount"><b>${format(c.spentCents)}</b><small>${Math.round(c.shareOfMonth * 100)}%</small></span><span class="chev">${ICON.right}</span>
      </span>
      ${c.goal ? `<span class="legend-goal">${goalBar(c.goal, true)}<span class="small soft">${esc(describeGoal(c.goal, W))}</span></span>` : ''}
    </button>`;
  });
  return card(`
    <h3 class="h-medium">${W.whereWent}</h3>
    ${s.totalCents > 0 ? donut(s) : ''}
    ${rows.join('')}
    <p class="small soft">Tap a slice to highlight it, or a ${W.vault} to see its expenses ${periodWords(period)}.</p>`, 'tight');
}

function foodCard(food, period = '') {
  const bar = food.types.filter((t) => t.spentCents > 0)
    .map((t) => `<span style="flex:${t.spentCents};background:${foodType(t.type).color}"></span>`).join('');
  const rows = food.types.map((t) => {
    const f = foodType(t.type);
    const times = t.count === 1 ? '1 time' : `${t.count} times`;
    return `<button class="legend" data-action="category" data-cat="ENTERTAINMENT" data-food="${f.id}" data-period="${period}"><span class="legend-row">
      ${emblem(f.icon)}
      <span class="row-main"><span class="h-small">${f.label}</span><span class="row-sub">${W.nicknames ? `${f.nickname} · ` : ''}${times}</span></span>
      <span class="row-amount"><b>${format(t.spentCents)}</b><small>${Math.round(t.shareOfFood * 100)}%</small></span><span class="chev">${ICON.right}</span>
    </span></button>`;
  });
  return card(`
    <div class="split-head"><h3 class="h-medium">${esc(W.food)}</h3><b>${format(food.totalCents)}</b></div>
    <div class="stack-bar">${bar}</div>
    ${rows.join('')}
    ${food.otherEntertainmentCents > 0 ? `<p class="small soft">Other entertainment (not food): ${format(food.otherEntertainmentCents)}</p>` : ''}`, 'tight');
}

function calendarCard() {
  const totals = dailyTotals(data.expenses, ui.month);
  const max = Math.max(0, ...Object.values(totals));
  const cells = calendarCells(ui.month).map((d) => {
    if (!d) return '<span></span>';
    const spent = totals[d] ?? 0;
    const heat = spent > 0 ? 0.15 + 0.6 * (spent / max) : 0;
    const cls = ['day', spent > 0 ? 'spent' : '', heat > 0.5 ? 'strong' : '', d === today() ? 'today' : '', d === ui.day ? 'selected' : ''].join(' ');
    return `<button class="${cls}" style="--heat:${heat.toFixed(2)}" data-action="day" data-day="${d}">
      <span>${Number(d.slice(8))}</span>${spent > 0 ? `<small>${compact(spent)}</small>` : ''}</button>`;
  });
  return card(`<div class="cal">${['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'].map((w) => `<span class="wd">${w}</span>`).join('')}${cells.join('')}</div>`);
}

/** On the current month's Ledger: last month in one line, until dismissed. */
function recapCard() {
  if (ui.month !== today().slice(0, 7)) return '';
  const month = recapMonthFor(today());
  if (data.recapDismissed === month) return '';
  const r = monthRecap(data, month);
  if (!r) return '';
  return card(`<h3 class="h-medium card-title">${emblem(EMBLEMS.reminder, 36)}${esc(W.recapHeading)}</h3>
    <p>${esc(recapText(r, W))}</p>
    <div class="ward-actions"><button class="link" data-action="recap-open" data-month="${month}">Open ${fmt(`${month}-01`, { month: 'long' })} ›</button>
      <span class="spacer"></span><button class="link" data-action="recap-dismiss" data-month="${month}">Dismiss</button></div>`, 'recap');
}

/** The newest few expenses of the month, with a link to show them all (or only the latest again). */
function ledgerList(expenses) {
  // The latest few here; the month's full list (with or without trips) is its own page.
  const more = `<button class="link more-link" data-action="go" data-to="#/all/${ui.month}">${expenses.length > LATEST_COUNT ? `Show all ${expenses.length} ›` : 'Open the full list ›'}</button>`;
  return `<div class="list">${ledgerPreview(expenses, false).map((e) => expenseRow(e)).join('')}</div>${more}`;
}

/** One day on a month's full list: a header with its date, count and total; tapped, it opens to its expenses. */
function listDay(d) {
  const open = ui.openDays.has(d.date);
  const count = d.expenses.length === 1 ? '1 expense' : `${d.expenses.length} expenses`;
  return `<button class="group-head day-toggle ${open ? 'open' : ''}" data-action="list-day" data-day="${d.date}" aria-expanded="${open}">
      <span class="day-title"><span class="h-small">${fmt(d.date, { weekday: 'short', day: 'numeric', month: 'short' })}</span><small class="soft">${count}</small></span>
      <span class="day-sum"><b>${format(d.totalCents)}</b><span class="chev">${ICON.right}</span></span>
    </button>
    ${open ? `<div class="list">${d.expenses.map((e) => expenseRow(e)).join('')}</div>` : ''}`;
}

/**
 * The full list's By vault card: a line per category (amount and share), Entertainment's food types under it,
 * for the expenses shown (with or without trips). Its heading folds it to one line. Rows open the category
 * for that month, keeping the trips choice.
 */
function listCategoriesCard(expenses, month, trips) {
  const lines = categoryTotals(expenses);
  if (!lines.length) return '';
  const open = ui.listCatsOpen;
  const total = lines.reduce((s, c) => s + c.cents, 0);
  const pct = (x) => `${Math.round(x * 100)}%`;
  const link = (cat, food = '') => `data-action="category" data-cat="${cat}" data-food="${food}" data-period="${month}" data-trips="${trips}"`;
  const top = lines[0];
  const summary = `${esc(category(top.category).label)} ${pct(top.share)} · ${lines.length === 1 ? `1 ${W.vault}` : `${lines.length} ${W.vaults}`}`;
  const rows = lines.map((c) => {
    const cat = category(c.category);
    const subs = [
      ...c.food.map((f) => `<button class="crow sub" ${link(c.category, f.type)}>${emblem(foodType(f.type).icon, 18)}<span class="name">${esc(foodType(f.type).label)}</span><span class="amt">${format(f.cents)}</span><span class="pct"></span></button>`),
      ...(c.otherCents ? [`<div class="crow sub"><span class="name">Other entertainment</span><span class="amt">${format(c.otherCents)}</span><span class="pct"></span></div>`] : []),
    ];
    return `<button class="crow" ${link(c.category)}>${emblem(cat.icon, 24)}<span class="name">${esc(cat.label)}</span><span class="amt">${format(c.cents)}</span><span class="pct">${pct(c.share)}</span><span class="chev">${ICON.right}</span></button>${subs.join('')}`;
  });
  return card(`<button class="cats-head ${open ? 'open' : ''}" data-action="list-cats" aria-expanded="${open}">
      <span class="cats-title"><span class="h-medium">${esc(W.byVault)}</span>${open ? '' : `<small class="soft">${summary}</small>`}</span>
      <span class="chev">${ICON.right}</span></button>
    ${open ? `<div class="thin-bar">${lines.map((c) => `<span style="flex:${c.cents};background:${category(c.category).color}"></span>`).join('')}</div>
      <div class="crows" data-total="${total}">${rows.join('')}</div>` : ''}`, 'list-cats');
}

/** A month's full list: by vault (folding), then the days (closed until tapped), with or without trips. */
function monthListScreen(month) {
  const m = /^\d{4}-\d{2}$/.test(month ?? '') ? month : ui.month;
  const all = data.expenses.filter((e) => monthOf(e.date) === m);
  const without = withoutTrips(all);
  const sum = (list) => list.reduce((s, e) => s + myShareCents(e), 0);
  const hasTrips = without.length !== all.length;
  const shown = hasTrips && ui.monthListTrips === 'without' ? without : all;
  const days = byDay(shown);
  const trips = hasTrips && ui.monthListTrips === 'without' ? 'without' : '';
  const toggle = hasTrips
    ? `<div class="segmented small-seg"><button class="${ui.monthListTrips !== 'without' ? 'on' : ''}" data-action="month-trips" data-v="with">With ${W.trips.toLowerCase()} · ${format(sum(all))}</button>
        <button class="${ui.monthListTrips === 'without' ? 'on' : ''}" data-action="month-trips" data-v="without">Without · ${format(sum(without))}</button></div>`
    : '';
  return `${header(monthTitle(m), { backBtn: true, icon: TAB_ICONS.month })}
  <main>
    ${card(`<span class="label">${esc(W.spentThisMonth)}</span>
      <p class="total">${format(sum(shown))}</p>
      <p class="small soft">${shown.length === 1 ? '1 expense' : `${shown.length} expenses`}${hasTrips && ui.monthListTrips === 'without' ? `, without ${W.trips.toLowerCase()}` : ''}</p>`)}
    ${toggle}
    ${listCategoriesCard(shown, m, trips)}
    ${days.length ? days.map(listDay).join('') : hint(W.ledgerEmpty)}
  </main>`;
}

/** Above the calendar: the month's total, how it compares with the month before, and three small facts. */
function glanceCard(g) {
  const monthName = fmt(`${ui.month}-01`, { month: 'long' });
  const before = g.upToDay != null
    ? `${fmt(`${g.previousMonth}-01`, { month: 'short' })} 1–${g.upToDay}`
    : fmt(`${g.previousMonth}-01`, { month: 'long' });
  const change = g.changePercent == null || g.totalCents === 0 ? ''
    : g.changePercent > 0 ? `<p class="up small">▲ ${g.changePercent}% more than ${before}</p>`
      : g.changePercent < 0 ? `<p class="down small">▼ ${-g.changePercent}% less than ${before}</p>`
        : `<p class="small soft">Same as ${before}</p>`;
  const tile = (value, label) => `<div class="glance-tile"><b>${value}</b><small>${label}</small></div>`;
  return card(`<h3 class="h-small">${esc(W.spentIn(monthName))}</h3>
    <p class="total">${format(g.totalCents)}</p>
    ${change}
    <div class="glance">
      ${tile(g.count, g.count === 1 ? 'expense' : 'expenses')}
      ${tile(format(g.dailyAverageCents), g.upToDay != null ? 'a day so far' : 'a day')}
      ${tile(g.busiest ? fmt(g.busiest.date, { weekday: 'short', day: 'numeric' }) : '–', g.busiest ? `busiest · ${format(g.busiest.cents)}` : 'busiest day')}
    </div>`);
}

/** On the web (no notifications), a banner after the reminder time when nothing was recorded today. */
function owlBanner() {
  if (nativeBridge || !settings.reminderOn || ui.month !== today().slice(0, 7)) return '';
  const now = new Date();
  if (`${pad(now.getHours())}:${pad(now.getMinutes())}` < settings.reminderTime) return '';
  if (data.expenses.some((e) => e.date === today())) return '';
  return `<button class="banner" data-action="quick">${esc(W.owlNothing)} Tap to record what you spent.</button>`;
}

function installBanner() {
  const ios = /iPhone|iPad|iPod/.test(navigator.userAgent);
  if (!ios || nativeBridge || navigator.standalone || storage.getItem('expensiarmus.installHintSeen')) return '';
  return `<div class="banner install">
    <span>✨ Install Expensiarmus: tap <b>Share</b> <span class="share-glyph">⬆︎</span> then <b>Add to Home Screen</b>.</span>
    <button class="icon-btn small" data-action="dismiss-install" aria-label="Dismiss">✕</button></div>`;
}

function monthSwitch(prev = 'prev-month', next = 'next-month', title = monthTitle(ui.month)) {
  return `<div class="month-switch">
      <button class="icon-btn gold" data-action="${prev}" aria-label="Previous">${ICON.left}</button>
      <h2 class="h-large">${title}</h2>
      <button class="icon-btn gold" data-action="${next}" aria-label="Next">${ICON.right}</button>
    </div>`;
}

/** On Annals → By year, only away from the current year: the Time-Turner brings you back to it. */
function yearTurner() {
  const thisYear = Number(today().slice(0, 4));
  if (ui.annals.view !== 'year' || ui.annals.year === thisYear) return '';
  const label = esc(W.thisMonth(String(thisYear)));
  return `<button class="icon-btn" data-action="this-year" aria-label="${label}" title="${label}"><span class="today-glyph"></span></button>`;
}

/** In the top bar, only away from the current month: a Time-Turner that brings you back to it. */
function timeTurner() {
  const thisMonth = today().slice(0, 7);
  if (ui.month === thisMonth) return '';
  const label = esc(W.thisMonth(monthTitle(thisMonth)));
  return `<button class="icon-btn" data-action="this-month" aria-label="${label}" title="${label}"><span class="today-glyph"></span></button>`;
}

/** The Ledger tab (overview of the month) and the Calendar tab. */
function monthScreen(calendar) {
  const s = monthlySummary(data, ui.month);
  const actions = `${timeTurner()}<button class="icon-btn" data-action="go" data-to="#/search" aria-label="${W.search}"><span class="search-glyph"></span></button>`;
  let body;
  if (!calendar) {
    body = `${recapCard()}${tripCard()}${totalCard(s)}
      ${breakdownCard(s, ui.month)}
      ${s.food.totalCents > 0 ? foodCard(s.food, ui.month) : ''}
      ${ornament(W.ledger)}
      ${s.expenses.length ? ledgerList(s.expenses) : hint(W.ledgerEmpty)}`;
  } else {
    const day = ui.day && monthOf(ui.day) === ui.month ? ui.day : null;
    const list = day ? onDay(data.expenses, day) : [];
    body = `${glanceCard(monthGlance(data.expenses, ui.month, today()))}
      ${calendarCard()}
      ${day
        ? `${ornament(fmt(day, { weekday: 'long', day: 'numeric', month: 'long' }))}
           <p class="center h-small">Spent: ${format(list.reduce((a, e) => a + myShareCents(e), 0))}</p>
           ${list.length ? `<div class="list">${list.map((e) => expenseRow(e)).join('')}</div>` : hint(W.dayEmpty)}`
        : hint(W.dayHint)}`;
  }
  return `${header('Expensiarmus', { actions, icon: EMBLEMS.logo })}
  <main class="with-tabs">
    ${installBanner()}${owlBanner()}
    ${monthSwitch()}
    ${body}
  </main>
  <button class="fab" data-action="quick"><span class="frame"><span class="frame-inner with-emblem"><span class="fab-glyph" aria-hidden="true"></span>${esc(W.fab)}</span></span></button>
  ${tabBar(calendar ? 'calendar' : 'month')}
  ${ui.quick ? quickSheet() : ''}`;
}

// ---------- Quick add ----------

/** Date for a new expense: the selected calendar day, today, or the 1st of the month being viewed. */
function newExpenseDate() {
  if (route().name === 'calendar' && ui.day && monthOf(ui.day) === ui.month) return ui.day;
  return ui.month === today().slice(0, 7) ? today() : `${ui.month}-01`;
}

const decimalKey = () => (settings.money === 'el' ? ',' : '.');
const quickAmountText = () => {
  const typed = ui.quick.amount || '0';
  return settings.money === 'el' ? `${typed} €` : `€${typed}`;
};

/** Quick add's favourites: tap one to fill in its title, category and split. They are edited in Settings → Favourites. */
function favRow(q) {
  const favs = data.favorites ?? [];
  if (!favs.length) return '';
  return `<div class="fav-head"><span class="h-small fav-title"><span class="star-on small-glyph"></span>Favourites</span></div>
    <div class="chips fav-chips">${favs.map((f, i) => `<button class="chip ${q.fav === i ? 'on' : ''}" data-action="fav-pick" data-i="${i}">${chipIcon(f.foodType ? foodType(f.foodType) : category(f.category))}${esc(f.title)}</button>`).join('')}</div>`;
}

/** The hint under the number pad: what is still missing, or what ✓ will record. */
function quickHelp(q) {
  if (q.error) return q.error;
  if (!q.cat) return 'Pick a category, then tap ✓ at the top';
  return `${q.food ? foodType(q.food).label : category(q.cat).label}${q.pct < 100 ? ` · your share ${q.pct}%` : ''} · tap ✓ to record`;
}

/**
 * Quick add: type the amount, pick a category tile (or a favourite), then tap the gold ✓ at the top.
 * Tiles only select, so you can change your mind before recording. The title is typed or comes
 * from a favourite; earlier titles are not suggested.
 */
function quickSheet() {
  const q = ui.quick;
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', decimalKey(), '0', '⌫'];
  const tiles = [
    ...CATEGORIES.map((c) => ({ cat: c.id, food: '', icon: c.icon, label: c.label })),
    ...FOOD_TYPES.map((f) => ({ cat: 'ENTERTAINMENT', food: f.id, icon: f.icon, label: f.label })),
  ];
  return `<div class="sheet-backdrop" data-action="quick-close"></div>
  <div class="sheet" role="dialog" aria-label="${esc(W.newExpense)}">
    <div class="sheet-head">
      <button class="link" data-action="quick-close">Cancel</button>
      <span class="h-small">${esc(W.newExpense)}</span>
      <button class="icon-btn" data-action="quick-record" aria-label="Record" title="Record"><span class="check-glyph"></span></button>
    </div>
    ${favRow(q)}
    <input class="quick-title" type="text" placeholder="Title (optional)" value="${esc(q.title)}" data-input="quick-title" autocapitalize="sentences">
    <div class="quick-amount" id="quick-amount">${esc(quickAmountText())}</div>
    <label class="quick-date">${emblem(TAB_ICONS.calendar, 24)}<input type="date" value="${q.date}" data-input="quick-date"></label>
    ${tripChips(q.trip, 'quick-trip')}
    <div class="numpad">${keys.map((k) => `<button data-action="quick-key" data-key="${k}">${k}</button>`).join('')}</div>
    <p class="small center ${q.error ? 'error' : 'soft'}" id="quick-help">${esc(quickHelp(q))}</p>
    <div class="tiles">${tiles.map((t) => `<button class="tile ${q.cat === t.cat && (q.food ?? '') === t.food ? 'glow' : ''}" data-action="quick-pick" data-cat="${t.cat}" data-food="${t.food}">
      ${emblem(t.icon, 32)}<span>${esc(t.label)}</span></button>`).join('')}</div>
    <button class="link center-link" data-action="quick-more">More details ›</button>
  </div>`;
}

function quickKey(key) {
  const q = ui.quick;
  let a = q.amount;
  if (key === '⌫') a = a.slice(0, -1);
  else if (key === '.' || key === ',') {
    if (!/[.,]/.test(a)) a = (a || '0') + key;
  } else {
    const dec = a.match(/[.,](\d*)$/);
    if (dec && dec[1].length >= 2) return;
    if (a.replace(/\D/g, '').length >= 8) return;
    a = a === '0' ? key : a + key;
  }
  q.amount = a;
  q.error = false;
  app.querySelector('#quick-amount').textContent = quickAmountText();
  const help = app.querySelector('#quick-help');
  help.className = 'small center soft';
  help.textContent = quickHelp(q);
}

/** Saves an expense from Quick add and offers Undo for a few seconds. */
function quickRecord(e) {
  commit(upsert(data, e));
  ui.quick = null;
  ui.month = monthOf(e.date);
  render();
  undoBar(`Recorded ${e.title} · ${format(myShareCents(e))}`, e.id);
}

function undoBar(text, id) {
  document.querySelectorAll('.undo-bar').forEach((x) => x.remove());
  const bar = document.createElement('div');
  bar.className = 'undo-bar';
  bar.innerHTML = `<span>${esc(text)}</span><button>Undo</button>`;
  bar.querySelector('button').addEventListener('click', () => {
    commit(remove(data, id));
    bar.remove();
    render();
  });
  document.body.append(bar);
  setTimeout(() => bar.remove(), 5000);
}

// ---------- add / edit ----------

function startDraft(id) {
  const e = id ? find(data, id) : null;
  const p = !e ? prefill : null;
  prefill = null;
  return {
    key: id ?? 'new',
    id: e?.id ?? null,
    title: e?.title ?? p?.title ?? '',
    amount: e ? forInput(e.amountCents) : p?.amount ?? '',
    date: e?.date ?? p?.date ?? newExpenseDate(),
    category: e?.category ?? p?.category ?? 'SUPERMARKET',
    foodType: e?.foodType ?? p?.foodType ?? null,
    split: e ? isSplit(e) : (p?.myPercent ?? 100) < 100,
    // A new expense is tied to the trip that is on today (or the one Quick add picked).
    tripId: e ? e.tripId ?? null : p && 'tripId' in p ? p.tripId : ongoingTrip(data.trips, today())?.id ?? null,
    myPercent: e && isSplit(e) ? e.myPercent : (p?.myPercent ?? 100) < 100 ? p.myPercent : 50,
    showErrors: false,
  };
}

function shareLine() {
  const cents = parseToCents(draft.amount);
  if (!draft.split || !cents) return '';
  return `You pay ${format(Math.floor((cents * draft.myPercent + 50) / 100))} of ${format(cents)}`;
}

/** Apparate's confirmation: what the file holds, what is new, and which rows could not be read. */
function importModal(r) {
  const n = r.added.length;
  const lines = importSummary(r).map((line) => `<p class="${line.startsWith('Line ') ? 'small error' : line.startsWith('…') ? 'small soft' : ''}">${esc(line)}</p>`).join('');
  const actions = n > 0
    ? `<button class="link" data-action="import-cancel">Cancel</button><button class="btn" data-action="import-confirm"><span class="frame"><span class="frame-inner">${n === 1 ? 'Add 1 expense' : `Add ${n} expenses`}</span></span></button>`
    : '<button class="btn" data-action="import-cancel"><span class="frame"><span class="frame-inner">OK</span></span></button>';
  return `<div class="modal-backdrop"><div class="card modal"><div class="paper">
      <h3 class="h-medium">${esc(W.importTitle)}</h3>
      <div class="import-summary">${lines}</div>
      <div class="modal-actions">${actions}</div>
    </div></div></div>`;
}

/** Reads the picked CSV and opens the confirmation. Nothing is added yet. */
async function readImport(input) {
  const file = input.files?.[0];
  input.value = ''; // picking the same file again should work too
  if (!file) return;
  try {
    ui.importing = importCsv(await file.text(), data.expenses, newId, data.trips ?? []);
  } catch {
    toast('That file could not be read.');
    return;
  }
  render();
}

function deleteModal(title, id = '') {
  return `<div class="modal-backdrop"><div class="card modal"><div class="paper">
      <h3 class="h-medium">${esc(W.deleteTitle)}</h3>
      <p>${esc(W.deleteBody(title))}</p>
      <div class="modal-actions"><button class="link" data-action="cancel-delete">${esc(W.keep)}</button><button class="btn danger" data-action="confirm-delete" data-id="${esc(id)}"><span class="frame"><span class="frame-inner">${esc(W.deleteBtn)}</span></span></button></div>
    </div></div></div>`;
}

function editScreen(id) {
  if (!draft || draft.key !== (id ?? 'new')) draft = startDraft(id);
  const d = draft;
  const cents = parseToCents(d.amount);
  const titleBad = d.showErrors && !d.title.trim();
  const amountBad = d.showErrors && !(cents > 0);
  const cat = category(d.category);
  const catChips = CATEGORIES.map((c) =>
    `<button class="chip ${d.category === c.id ? 'on' : ''}" data-action="edit-cat" data-cat="${c.id}">${chipIcon(c)}${c.label}</button>`).join('');
  const foodChips = d.category === 'ENTERTAINMENT'
    ? `<div class="field"><span class="h-small">Is it food?</span><div class="chips">
        <button class="chip ${!d.foodType ? 'on' : ''}" data-action="edit-food" data-food="">Not food</button>
        ${FOOD_TYPES.map((f) => `<button class="chip ${d.foodType === f.id ? 'on' : ''}" data-action="edit-food" data-food="${f.id}">${chipIcon(f)}${f.label}</button>`).join('')}
      </div><p class="small soft">${esc(d.foodType ? withNickname(foodType(d.foodType)) : 'Cinema, theatre, concerts, etc.')}</p></div>`
    : '';
  const split = d.split
    ? `<div class="field">
        <p id="pct-label">My share: ${d.myPercent}% · Theirs: ${100 - d.myPercent}%</p>
        <input type="range" min="1" max="99" step="1" value="${d.myPercent}" data-input="edit-pct">
        <div class="chips">${[50, 33, 25].map((p) => `<button class="chip" data-action="edit-pct" data-pct="${p}">${p === 50 ? '50-50' : `${p}%`}</button>`).join('')}</div>
        <p id="share-line">${shareLine()}</p>
      </div>`
    : '';
  // Saving is the gold ✓ in the top bar, like Quick add's; deleting lives on the Ledger entry page.
  const check = `<button class="icon-btn" data-action="save" aria-label="${esc(W.save)}" title="${esc(W.save)}"><span class="check-glyph"></span></button>`;
  return `${header(d.id ? W.editExpense : W.newExpense, { backBtn: true, actions: check })}
  <main class="form">
    ${!d.id && data.favorites?.length ? `<div class="field"><span class="h-small fav-title"><span class="star-on small-glyph"></span>Favourites</span>
      <div class="chips">${data.favorites.map((f, i) => `<button class="chip" data-action="edit-fav" data-i="${i}">${chipIcon(f.foodType ? foodType(f.foodType) : category(f.category))}${esc(f.title)}</button>`).join('')}</div></div>` : ''}
    <div class="field"><label class="label" for="edit-title">Title</label>
      <input id="edit-title" type="text" value="${esc(d.title)}" placeholder="e.g. Butterbeer with friends" data-input="edit-title" class="${titleBad ? 'bad' : ''}" autocapitalize="sentences" autocomplete="off">
      ${titleBad ? '<span class="error">Enter a title</span>' : ''}</div>
    <label class="field"><span class="label">${esc(W.amount)}</span>
      <span class="money-input ${amountBad ? 'bad' : ''}"><span>€</span><input type="text" inputmode="decimal" value="${esc(d.amount)}" placeholder="0${decimalKey()}00" data-input="edit-amount"></span>
      ${amountBad ? `<span class="error">Enter an amount, e.g. 12${decimalKey()}50</span>` : ''}</label>
    <label class="field"><span class="label">Date</span>
      <input type="date" value="${d.date}" data-input="edit-date"></label>
    ${tripChips(d.tripId, 'edit-trip')}
    <div class="field"><span class="h-small">Category</span><div class="chips">${catChips}</div>
      <p class="small soft">${esc(withNickname(cat))}</p></div>
    ${foodChips}
    <label class="switch-row"><span><span class="h-small">${esc(W.split)}</span><span class="small soft">Only your share counts toward totals and goals</span></span>
      <input type="checkbox" class="switch" ${d.split ? 'checked' : ''} data-input="edit-split"></label>
    ${split}
  </main>`;
}

/** Fills the form from a suggestion: amount, category, food type and split. */
function saveDraft() {
  const d = draft;
  const cents = parseToCents(d.amount);
  if (!d.title.trim() || !(cents > 0) || !d.date) {
    d.showErrors = true;
    render();
    return;
  }
  const e = makeExpense({
    id: d.id ?? newId(),
    date: d.date,
    title: d.title.trim(),
    amountCents: cents,
    category: d.category,
    myPercent: d.split ? Math.min(99, Math.max(1, d.myPercent)) : 100,
    foodType: d.category === 'ENTERTAINMENT' ? d.foodType : null,
    tripId: d.tripId ?? null,
  });
  commit(upsert(data, e));
  ui.month = monthOf(e.date);
  draft = null;
  back();
}

// ---------- read-only view ----------

function viewScreen(id) {
  const e = find(data, id);
  if (!e) {
    setTimeout(back, 0); // deleted from the edit screen: nothing to show
    return header(W.entry, { backBtn: true, icon: TAB_ICONS.month });
  }
  const c = category(e.category);
  const f = e.foodType ? foodType(e.foodType) : null;
  // A detail line; [icon] puts the painted emblem before the value (categories, food types, trips).
  const row = (label, value, note = '', icon = '') => `<div class="detail"><span class="h-small">${label}</span><span class="detail-value">${icon ? chipIcon({ icon }) : ''}${esc(value)}</span>${note ? `<span class="small soft italic">${esc(note)}</span>` : ''}</div>`;
  // Amend and delete are the labelled buttons at the bottom; the top bar only has Back.
  return `${header(W.entry, { backBtn: true, icon: TAB_ICONS.month })}
  <main>
    ${card(`<div class="legend-row">${emblem(f ? f.icon : c.icon, 52)}
        <span class="row-main"><span class="h-large">${esc(e.title)}</span>
          <span class="total">${format(myShareCents(e))}</span>
          ${isSplit(e) ? `<span class="small soft">of ${format(e.amountCents)} in full</span>` : ''}</span>
        ${favStar(e)}</div>
      <hr class="gold-rule">
      ${row('Date', fmt(e.date, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }))}
      ${row('Category', c.label, W.nicknames ? c.nickname : '', c.icon)}
      ${f ? row('Food', f.label, W.nicknames ? f.nickname : '', f.icon) : ''}
      ${row('Split', splitLine(e, format))}
      ${e.tripId && (data.trips ?? []).some((t) => t.id === e.tripId) ? row(W.trip, data.trips.find((t) => t.id === e.tripId).name, '', EMBLEMS.trip) : ''}`)}
    <button class="btn wide" data-action="amend" data-id="${esc(e.id)}"><span class="frame"><span class="frame-inner">${esc(W.amend)}</span></span></button>
    <button class="link center-link" data-action="ask-delete">${esc(W.deleteLink)}</button>
  </main>${ui.modal ? deleteModal(e.title, e.id) : ''}`;
}

/** The star on an expense: a favourite keeps its title, category and split for Quick add. */
function favStar(e) {
  const on = isFavorite(data.favorites, e);
  const label = on ? 'Remove from favourites' : 'Add to favourites';
  return `<button class="icon-btn fav-star" data-action="fav-toggle" data-id="${esc(e.id)}" aria-label="${label}" title="${label}"><span class="${on ? 'star-on' : 'star-off'}"></span></button>`;
}

// ---------- trips ----------

const tripDates = (t) => (t.start === t.end
  ? fmt(t.start, { day: 'numeric', month: 'short', year: 'numeric' })
  : `${fmt(t.start, { day: 'numeric', month: 'short' })} – ${fmt(t.end, { day: 'numeric', month: 'short', year: 'numeric' })}`);
function tripStatusText(st) {
  if (st.state === 'upcoming') return st.inDays === 1 ? 'tomorrow' : `in ${st.inDays} days`;
  if (st.state === 'ongoing') return `day ${st.day} of ${st.days}`;
  return st.days === 1 ? '1 day' : `${st.days} days`;
}

/** Trip chips for recording: the trips not over yet, the one on today ticked; tap again to untick. */
function tripChips(current, action) {
  const offer = tripsToOffer(data.trips, today());
  const extra = current && !offer.some((t) => t.id === current) ? (data.trips ?? []).filter((t) => t.id === current) : [];
  const trips = [...offer, ...extra];
  if (!trips.length) return '';
  return `<div class="chips trip-chips"><span class="h-small trip-label">${chipIcon({ icon: EMBLEMS.trip })}${esc(W.trip)}</span>${trips.map((t) =>
    `<button class="chip ${current === t.id ? 'on' : ''}" data-action="${action}" data-id="${esc(t.id)}">${esc(t.name)}</button>`).join('')}</div>`;
}

/** On the current month's Ledger, while a trip is on: where it stands. */
function tripCard() {
  if (ui.month !== today().slice(0, 7)) return '';
  const t = ongoingTrip(data.trips, today());
  if (!t) return '';
  const s = tripSummary(data, t.id, today());
  return card(`<button class="ward" data-action="go" data-to="#/trip/${esc(t.id)}"><span class="legend-row">${emblem(EMBLEMS.trip, 36)}
      <span class="row-main"><span class="h-small">${esc(t.name)}</span><span class="row-sub">${esc(tripStatusText(s.status))}</span></span>
      <b class="h-medium">${format(s.totalCents)}${s.budget ? `<small> of ${format(s.budget.goalCents)}</small>` : ''}</b><span class="chev">${ICON.right}</span></span>
    ${s.budget ? goalBar(s.budget, true) : ''}</button>`);
}

/** One trip: total against its budget, per day, per category, and its expenses. */
function tripScreen(id) {
  const s = tripSummary(data, id, today());
  if (!s) { setTimeout(back, 0); return header(W.trips, { backBtn: true }); }
  const t = s.trip;
  const cats = s.byCategory.map((c) => {
    const look = c.foodType ? foodType(c.foodType) : category(c.category);
    return `<div class="legend-row">${emblem(look.icon, 30)}<span class="row-main"><span class="h-small">${esc(c.label)}</span></span>
      <span class="row-amount"><b>${format(c.cents)}</b><small>${s.totalCents ? Math.round((c.cents * 100) / s.totalCents) : 0}%</small></span></div>`;
  }).join('');
  return `${header(t.name, { backBtn: true, icon: EMBLEMS.trip })}
  <main>
    ${card(`<span class="label">${esc(tripDates(t))} · ${esc(tripStatusText(s.status))}</span>
      <p class="total">${format(s.totalCents)}</p>
      ${s.budget ? `${goalBar(s.budget)}<span class="small">${esc(describeGoal(s.budget, W))}</span>` : '<p class="small soft">No budget set.</p>'}
      <p class="small soft">${s.count === 1 ? '1 expense' : `${s.count} expenses`} · ${format(s.perDayCents)} a day${s.status.state === 'ongoing' ? ' so far' : ''}</p>`)}
    ${s.count ? card(`<h3 class="h-medium">${esc(W.whereWent)}</h3><div class="trip-cats">${cats}</div>`) : ''}
    ${s.count ? `${ornament(W.ledger)}<div class="list">${s.expenses.map((e) => expenseRow(e, true)).join('')}</div>` : hint(`Nothing recorded for this ${W.trip.toLowerCase()} yet. Pick it when you record an expense.`)}
    <button class="link center-link" data-action="go" data-to="#/trips">Edit ${esc(W.trip.toLowerCase())} ›</button>
  </main>`;
}

/** Settings → Trips: add, change or delete trips (name, first and last day, optional budget). */
function tripsScreen() {
  const trips = data.trips ?? [];
  const ed = ui.tripEdit;
  const editor = () => `<div class="ward-editor">
      <label class="field"><span class="label">Name</span><input type="text" value="${esc(ed.name)}" data-input="te-name" placeholder="e.g. Crete 2027" autocapitalize="words" autocomplete="off"></label>
      <div class="two-dates"><label class="field"><span class="label">First day</span><input type="date" value="${ed.start}" data-input="te-start"></label>
        <label class="field"><span class="label">Last day</span><input type="date" value="${ed.end}" data-input="te-end"></label></div>
      <label class="field"><span class="label">Budget (optional)</span><span class="money-input"><span>€</span><input type="text" inputmode="decimal" value="${esc(ed.budget)}" placeholder="e.g. 1200" data-input="te-budget"></span></label>
      ${ed.error ? `<p class="error">${esc(ed.error)}</p>` : ''}
      ${ed.confirmDelete ? `<p>Delete ${esc(ed.name)}? Its expenses stay, no longer tied to it.</p>` : ''}
      <div class="ward-actions">
        ${ed.id && !ed.confirmDelete ? '<button class="link danger-link" data-action="te-delete">Delete</button>' : ''}
        ${ed.confirmDelete ? '<button class="link danger-link" data-action="te-delete-yes">Delete it</button>' : ''}
        <span class="spacer"></span>
        <button class="link" data-action="te-cancel">Cancel</button>
        ${ed.confirmDelete ? '' : '<button class="btn" data-action="te-save"><span class="frame"><span class="frame-inner">Save</span></span></button>'}
      </div></div>`;
  const cards = trips.map((t) => card(ed && ed.id === t.id ? editor() : `<button class="ward" data-action="te-open" data-id="${esc(t.id)}"><span class="legend-row">${emblem(EMBLEMS.trip, 36)}
      <span class="row-main"><span class="h-small">${esc(t.name)}</span><span class="row-sub">${esc(tripDates(t))}${t.budgetCents ? ` · budget ${format(t.budgetCents)}` : ''}</span></span><span class="chev">${ICON.right}</span></span></button>`));
  return `${header(W.trips, { backBtn: true, icon: EMBLEMS.trip })}
  <main>
    <p class="soft">Give each trip its dates and, if you like, one total budget. While a trip is on it is ticked when you record; its expenses keep their category and still count in their month. See them in Annals → ${esc(W.trips)}.</p>
    ${trips.length || ed ? '' : hint(`No ${W.trips.toLowerCase()} yet.`)}
    ${cards.join('')}
    ${ed && !ed.id ? card(`<h3 class="h-medium">${esc(W.newTrip)}</h3>${editor()}`) : ''}
    ${ed ? '' : `<button class="btn wide" data-action="te-add"><span class="frame"><span class="frame-inner with-emblem">${emblem(EMBLEMS.trip, 26)}${esc(W.newTrip)}</span></span></button>`}
  </main>`;
}

function saveTripEdit() {
  const ed = ui.tripEdit;
  const budget = ed.budget.trim() ? parseToCents(ed.budget) : null;
  if (ed.budget.trim() && budget == null) { ed.error = 'The budget must be an amount, or left empty.'; render(); return; }
  const r = saveTrip(data.trips, { id: ed.id ?? newId(), name: ed.name, start: ed.start, end: ed.end, budgetCents: budget });
  if (r.error) { ed.error = r.error; render(); return; }
  ui.tripEdit = null;
  commit({ ...data, trips: r.list });
  render();
}

// ---------- favourites ----------

const SHARES = [[100, 'Not split'], [50, '50-50'], [33, '33%'], [25, '25%']];

/** The Favourites page: up to five, each editable (title, category, split) or removable; add new ones here too. */
function favoritesScreen() {
  const favs = data.favorites ?? [];
  const ed = ui.favEdit;
  const editor = (f) => {
    const catChips = CATEGORIES.map((c) => `<button class="chip ${f.category === c.id ? 'on' : ''}" data-action="fe-cat" data-cat="${c.id}">${chipIcon(c)}${c.label}</button>`).join('');
    const foodChips = f.category === 'ENTERTAINMENT'
      ? `<span class="h-small">Is it food?</span><div class="chips"><button class="chip ${!f.foodType ? 'on' : ''}" data-action="fe-food" data-food="">Not food</button>${FOOD_TYPES.map((t) => `<button class="chip ${f.foodType === t.id ? 'on' : ''}" data-action="fe-food" data-food="${t.id}">${chipIcon(t)}${t.label}</button>`).join('')}</div>`
      : '';
    const shares = SHARES.some(([p]) => p === f.myPercent) ? SHARES : [...SHARES, [f.myPercent, `${f.myPercent}%`]];
    return `<div class="ward-editor">
      <label class="field"><span class="label">Title</span><input type="text" value="${esc(f.title)}" data-input="fe-title" autocapitalize="sentences" autocomplete="off"></label>
      <span class="h-small">Category</span><div class="chips">${catChips}</div>
      ${foodChips}
      <span class="h-small">Your share</span><div class="chips">${shares.map(([p, l]) => `<button class="chip ${f.myPercent === p ? 'on' : ''}" data-action="fe-pct" data-pct="${p}">${l}</button>`).join('')}</div>
      ${ed.error ? `<p class="error">${esc(ed.error)}</p>` : ''}
      <div class="ward-actions">
        ${ed.index != null ? '<button class="link danger-link" data-action="fe-remove">Remove</button>' : ''}
        <span class="spacer"></span>
        <button class="link" data-action="fe-cancel">Cancel</button>
        <button class="btn" data-action="fe-save"><span class="frame"><span class="frame-inner">Save</span></span></button>
      </div></div>`;
  };
  const cards = favs.map((f, i) => {
    const look = f.foodType ? foodType(f.foodType) : category(f.category);
    const sub = [look.label, f.myPercent < 100 ? `your share ${f.myPercent}%` : ''].filter(Boolean).join(' · ');
    return card(ed && ed.index === i ? editor(ed) : `<button class="ward" data-action="fe-open" data-i="${i}"><span class="legend-row">${emblem(look.icon, 36)}
        <span class="row-main"><span class="h-small">${esc(f.title)}</span><span class="row-sub">${esc(sub)}</span></span><span class="chev">${ICON.right}</span></span></button>`);
  });
  const adding = ed && ed.index == null
    ? card(`<h3 class="h-medium">A new favourite</h3>${editor(ed)}`)
    : favs.length < MAX_FAVORITES && !ed
      ? `<button class="btn wide" data-action="fe-add"><span class="frame"><span class="frame-inner with-emblem"><span class="star-on title-glyph"></span>Add a favourite</span></span></button>`
      : '';
  return `${header('Favourites', { backBtn: true, glyph: '<span class="star-on"></span>' })}
  <main>
    <p class="soft">Up to ${MAX_FAVORITES}. In ${esc(W.newExpense)}, tap one to fill in its title, category and split, then type the amount. You can also star any expense on its page.</p>
    ${favs.length || ed ? '' : hint('No favourites yet.')}
    ${cards.join('')}
    ${adding}
  </main>`;
}

function saveFavoriteEdit() {
  const ed = ui.favEdit;
  const r = saveFavorite(data.favorites, ed.index, ed);
  if (r.error) { ed.error = r.error; render(); return; }
  ui.favEdit = null;
  commit({ ...data, favorites: r.list });
  render();
}

// ---------- category ----------

/** "this month" / "in 2026" / "of all time": what a category opened from that card will list. */
const periodWords = (period) => (!period ? 'of all time' : period.length === 7 ? 'for this month' : `in ${period}`);
const periodTitle = (period) => (!period ? 'All time' : period.length === 7 ? monthTitle(period) : period);
/** The address of a category page, keeping the food type and the period when there are any. */
const categoryPath = (cat, food, period) => `#/category/${cat}${food || period ? `/${food ?? ''}` : ''}${period ? `/${period}` : ''}`;

/** A category's expenses for the period it was opened from (a month, a year, or all time), grouped by month. */
function categoryScreen(catId, foodId, period = '') {
  const cat = category(catId) ?? CATEGORIES[0];
  const all = byCategory(inPeriod(data.expenses, period), cat.id, foodId);
  const without = byCategory(withoutTrips(inPeriod(data.expenses, period)), cat.id, foodId);
  const sum = (gs) => gs.reduce((a, g) => a + g.totalCents, 0);
  const hasTrips = sum(all) !== sum(without) || all.some((g) => g.expenses.some((e) => e.tripId));
  const groups = hasTrips && ui.catTrips === 'without' ? without : all;
  const tripFilter = hasTrips
    ? `<div class="segmented small-seg"><button class="${ui.catTrips !== 'without' ? 'on' : ''}" data-action="cat-trips" data-v="with">With ${W.trips.toLowerCase()} · ${format(sum(all))}</button>
        <button class="${ui.catTrips === 'without' ? 'on' : ''}" data-action="cat-trips" data-v="without">Without · ${format(sum(without))}</button></div>`
    : '';
  const count = groups.reduce((a, g) => a + g.expenses.length, 0);
  const foodChips = cat.id === 'ENTERTAINMENT'
    ? `<div class="chips">
        <button class="chip ${!foodId ? 'on' : ''}" data-action="cat-food" data-food="">All</button>
        ${FOOD_TYPES.map((f) => `<button class="chip ${foodId === f.id ? 'on' : ''}" data-action="cat-food" data-food="${f.id}">${chipIcon(f)}${f.label}</button>`).join('')}
      </div>`
    : '';
  return `${header(cat.label, { backBtn: true, icon: (foodId ? foodType(foodId) : cat).icon })}
  <main>
    ${card(`<div class="legend-row">${emblem(cat.icon, 44)}
        <span class="row-main">${W.nicknames ? `<span class="h-small italic">${esc(cat.nickname)}</span>` : ''}<span>${esc(cat.hint)}</span></span></div>
      <span class="label">${esc(periodTitle(period))}${foodId ? ` · ${foodType(foodId).label}` : ''}</span>
      <p class="total">${format(groups.reduce((a, g) => a + g.totalCents, 0))}</p>
      <p class="small soft">${count === 1 ? '1 expense' : `${count} expenses`}${period.length === 7 ? '' : ` in ${groups.length === 1 ? '1 month' : `${groups.length} months`}`}</p>`)}
    ${tripFilter}
    ${foodChips}
    ${groups.length ? '' : hint(W.categoryEmpty)}
    ${groups.map((g) => `<div class="group-head"><span class="h-small">${monthTitle(g.month)}</span><b>${format(g.totalCents)}</b></div>
      <div class="list">${g.expenses.map((e) => expenseRow(e)).join('')}</div>`).join('')}
  </main>`;
}

// ---------- search ----------

function searchResults() {
  if (!finder.q.trim() && finder.cats.length === 0) return hint(W.searchHint);
  const found = search(data.expenses, finder.q, finder.cats);
  return `<p class="h-small">${found.length === 1 ? '1 expense' : `${found.length} expenses`} · ${format(found.reduce((a, e) => a + myShareCents(e), 0))}</p>
    ${found.length ? `<div class="list">${found.map((e) => expenseRow(e, true)).join('')}</div>` : hint(W.searchNothing)}`;
}

function searchScreen() {
  return `${header(W.search, { backBtn: true, glyph: '<span class="search-glyph"></span>' })}
  <main>
    <label class="search-box">${ICON.search}<input type="search" value="${esc(finder.q)}" placeholder="${esc(W.searchPlaceholder)}" data-input="search-q" enterkeyhint="search"></label>
    <div class="chips">${CATEGORIES.map((c) => `<button class="chip ${finder.cats.includes(c.id) ? 'on' : ''}" data-action="search-cat" data-cat="${c.id}">${chipIcon(c)}${c.label}</button>`).join('')}</div>
    <div id="results">${searchResults()}</div>
  </main>`;
}

// ---------- goals (Protego tab) ----------

function wardEditor(m, fixed, choices, ed) {
  const name = monthName(m);
  const picker = fixed ? '' : `<span class="h-small">${esc(W.whichCategory)}</span>
    <div class="chips">${choices.map((c) => `<button class="chip ${ed.picked === c.id ? 'on' : ''}" data-action="ward-pick" data-cat="${c.id}">${chipIcon(c)}${esc(c.label ?? W.wholeMonth)}</button>`).join('')}</div>`;
  return `<div class="ward-editor">
    ${picker}
    <label class="field"><span class="label">Goal for ${name}</span>
      <span class="money-input"><span>€</span><input type="text" inputmode="decimal" value="${esc(ed.amount)}" placeholder="e.g. 60" data-input="ward-amount"></span>
      <span class="error" id="ward-error" hidden>Enter an amount, e.g. 60</span></label>
    <label class="switch-row"><span><span class="h-small">Every month from ${name} on</span>
      <span class="small soft" id="ward-scope">${ed.onwards ? `Also for the months after ${name}` : `Only ${monthTitle(m)}`}</span></span>
      <input type="checkbox" class="switch" ${ed.onwards ? 'checked' : ''} data-input="ward-onwards"></label>
    <div class="ward-actions">
      ${fixed ? '<button class="link danger-link" data-action="ward-remove">Remove</button>' : ''}
      <span class="spacer"></span>
      <button class="link" data-action="ward-cancel">Cancel</button>
      <button class="btn" data-action="ward-save"><span class="frame"><span class="frame-inner">Save</span></span></button>
    </div>
  </div>`;
}

function goalsScreen() {
  const m = ui.month;
  const s = monthlySummary(data, m);
  const ed = ui.goalEdit;
  // The whole month comes first, then one ward per category.
  const whole = { ...WHOLE_MONTH, label: W.wholeMonth };
  const vaults = [whole, ...CATEGORIES];
  const spentOn = (id) => (id === 'TOTAL' ? s.totalCents : s.categories.find((x) => x.category === id).spentCents);
  const active = vaults.map((c) => ({ c, entry: goalFor(data.goals, goalKey(c.id), m) })).filter((x) => x.entry);
  const free = vaults.filter((c) => !active.some((a) => a.c.id === c.id));
  const wards = active.map(({ c, entry }) => {
    const g = goalProgress(entry.cents, spentOn(c.id));
    const sub = c.id === 'TOTAL' ? `${W.wholeMonthSub} · ${scopeText(entry, m)}` : scopeText(entry, m);
    return card(`<button class="ward" data-action="ward-edit" data-cat="${c.id}">
        <span class="legend-row">${emblem(c.icon, 36)}
          <span class="row-main"><span class="h-small">${esc(c.label)}</span><span class="row-sub italic">${esc(sub)}</span></span>
          <b class="h-medium">${format(entry.cents)}</b></span>
        ${goalBar(g)}<span class="small">${esc(describeGoal(g, W))}</span>
      </button>
      ${ed && ed.cat === c.id ? wardEditor(m, true, [], ed) : ''}`);
  });
  const adding = ed && ed.adding
    ? card(`<h3 class="h-medium">${esc(W.aNewWard)}</h3>${wardEditor(m, false, free, ed)}`)
    : free.length ? `<button class="btn wide" data-action="ward-add"><span class="frame"><span class="frame-inner with-emblem">${emblem(TAB_ICONS.goals, 26)}${esc(withoutEmoji(W.newWard))}</span></span></button>` : '';
  const name = monthName(m);
  const modal = ed && ed.confirmRemove
    ? `<div class="modal-backdrop"><div class="card modal"><div class="paper">
        <h3 class="h-medium">${esc(W.removeWard)}</h3>
        <p>Remove it for ${name} only, or for ${name} and every month after?</p>
        <div class="modal-actions"><button class="link" data-action="ward-remove-cancel">${esc(W.keep)}</button>
          <button class="link" data-action="ward-remove-one">Only ${name}</button>
          <button class="btn danger" data-action="ward-remove-on"><span class="frame"><span class="frame-inner">From ${name} on</span></span></button></div>
      </div></div></div>`
    : '';
  return `${header(`${W.goals} · ${name}`, { icon: TAB_ICONS.goals, actions: timeTurner() })}
  <main class="with-tabs">
    ${monthSwitch()}
    <p class="soft">${esc(W.goalsIntro(monthTitle(m)))}</p>
    ${active.length || (ed && ed.adding) ? '' : hint(W.noGoals)}
    ${wards.join('')}
    ${adding}
  </main>${tabBar('goals')}${modal}`;
}

function saveWard() {
  const ed = ui.goalEdit;
  const cents = parseToCents(ed.amount);
  const cat = ed.cat ?? ed.picked;
  if (!(cents > 0) || !cat) {
    app.querySelector('#ward-error').hidden = false;
    return;
  }
  ui.goalEdit = null;
  commit({ ...data, goals: setGoal(data.goals, goalKey(cat), ui.month, cents, ed.onwards) });
  render();
}

function removeWard(onwards) {
  const cat = ui.goalEdit.cat;
  ui.goalEdit = null;
  commit({ ...data, goals: removeGoal(data.goals, goalKey(cat), ui.month, onwards) });
  render();
}

// ---------- settings ----------

function choice(action, value, current, label) {
  return `<button class="chip ${current === value ? 'on' : ''}" data-action="${action}" data-v="${value}">${label}</button>`;
}

function settingsScreen() {
  const denied = window.expensiarmusOwlStatus === 'denied';
  const t = settings.reminderTime;
  const reminderHelp = nativeBridge
    ? `${denied ? '<p class="error">Notifications are switched off for Expensiarmus. Turn them on in iPhone Settings → Notifications → Expensiarmus.</p>' : ''}
       <p class="small soft">Every day at ${t}: <i>"${esc(W.owlNothing)}"</i>, or the day's count and total. Tap it to record an expense.</p>`
    : `<p class="small soft">A web app can't send scheduled notifications on iPhone, so let the <b>Reminders</b> app do it: add a reminder <i>"Record today's expenses in Expensiarmus"</i>, tap <b>ⓘ</b>, turn on <b>Date</b> and <b>Time</b>, set <b>${t}</b>, <b>Repeat: Daily</b>. After ${t} the app also shows a banner if nothing was recorded that day.</p>`;
  return `${header(W.settings, { icon: TAB_ICONS.settings })}
  <main class="with-tabs">
    ${card(`<h3 class="h-medium card-title"><span class="star-on title-glyph"></span>Favourites</h3>
      <p class="small soft">Up to ${MAX_FAVORITES} expenses you record often, ready in ${esc(W.newExpense)}: their title, category and split are filled in, you type the amount.</p>
      <button class="btn" data-action="go" data-to="#/favorites"><span class="frame"><span class="frame-inner">${(data.favorites ?? []).length ? 'Edit favourites' : 'Add favourites'}</span></span></button>`)}
    ${card(`<h3 class="h-medium card-title">${emblem(EMBLEMS.trip, 36)}${esc(W.trips)}</h3>
      <p class="small soft">Trips with their dates and an optional budget. Their expenses keep their category and month; see each trip in Annals → ${esc(W.trips)}.</p>
      <button class="btn" data-action="go" data-to="#/trips"><span class="frame"><span class="frame-inner">${(data.trips ?? []).length ? `Edit ${W.trips.toLowerCase()}` : esc(W.newTrip)}</span></span></button>`)}
    ${card(`<h3 class="h-medium card-title">${emblem(EMBLEMS.reminder, 36)}${esc(withoutEmoji(W.reminder))}</h3>
      <label class="switch-row"><span><span class="h-small">${esc(W.reminderSwitch)}</span><span class="small soft">A daily nudge to record the day's expenses</span></span>
        <input type="checkbox" class="switch" ${settings.reminderOn ? 'checked' : ''} data-input="set-reminder"></label>
      <label class="field"><span class="label">Time</span><input type="time" value="${t}" data-input="set-time"></label>
      ${reminderHelp}`)}
    ${card(`<h3 class="h-medium">Wizard words</h3>
      <label class="switch-row"><span><span class="h-small">Use wizard words</span>
        <span class="small soft">Protego, Accio, Evanesco… Off: Goals, Search, Delete.</span></span>
        <input type="checkbox" class="switch" ${settings.wizard ? 'checked' : ''} data-input="set-wizard"></label>`)}
    ${card(`<h3 class="h-medium">Number format</h3>
      <div class="chips">${choice('set-money', 'en', settings.money, '€1,234.56')}${choice('set-money', 'el', settings.money, '1.234,56 €')}</div>`)}
    ${card(`<h3 class="h-medium">Theme</h3>
      <div class="chips">${choice('set-theme', 'system', settings.theme, `${chipIcon({ icon: EMBLEMS.themeSystem })}Like the phone`)}${choice('set-theme', 'light', settings.theme, `${chipIcon({ icon: EMBLEMS.themeLight })}Light`)}${choice('set-theme', 'dark', settings.theme, `${chipIcon({ icon: EMBLEMS.themeDark })}Dark`)}</div>`)}
    ${card(`<h3 class="h-medium card-title">${emblem(EMBLEMS.geminio, 36)}${esc(withoutEmoji(W.exportTitle))}</h3>
      <p class="small soft">All your expenses as a CSV file, for a backup or a spreadsheet.</p>
      <button class="btn" data-action="export"><span class="frame"><span class="frame-inner">${esc(W.exportButton)}</span></span></button>`)}
    ${card(`<h3 class="h-medium card-title">${emblem(EMBLEMS.apparate, 36)}${esc(W.importTitle)}</h3>
      <p class="small soft">Bring expenses in from a CSV: a Geminio backup, or a spreadsheet with the same columns. You see what will be added first; expenses already in the ledger are skipped.</p>
      <button class="btn" data-action="import"><span class="frame"><span class="frame-inner">${esc(W.importButton)}</span></span></button>
      <input type="file" id="import-file" accept=".csv,text/csv,text/comma-separated-values,text/plain" data-input="import-file" hidden>`)}
  </main>${tabBar('settings')}${ui.importing ? importModal(ui.importing) : ''}`;
}

// ---------- annals (tab) ----------

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Vertical bars; tapping one selects it. */
function barChart(bars, selected) {
  const w = 300;
  const h = 150;
  const slot = w / bars.length;
  const bw = slot * 0.62;
  const max = Math.max(1, ...bars.map((b) => b.cents));
  const rects = bars.map((b, i) => {
    const bh = b.cents > 0 ? Math.max(2, (b.cents / max) * (h - 4)) : 0;
    const dim = selected != null && selected !== i;
    return `<rect x="${i * slot}" y="0" width="${slot}" height="${h}" fill="transparent" stroke="none" data-action="bar" data-index="${i}"/>
      ${bh ? `<rect class="bar ${selected === i ? 'on' : ''}" x="${(i * slot + (slot - bw) / 2).toFixed(1)}" y="${(h - bh).toFixed(1)}" width="${bw.toFixed(1)}" height="${bh.toFixed(1)}" rx="3" opacity="${dim ? 0.3 : 1}" data-action="bar" data-index="${i}"/>` : ''}`;
  });
  return `<div class="bars">
    <svg viewBox="0 0 ${w} ${h + 2}" preserveAspectRatio="none">${rects.join('')}<line x1="0" y1="${h + 1}" x2="${w}" y2="${h + 1}" class="baseline"/></svg>
    <div class="bar-labels">${bars.map((b, i) => `<span class="${selected === i ? 'on' : ''}">${esc(b.label)}</span>`).join('')}</div>
  </div>`;
}

function annalsTabs(a) {
  return `<div class="segmented">
      <button class="${a.view === 'year' ? 'on' : ''}" data-action="annals-view" data-view="year">${chipIcon({ icon: TAB_ICONS.calendar })}By year</button>
      <button class="${a.view === 'all' ? 'on' : ''}" data-action="annals-view" data-view="all">${chipIcon({ icon: TAB_ICONS.annals })}All time</button>
      <button class="${a.view === 'trips' ? 'on' : ''}" data-action="annals-view" data-view="trips">${chipIcon({ icon: EMBLEMS.trip })}${esc(W.trips)}</button>
    </div>`;
}

/** Annals → Trips: every trip with its dates, where it stands, what it cost and its budget. */
function annalsTrips(a) {
  const trips = data.trips ?? [];
  const cards = trips.map((t) => {
    const s = tripSummary(data, t.id, today());
    return card(`<button class="ward" data-action="go" data-to="#/trip/${esc(t.id)}"><span class="legend-row">${emblem(EMBLEMS.trip, 36)}
        <span class="row-main"><span class="h-small">${esc(t.name)}</span><span class="row-sub">${esc(tripDates(t))} · ${esc(tripStatusText(s.status))}</span></span>
        <b class="h-medium">${format(s.totalCents)}</b><span class="chev">${ICON.right}</span></span>
      ${s.budget ? `${goalBar(s.budget)}<span class="small">${esc(describeGoal(s.budget, W))}</span>` : ''}</button>`);
  });
  return `${header(W.annals, { icon: TAB_ICONS.annals })}
  <main class="with-tabs">
    ${annalsTabs(a)}
    ${trips.length ? cards.join('') : hint(`No ${W.trips.toLowerCase()} yet. Add one in Settings → ${W.trips}.`)}
    <button class="btn wide" data-action="go" data-to="#/trips"><span class="frame"><span class="frame-inner with-emblem">${emblem(EMBLEMS.trip, 26)}${trips.length ? `Edit ${W.trips.toLowerCase()}` : esc(W.newTrip)}</span></span></button>
  </main>${tabBar('annals')}`;
}

function annalsScreen() {
  const a = ui.annals;
  if (a.view === 'trips') return annalsTrips(a);
  const t = today();
  const s = a.view === 'year' ? yearSummary(data, a.year, t) : allTimeSummary(data, t);
  const bar = a.bar != null ? s.bars[a.bar] : null;
  const barInfo = bar
    ? `<div class="split-head"><span class="h-small">${bar.month ? monthTitle(bar.month) : bar.year}: ${format(bar.cents)}</span>
        <button class="link" data-action="${bar.month ? 'open-month' : 'open-year'}" data-month="${bar.month ?? ''}" data-year="${bar.year ?? ''}">${bar.month ? 'Open month ›' : 'Open year ›'}</button></div>`
    : '<p class="small soft">Tap a bar to see its total.</p>';
  let change = '';
  if (s.changePercent != null) {
    const span = a.year === Number(t.slice(0, 4)) ? `Jan–${MONTH_NAMES[Number(t.slice(5, 7)) - 1]} ${a.year - 1}` : String(a.year - 1);
    change = s.changePercent > 0
      ? `<p class="up">▲ ${s.changePercent}% more than ${span}</p>`
      : s.changePercent < 0 ? `<p class="down">▼ ${-s.changePercent}% less than ${span}</p>` : `<p>Same as ${span}</p>`;
  }
  const facts = [s.expenseCount === 1 ? '1 expense' : `${s.expenseCount} expenses`,
    s.biggest ? `biggest: ${s.biggest.title}, ${format(myShareCents(s.biggest))}` : ''].filter(Boolean).join(' · ');
  return `${header(W.annals, { icon: TAB_ICONS.annals, actions: yearTurner() })}
  <main class="with-tabs">
    ${annalsTabs(a)}
    ${a.view === 'year' ? monthSwitch('prev-year', 'next-year', String(a.year)) : ''}
    ${card(`
      <h3 class="h-small">${a.view === 'year' ? W.spentIn(a.year) : W.spentAllTime}</h3>
      <p class="total">${format(s.totalCents)}</p>
      ${s.paidTotalCents !== s.totalCents ? `<p class="small soft">Your share. Full bills came to ${format(s.paidTotalCents)}.</p>` : ''}
      ${s.monthsCounted > 0 ? `<p>Monthly average: ${format(s.averagePerMonthCents)} over ${s.monthsCounted === 1 ? '1 month' : `${s.monthsCounted} months`}</p>` : ''}
      ${change}
      <p class="small soft">${esc(facts)}</p>`)}
    ${card(`<h3 class="h-medium">${a.view === 'year' ? 'Month by month' : 'Year by year'}</h3>
      ${s.bars.length ? barChart(s.bars, a.bar) + barInfo : '<p class="soft">Nothing recorded yet.</p>'}`)}
    ${breakdownCard(s, a.view === 'year' ? String(a.year) : '')}
    ${s.food.totalCents > 0 ? foodCard(s.food, a.view === 'year' ? String(a.year) : '') : ''}
  </main>${tabBar('annals')}`;
}

// ---------- CSV export ----------

async function exportCsv() {
  const name = `expensiarmus-${today()}.csv`;
  const text = '﻿' + toCsv(data.expenses, data.trips ?? []);
  if (nativeBridge) {
    post({ type: 'share', filename: name, text });
    return;
  }
  const blob = new Blob([text], { type: 'text/csv' });
  const file = new File([blob], name, { type: 'text/csv' });
  try {
    if (navigator.canShare?.({ files: [file] })) {
      await navigator.share({ files: [file], title: 'Expensiarmus ledger' });
    } else {
      const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(blob), download: name });
      a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    }
    toast(W.exported);
  } catch (err) {
    if (err?.name !== 'AbortError') toast(W.exportFailed);
  }
}

function toast(text) {
  const t = Object.assign(document.createElement('div'), { className: 'toast', textContent: text });
  document.body.append(t);
  setTimeout(() => t.remove(), 2600);
}

// ---------- rendering and events ----------

function render() {
  const r = route();
  if (r.name !== 'edit') draft = null;
  if (r.name !== ui.modalScreen) ui.modal = false;
  ui.modalScreen = r.name;
  if (r.name !== 'month' && r.name !== 'calendar') ui.quick = null;
  if (r.name !== 'goals') ui.goalEdit = null;
  if (r.name !== 'favorites') ui.favEdit = null;
  if (r.name !== 'trips') ui.tripEdit = null;
  if (r.name !== 'category') ui.catTrips = null;
  if (r.name !== 'all') ui.monthListTrips = null;
  // Opened days stay open while you look at an expense or a category and come back; a tab, or another month, closes them.
  if (TABS.includes(r.name) || (r.name === 'all' && r.a !== ui.openDaysMonth)) ui.openDays = new Set();
  if (r.name === 'all') ui.openDaysMonth = r.a;
  if (r.name !== ui.lastScreen && TABS.includes(r.name)) ui.highlighted = null;
  ui.lastScreen = r.name;
  switch (r.name) {
    case 'edit': app.innerHTML = editScreen(r.a); break;
    case 'view': app.innerHTML = viewScreen(r.a); break;
    case 'category': app.innerHTML = categoryScreen(r.a, r.b, r.c ?? ''); break;
    case 'search': app.innerHTML = searchScreen(); break;
    case 'favorites': app.innerHTML = favoritesScreen(); break;
    case 'trips': app.innerHTML = tripsScreen(); break;
    case 'all': app.innerHTML = monthListScreen(r.a); break;
    case 'trip': app.innerHTML = tripScreen(r.a); break;
    case 'goals': app.innerHTML = goalsScreen(); break;
    case 'settings':
    case 'owl': app.innerHTML = settingsScreen(); break;
    case 'annals': app.innerHTML = annalsScreen(); break;
    case 'calendar': app.innerHTML = monthScreen(true); break;
    default: app.innerHTML = monthScreen(false);
  }
}

const actions = {
  back,
  go: (el) => go(el.dataset.to),
  tab: (el) => { navDepth = 0; location.replace(el.dataset.to); },
  'month-trips': (el) => { ui.monthListTrips = el.dataset.v; render(); },
  'list-cats': () => { ui.listCatsOpen = !ui.listCatsOpen; render(); },
  'list-day': (el) => {
    const d = el.dataset.day;
    if (ui.openDays.has(d)) ui.openDays.delete(d); else ui.openDays.add(d);
    render();
  },
  'recap-open': (el) => { ui.month = el.dataset.month; ui.highlighted = null; render(); },
  'recap-dismiss': (el) => { commit({ ...data, recapDismissed: el.dataset.month }); render(); },
  'this-month': () => { ui.month = today().slice(0, 7); ui.day = today(); ui.highlighted = null; ui.goalEdit = null; render(); },
  'prev-month': () => { ui.month = shiftMonth(ui.month, -1); ui.highlighted = null; ui.goalEdit = null; render(); },
  'next-month': () => { ui.month = shiftMonth(ui.month, 1); ui.highlighted = null; ui.goalEdit = null; render(); },
  slice: (el) => {
    const c = el.dataset.cat;
    ui.highlighted = ui.highlighted === c ? null : c;
    render();
  },
  'slice-clear': () => { ui.highlighted = null; render(); },
  'annals-view': (el) => { ui.annals = { ...ui.annals, view: el.dataset.view, bar: null }; ui.highlighted = null; render(); },
  'this-year': () => { ui.annals = { ...ui.annals, year: Number(today().slice(0, 4)), bar: null }; ui.highlighted = null; render(); },
  'prev-year': () => { ui.annals = { ...ui.annals, year: ui.annals.year - 1, bar: null }; ui.highlighted = null; render(); },
  'next-year': () => { ui.annals = { ...ui.annals, year: ui.annals.year + 1, bar: null }; ui.highlighted = null; render(); },
  bar: (el) => { const i = Number(el.dataset.index); ui.annals = { ...ui.annals, bar: ui.annals.bar === i ? null : i }; render(); },
  'open-month': (el) => { ui.month = el.dataset.month; ui.highlighted = null; navDepth = 0; location.replace('#/'); },
  'open-year': (el) => { ui.annals = { view: 'year', year: Number(el.dataset.year), bar: null }; ui.highlighted = null; render(); },
  day: (el) => { ui.day = el.dataset.day; render(); },
  category: (el) => {
    go(categoryPath(el.dataset.cat, el.dataset.food, el.dataset.period));
    // From the full list, the category opens with the same trips choice.
    ui.catTrips = el.dataset.trips === 'without' ? 'without' : null;
    render();
  },
  'cat-food': (el) => location.replace(categoryPath(route().a, el.dataset.food, route().c)),
  open: (el) => go(`#/view/${encodeURIComponent(el.dataset.id)}`),
  amend: (el) => go(`#/edit/${encodeURIComponent(el.dataset.id)}`),
  export: () => exportCsv(),
  import: () => app.querySelector('#import-file')?.click(),
  'import-cancel': () => { ui.importing = null; render(); },
  'import-confirm': () => {
    const added = ui.importing?.added ?? [];
    const newTrips = ui.importing?.newTrips ?? [];
    ui.importing = null;
    if (added.length) {
      const trips = newTrips.reduce((list, t) => saveTrip(list, t).list ?? list, data.trips ?? []);
      commit({ ...data, trips, expenses: [...data.expenses, ...added] });
    }
    toast(W.imported(added.length));
    render();
  },
  quick: () => { ui.quick = { title: '', amount: '', date: newExpenseDate(), error: false, cat: null, food: null, pct: 100, fav: null, trip: ongoingTrip(data.trips, today())?.id ?? null }; render(); },
  'quick-close': () => { ui.quick = null; render(); },
  'quick-key': (el) => quickKey(el.dataset.key),
  'quick-more': () => {
    const q = ui.quick;
    // A picked favourite also carries its category, food type and split to the full form.
    // The picked category (and a favourite's split) go on to the full form.
    prefill = { title: q.title, amount: q.amount, date: q.date, category: q.cat, foodType: q.food, myPercent: q.pct, tripId: q.trip ?? null };
    ui.quick = null;
    go('#/edit');
  },
  'quick-pick': (el) => {
    const q = ui.quick;
    q.cat = el.dataset.cat;
    q.food = el.dataset.food || null;
    const f = q.fav != null ? data.favorites[q.fav] : null;
    // A favourite's split stays only while its own category is picked.
    if (!f || f.category !== q.cat || (f.foodType ?? null) !== q.food) { q.fav = null; q.pct = 100; }
    q.error = null;
    render();
  },
  'quick-record': () => {
    const q = ui.quick;
    const cents = parseToCents(q.amount);
    q.error = !(cents > 0) ? 'Type an amount first' : !q.cat ? 'Pick a category first' : null;
    if (q.error) { render(); return; }
    quickRecord(makeExpense({ id: newId(), date: q.date, title: q.title.trim() || defaultTitle(q.cat, q.food), amountCents: cents, category: q.cat, foodType: q.food, myPercent: q.pct ?? 100, tripId: q.trip ?? null }));
  },
  'fav-pick': (el) => {
    const q = ui.quick;
    const i = Number(el.dataset.i);
    if (q.fav === i) { q.fav = null; render(); return; }
    const f = data.favorites[i];
    Object.assign(q, { fav: i, title: f.title, cat: f.category, food: f.foodType ?? null, pct: f.myPercent ?? 100, error: null });
    render();
  },
  'fe-open': (el) => { const i = Number(el.dataset.i); ui.favEdit = { ...data.favorites[i], index: i, error: null }; render(); },
  'fe-add': () => { ui.favEdit = { index: null, title: '', category: 'SUPERMARKET', foodType: null, myPercent: 100, error: null }; render(); },
  'fe-cat': (el) => { const ed = ui.favEdit; ed.category = el.dataset.cat; if (ed.category !== 'ENTERTAINMENT') ed.foodType = null; render(); },
  'fe-food': (el) => { ui.favEdit.foodType = el.dataset.food || null; render(); },
  'fe-pct': (el) => { ui.favEdit.myPercent = Number(el.dataset.pct); render(); },
  'fe-cancel': () => { ui.favEdit = null; render(); },
  'fe-save': saveFavoriteEdit,
  'fe-remove': () => { const i = ui.favEdit.index; ui.favEdit = null; commit({ ...data, favorites: removeFavoriteAt(data.favorites, i) }); render(); },
  'edit-fav': (el) => {
    const f = data.favorites[Number(el.dataset.i)];
    Object.assign(draft, { title: f.title, category: f.category, foodType: f.foodType ?? null, split: (f.myPercent ?? 100) < 100, myPercent: (f.myPercent ?? 100) < 100 ? f.myPercent : 50 });
    render();
  },
  'quick-trip': (el) => { ui.quick.trip = ui.quick.trip === el.dataset.id ? null : el.dataset.id; render(); },
  'edit-trip': (el) => { draft.tripId = draft.tripId === el.dataset.id ? null : el.dataset.id; render(); },
  'cat-trips': (el) => { ui.catTrips = el.dataset.v; render(); },
  'te-open': (el) => { const t = data.trips.find((x) => x.id === el.dataset.id); ui.tripEdit = { id: t.id, name: t.name, start: t.start, end: t.end, budget: t.budgetCents ? forInput(t.budgetCents) : '', error: null }; render(); },
  'te-add': () => { ui.tripEdit = { id: null, name: '', start: today(), end: today(), budget: '', error: null }; render(); },
  'te-cancel': () => { ui.tripEdit = null; render(); },
  'te-save': saveTripEdit,
  'te-delete': () => { ui.tripEdit.confirmDelete = true; render(); },
  'te-delete-yes': () => { const id = ui.tripEdit.id; ui.tripEdit = null; commit(deleteTrip(data, id)); render(); },
  'fav-toggle': (el) => {
    const e = find(data, el.dataset.id);
    const next = toggleFavorite(data.favorites, e);
    if (next === data.favorites) { toast(`You have ${MAX_FAVORITES} favourites already. Remove one first, in Settings → Favourites.`); return; }
    commit({ ...data, favorites: next });
    toast(isFavorite(next, e) ? `${e.title} is now a favourite` : `${e.title} is no longer a favourite`);
    render();
  },
  'edit-cat': (el) => { draft.category = el.dataset.cat; if (draft.category !== 'ENTERTAINMENT') draft.foodType = null; render(); },
  'edit-food': (el) => { draft.foodType = el.dataset.food || null; render(); },
  'edit-pct': (el) => { draft.myPercent = Number(el.dataset.pct); render(); },
  save: saveDraft,
  'ask-delete': () => { ui.modal = true; render(); },
  'cancel-delete': () => { ui.modal = false; render(); },
  'confirm-delete': (el) => {
    const id = el.dataset.id;
    commit(remove(data, id));
    ui.modal = false;
    draft = null;
    back();
  },
  'search-cat': (el) => {
    const c = el.dataset.cat;
    finder.cats = finder.cats.includes(c) ? finder.cats.filter((x) => x !== c) : [...finder.cats, c];
    el.classList.toggle('on');
    app.querySelector('#results').innerHTML = searchResults();
  },
  'ward-edit': (el) => {
    const entry = goalFor(data.goals, goalKey(el.dataset.cat), ui.month);
    ui.goalEdit = { cat: el.dataset.cat, amount: forInput(entry.cents), onwards: entry.onwards };
    render();
  },
  'ward-add': () => {
    const free = [WHOLE_MONTH, ...CATEGORIES].filter((c) => !goalFor(data.goals, goalKey(c.id), ui.month));
    ui.goalEdit = { cat: null, adding: true, picked: free[0]?.id, amount: '', onwards: false };
    render();
  },
  'ward-pick': (el) => { ui.goalEdit.picked = el.dataset.cat; render(); },
  'ward-cancel': () => { ui.goalEdit = null; render(); },
  'ward-save': saveWard,
  'ward-remove': () => { ui.goalEdit.confirmRemove = true; render(); },
  'ward-remove-cancel': () => { ui.goalEdit.confirmRemove = false; render(); },
  'ward-remove-one': () => removeWard(false),
  'ward-remove-on': () => removeWard(true),
  'set-money': (el) => updateSettings({ money: el.dataset.v }),
  'set-theme': (el) => updateSettings({ theme: el.dataset.v }),
  'dismiss-install': () => { storage.setItem('expensiarmus.installHintSeen', '1'); render(); },
};

const inputs = {
  'edit-title': (el) => { draft.title = el.value; },
  'edit-amount': (el) => { draft.amount = el.value; const s = app.querySelector('#share-line'); if (s) s.textContent = shareLine(); },
  'edit-date': (el) => { draft.date = el.value; },
  'edit-split': (el) => { draft.split = el.checked; render(); },
  'edit-pct': (el) => {
    draft.myPercent = Number(el.value);
    app.querySelector('#pct-label').textContent = `My share: ${draft.myPercent}% · Theirs: ${100 - draft.myPercent}%`;
    app.querySelector('#share-line').textContent = shareLine();
  },
  'fe-title': (el) => { ui.favEdit.title = el.value; },
  'te-name': (el) => { ui.tripEdit.name = el.value; },
  'te-start': (el) => { ui.tripEdit.start = el.value; if (ui.tripEdit.end < el.value) ui.tripEdit.end = el.value; render(); },
  'te-end': (el) => { ui.tripEdit.end = el.value; },
  'te-budget': (el) => { ui.tripEdit.budget = el.value; },
  'quick-title': (el) => { ui.quick.title = el.value; },
  'import-file': (el) => { readImport(el); },
  'quick-date': (el) => { if (el.value) ui.quick.date = el.value; },
  'search-q': (el) => { finder.q = el.value; app.querySelector('#results').innerHTML = searchResults(); },
  'ward-amount': (el) => { ui.goalEdit.amount = el.value; app.querySelector('#ward-error').hidden = true; },
  'ward-onwards': (el) => {
    ui.goalEdit.onwards = el.checked;
    app.querySelector('#ward-scope').textContent = el.checked ? `Also for the months after ${monthName(ui.month)}` : `Only ${monthTitle(ui.month)}`;
  },
  'set-reminder': (el) => updateSettings({ reminderOn: el.checked }),
  'set-time': (el) => { if (validTime(el.value)) updateSettings({ reminderTime: el.value }); },
  'set-wizard': (el) => updateSettings({ wizard: el.checked }),
};

// Settings controls change on "change" only, so typing a time doesn't re-render mid-edit.
const ON_CHANGE = ['te-start', 'te-end', 'import-file', 'edit-split', 'edit-date', 'quick-date', 'ward-onwards', 'set-reminder', 'set-time', 'set-wizard'];

app.addEventListener('click', (ev) => {
  const el = ev.target.closest('[data-action]');
  if (el) actions[el.dataset.action]?.(el, ev);
});
app.addEventListener('input', (ev) => {
  const el = ev.target.closest('[data-input]');
  if (el && !ON_CHANGE.includes(el.dataset.input)) inputs[el.dataset.input]?.(el);
});
app.addEventListener('change', (ev) => {
  const el = ev.target.closest('[data-input]');
  if (el && ON_CHANGE.includes(el.dataset.input)) inputs[el.dataset.input]?.(el);
});
window.addEventListener('hashchange', render);

// Safari ignores user-scalable=no, so cancel its pinch gestures here (double-tap is off via CSS).
for (const type of ['gesturestart', 'gesturechange', 'gestureend']) {
  document.addEventListener(type, (ev) => ev.preventDefault(), { passive: false });
}
document.addEventListener('touchmove', (ev) => { if (ev.touches.length > 1) ev.preventDefault(); }, { passive: false });

navigator.storage?.persist?.();
if (!nativeBridge && 'serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});

// Hooks the iPhone app calls: open a screen from a notification, report notification permission.
window.expensiarmusOpen = (hash) => {
  navDepth = 0;
  if (hash === '#/edit') {
    // The owl opens Quick add on the Ledger.
    location.hash = '#/';
    ui.quick = { title: '', amount: '', date: today(), error: false, cat: null, food: null, pct: 100, fav: null, trip: ongoingTrip(data.trips, today())?.id ?? null };
    render();
    return;
  }
  location.hash = hash;
};
window.addEventListener('owlstatus', () => { if (route().name === 'settings') render(); });
// Keep the 30-day window of owls rolling whenever the app comes back to the foreground.
// The widget catches up too (a new month, or a trip's spending recorded elsewhere).
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') { scheduleOwls(); updateWidget(); } });

applySettings();
render();
scheduleOwls();
updateWidget();
