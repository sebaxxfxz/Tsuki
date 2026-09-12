# 10.01 — Parsers Multiplataforma y Soporte ArchiveTune

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Arquitectura del importador universal de listas de reproducción en `playlistimport/`:
- **Formatos Soportados**:
  1. Archivos CSV de Exportify de Spotify (`SpotifyPlaylistParser.kt`).
  2. Archivos JSON de exportación oficial de Spotify Takeout.
  3. Archivos M3U / M3U8 universales (`PlaylistParsers.kt`).
  4. Archivos CSV genéricos con detección automática de columnas de Título y Artista.
  5. Copias de seguridad ZIP de la aplicación ArchiveTune (`ArchiveTuneBackupParser`).
- Extrae listas de canciones normalizadas (`ImportedSong`) con título, artista, álbum y duración aproximada para ser resueltas en YouTube Music.

**Archivos fuente clave:**
- [`playlistimport/PlaylistParsers.kt:L1-308`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/PlaylistParsers.kt#L1-L308)
- [`playlistimport/SpotifyPlaylistParser.kt:L1-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/SpotifyPlaylistParser.kt#L1-L120)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a cualquier usuario migrar toda su música desde Spotify, iTunes, Winamp, VLC o ArchiveTune a TSuki sin tener que rehacer sus listas manualmente canción por canción.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Módulo de interoperabilidad en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El soporte para archivos UTF-8 con o sin marca de orden de bytes (BOM `\uFEFF`). Omitir el filtrado de BOM rompe la primera línea del archivo CSV o M3U.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Parseo en `Dispatchers.IO` para evitar bloqueos con listas de miles de canciones.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Resolucion y Matching Difuso]], [[07 - ImportSongResolver Semaphore 3]].

---

## 7. Guía rápida para una IA nueva
- Para parsear cualquier archivo de texto de playlist: `PlaylistParsers.parse(content, fileExtension)`.
