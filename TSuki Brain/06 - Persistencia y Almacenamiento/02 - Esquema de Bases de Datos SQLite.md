# 06.02 — Esquema Maestro de las 5 Bases de Datos SQLite

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Cataloga los esquemas SQL completos de las 5 bases de datos SQLite nativas del proyecto:

1. **`tsuki_playlists.db` (`LocalPlaylistManager`)**:
   - `local_playlists`: `id TEXT PRIMARY KEY`, `name TEXT NOT NULL`, `created_at INTEGER NOT NULL`, `custom_cover_uri TEXT`.
   - `local_playlist_songs`: `playlist_id TEXT NOT NULL`, `position INTEGER NOT NULL`, `track_json TEXT NOT NULL`, `PRIMARY KEY (playlist_id, position)`.
2. **`tsuki_history.db` (`WatchHistoryManager`)**:
   - `watch_history`: `video_id TEXT PRIMARY KEY`, `title TEXT`, `artist TEXT`, `thumbnail_url TEXT`, `duration_seconds INTEGER`, `last_watched INTEGER`, `play_count INTEGER DEFAULT 1`.
   - `play_events`: `id INTEGER PRIMARY KEY AUTOINCREMENT`, `video_id TEXT NOT NULL`, `timestamp INTEGER NOT NULL`, `duration_played_ms INTEGER NOT NULL`.
3. **`tsuki_favorites.db` (`FavoritesManager`)**:
   - `favorites`: `video_id TEXT PRIMARY KEY`, `track_json TEXT NOT NULL`, `added_at INTEGER NOT NULL`.
4. **`lyrics.db` (`LyricsDatabase`)**:
   - `lyrics`: `video_id TEXT PRIMARY KEY`, `raw_lyrics TEXT NOT NULL`, `has_word_sync INTEGER DEFAULT 0`, `cached_timestamp INTEGER NOT NULL`.
5. **`tsuki_tags.db` (`TrackTagsManager`)**:
   - `track_tags`: `video_id TEXT NOT NULL`, `mood TEXT NOT NULL`, `added_at INTEGER NOT NULL`, `PRIMARY KEY (video_id, mood)`.
6. **`tsuki_recognition.db` (`RecognitionHistoryManager`)**:
   - `recognition_history`: `id INTEGER PRIMARY KEY AUTOINCREMENT`, `shazam_id TEXT`, `title TEXT NOT NULL`, `artist TEXT NOT NULL`, `album TEXT`, `cover_url TEXT`, `matched_at INTEGER NOT NULL`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza la integridad de datos, serialización unívoca y previene errores de sintaxis en consultas SQL manuales.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Referencia de esquemas en `06 - Persistencia y Almacenamiento`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los nombres exactos de las bases de datos y columnas. Cambiarlos sin script de migración corromperá la app de los usuarios al actualizar.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Cierre riguroso de `Cursor` mediante `.use { ... }` para prevenir fugas de cursores SQLite.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Arquitectura SQLite Sin Room]], [[04 - Sistema de Backup y Restauracion]].

---

## 7. Guía rápida para una IA nueva
- Para inspeccionar una base de datos en dispositivo: `adb -s fb74ec96 shell "sqlite3 /data/data/com.example.tsuki/databases/tsuki_playlists.db .schema"`.
