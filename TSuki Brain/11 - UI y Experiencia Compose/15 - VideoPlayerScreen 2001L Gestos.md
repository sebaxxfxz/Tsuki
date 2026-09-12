# 11.15 — VideoPlayerScreen Gestos y Fullscreen (2001L)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla completa dedicada a la reproducción de videos musicales de YouTube (`ui/player/VideoPlayerScreen.kt`, 2001 líneas):
- **Motor de Video ExoPlayer**: Reproducción de streams de video de alta calidad (1080p, 720p, 480p) con cambio dinámico de resolución.
- **Gestos Táctiles Avanzados**:
  - Deslizamiento vertical en la mitad izquierda de la pantalla: Control de brillo de la pantalla.
  - Deslizamiento vertical en la mitad derecha de la pantalla: Control de volumen del sistema.
  - Doble toque en los laterales: Salto rápido de 10 segundos hacia adelante o hacia atrás con onda animada.
  - Pellizco para hacer zoom (Pinch-to-zoom): Alterna entre modo ajuste (*Fit*), relleno (*Fill* recortando bordes) y estiramiento (*Stretch*).
- **Modo Picture-in-Picture (PiP)**: Minimiza la ventana a un reproductor flotante del sistema Android al salir de la app.
- **Capítulos y Comentarios**: Panel lateral deslizable para navegar por los capítulos del video y leer comentarios de YouTube.

**Archivos fuente clave:**
- [`ui/player/VideoPlayerScreen.kt:L1-2001`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/VideoPlayerScreen.kt#L1-L2001)
- [`playback/PlayerController.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Proporciona una experiencia de visualización de videos musicales de máxima calidad, permitiendo al usuario disfrutar de videoclips oficiales sin depender de la aplicación oficial de YouTube.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/player/` como la vista hermana del reproductor de audio dedicada exclusivamente al video.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Handoff de Audio a Video**: Al abrir `VideoPlayerScreen`, el stream de solo-audio de ExoPlayer debe detenerse o transferirse al stream de video con sincronización estricta de posición de tiempo para no duplicar el uso de ancho de banda ni reproducir audio desfasado.
- **Bloqueo de Orientación**: Debe responder correctamente a la rotación automática del sensor del dispositivo y ocultar las barras del sistema (`SystemUiController` / `WindowInsetsControllerCompat`).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Los controles flotantes se ocultan automáticamente tras 3 segundos de inactividad táctil mediante una corrutina con retardo cancelable.

---

## 6. Flujo y conexiones
- Integrado con: `playback/PlayerController.kt` y `network/YouTubeExtractor.kt`.

---

## 7. Guía rápida para una IA nueva
- Para modificar los gestos de brillo o volumen, revisa `VideoPlayerScreen.kt:L450-580`.
