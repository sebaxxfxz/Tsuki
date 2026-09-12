# 11.06 — PlayerControlsV9 Pastillas y Slider

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Renderiza los controles de transporte, la barra de reproducción ondulada y los botones de acción rápida del reproductor (`ui/player/PlayerControlsV9.kt`):
- **Slider Ondulado `M3WavySlider`**: Barra de progreso con forma de onda animada basada en Material 3 Expressive que refleja el avance temporal de la canción.
- **Botón Play/Pause Central**: Botón tonal grande con morphing animado entre los iconos de reproducción y pausa mediante resortes físicos.
- **Controles de Navegación**: Botones Anterior / Siguiente con soporte para saltos rápidos y rebobinado al inicio de la pista si la reproducción supera los 3 segundos.
- **Modos de Reproducción**: Botones para alternar Aleatorio (Shuffle) y Repetición (Off / All / One).
- **Pastillas de Estado (Pill Badges)**:
  - Pastilla de Calidad: Muestra el bitrate y formato de audio (ej. `Hi-Res 24-bit`, `Opus 160k`, `AAC 256k`).
  - Pastilla de Sleep Timer: Muestra una cuenta regresiva si el temporizador de apagado está activo.

**Archivos fuente clave:**
- [`ui/player/PlayerControlsV9.kt:L1-350`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/PlayerControlsV9.kt#L1-L350)
- [`ui/components/M3Motion.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/M3Motion.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Proporciona la interfaz táctil principal para la manipulación directa de la reproducción sonora con una respuesta física inmediata y una estética expresiva de vanguardia.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Pertenece a `ui/player/` al ser un componente exclusivo de la pantalla del reproductor completo.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Contrato Estricto de `M3WavySlider`**: El parámetro `value` del slider **DEBE recibir un número flotante normalizado entre `0.0f` y `1.0f`**. Jamás pases milisegundos directos al slider; pasar milisegundos (ej. `180000f`) rompe la escala y congela el renderizado de la onda.
- **Lógica de Anterior (Previous)**: Si la posición actual es superior a 3000ms, presionar Anterior debe rebobinar la pista a 0ms; si es menor a 3000ms, debe saltar a la canción anterior de la cola.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- El arrastre del slider (*scrubbing*) gestiona un estado local temporal `scrubPosition` para que la barra se mueva de forma fluida bajo el dedo sin saltar hacia atrás por la actualización de ExoPlayer hasta que el usuario levanta el dedo (`onSeekFinished`).
- Uso de `FilledIconButton` de gran tamaño (64x64dp) para el botón central de Play/Pause.

---

## 6. Flujo y conexiones
- Recibe estado y emite callbacks hacia [[05 - MusicPlayerScreenV9 Orquestador 1466L]].
- Tokens de animación desde [[32 - M3Motion Tokens Springs Sliders]].

---

## 7. Guía rápida para una IA nueva
- Si vas a modificar el comportamiento del slider, asegúrate de mantener la conversión: `val fraction = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f`.
