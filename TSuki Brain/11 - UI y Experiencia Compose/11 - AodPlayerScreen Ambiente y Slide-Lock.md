# 11.11 — AodPlayerScreen Ambiente y Slide-Lock

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla de visualización continua de ultra-bajo consumo energético inspirada en Always-on-Display (`ui/player/AodPlayerScreen.kt`):
- **Fondo Negro Absoluto `#000000`**: Apaga completamente los diodos orgánicos en pantallas OLED y AMOLED, logrando un consumo de batería casi nulo.
- **Mecanismo Anti Quemado de Pantalla (Pixel Shift)**: Cada 60 segundos desplaza de forma imperceptible la posición de los textos y elementos gráficos en 2 a 4 píxeles para prevenir el desgaste prematuro de los píxeles del panel.
- **Reloj Digital Minimalista y Batería**: Muestra la hora del sistema en formato grande con bajo brillo y porcentaje de batería restante.
- **Controles de Música Monocromáticos**: Botones mínimos para pausar y saltar canciones en color blanco tenue con opacidad reducida.
- **Desbloqueo por Deslizamiento (Slide-to-Unlock)**: Gesto de deslizamiento en la parte inferior para salir del modo AoD y regresar al reproductor normal, evitando toques accidentales en el bolsillo.

**Archivos fuente clave:**
- [`ui/player/AodPlayerScreen.kt:L1-210`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/AodPlayerScreen.kt#L1-L210)
- [`ui/player/MusicPlayerScreenV9.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/MusicPlayerScreenV9.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite usar el teléfono como un reloj despertador musical o pantalla ambiental en un soporte de escritorio durante la noche sin desgastar la batería ni quemar la pantalla OLED.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/player/` al ser un modo de visualización alternativo de la reproducción activa.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Fondo Negro Puro**: El fondo debe ser inmutablemente `Color.Black` (`#000000`). No aplicar gradientes, transparencias ni grises.
- **Brillo y Opacidad**: Los elementos no deben superar el 60-70% de opacidad para garantizar el confort visual en la oscuridad.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Mantiene la pantalla encendida mediante `FLAG_KEEP_SCREEN_ON` en la ventana mientras el modo AoD está activo y se descarta al salir.

---

## 6. Flujo y conexiones
- Lanzado desde el menú de opciones de [[05 - MusicPlayerScreenV9 Orquestador 1466L]].

---

## 7. Guía rápida para una IA nueva
- Si modificas los elementos visuales de AoD, verifica que el `pixelShiftOffset` siga aplicándose al modificador raíz.
