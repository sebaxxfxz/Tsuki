# 04.10 — Extracción de Comentarios y Fallback NewPipe

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Extrae y muestra los comentarios de la comunidad para canciones y videos:
- **Intento Primario (InnerTube)**:
  - Intenta consultar comentarios vía `/youtubei/v1/next` utilizando las credenciales del usuario (`cookie` y `visitorData`).
- **Fallback Secundario (NewPipeExtractor)**:
  - Si el usuario no ha iniciado sesión o si YouTube Music devuelve error 401/403 en modo anónimo, la app conmuta automáticamente a `NewPipeExtractor ServiceList.YouTube.getCommentsExtractor(url)`.
  - NewPipe resuelve los comentarios parseando la versión pública de YouTube Web sin requerir autenticación.

**Archivos fuente clave:**
- [`network/TSukiInnerTubeClient.kt:L480-550`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L480-L550)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que los comentarios sigan siendo visibles incluso para usuarios que utilizan la aplicación sin iniciar sesión con cuenta de Google.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Resiliencia de extracción de contenido en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El bloque `try-catch` con fallback a NewPipeExtractor.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Comentarios paginados en demanda mientras el usuario se desplaza por el sheet de comentarios.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - YouTubeExtractor y Ciphers]], [[15 - VideoPlayerScreen 2001L Gestos]].

---

## 7. Guía rápida para una IA nueva
- Para cargar comentarios, invoca `TSukiInnerTubeClient.fetchComments(videoId, visitorData, cookie)`.
