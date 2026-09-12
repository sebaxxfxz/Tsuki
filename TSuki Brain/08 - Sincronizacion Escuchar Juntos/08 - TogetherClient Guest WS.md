# 08.08 — TogetherClient (Cliente WebSocket de Oyente)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa el cliente WebSocket que se conecta a una sala compartida en `together/TogetherClient.kt`:
- Construido sobre el cliente HTTP Ktor con motor OkHttp.
- Negocia la conexión enviando `ClientHello`.
- Escucha el flujo entrante de mensajes `RoomStateMessage`:
  - Si la canción cambió, descarga el stream y carga la nueva pista en `PlayerController`.
  - Si la posición difiere significativamente del host (superando los umbrales de drift), aplica un `seekTo` correctivo.
- Envía latidos periódicos (`HeartbeatPing`) para medir RTT.
- Gestiona reintentos automáticos con reconexión suave ante cortes breves de red Wi-Fi o móvil.

**Archivos fuente clave:**
- [`together/TogetherClient.kt:L25-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherClient.kt#L25-L180)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a cualquier dispositivo unirse a una sesión musical como oyente perfectamente sincronizado.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Cliente de red colaborativo en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La rutina de reconexión con backoff para evitar saturar el servidor anfitrión.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Se desconecta limpiamente al salir de la pantalla o al pulsar "Abandonar sala".

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Protocolo Hibrido LAN y Cloud Relay]], [[12 - TogetherPlaybackSync Drift y QueueHash]].

---

## 7. Guía rápida para una IA nueva
- Para conectar como oyente: `TogetherClient.connect(endpoint, roomSecret)`.
