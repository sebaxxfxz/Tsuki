# 09.05 — Modelos DTO de Respuesta de Shazam

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define los objetos de transferencia de datos (DTOs) que parsean la respuesta JSON de Shazam en `shazam/models/ShazamModels.kt`:
- `ShazamResponse`: Contenedor raíz con lista de coincidencias.
- `ShazamTrack`: Título, subtítulo (artista), clave única (`key`), sección de metadatos (álbum, discográfica, año) y carátulas (`images.coverart`).
- Mapea el resultado a una entidad de dominio `MediaTrack` lista para ser reproducida o guardada en la base de datos de historial de reconocimiento.

**Archivos fuente clave:**
- [`shazam/models/ShazamModels.kt:L1-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/shazam/models/ShazamModels.kt#L1-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
Desacopla la respuesta cruda de la API de Shazam del modelo de datos interno de TSuki.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Modelos de red en `shazam/models/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Las anotaciones `@SerialName` que mapean campos opcionales que Shazam a menudo omite en canciones independientes.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Parseo tolerante con `ignoreUnknownKeys = true`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[04 - Shazam Cliente Anti-Ban Cola]], [[10 - Modelos de Dominio MediaTrack y Lyrics]].

---

## 7. Guía rápida para una IA nueva
- Para convertir una respuesta de Shazam a track de TSuki: `shazamTrack.toMediaTrack()`.
