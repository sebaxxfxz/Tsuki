# 11.30 — TSukiShortsScreen y Feed Vertical

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Feed vertical continuo de videos cortos musicales al estilo TikTok y YouTube Shorts (`ui/screens/TSukiShortsScreen.kt`):
- **Navegación Paginada Vertical (`VerticalPager`)**: Deslizamiento vertical instantáneo entre clips de video musicales de alta definición.
- **Reproducción Inteligente con Precarga**:
  - Inicia la reproducción del video activo de inmediato al centrarse en pantalla.
  - Precarga el buffer del siguiente video en segundo plano para una transición con cero segundos de retraso al deslizar.
  - Pausa y libera recursos del video anterior para ahorrar memoria GPU y datos móviles.
- **Superposiciones de Interacción Rápida**:
  - Botón Me Gusta con microanimación de partículas.
  - Botón "Escuchar Canción Completa": Transfiere la pista al reproductor principal de música sin perder el hilo.
  - Tarjeta flotante inferior con título, artista y carátula rotatoria.

**Archivos fuente clave:**
- [`ui/screens/TSukiShortsScreen.kt:L1-390`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/TSukiShortsScreen.kt#L1-L390)
- [`ui/components/VideoCardShort.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/VideoCardShort.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Ofrece un canal moderno y dinámico de descubrimiento musical visual para usuarios que prefieren explorar nueva música a través de momentos destacados y clips cortos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como experiencia audiovisual vertical independiente.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Gestión del Enfoque de Audio**: Al entrar a `TSukiShortsScreen`, la reproducción de audio del reproductor principal debe pausarse de forma controlada; al presionar "Escuchar Canción Completa", el feed vertical debe cerrarse y restaurar la sesión principal.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Uso de `ExoPlayer` optimizado con `LoadControl` de baja latencia para arranque instantáneo de video.

---

## 6. Flujo y conexiones
- Conexión con: `network/YouTubeExtractor.kt` y `playback/PlayerController.kt`.

---

## 7. Guía rápida para una IA nueva
- Para cargar más clips en el feed conforme el usuario se acerca al final, intercepta el índice en el `snapshotFlow { pagerState.currentPage }`.
