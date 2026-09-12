# 03.13 — TogetherManager y Control Colaborativo

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Actúa como el mediador entre el reproductor local `PlayerController` y el módulo distribuido `together/`:
- Intercepta comandos de usuario (play, pause, seek, saltar pista, reordenar).
- Si el usuario es **Anfitrión (Host)**: difunde el nuevo estado de la sala a todos los oyentes conectados vía WebSocket.
- Si el usuario es **Invitado (Guest)**:
  - Si tiene permisos concedidos: envía una solicitud de control (`ControlRequest`) al anfitrión.
  - Si no tiene permisos: revierte la acción local y muestra una notificación de permisos requeridos.
- **Supresión de Avance Local**: En invitados, `shouldSuppressLocalPlaybackAdvance()` bloquea que la app pase automáticamente a la siguiente canción al terminar el archivo si el anfitrión aún no ha emitido el cambio.
- **Supresión de Eco (`suppressEchoUntilElapsedMs`)**: Ignora actualizaciones reflejadas de vuelta por la red durante una ventana de 700ms para evitar bucles de rebote.

**Archivos fuente clave:**
- [`playback/TogetherManager.kt:L45-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/TogetherManager.kt#L45-L120)
- [`together/TogetherPlaybackSync.kt:L5-47`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherPlaybackSync.kt#L5-L47)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite que múltiples dispositivos escuchen exactamente la misma canción de forma síncrona sin que las acciones de un participante desorganicen a los demás.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Es el conector entre la capa de audio (`playback/`) y la capa colaborativa (`together/`).

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La guardia `shouldSuppressLocalPlaybackAdvance()`. Si se desactiva, los invitados con conexiones más rápidas avanzarán de pista antes de tiempo.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Estado colaborativo reflejado en la UI mediante `TogetherScreen`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Protocolo Hibrido LAN y Cloud Relay]], [[02 - Algoritmo de Sincronizacion y Drift]].

---

## 7. Guía rápida para una IA nueva
- Para consultar si hay una sesión compartida activa, usa `TogetherManager.isSessionActive()`.
