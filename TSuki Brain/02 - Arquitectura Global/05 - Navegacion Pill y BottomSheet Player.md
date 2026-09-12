# 02.05 — Navegación Pill y Bottom-Sheet Player

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Píldora flotante `surfaceContainerHigh+outlineVariant` con tabs; player en sheet custom `Animatable<Dp>` 3 anclas (dismissed/collapsed 138dp+insets/expanded) + scrim + nested-scroll, sin `ModalBottomSheet` para 60fps. Mini → V9 (audio) o `PlayerScreen(video)` + `CornerPip`. Back ordenado Together/Shorts/sheet/video.

**Archivos fuente que documenta:**
- `MainActivity.kt:263 TSukiMainScreen, 252 TSukiDestination, 410 sheetState`
- `ui/components/TSukiPillNavBar.kt:50`
- `ui/player/PlayerBottomSheet.kt:60`

## 2. PARA QUÉ existe (problema que resuelve)
El sheet del sistema no da 60fps con gestos anidados ni PiP 16:9 condicional.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque es esqueleto de navegación global, no de un screen.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO cambiar anclas/springs (`BottomSheetSpring/SoftSpring` de `M3MotionTokens`), `progress 150-154`, fling ±250.
- NO romper `BackHandler` ordenado.
- NO PiP fuera de `VideoExpanded+playing`.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Iconos `Icons.Rounded`, pastilla seleccionada `secondaryContainer`, escala `Animatable`. `VelocityTracker` + `bottomSheetDraggable`.

## 6. Flujo y conexiones
`pendingWidgetIntent/destination/open_player` abre player desde widget/notif/deep-link. `onUserLeaveHint` decide PiP.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


