# 12.05 — Patrones UI Ganados M3

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Compendio de patrones arquitecturales y reglas de oro de Jetpack Compose y Material 3 Expressive descubiertos y consolidados durante el desarrollo de TSuki:
- **1. Contrato `M3WavySlider`**: El parámetro `value` **exige estrictamente una fracción `0.0f..1.0f`**. Jamás pasar milisegundos brutos.
- **2. Progreso en MiniPlayer en Fase de Dibujo (Draw Phase)**: La barra de progreso de reproducción del MiniPlayer no debe provocar recomposiciones. Debe leerse como lambda `() -> Float` dentro del bloque `Canvas { ... }`.
- **3. Claves Compuestas en `LazyColumn`**: Las listas con canciones (como la cola o playlists) deben usar claves compuestas: `key = { index, item -> "${item.id}_#$index" }` para tolerar canciones repetidas sin provocar caídas por claves duplicadas.
- **4. Desactivación de Hardware Bitmaps en Coil**: Al cargar carátulas para extracción de paleta con `PlayerColorExtractor`, se debe configurar obligatoriamente `allowHardware(false)` para no provocar excepciones de GPU en la librería `Palette`.
- **5. Modo Negro Puro OLED**: Las superficies de fondo deben verificar `AppearancePreferences.pureBlackOled` y aplicar `#000000` absoluto para apagar los diodos orgánicos.
- **6. Física de Resortes en Interacciones**: Prohibido usar `tween()` en gestos táctiles de arrastre o pulsación; usar resortes físicos definidos en `M3Motion`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene la reaparición de bugs complejos de rendimiento (caídas de FPS, calentamiento de batería) y crashes fatales en tiempo de ejecución.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `12 - Guia de Desarrollo y Comandos/` como guía de estilo y mejores prácticas obligatorias.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Ninguna de las 6 reglas anteriores puede ser violada ni "simplificada" por una IA o desarrollador sin provocar una regresión inmediata.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Código limpio, desacoplado y con cero comentarios en los archivos fuente Kotlin.

---

## 6. Flujo y conexiones
- Fundamenta todo el código de: [[01 - Sistema de Diseno Material 3 Expressive|11.01 - M3 Expressive]], [[05 - MusicPlayerScreenV9 Orquestador 1466L|11.05 - MusicPlayerScreenV9]].

---

## 7. Guía rápida para una IA nueva
- Si vas a tocar una lista o un slider en la UI, repasa mentalmente los 6 patrones ganados antes de enviar tu diff.
