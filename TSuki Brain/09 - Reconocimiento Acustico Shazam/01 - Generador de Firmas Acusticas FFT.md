# 09.01 — Generador de Firmas Acusticas FFT y Espectrograma

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa de forma nativa el algoritmo de generación de firmas acústicas (*Audio Fingerprinting*) compatible con Shazam sin requerir librerías propietarias en `shazam/ShazamSignatureGenerator.kt`:
1. **Captura PCM**: Audio mono a 16.000 Hz, 16 bits por muestra en bloques de 128 muestras.
2. **Ventana de Hann**: Aplica una ventana de Hann sobre un búfer circular de 2048 muestras para minimizar la fuga espectral (*spectral leakage*).
3. **Transformada Rápida de Fourier (FFT 2048)**: Transforma las muestras del dominio temporal al dominio frecuencial.
4. **Propagación de Picos (*Peak Spreading*)**: Identifica los armónicos dominantes y los agrupa en bandas de frecuencia acústicas.
5. **Empaquetado Binario**:
   - Ensambla una cabecera con número mágico, metadatos y tabla de picos.
   - Calcula la suma de verificación CRC-32 sobre el cuerpo binario.
   - Codifica el resultado final en Base64 URL-safe.

**Archivos fuente clave:**
- [`shazam/ShazamSignatureGenerator.kt:L20-220`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/shazam/ShazamSignatureGenerator.kt#L20-L220)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite identificar qué canción suena en el ambiente (en un bar, radio o altavoz externo) directamente desde la app sin depender del SDK binario cerrado de Apple/Shazam.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Núcleo del algoritmo acústico en `shazam/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El tamaño del búfer de FFT (2048 muestras) y la frecuencia de muestreo de 16 kHz. Shazam rechaza firmas con resoluciones diferentes.
- El cálculo de CRC32 en la cabecera binaria.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Procesamiento en `Dispatchers.Default` para no bloquear la captura de audio.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - MusicRecognizer AudioRecord 16kHz]], [[03 - ShazamSignatureGenerator FFT 2048]].

---

## 7. Guía rápida para una IA nueva
- Para generar una firma, pasa las muestras capturadas a `ShazamSignatureGenerator.generateSignature(samples)`.
