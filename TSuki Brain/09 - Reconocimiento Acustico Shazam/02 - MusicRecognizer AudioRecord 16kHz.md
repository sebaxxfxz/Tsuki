# 09.02 — MusicRecognizer (Grabación AudioRecord a 16 kHz)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Administra el hardware del micrófono mediante la API nativa `android.media.AudioRecord` en `shazam/MusicRecognizer.kt`:
- **Configuración Acústica**:
  - Fuente de audio: `MediaRecorder.AudioSource.MIC`.
  - Tasa de muestreo: 16.000 Hz.
  - Canales: `AudioFormat.CHANNEL_IN_MONO`.
  - Codificación: `AudioFormat.ENCODING_PCM_16BIT`.
- **Monitoreo de Amplitud en Tiempo Real**:
  - Calcula la amplitud media RMS de cada bloque capturado y emite un flujo de porcentaje (0f a 1f) consumido por la animación de radar de `RecognitionScreen`.
- **Ventana de Muestreo de 4 a 8 Segundos**:
  - Acumula muestras durante 4 segundos. Si genera una firma válida, la envía; si la red reporta "no match", extiende la captura hasta 8 segundos antes de rendirse.

**Archivos fuente clave:**
- [`shazam/MusicRecognizer.kt:L20-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/shazam/MusicRecognizer.kt#L20-L140)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza la captura limpia de audio del entorno respetando los permisos de Android y los estados de grabación del micrófono.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Captura física de audio en `shazam/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La liberación de `AudioRecord.release()` en bloques `finally`: no liberar el micrófono bloquea el hardware para otras aplicaciones del teléfono.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Solicitud previa del permiso en tiempo de ejecución `RECORD_AUDIO` en la UI.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Generador de Firmas Acusticas FFT]], [[06 - RecognitionScreen y Flujo UX]].

---

## 7. Guía rápida para una IA nueva
- Para iniciar el reconocimiento: `MusicRecognizer.recognize(context)`.
