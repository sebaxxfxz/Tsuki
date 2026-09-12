# 03.10 — Sleep Timer (fade 15s) y Private Mode

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Sleep apaga con fade 15s. Private evita `recordPlayback:166` y `recordPlayEvent:356`. `setPlayerMode` conmuta Video↔Audio, `playTrackUrl:1482` para streams directos.

**Archivos fuente que documenta:**
- `playback/PlayerController.kt:1412 sleepTimer, 1443 setPlayerMode Video-Audio, 154 privateMode`
- `data/local/PlayerPreferences.kt:154 privateMode`
- `data/local/WatchHistoryManager.kt:89 privateMode gate`

## 2. PARA QUÉ existe (problema que resuelve)
Dormirse con música sin fade corta de golpe; sin private el historial se contamina.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `playback/` porque controla `playWhenReady`/volumen; el flag vive en PlayerPreferences pero lo respeta historial.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO cambiar fade 15s sin probar con crossfade activo.
- NO registrar historial en private aunque el tracker acumule.
- `pendingPauseAfterRemoteStart:181` para guest: no quitar.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Settings con `TogglePreference` M3, sin alphas manuales. Diálogo destructivo con confirmación si vacía historial.

## 6. Flujo y conexiones
Timer → baja ganancia (`setOutputGainMb`) → pausa → Together `onLocalPlayChanged:968` difunde. Private → skip en `record*`.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


