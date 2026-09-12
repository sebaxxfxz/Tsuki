# 11.34 — Widgets Glance y TSukiGlanceReceiver (Keys, Sync, Actions, Theme)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Arquitectura modular completa de los widgets de pantalla de inicio basados en **Jetpack Glance** (`ui/widget/glance/`):
- **`TSukiGlanceWidget`**: Composable raíz de Glance que implementa la interfaz visual adaptativa según el tamaño asignado por el launcher de Android (`LocalSize.current`).
- **`TSukiGlanceReceiver`**: `GlanceAppWidgetReceiver` registrado en `AndroidManifest.xml` que recibe los eventos de ciclo de vida del widget y vincula el widget con el sistema.
- **`TSukiGlanceKeys`**: Definición de claves de preferencias (`Preferences.Key<T>`) para almacenar el estado persistente del widget en el DataStore interno de Glance (`TITLE_KEY`, `ARTIST_KEY`, `IS_PLAYING_KEY`, `ARTWORK_URI_KEY`, `DURATION_KEY`, `POSITION_KEY`).
- **`TSukiGlanceActions`**: Callbacks de acción (`ActionCallback`) que despachan comandos de reproducción (Play/Pause, Next, Previous, Favorite) hacia `TSukiPlaybackService` sin levantar la Activity principal.
- **`TSukiGlanceSync`**: Motor de sincronización que actualiza el estado de los widgets cada vez que cambia la canción o el estado de reproducción en el servicio musical.
- **`TSukiGlanceTheme`**: Esquema de colores adaptativo para widgets compatible con Material You y modo oscuro del sistema.

**Archivos fuente clave:**
- [`ui/widget/glance/TSukiGlanceWidget.kt:L1-320`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceWidget.kt#L1-L320)
- [`ui/widget/glance/TSukiGlanceReceiver.kt:L1-45`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceReceiver.kt#L1-L45)
- [`ui/widget/glance/TSukiGlanceKeys.kt:L1-35`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceKeys.kt#L1-L35)
- [`ui/widget/glance/TSukiGlanceActions.kt:L1-80`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceActions.kt#L1-L80)
- [`ui/widget/glance/TSukiGlanceSync.kt:L1-110`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceSync.kt#L1-L110)
- [`ui/widget/glance/TSukiGlanceTheme.kt:L1-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceTheme.kt#L1-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
Separa limpiamente las responsabilidades de renderizado, recepción de eventos, almacenamiento de estado y comunicación con el servicio en los widgets de escritorio.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/widget/glance/` para aislar el framework de Glance de los composables normales de la app.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Registro en `AndroidManifest.xml`**: `TSukiGlanceReceiver` debe mantener el filtro de intent `android.appwidget.action.APPWIDGET_UPDATE` y apuntar al recurso XML de configuración de widget.
- **Acciones Rápidas no Bloqueantes**: Las acciones en `TSukiGlanceActions` deben enviar intents ligeros y no realizar operaciones de red en el hilo de la acción.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Actualización atómica del widget mediante `GlanceAppWidgetManager.getGlanceIds(...)` y `updateAppWidgetState`.

---

## 6. Flujo y conexiones
- Sincronizado desde: `playback/TSukiPlaybackService.kt`.
- Resumen conceptual en: [[03 - Widgets de Pantalla de Inicio Glance]].

---

## 7. Guía rápida para una IA nueva
- Para forzar la actualización de todos los widgets activos, llama a `TSukiGlanceSync.updateWidgetState(context, playerUiState)`.
