// Expensiarmus web app: screens, navigation and event handling. Logic lives in the other modules.
import { CATEGORIES, FOOD_TYPES, TAB_ICONS, EMBLEMS, WHOLE_MONTH, goalKey, withoutEmoji, category, foodType, makeExpense, myShareCents, isSplit, splitLine, upsert, remove, find, monthOf } from './model.js';
import { parseToCents, format, forInput, compact, setMoneyStyle } from './money.js';
import { monthlySummary, describeGoal, goalProgress, yearSummary, allTimeSummary } from './summary.js';
import { goalFor, setGoal, removeGoal, scopeText, alertsDue, markAlertsSent } from './goals.js';
import { dailyTotals, onDay, calendarCells, byCategory, inPeriod, normalize, isFavorite, toggleFavorite, MAX_FAVORITES, search, shiftMonth, suggestions, defaultTitle, ledgerPreview, monthGlance, LATEST_COUNT } from './queries.js';
import { toCsv, importCsv, importSummary } from './csv.js';
import { load, save, KEY } from './store.js';
import { upcomingReminders } from './reminder.js';
import { wordsFor } from './words.js';
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
  ledgerAll: null, // the month whose whole ledger is shown; otherwise only the latest few
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
  render();
}

/** Asks the iPhone app to (re)schedule the evening owls for the coming days, at the chosen time. */
function scheduleOwls() {
  post({
    type: 'schedule',
    enabled: settings.reminderOn,
    reminders: settings.reminderOn ? upcomingReminders(data.expenses, new Date(), 30, settings.reminderTime, W) : [],
  });
}

function commit(next) {
  // Warn once when a category of the current month reaches 80% of its goal, or goes over.
  const alerts = alertsDue(next, today().slice(0, 7), W);
  data = alerts.length ? markAlertsSent(next, alerts) : next;
  save(storage, data);
  // The iPhone app keeps a copy outside the web view's storage, as a backup.
  post({ type: 'save', key: KEY, json: storage.getItem(KEY) });
  scheduleOwls();
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
  const details = [when, c.label + (f ? ` › ${f.label}` : ''), isSplit(e) ? `my share ${e.myPercent}%` : ''].filter(Boolean).join(' · ');
  return `<button class="row" data-action="open" data-id="${esc(e.id)}">
    ${emblem(f ? f.icon : c.icon)}
    <span class="row-main"><span class="row-title">${esc(e.title)}</span><span class="row-sub">${esc(details)}</span></span>
    <span class="row-amount"><b>${format(myShareCents(e))}</b>${isSplit(e) ? `<small>of ${format(e.amountCents)}</small>` : ''}</span>
  </button>`;
}

/** Earlier titles matching what is typed. Tapping one only fills in the title; no amount is copied. */
function suggestionList(typed, action) {
  const list = suggestions(data.expenses, typed).filter((s) => normalize(s.title) !== normalize(typed));
  if (!list.length) return '';
  return `<div class="suggestions">${list.map((s, i) => {
    const c = category(s.category);
    const f = s.foodType ? foodType(s.foodType) : null;
    return `<button class="suggestion" data-action="${action}" data-i="${i}">
      ${emblem(f ? f.icon : c.icon, 26)}
      <span class="row-main"><span class="row-title">${esc(s.title)}</span><span class="row-sub">${esc(f ? f.label : c.label)}</span></span></button>`;
  }).join('')}</div>`;
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
    <p class="small soft">Tap a slice to highlight it, or a ${W.vault} to see its expenses ${periodWords(period)}.</p>`);
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
    ${food.otherEntertainmentCents > 0 ? `<p class="small soft">Other entertainment (not food): ${format(food.otherEntertainmentCents)}</p>` : ''}`);
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

