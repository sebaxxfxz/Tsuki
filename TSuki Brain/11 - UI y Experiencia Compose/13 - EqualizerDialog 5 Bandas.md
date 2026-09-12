# 11.13 — EqualizerDialog 5 Bandas y Efectos DSP

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Interfaz de usuario para el control del procesador de señal digital (DSP) de audio (`ui/player/EqualizerDialog.kt`):
- **Ecualizador Paramétrico de 5 Bandas**: Sliders verticales interactivos para ajustar la ganancia en frecuencias clave:
  - 60 Hz (Sub-graves / Bajos profundos)
  - 230 Hz (Graves y cuerpo de batería)
  - 910 Hz (Rango medio / Voces principales)
  - 3.6 kHz (Presencia vocal y guitarras)
  - 14 kHz (Brillo y agudos de platillos)
- **Efecto Bass Boost**: Slider para amplificar los graves mediante el hardware DSP del dispositivo.
- **Efecto Virtualizer (Sonido Espacial)**: Slider para expandir el escenario sonoro en auriculares estéreo.
- **Selector de Presets Preconfigurados**: Acceso rápido a curvas populares: Rock, Pop, Jazz, Clásica, Flat, Electrónica y Vocal Boost.
- **Persistencia Inmediata**: Guarda las curvas en `PlayerPreferences` para restaurarlas al reiniciar la aplicación.

**Archivos fuente clave:**
- [`ui/player/EqualizerDialog.kt:L1-290`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/EqualizerDialog.kt#L1-L290)
- [`playback/AudioEqualizerHelper.kt:L1-210`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/AudioEqualizerHelper.kt#L1-L210)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a audiófilos y usuarios exigentes calibrar la respuesta acústica según sus auriculares, altavoces Bluetooth o preferencias personales.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/player/` al desplegarse como un diálogo modal accesible directamente desde el reproductor.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Rango de Ganancia en Milidecibelios**: La API nativa de Android `android.media.audiofx.Equalizer` opera en milidecibelios (`mB`), típicamente entre `-1500 mB` y `+1500 mB` (-15dB a +15dB). No normalizar a enteros arbitrarios sin realizar la conversión adecuada.
- **Liberación Segura de Efectos**: Asegurar que los efectos se apliquen únicamente sobre el `audioSessionId` activo de ExoPlayer.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Diálogo estilizado con esquinas redondeadas de `28dp` y fondo `surfaceContainerHigh`. Sliders con indicación numérica en decibelios en tiempo real.

---

## 6. Flujo y conexiones
- Controla: `playback/AudioEqualizerHelper.kt`.
- Invocado desde: [[05 - MusicPlayerScreenV9 Orquestador 1466L]].

---

## 7. Guía rápida para una IA nueva
- Para resetear el ecualizador a plano, invoca el preset `FLAT` que coloca todas las bandas en `0 mB`.
