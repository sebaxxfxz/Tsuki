# 12.06 — Testing Unitario y Sin CI

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Documenta el estado actual de la suite de pruebas y la política de integración en TSuki:
- **Suite de Pruebas Actual**:
  - Existe exclusivamente la prueba unitaria base en `app/src/test/java/com/example/tsuki/ExampleUnitTest.kt`.
  - No existen pruebas instrumentadas de UI (`androidTest/`) ni suite de integración automatizada.
- **Ausencia de Integración Continua (Sin CI)**:
  - No existe directorio `.github/workflows/` ni pipelines de GitHub Actions en el repositorio.
  - La verificación depende al 100% de la validación local estricta antes de cada commit.
- **Comandos de Prueba Local**:
  ```bash
  JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew testDebugUnitTest
  ```
- **Análisis Estático con Detekt**:
  ```bash
  ./scripts/run-detekt.sh
  ```

---

## 2. PARA QUÉ existe (problema que resuelve)
Clarifica que la responsabilidad de no romper la compilación ni introducir regresiones recae enteramente en el desarrollador y en el agente de IA que trabaja sobre el código local.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `12 - Guia de Desarrollo y Comandos/` como advertencia y guía de calidad.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **No asumir que un bot de CI probará el código**: No hagas push ni afirmes que una tarea está completada sin haber corrido `compileDebugKotlin` localmente.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Verificación exhaustiva local de sintaxis, tipos y comportamiento en dispositivo físico.

---

## 6. Flujo y conexiones
- Vinculado a: [[01 - Toolchain Comandos y Depuracion]], [[02 - Compilacion Rapida vs APK]].

---

## 7. Guía rápida para una IA nueva
- Ejecuta siempre `compileDebugKotlin` como prueba de humo mínima indispensable.
