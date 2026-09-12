# 03.08 — Política de Reintentos y Generaciones de Reproducción

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa la resiliencia ante cortes de red y saltos rápidos de pista en `playback/PlayerController.kt`:
- **Contador Monotónico `playGeneration`**: Entero que se incrementa en cada orden de reproducción (`playTrack`, `playQueue`, `playNext`). Toda tarea asíncrona de extracción de URLs o carga de letras verifica `if (generation != currentGeneration) return`. Si el usuario pulsa "Siguiente" tres veces seguidas, las dos peticiones anteriores se descartan inmediatamente al completar la red sin pisar la pista elegida.
- **Política de Reintentos Exponenciales**:
  - `MAX_RETRY_PER_SONG = 4`.
  - `BASE_RETRY_MS = 2000L`, con crecimiento exponencial hasta un techo de `15000L`.
- **Manejo de HTTP 403 Forbidden (URLs caducadas)**: Si ExoPlayer falla con error HTTP 403 (enlace CDN de GoogleVideo expirado), el interceptor purga la clave de `urlCache` y solicita una URL fresca a `YouTubeExtractor`.
- **Salto Defensivo (`handleFinalFailure`)**: Si tras 4 reintentos la pista sigue sin responder, la app emite una alerta discreta y avanza automáticamente a la siguiente canción de la cola.

**Archivos fuente clave:**
- [`playback/PlayerController.kt:L950-1040`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt#L950-L1040)

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene condiciones de carrera causadas por la latencia variable de las peticiones HTTP y asegura que la reproducción nunca se detenga en bucle infinito ante un enlace caído.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Lógica de resiliencia del controlador de audio en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El chequeo de `generation == playGeneration` en callbacks asíncronos.
- La invalidación forzada de `urlCache.remove(videoId)` ante errores 403.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Backoff exponencial no bloqueante implementado mediante `delay(...)` en corrutinas.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - PlayerController Central]], [[02 - YouTubeExtractor y Ciphers]].

---

## 7. Guía rápida para una IA nueva
- Si modificas el método `playQueue`, asegúrate de incrementar `playGeneration` antes de lanzar corrutinas hijas.
