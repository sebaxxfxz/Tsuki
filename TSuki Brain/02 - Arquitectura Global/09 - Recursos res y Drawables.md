# 02.09 — Recursos `res/`: Drawables, Widgets, XML

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Recursos que sostienen notif (icono blanco monocromo), launcher, widgets RemoteViews/Glance, FileProvider (`file_paths.xml`), Auto (`automotive_app_desc.xml`), backup rules. `media3_notification_small_icon` es el fix al `setSmallIcon` inexistente en Media3 1.5.1.

**Archivos fuente que documenta:**
- `res/drawable/*` (ic_launcher, kanji, media3_notification_small_icon, widget_*)
- `res/layout/widget_tsuki_player + previews + glance_initial`
- `res/values/colors strings themes`, `res/xml/*` (automotive, backup, file_paths, shortcuts, glance infos, widget info)

## 2. PARA QUÉ existe (problema que resuelve)
Borrar un XML rompe manifest en runtime aunque compile.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque son assets globales compartidos por playback/UI/widgets.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO borrar `file_paths.xml` (PixelCopy share), ni infos de widgets.
- NO usar colores hardcodeados fuera de tokens; drawables de widget en `widget_*`.
- Previews `widget_preview_*` deben coincidir con tamaños Glance.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Vectores monocromos para notif, `AsyncImage` Coil en Compose, RemoteViews en legacy.

## 6. Flujo y conexiones
`tsuki_glance_widget_info*.xml` (5 tamaños) + `tsuki_widget_info.xml` legacy + `shortcuts.xml` + `backup_rules/data_extraction_rules`.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


