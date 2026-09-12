# 11.10 — PlayerBottomSheet Anclas 3 Niveles

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa la hoja inferior deslizante que aloja el reproductor con 3 estados de anclaje físico (`ui/player/PlayerBottomSheet.kt`):
- **3 Estados de Anclaje (`SheetValue`)**:
  - `COLLAPSED`: Hoja replegada en la parte inferior mostrando exclusivamente el `MiniPlayer` (altura fija de ~64dp más insets del sistema).
  - `HALF_EXPANDED`: Estado intermedio útil en tablets o pantallas grandes, mostrando la carátula compacta y la cola o letras simultáneamente.
  - `EXPANDED`: Pantalla completa mostrando la totalidad de `MusicPlayerScreenV9`.
- **Física de Deslizamiento Suave**: Integración con resortes de velocidad consciente (`VelocityTracker`) para responder al impulso del gesto del usuario.
- **Manejo de Insets de Sistema**: Soporte estricto Edge-to-Edge (`WindowInsets.systemBars`), adaptando la posición del miniplayer para no solaparse con la barra de navegación gestual de Android.

**Archivos fuente clave:**
- [`ui/player/PlayerBottomSheet.kt:L1-280`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/PlayerBottomSheet.kt#L1-L280)
- [`MainActivity.kt:L690-720`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/MainActivity.kt#L690-L720)

---

## 2. PARA QUÉ existe (problema que resuelve)
Proporciona la transición gestual continua entre la exploración de contenido y el control detallado de la música, emulando la fluidez de aplicaciones de clase mundial.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/player/` al ser el contenedor contenedor directo de la interfaz del reproductor.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Interceptación de Gestos Horizontales**: El controlador de gestos verticales de la hoja no debe interceptar eventos de deslizamiento horizontal cuando el usuario está arrastrando el `ArtworkPagerV9` o la cola.
- **Incompatibilidad de Insets**: No hardcodear márgenes inferiores fijos en píxeles.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Uso de `rememberSwipeableState` / `AnchoredDraggableState` con anclas calculadas dinámicamente en base a la altura de la ventana.

---

## 6. Flujo y conexiones
- Contiene a: [[05 - MusicPlayerScreenV9 Orquestador 1466L]] y [[02 - Arquitectura del Reproductor y MiniPlayer]].

---

## 7. Guía rápida para una IA nueva
- Para expandir el reproductor programáticamente desde código (ej. al tocar una canción en una lista), invoca `coroutineScope.launch { sheetState.animateTo(SheetValue.EXPANDED) }`.
