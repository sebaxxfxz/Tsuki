# 03.18 — Modos Audio/Video y Selección de Calidad

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Administra la conmutación dinámica entre reproducción de audio en segundo plano y reproducción de video a pantalla completa en `PlayerController.kt`:
- **Conmutación de Modo (`setPlayerMode`)**: Permite cambiar entre `PlayerMode.MiniPlayer`, `PlayerMode.FullPlayer` y `PlayerMode.VideoPlayer` preservando la posición exacta de reproducción en milisegundos.
- **Selección de Calidad de Video (`setQuality`)**: Cambia la resolución del stream (Auto, 1080p, 720p, 480p, 360p) recargando el `MediaItem` en `ExoPlayer` con la URL correspondiente sin perder el progreso actual.
- **Selección de Pista de Audio Doblada (`setAudioTrack`)**: Soporta videos con múltiples pistas de doblaje de YouTube (ej. audio original en inglés vs doblaje en español).

**Archivos fuente clave:**
- [`playback/PlayerController.kt:L1220-1320`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt#L1220-L1320)
- [`ui/player/VideoPlayerScreen.kt:L1-450`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/VideoPlayerScreen.kt#L1-L450)

---

## 2. PARA QUÉ existe (problema que resuelve)
Brinda una experiencia unificada donde una misma pista puede disfrutarse como canción con carátula o como video musical con selección de calidad sin reiniciar la reproducción.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Lógica de control multimedia en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El paso de posición: al recargar el `MediaItem` por cambio de calidad o pista de audio, se DEBE invocar `player.seekTo(currentPositionMs)`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Opciones de calidad expuestas en la UI mediante menús desplegables en `VideoPlayerScreen`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[15 - VideoPlayerScreen 2001L Gestos]], [[02 - YouTubeExtractor y Ciphers]].

---

## 7. Guía rápida para una IA nueva
- Para cambiar la calidad de video, invoca `PlayerController.setQuality(qualityOption)`.
