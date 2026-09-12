# 01.02 — Metodología de Trabajo y Convenciones

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Establece las reglas operativas de ingeniería adoptadas en el repositorio:
1. **Exploración previa delegada**: Usar subagentes o lecturas dirigidas con `grep -n` antes de proponer modificaciones. Nunca solicitar volcados completos de archivos grandes.
2. **Consultar antes de inventar UX**: No asumir comportamientos de interfaz sin preguntar al usuario si implican nuevas pantallas o cambios en flujos clave.
3. **Plan unificado en un solo mensaje**: Proponer las modificaciones, archivos afectados y comandos de verificación antes de iniciar la edición.
4. **Ediciones quirúrgicas**: Mantener imports, formateo y firmas preexistentes.
5. **Cero comentarios en código Kotlin**: Prohibido agregar `//` o `/** */` en `app/src/`. La documentación vive en `TSuki Brain/`.
6. **Compilación de verificación**: Comprobar sintaxis y tipos con `compileDebugKotlin`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Mantiene un código limpio, sin ruido de comentarios obsoletos, previene regresiones y garantiza que el repositorio sea legible y mantenible.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Normativa metodológica en `01 - Reglas y Zona de Peligro`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Prohibido hacer `git push --force`, alterar configuraciones globales de git o saltarse los checks de compilación.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Verificación con JDK 26 y Gradle wrapper nativo.
- Respuestas claras en español.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[03 - Cero Comentarios en Kotlin]], [[04 - Ediciones Quirurgicas y Delegated-State]], [[07 - Checklist Pre-Commit y Verificacion]].

---

## 7. Guía rápida para una IA nueva
- Aplica siempre la secuencia: Explorar → Planificar → Editar con precisión → Compilar → Reportar.
