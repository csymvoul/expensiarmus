// Offline support: serve the app from the cache, refresh the cache in the background.
// Bump VERSION when files change so old caches are cleared.
const VERSION = 'expensiarmus-web-1.9.0';
const SHELL = [
  './', 'index.html', 'manifest.webmanifest', 'css/style.css', 'fonts/cinzel.ttf',
  'js/app.js', 'js/model.js', 'js/money.js', 'js/summary.js', 'js/queries.js', 'js/csv.js', 'js/store.js', 'js/reminder.js', 'js/goals.js', 'js/words.js', 'js/settings.js',
  'icons/favicon-64.png', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png', 'icons/apple-touch-icon.png',
  'icons/emblems/cat_rent.png', 'icons/emblems/cat_supermarket.png', 'icons/emblems/cat_entertainment.png', 'icons/emblems/cat_bills.png', 'icons/emblems/cat_shopping.png', 'icons/emblems/cat_fuel.png', 'icons/emblems/cat_other.png', 'icons/emblems/food_delivery.png', 'icons/emblems/food_dine_out.png', 'icons/emblems/food_day_to_day.png', 'icons/emblems/tab_ledger.png', 'icons/emblems/tab_calendar.png', 'icons/emblems/tab_annals.png', 'icons/emblems/tab_goals.png', 'icons/emblems/tab_settings.png',
  'icons/emblems/logo_pouch.png', 'icons/emblems/search_light.png', 'icons/emblems/search_dark.png', 'icons/emblems/set_geminio.png', 'icons/emblems/set_reminder.png', 'icons/emblems/set_theme_system.png', 'icons/emblems/set_theme_light.png', 'icons/emblems/set_theme_dark.png',
];

self.addEventListener('install', (event) => {
  // Bypass the browser's HTTP cache so a new version never mixes with stale files of the old one.
  event.waitUntil(
    caches.open(VERSION)
      .then((c) => c.addAll(SHELL.map((url) => new Request(url, { cache: 'reload' }))))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET' || new URL(event.request.url).origin !== location.origin) return;
  event.respondWith(
    caches.open(VERSION).then(async (cache) => {
      const cached = await cache.match(event.request, { ignoreSearch: true });
      const fresh = fetch(event.request, { cache: 'no-cache' })
        .then((res) => {
          if (res.ok) cache.put(event.request, res.clone());
          return res;
        })
        .catch(() => cached);
      return cached || fresh;
    }),
  );
});
