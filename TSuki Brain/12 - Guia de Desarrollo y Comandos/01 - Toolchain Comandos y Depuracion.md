# 12.01 — Toolchain, Comandos y Depuración

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define y documenta el entorno de desarrollo oficial, herramientas del sistema y comandos de compilación/ejecución para TSuki:
- **Entorno de Trabajo Local**:
  - **Sistema Operativo**: Linux x86_64
  - **JDK Oficial**: OpenJDK 26 (`/home/sebaxxfxz/.jdks/openjdk-26.0.2`)
  - **Android SDK**: `/home/sebaxxfxz/Android/Sdk`
  - **Dispositivo de Pruebas Físico**: Dispositivo ADB serial `fb74ec96`
- **Versiones del Stack (`gradle/libs.versions.toml`)**:
  - AGP (Android Gradle Plugin): `9.3.1`
  - Gradle: `9.3.1`
  - Kotlin: `2.2.10`
  - Compose BOM: `2026.06.01`
  - Media3 / ExoPlayer: `1.5.1`
  - SQLite Nativo / Room-less: SQLiteDatabase crudo + DataStore
  - `minSdk 24` | `targetSdk 35` | `compileSdk 36`

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza reproducibilidad absoluta entre sesiones de desarrollo y previene incompatibilidades entre versiones de Java y Gradle. Asegura que cualquier agente de IA o ingeniero humano ejecute exactamente los mismos comandos de compilación sin romper el entorno local.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `12 - Guia de Desarrollo y Comandos/` como manual operacional de primer orden.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Ruta de `JAVA_HOME`**: Debe ser siempre `/home/sebaxxfxz/.jdks/openjdk-26.0.2`. Usar el Java del sistema (`/usr/bin/java`) puede causar errores de compilación con AGP 9.3.1 y Kotlin 2.2.10.
- **`local.properties`**: Archivo ignorado en git (`.gitignore`). Contiene la ruta del SDK y la clave secreta `TOGETHER_BEARER_TOKEN`. No commitear ni borrar este archivo.
- **Dispositivo Físico `fb74ec96`**: No realizar pulsaciones de pantalla aleatorias sin autorización explícita del usuario.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- **Flujo de Modificación y Verificación**:
  1. Realizar edición atómica en código Kotlin (sin comentarios `//` ni `/** */`).
  2. Ejecutar verificación rápida: `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
  3. Si es necesario instalar en el terminal: `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:assembleDebug`.
  4. Desplegar mediante ADB con comando encadenado.

---

## 6. Flujo y conexiones
- Comandos detallados en: [[02 - Compilacion Rapida vs APK]], [[03 - Instalacion adb fb74ec96 y Logcat]].

---

## 7. Guía rápida para una IA nueva
- Para validar cualquier cambio de código en 2 segundos:
  `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`
