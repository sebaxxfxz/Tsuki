# 10.05 — SpotifyTrackMatcher (Similitud de Bigramas 0.60)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Algoritmo de coincidencia léxica mediante bi-gramas de caracteres en `playlistimport/SpotifyTrackMatcher.kt`:
- Descompone dos cadenas de texto (título de Spotify vs título de YouTube) en pares de caracteres adyacentes (*bi-gramas*):
  - `"rock"` -> `["ro", "oc", "ck"]`.
- Calcula el coeficiente de Sorensen-Dice:
  $$Similitud = rac{2 \cdot |B_1 \cap B_2|}{|B_1| + |B_2|}$$
- **Umbral de Aceptación: `>= 0.60`**: Si la similitud supera el 60% y el artista coincide, la pista se declara coincidencia válida.

**Archivos fuente clave:**
- [`playlistimport/SpotifyTrackMatcher.kt:L10-70`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/SpotifyTrackMatcher.kt#L10-L70)

---

## 2. PARA QUÉ existe (problema que resuelve)
Los títulos en YouTube a menudo contienen variaciones mínimas de puntuación, mayúsculas o espacios respecto a Spotify. El análisis por bigramas tolera estos desajustes menores con rapidez computacional superior a Levenshtein puro.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Algoritmo de coincidencia en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El umbral de `0.60`. Bajarlo a 0.50 genera falsos positivos con remixes no deseados.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Probado exhaustivamente contra un dataset de 500 canciones internacionales.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Resolucion y Matching Difuso]], [[06 - FuzzyMatcher Fallback 0.50]].

---

## 7. Guía rápida para una IA nueva
- Para comparar dos títulos: `SpotifyTrackMatcher.matchScore(spotifyTitle, ytmTitle) >= 0.60f`.
