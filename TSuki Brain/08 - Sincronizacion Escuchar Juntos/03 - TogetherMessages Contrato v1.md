# 08.03 — TogetherMessages (Especificación del Protocolo v1)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define la gramática completa y tipada de los mensajes WebSocket del protocolo "Escuchar Juntos":
- **`const val TogetherProtocolVersion: Int = 1`**: Versión canónica del protocolo.
- **Mensajes de Handshake**:
  - `ClientHello`: Enviado por el invitado al conectar (`clientVersion`, `userId`, `userName`).
  - `ServerWelcome`: Respuesta del anfitrión confirmando conexión con `protocolVersion`, `roomId`, `assignedRole`.
- **Mensajes de Estado**:
  - `RoomStateMessage`: Estado global de la sala emitido por el anfitrión (`currentTrack`, `positionMs`, `isPlaying`, `hostTimestamp`, `queueHash`, `members`).
  - `QueueSyncMessage`: Cola completa de reproducción serializada.
- **Mensajes de Control**:
  - `ControlRequest`: Intención enviada por invitado (`action`: `"play"`, `"pause"`, `"seek_to"`, `"next"`, `"previous"`, `targetPositionMs`).
  - `AddTrackRequest`: Solicitud para añadir una canción a la cola compartida.
- **Mensajes de Pulso**:
  - `HeartbeatPing` y `HeartbeatPong`: Paquetes ligeros para estimación continua de RTT y latencia.

**Archivos fuente clave:**
- [`together/TogetherMessages.kt:L7-93`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherMessages.kt#L7-L93)

---

## 2. PARA QUÉ existe (problema que resuelve)
Define un contrato inmutable e inequívoco para que clientes de diferentes dispositivos y el relay online se comuniquen sin errores de serialización.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Contrato del protocolo en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Ningún `@SerialName` existente. Modificar `"seek_to"` por `"seek"` romperá la comunicación con versiones anteriores.
- `TogetherProtocolVersion = 1`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Serialización mediante `kotlinx.serialization.json.Json`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[05 - TogetherJson Discriminador Type]], [[01 - Protocolo Hibrido LAN y Cloud Relay]].

---

## 7. Guía rápida para una IA nueva
- Todo nuevo mensaje debe heredar de la interfaz base sellada y declarar su `@SerialName` único.
