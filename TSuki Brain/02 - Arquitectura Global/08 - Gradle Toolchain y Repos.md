# 02.08 — Gradle Toolchain y Repos (JDK 26 + AGP 9.3.1)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Single-module `:app` (`com.example.tsuki`, min24/target35/compile36). Repos solo en settings. Versiones centralizadas en TOML. `local.properties` gitignored con SDK + `TOGETHER_BEARER_TOKEN`.

**Archivos fuente que documenta:**
- `gradle/libs.versions.toml` (AGP 9.3.1, Kotlin 2.2.10, Compose BOM 2026.06.01, Media3 1.5.1, NewPipeExtractor 0.26.5, lyrics-ui 1.0.19)
- `settings.gradle.kts` FAIL_ON_PROJECT_REPOS + jitpack
- `gradle.properties`, `build.gradle.kts`, `app/build.gradle.kts`

## 2. PARA QUÉ existe (problema que resuelve)
Añadir repos en `app/build` rompe `FAIL_ON_PROJECT_REPOS`. Cambiar BOM/Media3 sin probar rompe karaoke/notif.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque es toolchain global.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO añadir repos fuera de `settings.gradle.kts`.
- NO bump AGP/Kotlin/BOM/Media3 sin `assembleDebug` + prueba en `fb74ec96`.
- `plans/*.md` no son verdad; el código manda.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Verificación rápida `compileDebugKotlin` (~2s), completa `assembleDebug`, APK en `app/build/outputs/apk/debug/`. Sin CI.

## 6. Flujo y conexiones
JDK en `/home/sebaxxfxz/.jdks/openjdk-26.0.2`, SDK `/home/sebaxxfxz/Android/Sdk`. `proguard-rules.pro` + `keepRules/rules.keep` para release.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


