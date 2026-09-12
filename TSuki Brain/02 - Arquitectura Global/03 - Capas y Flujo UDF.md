# 02.03 — Capas y Flujo Unidireccional (UDF)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Presentación observa `PlayerUiState/playbackTick`; Playback comanda sesión/cola/DSP; Red devuelve `MediaTrack/StreamResult`; Datos persisten offline y rankean. Flujo en una dirección evita ciclos UI↔Player.

**Archivos fuente que documenta:**
- `ui/` (Compose) → `playback/` (Media3) → `network/` (InnerTube/Extractor) → `data/` (SQLite/DataStore/Neuro)
- `domain/model/` puro

## 2. PARA QUÉ existe (problema que resuelve)
Sin UDF, `ui/screens` llamarían a ExoPlayer directo y romperían crossfade/together/autoqueue.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque es la regla arquitectónica madre.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO llamar ExoPlayer desde `ui/`; solo `PlayerController`.
- NO poner HTTP en `ui/`; usar `network/` o `data/repository`.
- NO guardar estado de negocio en composables; va en `uiState`/DataStore/DB.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Surgical edits, `collectAsStateWithLifecycle`, `derivedStateOf`, `stateIn WhileSubscribed5000` en ViewModels.

## 6. Flujo y conexiones
Diagrama en mapa central. `TogetherManager` intercepta UDF sin romperlo (anti-eco `applyingRemote/suppressEchoUntil`).

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


