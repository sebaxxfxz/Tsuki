# 05.03 — Renderizado Visual y Smooth Position a 60/120 FPS

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la animación y el desplazamiento suave de las letras de canciones en `ui/player/lyrics/KaraokeSyncedLyrics.kt`:
- **Interpolación Nanométrica (`rememberSmoothPositionState`)**:
  - Emplea `withFrameNanos` en un bucle continuo para extrapolar la posición de audio entre los ticks periódicos de ExoPlayer (que llegan cada 100-250ms).
  - Multiplica el delta de tiempo por `playbackSpeed` (ej. 0.75x, 1.25x, 1.5x) para mantener las sílabas iluminadas al tempo correcto si el usuario cambia la velocidad.
- **Filtro de Corrección de Deriva (*Drift Correction*)**:
  - Si la posición interpolada se adelanta más de `SMOOTH_POSITION_MAX_FORWARD_DRIFT_MS = 80L`, amortigua el avance.
  - Si se atrasa más de `SMOOTH_POSITION_MAX_BACKWARD_DRIFT_MS = 180L` (ej. tras un seek o lag de buffer), aplica un factor de convergencia de `0.55f`.
  - Si la deriva supera los 500ms, fuerza un salto inmediato (`snapTo`) sin transición para evitar desincronizaciones perceptibles.

**Archivos fuente clave:**
- [`ui/player/lyrics/KaraokeSyncedLyrics.kt:L20-75`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/lyrics/KaraokeSyncedLyrics.kt#L20-L75)
- [`ui/player/SyncedLyricsView.kt:L50-130`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/SyncedLyricsView.kt#L50-L130)

---

## 2. PARA QUÉ existe (problema que resuelve)
ExoPlayer reporta la posición de reproducción a intervalos discretos. Si se sincronizan las sílabas directamente con los eventos de ExoPlayer, el resaltado karaoke se ve a saltos toscos. La interpolación con deriva garantiza un flujo fluido a 60 o 120 cuadros por segundo sin jank visual.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Capa de animación de letras en `ui/player/lyrics/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Las constantes `SMOOTH_POSITION_MAX_FORWARD_DRIFT_MS = 80L`, `BACKWARD = 180L` y el factor `0.55f`. Están calibrados para amortiguar el jitter de Media3 en pantallas a 120 Hz.
- El factor `playbackSpeed`: debe multiplicarse siempre por el delta de nanosegundos.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Se probó en pantallas con tasa de refresco variable (60Hz / 90Hz / 120Hz).

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Parsers TTML y Enhanced LRC]], [[12 - KaraokeWordByWord vs KaraokeLyricRow]].

---

## 7. Guía rápida para una IA nueva
- Para obtener la posición suavizada en Compose, utiliza `rememberSmoothPositionState(positionMs, isPlaying, speed)`.
