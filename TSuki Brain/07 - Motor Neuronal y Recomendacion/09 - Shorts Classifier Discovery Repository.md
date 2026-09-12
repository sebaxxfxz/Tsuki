# 07.09 — Módulo de Shorts: Clasificador, Descubrimiento y Repositorio

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la experiencia de videos musicales verticales cortos:
1. **`TSukiShortsClassifier.kt`**:
   - Clasifica si un video de YouTube califica como Short musical:
     - Duración estricta: `durationSeconds in 1..65`.
     - Título o descripción con `#shorts` o `#short`.
2. **`TSukiShortsDiscoveryEngine.kt`**:
   - Busca Shorts en canales de música suscritos y tendencias.
   - Aplica deduplicación por similitud de títulos con Levenshtein (`sim > 0.6`).
3. **`TSukiShortsRepository.kt`**:
   - Expone el flujo reactivo infinito que alimenta el reproductor vertical `TSukiShortsScreen`.

**Archivos fuente clave:**
- [`data/shorts/TSukiShortsClassifier.kt:L5-25`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/shorts/TSukiShortsClassifier.kt#L5-L25)
- [`data/shorts/TSukiShortsDiscoveryEngine.kt:L13-80`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/shorts/TSukiShortsDiscoveryEngine.kt#L13-L80)
- [`data/shorts/TSukiShortsRepository.kt:L18-95`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/shorts/TSukiShortsRepository.kt#L18-L95)
- [`ui/screens/TSukiShortsScreen.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/TSukiShortsScreen.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Ofrece una experiencia ágil de descubrimiento musical visual en formato vertical sin mezclar videos largos en el feed.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Capa de datos de Shorts en `data/shorts/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El techo de duración de 65 segundos: YouTube restringe los Shorts a un máximo de 60s (con 5s de tolerancia técnica).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Pausa automática de la reproducción de música de fondo al ingresar a `TSukiShortsScreen`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[28 - SubscriptionsScreen y RSS de Canales]], [[04 - WEB_REMIX vs WEB Doble Cliente]].

---

## 7. Guía rápida para una IA nueva
- Para consultar el feed de shorts: `TSukiShortsRepository.getInstance(context).getShortsFeed()`.
