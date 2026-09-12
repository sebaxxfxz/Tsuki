# 11.32 — M3Motion Tokens Springs Sliders

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define la física y los tokens oficiales del sistema de animación de TSuki (`ui/components/M3Motion.kt`):
- **Especificaciones de Resortes Físicos (Spring Specs)**:
  - `SpringStandard`: Resorte equilibrado para transiciones de pantalla y movimiento de contenedores (`dampingRatio = Spring.DampingRatioNoBouncy`, `stiffness = Spring.StiffnessMediumLow`).
  - `SpringSnappy`: Resorte rápido y reactivo para botones, iconos y cambios de estado instantáneos (`stiffness = Spring.StiffnessMedium`).
  - `SpringBouncy`: Resorte con rebote controlado para celebraciones y microinteracciones lúdicas (ej. botón Me Gusta).
- **Transiciones de Pantalla Compartidas (Shared Transitions)**: Define las curvas de interpolación para elementos compartidos entre listas y vistas de detalle.
- **Tokens de Animación para Sliders**: Parámetros de animación para el deslizamiento suave y la deformación de onda en `M3WavySlider`.

**Archivos fuente clave:**
- [`ui/components/M3Motion.kt:L1-85`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/M3Motion.kt#L1-L85)

---

## 2. PARA QUÉ existe (problema que resuelve)
Prohíbe animaciones lineales o duraciones arbitrarias en milisegundos que hacen que una app se sienta mecánica y falsa. Los resortes físicos responden de forma natural a la velocidad del dedo y permiten interrupciones limpias en mitad del movimiento.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/components/` accesible para cualquier composable del proyecto.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **No reemplazar por `tween()` en gestos**: Nunca usar `tween()` en animaciones provocadas por gestos táctiles de arrastre o pulsación. Los springs son obligatorios.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Reutilización de los objetos singleton de `M3Motion` en modificadores como `animateContentSize` y `animateFloatAsState`.

---

## 6. Flujo y conexiones
- Utilizado en: [[06 - PlayerControlsV9 Pastillas y Slider]], [[08 - PlayerBackgroundV9 Gradiente]], [[01 - Sistema de Diseno Material 3 Expressive]].

---

## 7. Guía rápida para una IA nueva
- Para animar una transición suave de tamaño, utiliza `Modifier.animateContentSize(animationSpec = M3Motion.SpringStandard)`.
