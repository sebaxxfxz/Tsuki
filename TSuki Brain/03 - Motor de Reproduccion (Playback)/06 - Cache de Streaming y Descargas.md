# 03.06 — Caché de Streaming y Descargas Offline

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la persistencia de medios de audio y video en dos niveles complementarios:
1. **Caché Transparente de Streaming (`PlayerCacheProvider.kt`)**:
   - Singleton de `SimpleCache` de AndroidX Media3 alojado en `cacheDir/exoplayer`.
   - Indexado por `StandaloneDatabaseProvider` en SQLite.
   - Aplica una política de expulsión LRU (*Least Recently Used*) configurable desde Ajustes (128 MB a 2048 MB, valor por defecto: 1024 MB).
   - Utilizado por `ExoPlayer` primario, secundario y por `NextTrackPrecacher`.
2. **Descargas Fuera de Línea Completas (`DownloadEngine.kt`)**:
   - Descarga archivos completos a `context.filesDir/tsuki_offline_downloads`.
   - Descarga segmentada paralela (4 hilos con `Content-Range`) para maximizar velocidad en redes de alta latencia.
   - Multiplexado nativo de video/audio mediante `MediaExtractor` y `MediaMuxer` (une video DASH 1080p con audio AAC sin requerir FFmpeg).
   - Metadatos persistidos en formato JSON adjunto (`$id.meta.json`).

**Archivos fuente clave:**
- [`playback/PlayerCacheProvider.kt:L15-52`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerCacheProvider.kt#L15-L52)
- [`playback/DownloadEngine.kt:L24-650`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/DownloadEngine.kt#L24-L650)
- [`playback/NextTrackPrecacher.kt:L14-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/NextTrackPrecacher.kt#L14-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
1. Evita descargar múltiples veces la misma canción si el usuario retrocede o la repite.
2. Permite escuchar listas y álbumes enteros en modo avión o sin cobertura móvil con fidelidad intacta.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Pertenece a `playback/` porque conecta directamente con las fuentes de datos `DataSource.Factory` de ExoPlayer.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Singleton SimpleCache**: Jamás instanciar dos `SimpleCache` sobre el mismo directorio. Provoca una excepción fatal `CacheException` por bloqueo concurrente de la base de datos de Media3.
- **`FLAG_IGNORE_CACHE_ON_ERROR`**: Debe mantenerse activo en `CacheDataSource.Factory` para que si un archivo en caché se corrompe, ExoPlayer caiga transparentemente a la red en lugar de detener la música.
- **Monotonicidad de PTS en Muxer**: En `DownloadEngine.kt`, los timestamps `presentationTimeUs` DEBEN satisfacer `maxOf(lastPts, videoTime)` para no violar los requisitos del contenedor MP4 de Android.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Vaciado de caché mediante diálogo destructivo con confirmación y confirmación por Snackbar en `SettingsScreen`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[15 - DownloadEngine Paralelo y Mux]], [[17 - PlayerCacheProvider SimpleCache]], [[11 - NextTrackPrecacher Buffer 14MB]].

---

## 7. Guía rápida para una IA nueva
- Para comprobar si una canción está disponible offline, llama a `DownloadEngine.isDownloaded(trackId)`.
