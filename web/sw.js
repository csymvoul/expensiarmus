// Offline support: serve the app from the cache, refresh the cache in the background.
// Bump VERSION when files change so old caches are cleared.
const VERSION = 'expensiarmus-web-1.6.2';
const SHELL = [
  './', 'index.html', 'manifest.webmanifest', 'css/style.css', 'fonts/cinzel.ttf',
  'js/app.js', 'js/model.js', 'js/money.js', 'js/summary.js', 'js/queries.js', 'js/csv.js', 'js/store.js', 'js/reminder.js', 'js/goals.js', 'js/words.js', 'js/settings.js',
  'icons/icon.svg', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/apple-touch-icon.png',
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
