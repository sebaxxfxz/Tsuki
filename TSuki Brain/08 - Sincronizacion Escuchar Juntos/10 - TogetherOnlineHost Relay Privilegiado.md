# 08.10 — TogetherOnlineHost (Anfitrión en la Nube vía Cloud Relay)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Permite al anfitrión coordinar una sesión cuando los invitados están en diferentes redes o utilizan datos móviles en `together/TogetherOnlineHost.kt`:
- Conecta mediante WebSocket autenticado al servidor relay en la nube.
- Inyecta la cabecera `Authorization: Bearer TOGETHER_BEARER_TOKEN` (almacenada en `local.properties`).
- Actúa como anfitrión privilegiado: el relay en la nube solo acepta cambios de estado provenientes de esta conexión autenticada.
- Retransmite el estado de la sala a través del servidor central hacia todos los participantes remotos.

**Archivos fuente clave:**
- [`together/TogetherOnlineHost.kt:L20-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherOnlineHost.kt#L20-L140)
- [`together/TogetherOnlineEndpoint.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherOnlineEndpoint.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite que amigos en diferentes ciudades o bajo redes 4G/5G puedan compartir su música en tiempo real sin estar bajo el mismo router Wi-Fi.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Módulo de relay online en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La protección del token `TOGETHER_BEARER_TOKEN`: nunca quemarlo en el repositorio público ni comitearlo en Git.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Normalización automática de endpoints HTTPS a WSS en `TogetherOnlineEndpoint.kt`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[11 - TogetherOnlineApi y Endpoint Bearer]], [[01 - Protocolo Hibrido LAN y Cloud Relay]].

---

## 7. Guía rápida para una IA nueva
- Para crear una sala remota: `TogetherOnlineHost.createOnlineRoom()`.
