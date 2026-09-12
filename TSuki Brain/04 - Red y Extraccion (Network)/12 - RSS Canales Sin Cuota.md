# 04.12 — Feeds RSS de Canales Sin Consumo de Cuota

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Descarga y procesa los últimos videos subidos por canales de YouTube a través de sus feeds XML públicos:
- URL del feed: `https://www.youtube.com/feeds/videos.xml?channel_id={channelId}`.
- Parsea el contenido mediante `XmlPullParser` nativo de Android:
  - `<yt:videoId>`: Identificador del video.
  - `<title>`: Título del contenido.
  - `<media:thumbnail>`: Carátula.
  - `<published>`: Fecha de publicación ISO 8601.
- Permite actualizar las suscripciones de decenas de canales en pocos segundos sin ejecutar scraping de páginas web pesadas ni consumir cuotas de API.

**Archivos fuente clave:**
- [`network/ChannelRssClient.kt:L21-178`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/ChannelRssClient.kt#L21-L178)
- [`data/subscriptions/NewVideosWorker.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/subscriptions/NewVideosWorker.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Es el mecanismo más rápido, ligero y privado que existe para monitorizar nuevos lanzamientos de artistas sin depender de cuentas de Google.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Cliente de red especializado en sindicación XML en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El analizador sintáctico `XmlPullParser` debe manejar namespaces XML (`yt:`, `media:`) para no perder el `videoId`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Utilizado intensivamente por `NewVideosWorker` en segundo plano.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[10 - Subscriptions Feed y NewVideosWorker]], [[22 - ChannelScreen y Exploracion de Artistas]].

---

## 7. Guía rápida para una IA nueva
- Para obtener los videos recientes de un canal, llama a `ChannelRssClient.fetchChannelVideos(channelId)`.
