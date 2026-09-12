# 02.04 — Inyección Manual con Singletons (sin Hilt)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Sin DI framework: cada capa expone `getInstance(applicationContext)` con doble-checked locking. Reduce magia y tiempo de build, pero exige no crear segundas instancias ni pasar Activity context.

**Archivos fuente que documenta:**
- `getInstance(context)` en PlayerController, InnerTube, Extractor, Managers, NeuroEngine, TogetherRepo
- `TSukiApp.kt` como raíz

## 2. PARA QUÉ existe (problema que resuelve)
Crear dos PlayerController o dos SimpleCache duplica DB y rompe1848 sync.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque explica por qué no hay módulos Hilt y dónde buscar la instancia canónica.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO guardar Activity/View context en singletons; usar `applicationContext`.
- NO crear `SimpleCache` fuera de `PlayerCacheProvider`.
- NO duplicar `OkHttpClient`; reutilizar pool 32/fastFallback.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
`object` para políticas puras (AudioQualityPolicy, UpdateChecker, NextTrackPrecacher), clase + `getInstance` para con estado.

## 6. Flujo y conexiones
Cada ficha indica su `getInstance:línea`. Si falta, es `object` puro.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


