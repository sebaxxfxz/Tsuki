# 06.05 — LocalPlaylistManager (Gestión de Playlists en tsuki_playlists.db)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Administrador de listas de reproducción locales en `data/local/LocalPlaylistManager.kt`:
- Gestiona la creación de listas (`createPlaylist`), renombramiento, eliminación y asignación de carátula personalizada (`custom_cover_uri`).
- Añade canciones individuales o en lotes serializando el objeto `MediaTrack` a JSON en `local_playlist_songs`.
- Mantiene el orden estricto de las pistas mediante el campo `position` (0..N).
- Permite reordenar canciones arrastrando en la UI actualizando las posiciones en una única transacción SQLite atómica.
- Emite `_playlistsVersion` (`StateFlow<Long>`) para refrescar la interfaz reactivamente.

**Archivos fuente clave:**
- [`data/local/LocalPlaylistManager.kt:L29-152`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/LocalPlaylistManager.kt#L29-L152)

---

## 2. PARA QUÉ existe (problema que resuelve)
Provee una experiencia completa de gestión de playlists locales sin requerir conexión a internet ni cuenta de YouTube.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Almacén de playlists locales en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La clave primaria compuesta: `PRIMARY KEY (playlist_id, position)`.
- El método `migrateTablePreservingData` para adición segura de columnas en upgrades.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Soporte para exportación a M3U y CSV mediante `PlaylistExporters`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Esquema de Bases de Datos SQLite]], [[18 - LibraryScreen y Likes Sincronizados]].

---

## 7. Guía rápida para una IA nueva
- Para crear una lista: `LocalPlaylistManager.getInstance(context).createPlaylist(name)`.
