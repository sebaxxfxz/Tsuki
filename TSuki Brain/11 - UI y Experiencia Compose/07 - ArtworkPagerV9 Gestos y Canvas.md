# 11.07 — ArtworkPagerV9 Gestos y Canvas

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona la presentación visual de la portada de la canción actual con capacidades avanzadas de animación y gestos (`ui/player/ArtworkPagerV9.kt`):
- **HorizontalPager con Paginación de Cola**: Permite deslizar horizontalmente hacia la izquierda o derecha para cambiar a la pista siguiente o anterior con animación fluida.
- **Integración con Canvas de Video Looping**: Si la canción dispone de un Spotify Canvas o fondo animado (gestionado por `CanvasArtworkPlayer`), conmuta de forma transparente entre la imagen estática y el video en bucle continuo sin cortes de audio.
- **Efecto de Elevación y Sombra Dinámica**: Proyecta una sombra suave coloreada acorde a la paleta dominante del álbum.
- **Gestos Táctiles**: Toque simple para alternar la visualización de controles, doble toque opcional para dar Me Gusta con microanimación de corazón.

**Archivos fuente clave:**
- [`ui/player/ArtworkPagerV9.kt:L1-310`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/ArtworkPagerV9.kt#L1-L310)
- [`ui/player/CanvasArtworkPlayer.kt:L1-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/CanvasArtworkPlayer.kt#L1-L180)
- [`ui/player/canvas/CanvasProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/canvas/CanvasProvider.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Transforma la experiencia visual estática tradicional de un reproductor en una experiencia viva e inmersiva, adaptando automáticamente el contenido entre carátulas cuadradas estándar y videos verticales de fondo.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Pertenece a `ui/player/` como la vista central de la primera pestaña del reproductor.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Consumo de Memoria en Coil**: Al cargar la imagen de la carátula, se debe respetar el tamaño del contenedor y no forzar decodificación en tamaño completo sin reescalado para evitar `OutOfMemoryError` en dispositivos de gama media.
- **Sincronización del Pager con la Cola**: El `currentPage` del `HorizontalPager` debe reflejar estrictamente el `currentIndex` de la cola de reproducción en `PlayerController`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- El reproductor de Canvas utiliza un `AndroidView` que aloja un `ExoPlayer` secundario configurado exclusivamente para video sin decodificación de audio, con volumen en cero para no colisionar con la reproducción musical principal.

---

## 6. Flujo y conexiones
- Sincronizado con: [[12 - Canvas Trio Provider Resolver DiskCache]], [[08 - PlayerBackgroundV9 Gradiente]], [[05 - MusicPlayerScreenV9 Orquestador 1466L]].

---

## 7. Guía rápida para una IA nueva
- Para consultar si hay un video de fondo activo, verifica el estado de `CanvasArtworkPlayer`.
