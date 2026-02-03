const APP_CACHE = "vw-app-v2";
const DATA_CACHE = "vw-data-v2";
const CORE_ASSETS = [
  "/",
  "/offline",
  "/manifest.webmanifest",
  "/icon.svg"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(APP_CACHE).then((cache) => cache.addAll(CORE_ASSETS))
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== APP_CACHE && key !== DATA_CACHE)
          .map((key) => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  );
});

async function cacheFirst(request) {
  const cache = await caches.open(DATA_CACHE);
  const cached = await cache.match(request);
  if (cached) return cached;
  const fresh = await fetch(request);
  if (fresh && fresh.ok) cache.put(request, fresh.clone());
  return fresh;
}

async function staleWhileRevalidate(request) {
  const cache = await caches.open(APP_CACHE);
  const cached = await cache.match(request);
  const fetchPromise = fetch(request).then((fresh) => {
    if (fresh && fresh.ok) cache.put(request, fresh.clone());
    return fresh;
  }).catch(() => undefined);
  return cached || fetchPromise;
}

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);

  if (url.origin === self.location.origin) {
    if (url.pathname.startsWith("/api/public/menu") || url.pathname.includes("/api/public/menu-item/") && url.pathname.endsWith("/modifiers")) {
      event.respondWith(cacheFirst(request));
      return;
    }
    if (request.destination === "image") {
      event.respondWith(cacheFirst(request));
      return;
    }
    if (url.pathname.startsWith("/_next/") || url.pathname === "/icon.svg") {
      event.respondWith(staleWhileRevalidate(request));
      return;
    }
    if (request.mode === "navigate") {
      event.respondWith(
        fetch(request)
          .then((response) => {
            const copy = response.clone();
            caches.open(APP_CACHE).then((cache) => cache.put(request, copy));
            return response;
          })
          .catch(() => caches.match(request).then((r) => r || caches.match("/offline")))
      );
      return;
    }
  }
});
