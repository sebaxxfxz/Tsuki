# 11.08 — PlayerBackgroundV9 Gradiente Dinámico

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Crea el fondo ambiental inmersivo para la pantalla del reproductor (`ui/player/PlayerBackgroundV9.kt`):
- **Gradiente Malla / Radial Dinámico**: Combina el color dominante extraído de la carátula del álbum con tonos de superficie oscurecidos para crear un ambiente visual envolvente.
- **Transición Fluida de Colores**: Cuando cambia la canción, los colores de fondo realizan una transición suave interpolada mediante `animateColorAsState` con especificación de resortes físicos (`M3Motion.SpringStandard`).
- **Garantía de Contraste WCAG**: Evalúa matemáticamente la luminancia del color dominante. Si la carátula es excesivamente clara o saturada, atenúa el color mediante `ColorUtils.blendARGB` con negro para asegurar una relación de contraste mínima de 4.5:1 para los textos y controles superpuestos.
- **Soporte Negro Puro OLED**: Si el usuario seleccionó tema OLED en ajustes, el fondo se degrada rápidamente a `#000000` en la parte media e inferior de la pantalla.

**Archivos fuente clave:**
- [`ui/player/PlayerBackgroundV9.kt:L1-160`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/PlayerBackgroundV9.kt#L1-L160)
- [`ui/theme/PlayerColorExtractor.kt:L1-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/PlayerColorExtractor.kt#L1-L120)

---

## 2. PARA QUÉ existe (problema que resuelve)
Elimina los fondos negros planos y aburridos sin sacrificar la legibilidad de los textos ni causar parpadeos bruscos de color entre canciones consecutivas.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/player/` al actuar como la capa base de renderizado en `MusicPlayerScreenV9`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Cálculo de Atenuación de Luminancia**: No eliminar la corrección de brillo en portadas blancas/amarillas. Sin esta atenuación, los títulos blancos y los controles se vuelven completamente invisibles.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Se dibuja utilizando modificadores `Modifier.drawBehind { ... }` o pinceles `Brush.radialGradient` y `Brush.verticalGradient` para máximo rendimiento en GPU sin recomposición.

---

## 6. Flujo y conexiones
- Recibe colores de: [[33 - Theme Color Shape Type PlayerColorExtractor]].
- Se dibuja en el fondo de: [[05 - MusicPlayerScreenV9 Orquestador 1466L]].

---

## 7. Guía rápida para una IA nueva
- Para alterar la suavidad de transición de color entre pistas, ajusta los parámetros de `spring()` en `animateColorAsState`.
