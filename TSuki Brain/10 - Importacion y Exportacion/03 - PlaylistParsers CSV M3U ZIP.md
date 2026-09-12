# 10.03 — PlaylistParsers (Parsers de CSV y M3U Estándar)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa los analizadores sintácticos de archivos planos en `playlistimport/PlaylistParsers.kt`:
1. **Parser M3U Extendido (`PlaylistM3uParser`)**:
   - Analiza directivas `#EXTINF:{duration},{artist} - {title}`.
   - Si el archivo contiene enlaces directos de YouTube (`https://music.youtube.com/watch?v={id}`), extrae directamente el `videoId` sin requerir búsqueda difusa posterior.
2. **Parser CSV Genérico (`PlaylistCsvParser`)**:
   - Analiza la fila de encabezados y detecta dinámicamente columnas de Título ("Track Name", "Title", "Song", "Nombre") y Artista ("Artist Name", "Artist", "Artista").
   - Maneja comillas dobles escapadas y celdas multilínea según el estándar RFC 4180.

**Archivos fuente clave:**
- [`playlistimport/PlaylistParsers.kt:L10-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/PlaylistParsers.kt#L10-L180)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite importar archivos de listas de reproducción exportados desde cualquier reproductor de escritorio (Winamp, Foobar2000, VLC).

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Parsers de archivos en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El manejo de comillas en CSV (`csvEscape` / `parseCsvLine`).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Tolerancia a formatos con delimitador por coma o punto y coma.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Parsers Multiplataforma y ArchiveTune]], [[08 - PlaylistExporters M3U CSV]].

---

## 7. Guía rápida para una IA nueva
- Si un archivo M3U contiene IDs de YouTube, asígnalos directamente a `ImportedSong.videoId`.
