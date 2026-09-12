# 03.15 — DownloadEngine (Descarga Paralela y Multiplexado MP4)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Motor de descarga multimedia offline de alto rendimiento:
1. **Descarga Segmentada por Rangos**:
   - Divide archivos de más de 2 MB en 4 segmentos paralelos con cabecera `Range: bytes=start-end`.
   - Descarga a archivos temporales `.seg0` a `.seg3` y los fusiona atómicamente.
   - Si el servidor CDN no soporta HTTP 206 Partial Content, cae de forma transparente a descarga secuencial simple.
2. **Multiplexado Nativo de Video/Audio (`muxAudioVideo`)**:
   - Para videos en 1080p, YouTube sirve el stream de video DASH (sin audio) y el stream de audio por separado.
   - `DownloadEngine` descarga ambos flujos y los une en un archivo `.mp4` contenedor utilizando `MediaExtractor` y `MediaMuxer` del SDK de Android sin consumir batería en transcodificación pesada.
3. **Persistencia de Metadatos**:
   - Guarda `$videoId.meta.json` junto al archivo multimedia con el título, artista, álbum, duración y tipo de medio.

**Archivos fuente clave:**
- [`playback/DownloadEngine.kt:L24-650`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/DownloadEngine.kt#L24-L650)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite descargas hasta 4 veces más rápidas en conexiones móviles y posibilita la descarga de videos musicales en Full HD con audio de alta fidelidad sin requerir binarios externos de FFmpeg.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Pertenece a `playback/` al materializar archivos locales consumidos por el reproductor.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Limpieza de archivos temporales `.part`, `.seg*`, `.mux.tmp` en caso de error o cancelación.
- Monotonicidad estricta de marcas de tiempo en el multiplexor (`MediaMuxer`).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Descargas de lotes en segundo plano respetando la restricción de red no medida si está configurada.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[06 - Cache de Streaming y Descargas]], [[18 - LibraryScreen y Likes Sincronizados]].

---

## 7. Guía rápida para una IA nueva
- Para iniciar una descarga, invoca `DownloadEngine.downloadTrack(track)` o `DownloadEngine.downloadVideo(track, quality)`.