/** The newest few expenses of the month, with a link to show them all (or only the latest again). */
function ledgerList(expenses) {
  const showAll = ui.ledgerAll === ui.month;
  const more = expenses.length > LATEST_COUNT
    ? `<button class="link more-link" data-action="ledger-all">${showAll ? `Show only the latest ${LATEST_COUNT}` : `Show all ${expenses.length} ›`}</button>`
    : '';
  return `<div class="list">${ledgerPreview(expenses, showAll).map((e) => expenseRow(e)).join('')}</div>${more}`;
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
    body = `${totalCard(s)}
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
  <button class="fab" data-action="quick"><span class="frame"><span class="frame-inner">${esc(W.fab)}</span></span></button>
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

/** Quick add's favourites: tap one to fill the title and category, then type the amount and Record. */
function favRow(q) {
  const favs = data.favorites ?? [];
  if (!favs.length) return '';
  return `<div class="fav-head"><span class="h-small fav-title"><span class="star-on small-glyph"></span>Favourites</span>
      <button class="link" data-action="fav-edit">${q.favEdit ? 'Done' : 'Edit'}</button></div>
    <div class="chips fav-chips">${favs.map((f, i) => {
      const look = f.foodType ? foodType(f.foodType) : category(f.category);
      return `<button class="chip ${q.fav === i && !q.favEdit ? 'on' : ''}" data-action="${q.favEdit ? 'fav-remove' : 'fav-pick'}" data-i="${i}">${chipIcon(look)}${esc(f.title)}${q.favEdit ? ' <span class="chip-x">×</span>' : ''}</button>`;
    }).join('')}</div>`;
}

function quickSheet() {
  const q = ui.quick;
  const fav = q.fav != null ? (data.favorites ?? [])[q.fav] : null;
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
      <button class="link" data-action="quick-more">More details ›</button>
    </div>
    ${favRow(q)}
    <input class="quick-title" type="text" placeholder="Title (optional)" value="${esc(q.title)}" data-input="quick-title" autocapitalize="sentences">
    <div id="quick-sugg">${suggestionList(q.title, 'quick-sugg')}</div>
    <div class="quick-amount" id="quick-amount">${esc(quickAmountText())}</div>
    <label class="quick-date">${emblem(TAB_ICONS.calendar, 28)}<input type="date" value="${q.date}" data-input="quick-date"></label>
    <div class="numpad">${keys.map((k) => `<button data-action="quick-key" data-key="${k}">${k}</button>`).join('')}</div>
    ${fav ? `<button class="btn wide fav-record" data-action="fav-record"><span class="frame"><span class="frame-inner">Record · ${esc(q.title.trim() || fav.title)}</span></span></button>` : ''}
    <p class="small center ${q.error ? 'error' : 'soft'}" id="quick-help">${q.error ? 'Type an amount first' : fav ? 'or tap a category to save it there' : 'Tap a category to save'}</p>
    <div class="tiles">${tiles.map((t) => `<button class="tile ${fav && fav.category === t.cat && (fav.foodType ?? '') === t.food ? 'glow' : ''}" data-action="quick-save" data-cat="${t.cat}" data-food="${t.food}">
      ${emblem(t.icon, 38)}<span>${esc(t.label)}</span></button>`).join('')}</div>
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
  help.textContent = q.fav != null ? 'or tap a category to save it there' : 'Tap a category to save';
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
    ui.importing = importCsv(await file.text(), data.expenses, newId);
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
  // Saving is the framed button at the bottom; deleting lives on the Ledger entry page.
  return `${header(d.id ? W.editExpense : W.newExpense, { backBtn: true })}
  <main class="form">
    <div class="field"><label class="label" for="edit-title">Title</label>
      <input id="edit-title" type="text" value="${esc(d.title)}" placeholder="e.g. Butterbeer with friends" data-input="edit-title" class="${titleBad ? 'bad' : ''}" autocapitalize="sentences" autocomplete="off">
      <div id="title-sugg">${d.id ? '' : suggestionList(d.title, 'edit-sugg')}</div>
      ${titleBad ? '<span class="error">Enter a title</span>' : ''}</div>
    <label class="field"><span class="label">${esc(W.amount)}</span>
      <span class="money-input ${amountBad ? 'bad' : ''}"><span>€</span><input type="text" inputmode="decimal" value="${esc(d.amount)}" placeholder="0${decimalKey()}00" data-input="edit-amount"></span>
      ${amountBad ? `<span class="error">Enter an amount, e.g. 12${decimalKey()}50</span>` : ''}</label>
    <label class="field"><span class="label">Date</span>
      <input type="date" value="${d.date}" data-input="edit-date"></label>
    <div class="field"><span class="h-small">Category</span><div class="chips">${catChips}</div>
      <p class="small soft">${esc(withNickname(cat))}</p></div>
    ${foodChips}
    <label class="switch-row"><span><span class="h-small">${esc(W.split)}</span><span class="small soft">Only your share counts toward totals and goals</span></span>
      <input type="checkbox" class="switch" ${d.split ? 'checked' : ''} data-input="edit-split"></label>
    ${split}
    <button class="btn wide" data-action="save"><span class="frame"><span class="frame-inner">${esc(W.save)}</span></span></button>
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
  const row = (label, value, note = '') => `<div class="detail"><span class="h-small">${label}</span><span>${esc(value)}</span>${note ? `<span class="small soft italic">${esc(note)}</span>` : ''}</div>`;
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
      ${row('Category', `${c.emoji} ${c.label}`, W.nicknames ? c.nickname : '')}
      ${f ? row('Food', `${f.emoji} ${f.label}`, W.nicknames ? f.nickname : '') : ''}
      ${row('Split', splitLine(e, format))}`)}
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

// ---------- category ----------

/** "this month" / "in 2026" / "of all time": what a category opened from that card will list. */
const periodWords = (period) => (!period ? 'of all time' : period.length === 7 ? 'for this month' : `in ${period}`);
const periodTitle = (period) => (!period ? 'All time' : period.length === 7 ? monthTitle(period) : period);
/** The address of a category page, keeping the food type and the period when there are any. */
const categoryPath = (cat, food, period) => `#/category/${cat}${food || period ? `/${food ?? ''}` : ''}${period ? `/${period}` : ''}`;

/** A category's expenses for the period it was opened from (a month, a year, or all time), grouped by month. */
function categoryScreen(catId, foodId, period = '') {
  const cat = category(catId) ?? CATEGORIES[0];
  const groups = byCategory(inPeriod(data.expenses, period), cat.id, foodId);
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

function annalsScreen() {
  const a = ui.annals;
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
    <div class="segmented">
      <button class="${a.view === 'year' ? 'on' : ''}" data-action="annals-view" data-view="year">${chipIcon({ icon: TAB_ICONS.calendar })}By year</button>
      <button class="${a.view === 'all' ? 'on' : ''}" data-action="annals-view" data-view="all">${chipIcon({ icon: TAB_ICONS.annals })}All time</button>
    </div>
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
  const text = '﻿' + toCsv(data.expenses);
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
  if (r.name !== ui.lastScreen && TABS.includes(r.name)) ui.highlighted = null;
  ui.lastScreen = r.name;
  switch (r.name) {
    case 'edit': app.innerHTML = editScreen(r.a); break;
    case 'view': app.innerHTML = viewScreen(r.a); break;
    case 'category': app.innerHTML = categoryScreen(r.a, r.b, r.c ?? ''); break;
    case 'search': app.innerHTML = searchScreen(); break;
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
  'ledger-all': () => { ui.ledgerAll = ui.ledgerAll === ui.month ? null : ui.month; render(); },
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
  category: (el) => go(categoryPath(el.dataset.cat, el.dataset.food, el.dataset.period)),
  'cat-food': (el) => location.replace(categoryPath(route().a, el.dataset.food, route().c)),
  open: (el) => go(`#/view/${encodeURIComponent(el.dataset.id)}`),
  amend: (el) => go(`#/edit/${encodeURIComponent(el.dataset.id)}`),
  export: () => exportCsv(),
  import: () => app.querySelector('#import-file')?.click(),
  'import-cancel': () => { ui.importing = null; render(); },
  'import-confirm': () => {
    const added = ui.importing?.added ?? [];
    ui.importing = null;
    if (added.length) commit({ ...data, expenses: [...data.expenses, ...added] });
    toast(W.imported(added.length));
    render();
  },
  quick: () => { ui.quick = { title: '', amount: '', date: newExpenseDate(), error: false }; render(); },
  'quick-close': () => { ui.quick = null; render(); },
  'quick-key': (el) => quickKey(el.dataset.key),
  'quick-more': () => {
    const q = ui.quick;
    // A picked favourite also carries its category, food type and split to the full form.
    const f = q.fav != null ? data.favorites[q.fav] : null;
    prefill = { title: q.title, amount: q.amount, date: q.date, category: f?.category, foodType: f?.foodType, myPercent: f?.myPercent };
    ui.quick = null;
    go('#/edit');
  },
  'quick-save': (el) => {
    const q = ui.quick;
    const cents = parseToCents(q.amount);
    if (!(cents > 0)) {
      q.error = true;
      const help = app.querySelector('#quick-help');
      help.className = 'small center error';
      help.textContent = 'Type an amount first';
      return;
    }
    const cat = el.dataset.cat;
    const food = el.dataset.food || null;
    quickRecord(makeExpense({ id: newId(), date: q.date, title: q.title.trim() || defaultTitle(cat, food), amountCents: cents, category: cat, foodType: food }));
  },
  'fav-pick': (el) => { const i = Number(el.dataset.i); ui.quick.fav = ui.quick.fav === i ? null : i; ui.quick.title = ui.quick.fav == null ? '' : data.favorites[i].title; render(); },
  'fav-edit': () => { ui.quick.favEdit = !ui.quick.favEdit; ui.quick.fav = null; render(); },
  'fav-remove': (el) => {
    const f = data.favorites[Number(el.dataset.i)];
    commit({ ...data, favorites: toggleFavorite(data.favorites, f) });
    if (!data.favorites.length) ui.quick.favEdit = false;
    render();
  },
  'fav-record': () => {
    const q = ui.quick;
    const f = data.favorites[q.fav];
    const cents = parseToCents(q.amount);
    if (!(cents > 0)) {
      q.error = true;
      const help = app.querySelector('#quick-help');
      help.className = 'small center error';
      help.textContent = 'Type an amount first';
      return;
    }
    quickRecord(makeExpense({ id: newId(), date: q.date, title: q.title.trim() || f.title, amountCents: cents, category: f.category, foodType: f.foodType, myPercent: f.myPercent }));
  },
  'fav-toggle': (el) => {
    const e = find(data, el.dataset.id);
    const next = toggleFavorite(data.favorites, e);
    if (next === data.favorites) { toast(`You have ${MAX_FAVORITES} favourites already. Remove one first (Edit, in ${W.newExpense}).`); return; }
    commit({ ...data, favorites: next });
    toast(isFavorite(next, e) ? `${e.title} is now a favourite` : `${e.title} is no longer a favourite`);
    render();
  },
  'quick-sugg': (el) => {
    const typed = ui.quick.title;
    const s = suggestions(data.expenses, typed).filter((x) => normalize(x.title) !== normalize(typed))[Number(el.dataset.i)];
    ui.quick.title = s.title;
    render();
  },
  'edit-sugg': (el) => {
    const typed = draft.title;
    draft.title = suggestions(data.expenses, typed).filter((x) => normalize(x.title) !== normalize(typed))[Number(el.dataset.i)].title;
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
  'edit-title': (el) => {
    draft.title = el.value;
    if (!draft.id) app.querySelector('#title-sugg').innerHTML = suggestionList(el.value, 'edit-sugg');
  },
  'edit-amount': (el) => { draft.amount = el.value; const s = app.querySelector('#share-line'); if (s) s.textContent = shareLine(); },
  'edit-date': (el) => { draft.date = el.value; },
  'edit-split': (el) => { draft.split = el.checked; render(); },
  'edit-pct': (el) => {
    draft.myPercent = Number(el.value);
    app.querySelector('#pct-label').textContent = `My share: ${draft.myPercent}% · Theirs: ${100 - draft.myPercent}%`;
    app.querySelector('#share-line').textContent = shareLine();
  },
  'quick-title': (el) => { ui.quick.title = el.value; app.querySelector('#quick-sugg').innerHTML = suggestionList(el.value, 'quick-sugg'); },
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
const ON_CHANGE = ['import-file', 'edit-split', 'edit-date', 'quick-date', 'ward-onwards', 'set-reminder', 'set-time', 'set-wizard'];

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
    ui.quick = { title: '', amount: '', date: today(), error: false };
    render();
    return;
  }
  location.hash = hash;
};
window.addEventListener('owlstatus', () => { if (route().name === 'settings') render(); });
// Keep the 30-day window of owls rolling whenever the app comes back to the foreground.
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') scheduleOwls(); });

applySettings();
render();
scheduleOwls();
