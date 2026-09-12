# 11.02 — Arquitectura del Reproductor y MiniPlayer

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define la jerarquía visual y la orquestación del estado de reproducción persistente en toda la aplicación:
- **`MiniPlayer` Flotante**: Barra de control persistente visible en todas las pantallas principales (Home, Music, Library, Settings), anclada milimétricamente sobre la barra de navegación `TSukiPillNavBar`.
- **`MusicPlayerScreenV9` en Hoja Deslizable**: Reproductor a pantalla completa que se desliza desde la parte inferior mediante gestos táctiles de arrastre con física de inercia.
- **Optimización de Fase de Dibujo (Draw Phase)**: La barra de progreso de reproducción en el `MiniPlayer` se dibuja dentro de un bloque `Canvas { ... }` leyendo un `progressProvider: () -> Float`, eliminando recomposiciones a 60/120 FPS.
- **Sincronización Unidireccional**: Consume directamente el `PlayerUiState` emitido por `PlayerController.uiState`.

**Archivos fuente clave:**
- [`ui/components/MiniPlayer.kt:L1-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/MiniPlayer.kt#L1-L260)
- [`ui/player/PlayerBottomSheet.kt:L1-280`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/PlayerBottomSheet.kt#L1-L280)
- [`ui/player/MusicPlayerScreenV9.kt:L112-400`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/MusicPlayerScreenV9.kt#L112-L400)
- [`MainActivity.kt:L680-730`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/MainActivity.kt#L680-L730)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que el usuario nunca pierda el control de la música sin importar en qué sección de la aplicación esté navegando. Resuelve de forma radical el problema de caídas de frames (jank) al actualizar la barra de progreso segundo a segundo.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
- `MiniPlayer.kt` reside en `ui/components/` al ser un componente reusable acoplado al andamio global (`MainActivity`).
- `PlayerBottomSheet.kt` y `MusicPlayerScreenV9.kt` residen en `ui/player/` al encapsular la lógica del reproductor completo.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **`progressProvider` en MiniPlayer**: Prohibido convertir el progreso en un parámetro `progress: Float` simple recolectado por recomposición en el composable padre. Debe permanecer como lambda `() -> Float` evaluada en la fase de renderizado del `Canvas`.
- **Gestos de Swipe**: El deslizamiento vertical entre MiniPlayer y FullPlayer está sincronizado con el teclado y los gestos del sistema Android. No añadir `pointerInput` bloqueantes en los contenedores raíz del reproductor.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Los cambios de estado de reproducción (play/pause/skip) son delegados inmediatamente a `PlayerController` mediante callbacks limpios (`onPlayPause`, `onNext`, `onPrevious`, `onSeek`).
- El MiniPlayer se oculta automáticamente cuando la cola está vacía o cuando el usuario entra en la pantalla de reproducción de video a pantalla completa (`VideoPlayerScreen`).

---

## 6. Flujo y conexiones
- Flujo de datos:
  `PlayerController.uiState` → `MainActivity` → `Box(Root)` → `MiniPlayer` (sobre `TSukiPillNavBar`) / `PlayerBottomSheet` → `MusicPlayerScreenV9`.
- Documentos vinculados: [[05 - MusicPlayerScreenV9 Orquestador 1466L]], [[35 - Componentes Core (MiniPlayer, TSukiPillNavBar, FastScrollBox, Sheets)]], [[10 - PlayerBottomSheet Anclas 3 Niveles]].

---

## 7. Guía rápida para una IA nueva
- Para saber si el reproductor está expandido, consulta el estado de la hoja modal en `MainActivity`.
- Si necesitas añadir un botón al MiniPlayer, revisa `MiniPlayer.kt:L120-180` asegurándote de usar `FilledTonalIconButton` con `modifier.size(36.dp)`.
