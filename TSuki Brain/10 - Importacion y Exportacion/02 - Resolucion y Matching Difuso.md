# 10.02 — Resolución de Canciones y Matching Difuso

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Algoritmo de resolución de pistas importadas contra el catálogo de YouTube Music:
- Una lista importada solo contiene texto plano (ej. `"Bohemian Rhapsody" - "Queen"`).
- `ImportSongResolver` realiza una búsqueda en InnerTube (`searchMusic`) con filtro `SONGS`.
- Para cada resultado devuelto, calcula una puntuación de coincidencia difusa combinando:
  1. Coincidencia de título mediante Bigramas (`SpotifyTrackMatcher.kt`, umbral `>= 0.60`).
  2. Coincidencia de artista.
  3. Coincidencia de duración temporal: penaliza candidatos cuya duración difiera en más de 8-12 segundos de la duración original de la pista.
  4. Fallback con ratio de ordenamiento de tokens Levenshtein (`FuzzyMatcher.kt`, umbral `>= 0.50`).

**Archivos fuente clave:**
- [`playlistimport/ImportSongResolver.kt:L20-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/ImportSongResolver.kt#L20-L140)
- [`playlistimport/SpotifyTrackMatcher.kt:L10-70`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/SpotifyTrackMatcher.kt#L10-L70)
- [`playlistimport/FuzzyMatcher.kt:L10-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/FuzzyMatcher.kt#L10-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que la canción encontrada en YouTube Music sea la versión de estudio original y no una versión en vivo, un cover de aficionados o una entrevista.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Algoritmos de coincidencia en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los umbrales de 0.60 para bigramas y 0.50 para token sort ratio. Reducirlos introduce falsos positivos con versiones incorrectas.
- La tolerancia de duración temporal.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Pruebas unitarias en `SpotifyImportTest.kt`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[07 - ImportSongResolver Semaphore 3]], [[23 - ImportPlaylistScreen y Asistente de Migracion]].

---

## 7. Guía rápida para una IA nueva
- Para resolver una canción: `ImportSongResolver.resolve(importedSong)`.
