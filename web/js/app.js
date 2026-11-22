// Expensiarmus web app: screens, navigation and event handling. Logic lives in the other modules.
import { CATEGORIES, CATEGORIES_WITH_GOALS, FOOD_TYPES, category, foodType, makeExpense, myShareCents, isSplit, splitLine, upsert, remove, find, monthOf } from './model.js';
import { parseToCents, format, forInput, compact } from './money.js';
import { monthlySummary, describeGoal, yearSummary, allTimeSummary } from './summary.js';
import { dailyTotals, onDay, calendarCells, byCategory, search, shiftMonth } from './queries.js';
import { toCsv } from './csv.js';
import { load, save, KEY } from './store.js';
import { upcomingReminders } from './reminder.js';

const app = document.getElementById('app');
const storage = window.localStorage;
let data = load(storage);

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
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const newId = () => (crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`);

const ui = {
  month: today().slice(0, 7),
  view: 'overview',
  day: today(),
  highlighted: null,
  menu: false,
  modal: false,
  annals: { view: 'year', year: Number(today().slice(0, 4)), bar: null },
};
let draft = null; // the expense being added or edited
const finder = { q: '', cats: [] };
let navDepth = 0;

// Inside the iPhone app the page can talk to Swift through this handler; on the web it is absent.
const nativeBridge = window.webkit?.messageHandlers?.expensiarmus ?? null;
const post = (message) => nativeBridge?.postMessage(message);
const REMINDER_KEY = 'expensiarmus.reminder';
const reminderOn = () => storage.getItem(REMINDER_KEY) !== 'off';

/** Asks the iPhone app to (re)schedule the 21:30 owls for the coming evenings. */
function scheduleOwls() {
  post({ type: 'schedule', enabled: reminderOn(), reminders: reminderOn() ? upcomingReminders(data.expenses, new Date()) : [] });
}

function commit(next) {
  data = next;
  save(storage, data);
  // The iPhone app keeps a copy outside the web view's storage, as a backup.
  post({ type: 'save', key: KEY, json: storage.getItem(KEY) });
  scheduleOwls();
}

// ---------- navigation ----------

function route() {
  const [name, a, b] = location.hash.replace(/^#\/?/, '').split('/');
  return { name: name || 'month', a: a ? decodeURIComponent(a) : null, b: b ? decodeURIComponent(b) : null };
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
  more: '<svg viewBox="0 0 24 24" class="filled"><circle cx="12" cy="5" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="12" cy="19" r="1.8"/></svg>',
  check: '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
  trash: '<svg viewBox="0 0 24 24"><path d="M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12"/></svg>',
  pen: '<svg viewBox="0 0 24 24"><path d="M4 20l4-1 11-11-3-3L5 16l-1 4zM14 6l3 3"/></svg>',
};

function header(title, { backBtn = false, actions = '' } = {}) {
  return `<header class="bar"><div class="bar-row">
    ${backBtn ? `<button class="icon-btn" data-action="back" aria-label="Back">${ICON.back}</button>` : ''}
    <h1>${esc(title)}</h1>
    <div class="bar-actions">${actions}</div>
  </div></header>`;
}

const card = (inner, cls = '') => `<section class="card ${cls}"><div class="paper">${inner}</div></section>`;
const emblem = (emoji, color, size = 30) =>
  `<span class="emblem" style="--c:${color};--s:${size}px">${emoji}</span>`;
const ornament = (text) => `<h2 class="ornament"><span>${esc(text)}</span></h2>`;
const hint = (text) => `<p class="hint">${esc(text)}</p>`;

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
    ${emblem(f ? f.emoji : c.emoji, c.color)}
    <span class="row-main"><span class="row-title">${esc(e.title)}</span><span class="row-sub">${esc(details)}</span></span>
    <span class="row-amount"><b>${format(myShareCents(e))}</b>${isSplit(e) ? `<small>of ${format(e.amountCents)}</small>` : ''}</span>
  </button>`;
}

