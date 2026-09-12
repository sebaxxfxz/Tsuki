# 02.10 — Modelos de Dominio (`MediaTrack`, `LyricsEntry`)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
`MediaTrack` universal (id/title/artist/videoId/durationMs/durationSeconds, `effectiveDurationSeconds`, `isLocal/LOCAL_AUDIO/STREAM_VIDEO`). `LyricsEntry(timeMs,text,words,durationMs,isInstrumental)` + `WordTimestamp(text,startSec,endSec,isBackground)` en segundos Double, `compareTo` + `.sorted()`, `@Immutable`, `HEAD_LYRICS_ENTRY`. Son el idioma entre extractor→player→lyrics→UI.

**Archivos fuente que documenta:**
- `domain/model/MediaTrack.kt:20`
- `domain/model/LyricsEntry.kt:1`

## 2. PARA QUÉ existe (problema que resuelve)
Mezclar ms/segundos rompe karaoke (clamp `end=min(rawEnd,nextStart)`, gap>3000→+4000).

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque `domain` puro no depende de Android y lo usan todas las capas.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO cambiar unidades: `LyricsEntry.time` ms Long, palabras segundos Double (conversión en `KaraokeSyncedLyrics:126-129`).
- NO quitar `videoId ?: id` ni `durationSeconds=ms/1000`.
- NO romper `compareTo`/sorted ni `hasWordSync` gate.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Puros, testeables, `@Immutable` para Compose. `hasWordSyncedLine` requiere `time>=0`.

## 6. Flujo y conexiones
Extractor mapea a `MediaTrack`; `LyricsUtils.parse*` a `LyricsEntry`; UI decide karaoke vs lista según `hasWordSynced`.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


