# 03.07 — PlayerUiState y PlaybackTick (Desacoplamiento de Estado)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Establece la separación arquitectónica entre dos flujos de estado emitidos por `PlayerController`:
1. **`StateFlow<PlayerUiState>` (`uiState`)**:
   - Flujo de baja frecuencia. Se actualiza únicamente ante eventos discretos: cambio de canción, toggle play/pause, cambio de modo (MiniPlayer/FullPlayer/Video), actualización de letras, ajuste de velocidad o reordenamiento de cola.
   - Anotado con `@Immutable` para que Jetpack Compose evite recomposiciones innecesarias.
2. **`StateFlow<PlaybackTick>` (`playbackTick`)**:
   - Flujo de alta frecuencia emitido por `startProgressTracker()` cada 100 a 250 milisegundos mientras la reproducción está activa.
   - Contiene únicamente: `positionMs: Long`, `durationMs: Long` y `bufferedPositionMs: Long`.

**Archivos fuente clave:**
- [`playback/PlayerController.kt:L128-215`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt#L128-L215)
- [`ui/player/PlayerControlsV9.kt:L389-480`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/PlayerControlsV9.kt#L389-L480)

---

## 2. PARA QUÉ existe (problema que resuelve)
Si `positionMs` estuviera dentro de `PlayerUiState`, cada tick de 100ms provocaría una recomposición completa de toda la pantalla del reproductor (incluyendo carátula, títulos, botones de transporte y menús). Separar `PlaybackTick` aísla las recomposiciones exclusivamente al deslizador de progreso y los textos de tiempo transcurrido.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Es el modelo de salida del controlador central en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO incorporar `positionMs` o `playbackPosition` dentro de `PlayerUiState`.
- NO realizar operaciones pesadas de E/S dentro del bucle emisor de `PlaybackTick`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- En Compose: los componentes que no requieren la posición en milisegundos observan solo `PlayerController.uiState`. El slider observa `PlayerController.playbackTick`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - PlayerController Central]], [[06 - PlayerControlsV9 Pastillas y Slider]].

---

## 7. Guía rápida para una IA nueva
- Al crear un componente que anime el tiempo, suscríbete a `PlayerController.playbackTick` mediante `collectAsStateWithLifecycle()`.
