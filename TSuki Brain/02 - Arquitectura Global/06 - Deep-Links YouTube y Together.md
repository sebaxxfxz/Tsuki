# 02.06 — Deep-Links YouTube y Together

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Regex `v=|youtu.be/|shorts/|embed/|live/ ([A-Za-z0-9_-]{11})` → `playFromSharedVideoId` → `fetchRelatedTracks (RDAMVM+automix)`. `SEND text/plain` (Compartir→TSuki). `tsuki://together` o `|||` → `TogetherDeepLink.pending`. Ignora `LAUNCHED_FROM_HISTORY/ACTION_MAIN`, dedup 2s.

**Archivos fuente que documenta:**
- `MainActivity.kt:127 regex, 205 handleExternalYouTubeIntent`
- `together/TogetherLink.kt:27 decode`
- `AndroidManifest.xml` intent-filters

## 2. PARA QUÉ existe (problema que resuelve)
Sin dedup ni gates, un share abre dos radios o IDs virtuales llegan a YTM.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque cruza manifest+activity+playback+together.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO cambiar regex sin probar youtu.be/shorts/embed/live.
- NO quitar dedup 2s ni `LAUNCHED_FROM_HISTORY` guard.
- NO aceptar links sin validar `port 1..65535 + sid/key`.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Probar con `adb shell am start -a VIEW -d <url>`. Verificar radio arranca y cola no duplica.

## 6. Flujo y conexiones
Manifest declara LAUNCHER + VIEW (youtube/m.youtube/music.youtube/youtu.be) + SEND + tsuki-together. Activity resuelve, PlayerController ejecuta, TogetherRepo consume pending.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


