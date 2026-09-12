# 09.04 — Cliente HTTP de Shazam y Manejo de Cuotas

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona las solicitudes HTTP contra los endpoints públicos de la API de Shazam en `shazam/Shazam.kt`:
- Endpoint: `https://amp.shazam.com/discovery/v5/es/ES/android/-/tag/{uuid}`.
- Inyecta cabeceras de cliente oficial Android de Shazam (User-Agent, Timezone, Device Model).
- Maneja respuestas:
  - Coincidencia positiva (`track` presente): Parsea título, artista, álbum, carátulas y enlaces.
  - Sin coincidencia: Retorna `RecognitionOutcome.NoMatch`.
  - Límite de tasa (HTTP 429): Aplica reintento con backoff exponencial.

**Archivos fuente clave:**
- [`shazam/Shazam.kt:L20-110`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/shazam/Shazam.kt#L20-L110)

---

## 2. PARA QUÉ existe (problema que resuelve)
Conecta la firma acústica local con los servidores globales de reconocimiento musical de Shazam.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Cliente de red de reconocimiento en `shazam/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El formato del JSON de envío: `{"signatures": [{"samplems": ..., "uri": "data:audio/vnd.shazam.sig;base64,..."}]}`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- UUID pseudo-aleatorio generado por sesión para evitar rastreo persistente.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[05 - ShazamModels DTOs SerialName]], [[06 - RecognitionScreen y Flujo UX]].

---

## 7. Guía rápida para una IA nueva
- Para enviar una firma: `Shazam.recognize(signatureBase64, sampleDurationMs)`.
