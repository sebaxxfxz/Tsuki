# 00.03 — Onboarding para IAs Nuevas

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Establece el protocolo obligatorio de inducción y reglas de comportamiento para cualquier modelo de Inteligencia Artificial que trabaje en el código de TSuki:
1. **Regla de Cero Comentarios**: No colocar comentarios `//` ni bloques `/** */` en archivos Kotlin bajo `app/src/`. Todas las explicaciones, justificaciones y advertencias se escriben exclusivamente en este cerebro (`TSuki Brain/`).
2. **Ediciones Quirúrgicas**: Prohibido reescribir bloques masivos (>100 líneas) si se puede solucionar con cambios puntuales preservando imports y formato.
3. **Compilación Rápida Obligatoria**: Antes de dar cualquier tarea por concluida, se debe ejecutar:
   ```bash
   JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin
   ```
4. **Dispositivo de Prueba Físico**: El dispositivo oficial conectado es `fb74ec96`. Para instalar:
   ```bash
   JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:assembleDebug
   adb -s fb74ec96 install -r app/build/outputs/apk/debug/app-debug.apk
   adb -s fb74ec96 shell monkey -p com.example.tsuki -c android.intent.category.LAUNCHER 1
   ```
5. **No Tocar Teléfono Sin Permiso**: Prohibido ejecutar toques de pantalla (`adb shell input tap`) o tomar capturas sin que el usuario lo solicite expresamente.

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene regresiones, roturas de estilo, quejas del usuario por comentarios basura en el código, fallos de compilación con Gradle 9.3.1 / JDK 26 y desajustes en el stack tecnológico.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Es la puerta de entrada operativa en `00 - Mapa Central`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No cambiar la versión del JDK ni la ruta `/home/sebaxxfxz/.jdks/openjdk-26.0.2`.
- No modificar el ID de dispositivo `fb74ec96`.
- No alterar `local.properties` (gitignored).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Exploración previa con subagentes antes de editar.
- Respuestas en español con enlaces directos a archivos (`[Archivo.kt](file:///ruta)`).
- Uso de tokens Material 3 Expressive.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Invariantes Intocables y Quirks Criticos]], [[02 - Metodologia de Trabajo y Convenciones]], [[01 - Toolchain Comandos y Depuracion]].

---

## 7. Guía rápida para una IA nueva
- Lee esta ficha.
- Abre [[01 - Invariantes Intocables y Quirks Criticos]].
- Localiza tu subsistema antes de proponer cambios.
