# 10.06 — FuzzyMatcher (Token Sort Ratio Fallback 0.50)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Algoritmo de respaldo de coincidencia difusa en `playlistimport/FuzzyMatcher.kt`:
- Se activa cuando el algoritmo de bigramas no alcanza el umbral de 0.60.
- Implementa el algoritmo **Token Sort Ratio** de Levenshtein:
  1. Convierte a minúsculas y elimina signos de puntuación.
  2. Divide la cadena en palabras individuales (*tokens*).
  3. Ordena alfabéticamente las palabras.
  4. Une las palabras ordenadas y calcula la distancia de edición Levenshtein normalizada.
- Resuelve casos donde el artista y el título aparecen invertidos en YouTube (ej. `"Queen - Bohemian Rhapsody"` vs `"Bohemian Rhapsody - Queen"`).
- **Umbral de Aceptación: `>= 0.50`**.

**Archivos fuente clave:**
- [`playlistimport/FuzzyMatcher.kt:L10-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/FuzzyMatcher.kt#L10-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
Rescata canciones legítimas cuyos títulos en YouTube tienen palabras en orden alternativo o incluyen sufijos contextuales.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Algoritmo difuso en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El paso de ordenamiento de tokens antes de calcular la distancia Levenshtein.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Funciones matemáticas puras sin dependencias de Android framework.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Resolucion y Matching Difuso]], [[05 - SpotifyTrackMatcher Bigrama 0.60]].

---

## 7. Guía rápida para una IA nueva
- Para calcular el ratio: `FuzzyMatcher.tokenSortRatio(str1, str2)`.
