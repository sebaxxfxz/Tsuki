# 02.08 — Gradle AGP 9.3.1 Kotlin 2.2.10

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Documenta el entorno de construcción y dependencias en `gradle/libs.versions.toml`, `build.gradle.kts` y `settings.gradle.kts`:
- **Versiones Principales**: Android Gradle Plugin (AGP) `9.3.1`, Kotlin `2.2.10`, Compose Compiler integrado en Kotlin 2.0+, Compose BOM `2026.06.01`.
- **SDK Targets**: `compileSdk = 36`, `targetSdk = 35`, `minSdk = 24`.
- **Repositorios**: Configurado con `FAIL_ON_PROJECT_REPOS` en `settings.gradle.kts`. Todos los repositorios (Google, MavenCentral, JitPack) deben declararse **únicamente** en `dependencyResolutionManagement.repositories`.
- **JDK Requerido**: OpenJDK 26 (`/home/sebaxxfxz/.jdks/openjdk-26.0.2`).

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza construcciones deterministas y reproducibles, evitando errores de sincronización causados por declarar repositorios dentro de módulos individuales.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Configuración de compilación en `02 - Arquitectura Global`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO agregar `repositories { ... }` dentro de `app/build.gradle.kts`. Violará la regla `FAIL_ON_PROJECT_REPOS` de Gradle.
- NO alterar la compatibilidad de Java: `sourceCompatibility = JavaVersion.VERSION_21`, `targetCompatibility = JavaVersion.VERSION_21`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Compilación verificada siempre con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Toolchain Comandos y Depuracion]], [[02 - Compilacion Rapida vs APK]].

---

## 7. Guía rápida para una IA nueva
- Para añadir una nueva dependencia, declárala primero en `gradle/libs.versions.toml` y luego referénciala en `app/build.gradle.kts`.
