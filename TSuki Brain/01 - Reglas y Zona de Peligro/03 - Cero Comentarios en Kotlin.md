# 01.03 — Cero Comentarios en Código Kotlin

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Estipula la regla absoluta del proyecto: **está terminantemente prohibido colocar comentarios de cualquier tipo (`//`, `/* ... */`, KDoc `/** ... */`) en los archivos Kotlin bajo `app/src/`**.
- Si durante una edición encuentras comentarios preexistentes o cabeceras GPL/Apache, elimínalos en el mismo cambio quirúrgico.
- Si una lógica es compleja y requiere explicación arquitectónica, detállala en la ficha correspondiente de `TSuki Brain/` citando el rango `archivo:línea`.

---

## 2. PARA QUÉ existe (problema que resuelve)
El usuario impone esta regla para mantener un código 100% limpio, sintético y libre de explicaciones obvias o desactualizadas generadas por asistentes de código.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Regla fundamental de estilo en `01 - Reglas y Zona de Peligro`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NUNCA insertes un comentario descriptivo, ni siquiera de una sola línea, en ningún archivo Kotlin de la app.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Si necesitas documentar por qué un método usa `ushr 1` o una fórmula trigonométrica, documéntalo en el Markdown de `TSuki Brain/`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Metodologia de Trabajo y Convenciones]], [[04 - Ediciones Quirurgicas y Delegated-State]].

---

## 7. Guía rápida para una IA nueva
- Antes de guardar cualquier archivo `.kt`, revisa que no contenga barras dobles `//` o bloques de comentarios.
