# 03 - SponsorBlock, Return YouTube Dislike y RSS de Canales

> **Ubicación:** `app/src/main/java/com/example/tsuki/network/`
> **Archivos:** `SponsorBlockClient.kt`, `ReturnYouTubeDislikeClient.kt`, `ChannelRssClient.kt`

---

## 🚫 SponsorBlock: Eliminación de Ruidos no Musicales

`SponsorBlockClient` consulta la API comunitaria de SponsorBlock:
- **Categorías consultadas:** `music_offtopic` (partes no musicales de videoclips como diálogos iniciales), `sponsor` (patrocinios pagados), `selfpromo` (promociones del propio artista de merchandising), `interaction` (recordatorios de suscribirse o dar like) e `intro`/`outro`.
- **Comportamiento:** Devuelve una lista de rangos temporales `Pair(startMs, endMs)`. El hilo de progreso de `PlayerController` salta el intervalo cuando la aguja de reproducción lo alcanza.

---

## 👍 Return YouTube Dislike (RYD)

`ReturnYouTubeDislikeClient` consulta la base de datos de votos de RYD mediante llamadas HTTPS no bloqueantes:
- **Datos obtenidos:** `likes`, `dislikes`, `rating` (0.0 a 5.0) y `viewCount`.
- **Visualización:** Permite a la interfaz de usuario mostrar una barra de aprobación fidedigna en la vista de video expandido.

---

## 📰 ChannelRssClient: Feeds de Canales Ultrarrápidos

Para consultar los videos más recientes de un artista o canal:
- En lugar de realizar peticiones JSON complejas y lentas a InnerTube, `ChannelRssClient` descarga el feed Atom XML nativo que Google mantiene en `https://www.youtube.com/feeds/videos.xml?channel_id=<channelId>`.
- Es procesado mediante un parser XML ligero, extrayendo los 15 videos más recientes en menos de 100ms.
