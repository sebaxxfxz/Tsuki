# 03.11 — NextTrackPrecacher (14MB al siguiente)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Descarga 14MB del siguiente audio a `SimpleCache` para transición sin buffering. Key `"${id}_$qualityLabel"` (diverge de `_vo` del controller: no renombrar unilateral). Salta si `isLocal:25` o `isVideoMode`.

**Archivos fuente que documenta:**
- `playback/NextTrackPrecacher.kt:14 object, 19 precache, 16 MAX 14MB, 17 MIN_USEFUL 512KB, 36 key, 43 FLAG_IGNORE_CACHE_ON_ERROR`
- Caller `PlayerController.kt:1038` a los 12s

## 2. PARA QUÉ existe (problema que resuelve)
Sin precache el crossfade prepara tarde y hay gap en red lenta.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `playback/` como I/O puro sin estado, junto a `PlayerCacheProvider`.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO subir MAX sin medir `cacheSizeMb 128-2048/-1`.
- NO quitar `IGNORE_CACHE_ON_ERROR`.
- NO precachear en `dataSaverActive` ni metered LOW.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Disparar a los 12s, no al inicio (ahorra datos si el usuario salta). `MIN_USEFUL` evita guardar fragmentos inútiles.

## 6. Flujo y conexiones
Controller decide → precacher escribe en `SimpleCache` compartido → secundario de crossfade lo reutiliza.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


