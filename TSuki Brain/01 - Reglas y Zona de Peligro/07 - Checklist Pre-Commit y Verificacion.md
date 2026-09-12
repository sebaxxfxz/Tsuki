# 01.07 — Checklist Pre-Commit y Verificación en Dispositivo

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Protocolo exhaustivo de verificación técnica previo a dar cualquier cambio por completado:
1. **Verificación de Sintaxis y Tipos (Rápida ~2s)**:
   ```bash
   JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin
   ```
2. **Construcción del APK de Depuración**:
   ```bash
   JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:assembleDebug
   ```
   El artefacto se genera en `app/build/outputs/apk/debug/app-debug.apk`.
3. **Instalación y Lanzamiento en Dispositivo (`fb74ec96`)**:
   ```bash
   adb -s fb74ec96 install -r app/build/outputs/apk/debug/app-debug.apk && adb -s fb74ec96 shell monkey -p com.example.tsuki -c android.intent.category.LAUNCHER 1
   ```
4. **Resolución de Firma Incompatible**:
   Si adb devuelve `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, desinstalar previamente:
   ```bash
   adb -s fb74ec96 uninstall com.example.tsuki
   ```
5. **Inspección de Crashes en Tiempo Real**:
   ```bash
   adb -s fb74ec96 logcat -d -s AndroidRuntime:E
   ```
6. **Verificación Visual**:
   Si el usuario solicita comprobar la pantalla:
   ```bash
   adb exec-out screencap -p > /tmp/screen_check.png
   ```
   Leer la imagen generada antes de afirmar que la interfaz es correcta.

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que ningún código roto, con errores de compilación en Kotlin 2.2.10 o excepciones en tiempo de ejecución llegue a manos del usuario.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Guía de control de calidad en `01 - Reglas y Zona de Peligro`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No asumir que un código compila porque "parece correcto": ejecutar siempre `compileDebugKotlin`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Flujo automatizado mediante comandos de terminal locales.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Toolchain Comandos y Depuracion]], [[02 - Compilacion Rapida vs APK]].

---

## 7. Guía rápida para una IA nueva
- Ejecuta `compileDebugKotlin` tras cada edición antes de entregar tu respuesta.