// ---------- month screen ----------

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
  const arcs = summary.categories.map((c, i) => {
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
    ? `<span class="big-emoji">${category(pick.category).emoji}</span><span class="h-small">${esc(category(pick.category).label)}</span><b>${format(pick.spentCents)}</b><small>${Math.round(pick.shareOfMonth * 100)}%</small>`
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
  const goal = s.overall
    ? `${goalBar(s.overall)}<p class="goal-text">${esc(describeGoal(s.overall))}</p>`
    : `<button class="link" data-action="go" data-to="#/goals">🛡 Cast Protego: set a monthly goal</button>`;
  return card(`
    <h3 class="h-small">Gold spent this month</h3>
    <p class="total">${format(s.totalCents)}</p>
    ${s.paidTotalCents !== s.totalCents ? `<p class="small soft">Your share. Full bills came to ${format(s.paidTotalCents)}.</p>` : ''}
    ${goal}
    <button class="link" data-action="annals">📊 See the whole year in the Annals</button>`);
}

function breakdownCard(s) {
  const rows = s.categories.map((c) => {
    const cat = category(c.category);
    const dim = (c.spentCents === 0 && !c.goal) || (ui.highlighted && ui.highlighted !== c.category);
    return `<button class="legend ${ui.highlighted === c.category ? 'glow' : ''} ${dim ? 'dim' : ''}" data-action="category" data-cat="${cat.id}">
      <span class="legend-row">
        ${emblem(cat.emoji, cat.color)}
        <span class="row-main"><span class="h-small">${esc(cat.label)}</span><span class="row-sub italic">${esc(cat.nickname)}</span></span>
        <span class="row-amount"><b>${format(c.spentCents)}</b><small>${Math.round(c.shareOfMonth * 100)}%</small></span><span class="chev">${ICON.right}</span>
      </span>
      ${c.goal ? `<span class="legend-goal">${goalBar(c.goal, true)}<span class="small soft">${esc(describeGoal(c.goal))}</span></span>` : ''}
    </button>`;
  });
  return card(`
    <h3 class="h-medium">Where the gold went</h3>
    ${s.totalCents > 0 ? donut(s) : ''}
    ${rows.join('')}
    <p class="small soft">Tap a slice to highlight it, or a vault to see all its expenses.</p>`);
}

function foodCard(food) {
  const bar = food.types.filter((t) => t.spentCents > 0)
    .map((t) => `<span style="flex:${t.spentCents};background:${foodType(t.type).color}"></span>`).join('');
  const rows = food.types.map((t) => {
    const f = foodType(t.type);
    return `<button class="legend" data-action="category" data-cat="ENTERTAINMENT" data-food="${f.id}"><span class="legend-row">
      ${emblem(f.emoji, f.color)}
      <span class="row-main"><span class="h-small">${f.label}</span><span class="row-sub">${f.nickname} · ${t.count === 1 ? '1 time' : `${t.count} times`}</span></span>
      <span class="row-amount"><b>${format(t.spentCents)}</b><small>${Math.round(t.shareOfFood * 100)}%</small></span><span class="chev">${ICON.right}</span>
    </span></button>`;
  });
  return card(`
    <div class="split-head"><h3 class="h-medium">Feasts &amp; Food</h3><b>${format(food.totalCents)}</b></div>
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

function owlBanner() {
  if (nativeBridge || ui.month !== today().slice(0, 7) || new Date().getHours() < 21) return '';
  if (data.expenses.some((e) => e.date === today())) return '';
  return `<button class="banner" data-action="go" data-to="#/edit">🦉 Owl post: nothing recorded today yet. Tap to record what you spent.</button>`;
}

function installBanner() {
  const ios = /iPhone|iPad|iPod/.test(navigator.userAgent);
  if (!ios || nativeBridge || navigator.standalone || storage.getItem('expensiarmus.installHintSeen')) return '';
  return `<div class="banner install">
    <span>✨ Install Expensiarmus: tap <b>Share</b> <span class="share-glyph">⬆︎</span> then <b>Add to Home Screen</b>.</span>
    <button class="icon-btn small" data-action="dismiss-install" aria-label="Dismiss">✕</button></div>`;
}

function monthScreen() {
  const s = monthlySummary(data, ui.month);
  const menu = ui.menu
    ? `<div class="menu">
        <button data-action="annals">📊 Annals · year &amp; all-time totals</button>
        <button data-action="go" data-to="#/goals">🛡 Protego · monthly goals</button>
        <button data-action="go" data-to="#/owl">🦉 Owl reminder at 21:30</button>
        <button data-action="export">📜 Geminio · export CSV</button>
      </div>`
    : '';
  const actions = `<button class="icon-btn" data-action="go" data-to="#/search" aria-label="Search">${ICON.search}</button>
    <button class="icon-btn" data-action="menu" aria-label="More">${ICON.more}</button>${menu}`;
  let body;
  if (ui.view === 'overview') {
    body = `${breakdownCard(s)}
      ${s.food.totalCents > 0 ? foodCard(s.food) : ''}
      ${ornament('The Ledger')}
      ${s.expenses.length ? `<div class="list">${s.expenses.map((e) => expenseRow(e)).join('')}</div>` : hint('The ledger is blank. Tap Record expense to write the first line.')}`;
  } else {
    const day = ui.day && monthOf(ui.day) === ui.month ? ui.day : null;
    const list = day ? onDay(data.expenses, day) : [];
    body = `${calendarCard()}
      ${day
        ? `${ornament(fmt(day, { weekday: 'long', day: 'numeric', month: 'long' }))}
           <p class="center h-small">Spent: ${format(list.reduce((a, e) => a + myShareCents(e), 0))}</p>
           ${list.length ? `<div class="list">${list.map((e) => expenseRow(e)).join('')}</div>` : hint('No gold left your vault this day.')}`
        : hint('Tap a day and say Revelio.')}`;
  }
  return `${header('Expensiarmus', { actions })}
  <main>
    ${installBanner()}${owlBanner()}
    <div class="month-switch">
      <button class="icon-btn gold" data-action="prev-month" aria-label="Previous month">${ICON.left}</button>
      <h2 class="h-large">${monthTitle(ui.month)}</h2>
      <button class="icon-btn gold" data-action="next-month" aria-label="Next month">${ICON.right}</button>
    </div>
    ${totalCard(s)}
    <div class="segmented">
      <button class="${ui.view === 'overview' ? 'on' : ''}" data-action="view" data-view="overview">📜 Overview</button>
      <button class="${ui.view === 'calendar' ? 'on' : ''}" data-action="view" data-view="calendar">📅 Calendar</button>
    </div>
    ${body}
  </main>
  <button class="fab" data-action="go" data-to="#/edit">✒️ Record expense</button>`;
}

// ---------- add / edit ----------

function startDraft(id) {
  const e = id ? find(data, id) : null;
  const inMonth = ui.day && monthOf(ui.day) === ui.month;
  const date = e?.date ?? (ui.view === 'calendar' && inMonth ? ui.day : ui.month === today().slice(0, 7) ? today() : `${ui.month}-01`);
  return {
    key: id ?? 'new',
    id: e?.id ?? null,
    title: e?.title ?? '',
    amount: e ? forInput(e.amountCents) : '',
    date,
    category: e?.category ?? 'SUPERMARKET',
    foodType: e?.foodType ?? null,
    split: e ? isSplit(e) : false,
    myPercent: e && isSplit(e) ? e.myPercent : 50,
    showErrors: false,
  };
}

function shareLine() {
  const cents = parseToCents(draft.amount);
  if (!draft.split || !cents) return '';
  return `You pay ${format(Math.floor((cents * draft.myPercent + 50) / 100))} of ${format(cents)}`;
}

function editScreen(id) {
  if (!draft || draft.key !== (id ?? 'new')) draft = startDraft(id);
  const d = draft;
  const cents = parseToCents(d.amount);
  const titleBad = d.showErrors && !d.title.trim();
  const amountBad = d.showErrors && !(cents > 0);
  const cat = category(d.category);
  const actions = `${d.id ? `<button class="icon-btn" data-action="ask-delete" aria-label="Delete">${ICON.trash}</button>` : ''}
    <button class="icon-btn" data-action="save" aria-label="Save">${ICON.check}</button>`;
  const catChips = CATEGORIES.map((c) =>
    `<button class="chip ${d.category === c.id ? 'on' : ''}" data-action="edit-cat" data-cat="${c.id}">${c.emoji} ${c.label}</button>`).join('');
  const foodChips = d.category === 'ENTERTAINMENT'
    ? `<div class="field"><span class="h-small">Is it food?</span><div class="chips">
        <button class="chip ${!d.foodType ? 'on' : ''}" data-action="edit-food" data-food="">Not food</button>
        ${FOOD_TYPES.map((f) => `<button class="chip ${d.foodType === f.id ? 'on' : ''}" data-action="edit-food" data-food="${f.id}">${f.emoji} ${f.label}</button>`).join('')}
      </div><p class="small soft">${esc(d.foodType ? `${foodType(d.foodType).nickname}: ${foodType(d.foodType).hint}` : 'Cinema, theatre, concerts, etc.')}</p></div>`
    : '';
  const split = d.split
    ? `<div class="field">
        <p id="pct-label">My share: ${d.myPercent}% · Theirs: ${100 - d.myPercent}%</p>
        <input type="range" min="1" max="99" step="1" value="${d.myPercent}" data-input="edit-pct">
        <div class="chips">${[50, 33, 25].map((p) => `<button class="chip" data-action="edit-pct" data-pct="${p}">${p === 50 ? '50-50' : `${p}%`}</button>`).join('')}</div>
        <p id="share-line">${shareLine()}</p>
      </div>`
    : '';
  const modal = ui.modal
    ? `<div class="modal-backdrop"><div class="card modal"><div class="paper">
        <h3 class="h-medium">Evanesco?</h3>
        <p>"${esc(d.title)}" will vanish from the ledger. This cannot be undone.</p>
        <div class="modal-actions"><button class="link" data-action="cancel-delete">Keep it</button><button class="btn danger" data-action="confirm-delete">Evanesco</button></div>
      </div></div></div>`
    : '';
  return `${header(d.id ? 'Amend expense' : 'Record expense', { backBtn: true, actions })}
  <main class="form">
    <label class="field"><span class="label">Title</span>
      <input type="text" value="${esc(d.title)}" placeholder="e.g. Butterbeer with friends" data-input="edit-title" class="${titleBad ? 'bad' : ''}" autocapitalize="sentences">
      ${titleBad ? '<span class="error">Enter a title</span>' : ''}</label>
    <label class="field"><span class="label">Gold spent</span>
      <span class="money-input ${amountBad ? 'bad' : ''}"><span>€</span><input type="text" inputmode="decimal" value="${esc(d.amount)}" placeholder="0.00" data-input="edit-amount"></span>
      ${amountBad ? '<span class="error">Enter an amount, e.g. 12.50</span>' : ''}</label>
    <label class="field"><span class="label">Date</span>
      <input type="date" value="${d.date}" data-input="edit-date"></label>
    <div class="field"><span class="h-small">Category</span><div class="chips">${catChips}</div>
      <p class="small soft">${esc(`${cat.nickname}: ${cat.hint}`)}</p></div>
    ${foodChips}
    <label class="switch-row"><span><span class="h-small">Split with a fellow wizard</span><span class="small soft">Only your share counts toward totals and goals</span></span>
      <input type="checkbox" class="switch" ${d.split ? 'checked' : ''} data-input="edit-split"></label>
    ${split}
    <button class="btn wide" data-action="save">Mischief managed · Save</button>
  </main>${modal}`;
}

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
    return header('📜 Ledger entry', { backBtn: true });
  }
  const c = category(e.category);
  const f = e.foodType ? foodType(e.foodType) : null;
  const row = (label, value, note = '') => `<div class="detail"><span class="h-small">${label}</span><span>${esc(value)}</span>${note ? `<span class="small soft italic">${esc(note)}</span>` : ''}</div>`;
  const actions = `<button class="icon-btn" data-action="ask-delete" aria-label="Delete">${ICON.trash}</button>
    <button class="icon-btn" data-action="amend" data-id="${esc(e.id)}" aria-label="Amend">${ICON.pen}</button>`;
  const modal = ui.modal
    ? `<div class="modal-backdrop"><div class="card modal"><div class="paper">
        <h3 class="h-medium">Evanesco?</h3>
        <p>"${esc(e.title)}" will vanish from the ledger. This cannot be undone.</p>
        <div class="modal-actions"><button class="link" data-action="cancel-delete">Keep it</button><button class="btn danger" data-action="confirm-delete" data-id="${esc(e.id)}">Evanesco</button></div>
      </div></div></div>`
    : '';
  return `${header('📜 Ledger entry', { backBtn: true, actions })}
  <main>
    ${card(`<div class="legend-row">${emblem(f ? f.emoji : c.emoji, c.color, 52)}
        <span class="row-main"><span class="h-large">${esc(e.title)}</span>
          <span class="total">${format(myShareCents(e))}</span>
          ${isSplit(e) ? `<span class="small soft">of ${format(e.amountCents)} in full</span>` : ''}</span></div>
      <hr class="gold-rule">
      ${row('Date', fmt(e.date, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }))}
      ${row('Category', `${c.emoji} ${c.label}`, c.nickname)}
      ${f ? row('Food', `${f.emoji} ${f.label}`, f.nickname) : ''}
      ${row('Split', splitLine(e, format))}`)}
    <button class="btn wide" data-action="amend" data-id="${esc(e.id)}">✏️ Amend expense</button>
    <button class="link center-link" data-action="ask-delete">Evanesco · delete</button>
  </main>${modal}`;
}

// ---------- category ----------

function categoryScreen(catId, foodId) {
  const cat = category(catId) ?? CATEGORIES[0];
  const groups = byCategory(data.expenses, cat.id, foodId);
  const count = groups.reduce((a, g) => a + g.expenses.length, 0);
  const foodChips = cat.id === 'ENTERTAINMENT'
    ? `<div class="chips">
        <button class="chip ${!foodId ? 'on' : ''}" data-action="cat-food" data-food="">All</button>
        ${FOOD_TYPES.map((f) => `<button class="chip ${foodId === f.id ? 'on' : ''}" data-action="cat-food" data-food="${f.id}">${f.emoji} ${f.label}</button>`).join('')}
      </div>`
    : '';
  return `${header(`${cat.emoji} ${cat.label}`, { backBtn: true })}
  <main>
    ${card(`<div class="legend-row">${emblem(cat.emoji, cat.color, 44)}
        <span class="row-main"><span class="h-small italic">${esc(cat.nickname)}</span><span>${esc(cat.hint)}</span></span></div>
      <span class="label">All time${foodId ? ` · ${foodType(foodId).label}` : ''}</span>
      <p class="total">${format(groups.reduce((a, g) => a + g.totalCents, 0))}</p>
      <p class="small soft">${count === 1 ? '1 expense' : `${count} expenses`} in ${groups.length === 1 ? '1 month' : `${groups.length} months`}</p>`)}
    ${foodChips}
    ${groups.length ? '' : hint('This vault is empty. Nothing recorded here yet.')}
    ${groups.map((g) => `<div class="group-head"><span class="h-small">${monthTitle(g.month)}</span><b>${format(g.totalCents)}</b></div>
      <div class="list">${g.expenses.map((e) => expenseRow(e)).join('')}</div>`).join('')}
  </main>`;
}

// ---------- search ----------

function searchResults() {
  if (!finder.q.trim() && finder.cats.length === 0) return hint('Say Accio! Type part of a title, or pick one or more vaults.');
  const found = search(data.expenses, finder.q, finder.cats);
  return `<p class="h-small">${found.length === 1 ? '1 expense' : `${found.length} expenses`} · ${format(found.reduce((a, e) => a + myShareCents(e), 0))}</p>
    ${found.length ? `<div class="list">${found.map((e) => expenseRow(e, true)).join('')}</div>` : hint('Revelio found nothing. Try another word.')}`;
}

function searchScreen() {
  return `${header('Accio', { backBtn: true })}
  <main>
    <label class="search-box">${ICON.search}<input type="search" value="${esc(finder.q)}" placeholder="Accio expense… (title or category)" data-input="search-q" enterkeyhint="search"></label>
    <div class="chips">${CATEGORIES.map((c) => `<button class="chip ${finder.cats.includes(c.id) ? 'on' : ''}" data-action="search-cat" data-cat="${c.id}">${c.emoji} ${c.label}</button>`).join('')}</div>
    <div id="results">${searchResults()}</div>
  </main>`;
}

// ---------- goals ----------

const goalValid = (text) => !text.trim() || parseToCents(text) > 0;
const goalCents = (text) => (text.trim() ? parseToCents(text) : null);

function goalsScreen() {
  const g = data.goals;
  const field = (key, label, cents) => `<label class="field"><span class="label">${label}</span>
    <span class="money-input"><span>€</span><input type="text" inputmode="decimal" value="${cents ? forInput(cents) : ''}" data-input="goal" data-key="${key}"></span>
    <span class="error" hidden>Enter an amount, or leave empty</span></label>`;
  return `${header('Protego · Goals', { backBtn: true })}
  <main class="form">
    <p class="soft">Protego! Cast a ward on your spending. Goals apply to every month until you change them; leave a field empty for no goal.</p>
    ${field('overall', 'Total per month', g.monthlyCents)}
    ${ornament('A ward for Revelry & Feasts')}
    ${CATEGORIES_WITH_GOALS.map(category).map((c) => field(c.id, `${c.emoji} ${c.label}`, g.perCategoryCents?.[c.id])).join('')}
    <p class="error" id="goal-warning" hidden></p>
    <button class="btn wide" data-action="save-goals">🛡 Cast the wards · Save</button>
  </main>`;
}

function checkGoals() {
  const inputs = [...app.querySelectorAll('[data-input="goal"]')];
  let ok = true;
  for (const i of inputs) {
    const valid = goalValid(i.value);
    i.closest('.money-input').classList.toggle('bad', !valid);
    i.closest('.field').querySelector('.error').hidden = valid;
    ok &&= valid;
  }
  const overall = goalCents(inputs.find((i) => i.dataset.key === 'overall').value);
  const sum = inputs.filter((i) => i.dataset.key !== 'overall').reduce((a, i) => a + (goalCents(i.value) ?? 0), 0);
  const warn = app.querySelector('#goal-warning');
  warn.hidden = !(ok && overall && sum > overall);
  warn.textContent = `The Entertainment goal (${format(sum)}) is more than the monthly total.`;
  app.querySelector('[data-action="save-goals"]').disabled = !ok;
  return ok;
}

function saveGoals() {
  if (!checkGoals()) return;
  const per = {};
  let monthly = null;
  for (const i of app.querySelectorAll('[data-input="goal"]')) {
    const v = goalCents(i.value);
    if (i.dataset.key === 'overall') monthly = v;
    else if (v) per[i.dataset.key] = v;
  }
  commit({ ...data, goals: { monthlyCents: monthly, perCategoryCents: per } });
  back();
}

// ---------- owl reminder ----------

function owlScreen() {
  if (nativeBridge) {
    const denied = window.expensiarmusOwlStatus === 'denied';
    return `${header('🦉 Owl reminder', { backBtn: true })}
  <main>
    ${card(`<h3 class="h-medium">A nightly owl at 21:30</h3>
      <label class="switch-row"><span><span class="h-small">Send the 21:30 owl</span>
        <span class="small soft">A notification every evening to record the day's expenses</span></span>
        <input type="checkbox" class="switch" ${reminderOn() ? 'checked' : ''} data-input="owl-toggle"></label>
      ${denied ? '<p class="error">Notifications are switched off for Expensiarmus. Turn them on in iPhone Settings → Notifications → Expensiarmus.</p>' : ''}
      <p class="small soft">If nothing was recorded that day it says <i>"🦉 Owl post: did you forget the ledger?"</i>, otherwise it shows the day's count and total. Tap it to go straight to Record expense.</p>`)}
  </main>`;
  }
  return `${header('🦉 Owl reminder', { backBtn: true })}
  <main>
    ${card(`<h3 class="h-medium">A nightly owl at 21:30</h3>
      <p>A web app can't send its own scheduled notifications on iPhone, so let iOS send the owl. Set it up once:</p>
      <ol class="steps">
        <li>Open the <b>Reminders</b> app and add a new reminder: <i>🦉 Record today's expenses in Expensiarmus</i>.</li>
        <li>Tap the <b>ⓘ</b> next to it, turn on <b>Date</b> and <b>Time</b>, and set the time to <b>21:30</b>.</li>
        <li>Set <b>Repeat</b> to <b>Daily</b>, then tap <b>Done</b>.</li>
      </ol>
      <p class="small soft">Every evening at 21:30 your iPhone will remind you. Open Expensiarmus from the home screen to record what you spent. Inside the app, an owl banner also appears after 21:00 if nothing has been recorded that day.</p>`)}
  </main>`;
}

// ---------- annals ----------

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
  return `${header('📊 Annals', { backBtn: true })}
  <main>
    <div class="segmented">
      <button class="${a.view === 'year' ? 'on' : ''}" data-action="annals-view" data-view="year">📅 By year</button>
      <button class="${a.view === 'all' ? 'on' : ''}" data-action="annals-view" data-view="all">✦ All time</button>
    </div>
    ${a.view === 'year' ? `<div class="month-switch">
      <button class="icon-btn gold" data-action="prev-year" aria-label="Previous year">${ICON.left}</button>
      <h2 class="h-large">${a.year}</h2>
      <button class="icon-btn gold" data-action="next-year" aria-label="Next year">${ICON.right}</button>
    </div>` : ''}
    ${card(`
      <h3 class="h-small">${a.view === 'year' ? `Gold spent in ${a.year}` : 'Gold spent, all time'}</h3>
      <p class="total">${format(s.totalCents)}</p>
      ${s.paidTotalCents !== s.totalCents ? `<p class="small soft">Your share. Full bills came to ${format(s.paidTotalCents)}.</p>` : ''}
      ${s.monthsCounted > 0 ? `<p>Monthly average: ${format(s.averagePerMonthCents)} over ${s.monthsCounted === 1 ? '1 month' : `${s.monthsCounted} months`}</p>` : ''}
      ${change}
      <p class="small soft">${esc(facts)}</p>`)}
    ${card(`<h3 class="h-medium">${a.view === 'year' ? 'Month by month' : 'Year by year'}</h3>
      ${s.bars.length ? barChart(s.bars, a.bar) + barInfo : '<p class="soft">Nothing recorded yet.</p>'}`)}
    ${breakdownCard(s)}
    ${s.food.totalCents > 0 ? foodCard(s.food) : ''}
  </main>`;
}

// ---------- CSV export ----------

async function exportCsv() {
  const name = `expensiarmus-${today()}.csv`;
  const blob = new Blob(['﻿' + toCsv(data.expenses)], { type: 'text/csv' });
  const file = new File([blob], name, { type: 'text/csv' });
  if (nativeBridge) {
    post({ type: 'share', filename: name, text: '\uFEFF' + toCsv(data.expenses) });
    return;
  }
  try {
    if (navigator.canShare?.({ files: [file] })) {
      await navigator.share({ files: [file], title: 'Expensiarmus ledger' });
    } else {
      const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(blob), download: name });
      a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    }
    toast('Geminio! Ledger copied to CSV.');
  } catch (err) {
    if (err?.name !== 'AbortError') toast('The spell failed: export did not work.');
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
  if (r.name !== 'month') ui.menu = false;
  if (r.name !== ui.lastScreen && (r.name === 'month' || r.name === 'annals')) ui.highlighted = null;
  ui.lastScreen = r.name;
  switch (r.name) {
    case 'edit': app.innerHTML = editScreen(r.a); break;
    case 'view': app.innerHTML = viewScreen(r.a); break;
    case 'category': app.innerHTML = categoryScreen(r.a, r.b); break;
    case 'search': app.innerHTML = searchScreen(); break;
    case 'goals': app.innerHTML = goalsScreen(); break;
    case 'owl': app.innerHTML = owlScreen(); break;
    case 'annals': app.innerHTML = annalsScreen(); break;
    default: app.innerHTML = monthScreen();
  }
  if (r.name === 'goals') checkGoals();
}

const actions = {
  back,
  go: (el) => go(el.dataset.to),
  menu: () => { ui.menu = !ui.menu; render(); },
  'prev-month': () => { ui.month = shiftMonth(ui.month, -1); ui.highlighted = null; render(); },
  'next-month': () => { ui.month = shiftMonth(ui.month, 1); ui.highlighted = null; render(); },
  view: (el) => { ui.view = el.dataset.view; render(); },
  slice: (el) => {
    const c = el.dataset.cat;
    ui.highlighted = ui.highlighted === c ? null : c;
    render();
  },
  'slice-clear': () => { ui.highlighted = null; render(); },
  annals: () => {
    ui.menu = false;
    ui.annals = { view: 'year', year: Number(ui.month.slice(0, 4)), bar: null };
    ui.highlighted = null;
    go('#/annals');
  },
  'annals-view': (el) => { ui.annals = { ...ui.annals, view: el.dataset.view, bar: null }; ui.highlighted = null; render(); },
  'prev-year': () => { ui.annals = { ...ui.annals, year: ui.annals.year - 1, bar: null }; ui.highlighted = null; render(); },
  'next-year': () => { ui.annals = { ...ui.annals, year: ui.annals.year + 1, bar: null }; ui.highlighted = null; render(); },
  bar: (el) => { const i = Number(el.dataset.index); ui.annals = { ...ui.annals, bar: ui.annals.bar === i ? null : i }; render(); },
  'open-month': (el) => { ui.month = el.dataset.month; ui.view = 'overview'; ui.highlighted = null; navDepth = 0; location.hash = '#/'; },
  'open-year': (el) => { ui.annals = { view: 'year', year: Number(el.dataset.year), bar: null }; ui.highlighted = null; render(); },
  day: (el) => { ui.day = el.dataset.day; render(); },
  category: (el) => go(`#/category/${el.dataset.cat}${el.dataset.food ? `/${el.dataset.food}` : ''}`),
  'cat-food': (el) => location.replace(`#/category/${route().a}${el.dataset.food ? `/${el.dataset.food}` : ''}`),
  open: (el) => go(`#/view/${encodeURIComponent(el.dataset.id)}`),
  amend: (el) => go(`#/edit/${encodeURIComponent(el.dataset.id)}`),
  export: () => { ui.menu = false; render(); exportCsv(); },
  'edit-cat': (el) => { draft.category = el.dataset.cat; if (draft.category !== 'ENTERTAINMENT') draft.foodType = null; render(); },
  'edit-food': (el) => { draft.foodType = el.dataset.food || null; render(); },
  'edit-pct': (el) => { draft.myPercent = Number(el.dataset.pct); render(); },
  save: saveDraft,
  'ask-delete': () => { ui.modal = true; render(); },
  'cancel-delete': () => { ui.modal = false; render(); },
  'confirm-delete': (el) => {
    const id = el.dataset.id || draft?.id;
    commit(remove(data, id));
    ui.modal = false;
    draft = null;
    back(); // from the edit screen this lands on the deleted expense's view, which steps back again
  },
  'search-cat': (el) => {
    const c = el.dataset.cat;
    finder.cats = finder.cats.includes(c) ? finder.cats.filter((x) => x !== c) : [...finder.cats, c];
    el.classList.toggle('on');
    app.querySelector('#results').innerHTML = searchResults();
  },
  'save-goals': saveGoals,
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
  'search-q': (el) => { finder.q = el.value; app.querySelector('#results').innerHTML = searchResults(); },
  goal: () => checkGoals(),
  'owl-toggle': (el) => { storage.setItem(REMINDER_KEY, el.checked ? 'on' : 'off'); scheduleOwls(); },
};

app.addEventListener('click', (ev) => {
  const el = ev.target.closest('[data-action]');
  if (!el) {
    if (ui.menu) { ui.menu = false; render(); }
    return;
  }
  if (ui.menu && el.dataset.action !== 'menu' && !el.closest('.menu')) ui.menu = false;
  actions[el.dataset.action]?.(el, ev);
});
const onInput = (ev) => {
  const el = ev.target.closest('[data-input]');
  if (el) inputs[el.dataset.input]?.(el);
};
app.addEventListener('input', onInput);
app.addEventListener('change', (ev) => {
  // Checkboxes and date pickers report through "change" on some browsers.
  if (ev.target.matches('[data-input="edit-split"], [data-input="edit-date"], [data-input="owl-toggle"]')) onInput(ev);
});
window.addEventListener('hashchange', render);

navigator.storage?.persist?.();
if (!nativeBridge && 'serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});

// Hooks the iPhone app calls: open a screen from a notification, report notification permission.
window.expensiarmusOpen = (hash) => { navDepth = 0; location.hash = hash; };
window.addEventListener('owlstatus', () => { if (route().name === 'owl') render(); });
// Keep the 30-day window of owls rolling whenever the app comes back to the foreground.
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') scheduleOwls(); });

render();
scheduleOwls();
