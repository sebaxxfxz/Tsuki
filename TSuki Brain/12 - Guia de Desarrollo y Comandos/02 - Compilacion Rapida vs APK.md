# 12.02 — Compilación Rápida vs APK

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Establece la distinción crítica entre dos tipos de compilación en el ciclo de vida de desarrollo de TSuki:
- **1. Compilación Rápida (`:app:compileDebugKotlin`)**:
  - Tiempo de ejecución: **~2 a 3 segundos**.
  - No empaqueta recursos, no corre D8/R8 (dexing), no firma ni genera el APK.
  - Verifica exhaustivamente sintaxis de Kotlin, chequeo estricto de tipos, resolución de imports y compatibilidad con el compilador de Jetpack Compose.
  - **Uso Obligatorio**: Ejecutar SIEMPRE inmediatamente después de editar cualquier archivo `.kt` antes de afirmar que la solución funciona.
- **2. Compilación Completa (`:app:assembleDebug`)**:
  - Tiempo de ejecución: **~15 a 30 segundos**.
  - Procesa recursos Android (`aapt2`), ejecuta el pipeline de empaquetado de assets y genera el archivo ejecutable binario en:
    `app/build/outputs/apk/debug/app-debug.apk`.
  - **Uso Obligatorio**: Ejecutar únicamente cuando se va a instalar la aplicación en el dispositivo físico `fb74ec96`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Ahorra hasta un 90% del tiempo de ciclo de retroalimentación durante el desarrollo. Evita esperar 30 segundos en cada pequeño refactor solo para comprobar si faltaba un import o si había un error tipográfico en Compose.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `12 - Guia de Desarrollo y Comandos/` para guiar a cualquier desarrollador en la optimización de sus tiempos de compilación.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Nunca afirmar éxito sin compilar**: Jamás decir al usuario "he arreglado el problema" sin haber corrido al menos `compileDebugKotlin` con código de salida 0.
- **Memoria del Gradle Daemon**: No matar el demonio de Gradle entre ediciones menores (`--no-daemon`), ya que el demonio mantiene en memoria caliente el AST de Kotlin y el caché incremental.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- **Comando de Compilación Rápida**:
  ```bash
  JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin
  ```
- **Comando de Generación de APK**:
  ```bash
  JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:assembleDebug
  ```

---

## 6. Flujo y conexiones
- Dependencia directa de: [[01 - Toolchain Comandos y Depuracion]], [[03 - Instalacion adb fb74ec96 y Logcat]].

---

## 7. Guía rápida para una IA nueva
- Si solo editaste código Kotlin: corre `compileDebugKotlin`.
- Si necesitas probar en el teléfono: corre `assembleDebug` y luego el script de instalación ADB.
