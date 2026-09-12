# 11.35 — Componentes Core (MiniPlayer, TSukiPillNavBar, FastScrollBox, Sheets)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Conjunto de componentes estructurales que vertebran la experiencia de navegación y uso diario en TSuki (`ui/components/`):
- **`MiniPlayer`**: Barra de reproducción flotante con carátula en miniatura, título y artista con marquesina automática, botón Play/Pause tonal, botón Siguiente y barra de progreso dibujada en la fase de dibujo (`Canvas`).
- **`TSukiPillNavBar`**: Barra de navegación flotante en forma de pastilla centrada en la parte inferior, con fondo tonal translúcido (`surfaceContainer`), esquinas circulares y elevación suave, evitando ocupar todo el ancho de la pantalla.
- **`FastScrollBox`**: Desplazador rápido con indicador alfabético flotante lateral para listas extensas de miles de canciones o artistas en la biblioteca.
- **`TagSongSheet`**: Hoja modal para asignar etiquetas temáticas personalizadas a canciones.
- **`AddToPlaylistSheet`**: Hoja modal para añadir rápidamente la pista actual a una o más listas de reproducción locales o remotas.
- **`ShimmerLoading`**: Efecto esquelético brillante de carga para tarjetas y filas mientras se descargan datos de red.
- **`CornerPipPlayer`**: Reproductor miniatura en esquina para previsualizaciones rápidas de video.

**Archivos fuente clave:**
- [`ui/components/MiniPlayer.kt:L1-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/MiniPlayer.kt#L1-L260)
- [`ui/components/TSukiPillNavBar.kt:L1-160`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/TSukiPillNavBar.kt#L1-L160)
- [`ui/components/FastScrollBox.kt:L1-190`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/FastScrollBox.kt#L1-L190)
- [`ui/components/TagSongSheet.kt:L1-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/TagSongSheet.kt#L1-L140)
- [`ui/components/AddToPlaylistSheet.kt:L1-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/AddToPlaylistSheet.kt#L1-L180)
- [`ui/components/ShimmerLoading.kt:L1-90`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/ShimmerLoading.kt#L1-L90)

---

## 2. PARA QUÉ existe (problema que resuelve)
Proporciona los bloques de construcción fundamentales que dan cohesión visual, rapidez de navegación y ergonomía táctil a toda la aplicación.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/components/` como elementos de diseño reutilizables entre múltiples pantallas.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **`progressProvider` en MiniPlayer**: Debe permanecer como una lambda no reactiva invocada dentro del `Canvas` para garantizar 60/120 FPS sin recomposiciones.
- **Cálculo de Insets en `TSukiPillNavBar`**: La barra de navegación debe respetar los insets de navegación del sistema (`navigationBarsPadding`) para no quedar tapada en dispositivos con navegación por tres botones.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Reutilización de modificadores y composición limpia sin acoplamientos rígidos con ViewModels.

---

## 6. Flujo y conexiones
- Ensamblado en: `MainActivity.kt`.
- Conectado a: [[02 - Arquitectura del Reproductor y MiniPlayer]], [[18 - LibraryScreen y Likes Sincronizados]].

---

## 7. Guía rápida para una IA nueva
- Para mostrar la hoja de añadir a playlist desde cualquier pantalla, utiliza `showAddToPlaylistSheet = true` pasando la pista actual.
