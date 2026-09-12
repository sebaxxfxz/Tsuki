# 09.03 — ShazamSignatureGenerator (Estructura Binaria y CRC32)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Detalla la estructura interna del payload binario generado por `ShazamSignatureGenerator.kt`:
- **Cabecera de Firma (Header)**:
  - Magic Bytes de identificación.
  - Versión del formato de firma.
  - Frecuencia de muestreo (16.000 Hz).
  - Número total de cuadros temporales analizados.
- **Cuerpo de Picos (Peaks Table)**:
  - Lista de tuplas `(frecuencia, tiempo, magnitud)` cuantizadas en enteros de 16 bits para compresión máxima.
- **Suma de Verificación**:
  - CRC32 calculado sobre el payload binario antes de la codificación final.
- **Codificación URL-Safe**:
  - Base64 sin saltos de línea listo para ser incluido en el cuerpo JSON de la petición HTTP.

**Archivos fuente clave:**
- [`shazam/ShazamSignatureGenerator.kt:L80-200`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/shazam/ShazamSignatureGenerator.kt#L80-L200)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza el cumplimiento estricto del protocolo binario de firmas de Shazam.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Módulo binario de audio en `shazam/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El orden de los bytes (*Endianness* Little-Endian) en la serialización del buffer.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Implementación matemática verificada contra muestras de audio reales.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Generador de Firmas Acusticas FFT]], [[04 - Shazam Cliente Anti-Ban Cola]].

---

## 7. Guía rápida para una IA nueva
- Si la API responde con error de firma inválida, revisa la cabecera binaria generada en este archivo.
