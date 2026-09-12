# 11.03 — Widgets de Pantalla de Inicio Glance

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa los widgets modernos de escritorio para Android 12+ utilizando **Jetpack Glance** (tecnología basada en Compose que compila a `RemoteViews`):
- **5 Tamaños Adaptativos (`LocalSize.current`)**:
  - Compacto (1x1): Botón Play/Pause con carátula en miniatura.
  - Barra Horizontal (2x1 / 4x1): Título, artista y controles de reproducción lineal.
  - Cuadrado Medio (2x2): Carátula destacada, metadatos y controles tonales inferiores.
  - Grande (3x3 / 4x2): Carátula expandida, barra de progreso y botón de favoritos.
  - Completo / Expandido (4x3 / 5x2): Información completa, controles avanzados y pistas sugeridas de la cola.
- **Acciones Asíncronas**: Envía intents de control a `TSukiPlaybackService` sin levantar la Activity principal.
- **Compatibilidad Legacy**: Fallback a `TSukiWidgetProvider` (`AppWidgetProvider` clásico) en versiones antiguas de Android.

**Archivos fuente clave:**
- [`ui/widget/glance/TSukiGlanceWidget.kt:L1-320`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceWidget.kt#L1-L320)
- [`ui/widget/glance/TSukiGlanceReceiver.kt:L1-45`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceReceiver.kt#L1-L45)
- [`ui/widget/glance/TSukiGlanceSync.kt:L1-110`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceSync.kt#L1-L110)
- [`ui/widget/glance/TSukiGlanceActions.kt:L1-80`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/glance/TSukiGlanceActions.kt#L1-L80)
- [`ui/widget/TSukiWidgetProvider.kt:L1-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/widget/TSukiWidgetProvider.kt#L1-L140)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a los usuarios pausar, reproducir, cambiar de canción y ver qué está sonando directamente desde el launcher de su teléfono Android, con una interfaz moderna y adaptativa que respeta el tema dinámico de Material You.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Se agrupa en `ui/widget/glance/` para aislar los composables especiales de Glance (que usan APIs de `androidx.glance.*` en lugar de `androidx.compose.material3.*`).

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **No mezclar paquetes de Compose**: Los widgets de Glance solo pueden usar componentes del paquete `androidx.glance.*` (`GlanceModifier`, `androidx.glance.layout.Box`, `androidx.glance.text.Text`). Usar un `androidx.compose.material3.Text` dentro de Glance provocará un crash fatal en tiempo de ejecución al inflar `RemoteViews`.
- **Carga de Bitmaps de Carátula**: Deben cargarse en background y comprimirse a un tamaño moderado (< 512KB) antes de pasarse a `RemoteViews` para evitar el temido error `TransactionTooLargeException` de Binder en Android.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Sincronización a través de `TSukiGlanceSync.updateWidgetState(context, state)` invocado por `TSukiPlaybackService` cada vez que cambia la pista o el estado de reproducción.
- Acciones despachadas con `actionRunCallback<PlayPauseActionCallback>()`.

---

## 6. Flujo y conexiones
- Documentación detallada en: [[34 - Widgets Glance y TSukiGlanceReceiver (Keys, Sync, Actions, Theme)]].
- Sincronización desde el servicio: `playback/TSukiPlaybackService.kt` → `TSukiGlanceSync.kt`.

---

## 7. Guía rápida para una IA nueva
- Si necesitas modificar el diseño del widget, edita `TSukiGlanceWidget.kt` asegurándote de no importar componentes estándar de Compose UI.
