# 10.09 — Importación de Respaldos ZIP de ArchiveTune (Extracción de song.db)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Extrae y parsea copias de seguridad de la aplicación ArchiveTune en `playlistimport/PlaylistParsers.kt`:
- Descomprime el archivo `.zip` de respaldo a una carpeta temporal privada en `context.cacheDir`.
- Extrae los archivos de base de datos: `song.db`, `song.db-wal` y `song.db-shm`.
- Abre la base de datos de forma nativa con `SQLiteDatabase.openDatabase(..., OPEN_READONLY)`:
  - Consulta la tabla `playlist` y selecciona la lista con mayor número de pistas asociadas.
  - Consulta la tabla `song` filtrando `WHERE liked = 1` para recuperar las canciones favoritas.
  - Cruza con la tabla intermedia `song_artist_map` y `artist` para resolver los nombres de los artistas asociados.
- Si las canciones tienen `id` de 11 caracteres de YouTube, se importan directamente sin necesidad de búsqueda difusa.
- Al terminar, ejecuta `tempDir.deleteRecursively()` para no dejar basura en el almacenamiento.

**Archivos fuente clave:**
- [`playlistimport/PlaylistParsers.kt:L200-280`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/PlaylistParsers.kt#L200-L280)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a miles de usuarios que utilizaban ArchiveTune migrar sus listas completas y sus favoritos a TSuki con preservación del 100% de los identificadores de video exactos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Parser especializado en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La extracción conjunta de `song.db-wal` y `song.db-shm`. Si SQLite estaba en modo WAL al respaldarse y se omite el WAL, la base de datos aparecerá vacía o corrupta.
- El borrado recursivo del directorio temporal en el bloque `finally`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Apertura en modo solo lectura (`OPEN_READONLY`) para evitar bloqueos.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Parsers Multiplataforma y ArchiveTune]], [[23 - ImportPlaylistScreen y Asistente de Migracion]].

---

## 7. Guía rápida para una IA nueva
- Para extraer playlists de un ZIP de ArchiveTune: `ArchiveTuneBackupParser.extractArchiveTuneDbPlaylists(zipFile, context)`.
