# 08.11 — TogetherOnlineApi y Resolución de Endpoints

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona la comunicación REST y la resolución de endpoints con el backend en la nube en `together/TogetherOnlineApi.kt` y `TogetherOnlineEndpoint.kt`:
- Realiza peticiones HTTP para crear salas remotas (`POST /v1/together/rooms`), consultar disponibilidad de salas y reportar estadísticas.
- **Resolución y Normalización de URLs (`TogetherOnlineEndpoint.kt`)**:
  - Soporta URLs relativas, esquemas `http://` -> `ws://` y `https://` -> `wss://`.
  - Persiste la URL base del servidor en Preferences DataStore (`together_endpoint`).

**Archivos fuente clave:**
- [`together/TogetherOnlineApi.kt:L15-80`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherOnlineApi.kt#L15-L80)
- [`together/TogetherOnlineEndpoint.kt:L17-76`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherOnlineEndpoint.kt#L17-L76)

---

## 2. PARA QUÉ existe (problema que resuelve)
Desacopla la dirección del servidor en la nube para permitir cambiar de backend o usar servidores privados auto-alojados sin recompilar la app.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Capa de red REST en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La conversión estricta a esquemas WebSocket `ws`/`wss`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Manejo de timeouts estrictos de 10 segundos en `OkHttp`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[10 - TogetherOnlineHost Relay Privilegiado]], [[03 - TogetherMessages Contrato v1]].

---

## 7. Guía rápida para una IA nueva
- Para configurar un servidor personalizado, usa `TogetherOnlineEndpoint.saveBaseUrl(context, url)`.
