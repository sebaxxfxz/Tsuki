# 08.09 — TogetherServer (Servidor Embebido LAN en Puerto 42117)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Levanta un servidor HTTP y WebSocket local directamente en el teléfono del anfitrión utilizando Ktor CIO en `together/TogetherServer.kt`:
- **Puerto de Escucha**: `42117` en todas las interfaces de red local (`0.0.0.0`).
- **Rutas Expuestas**:
  - `GET /`: Comprobación básica de salud (*Health Check*).
  - `webSocket("/together")`: Endpoint principal de sincronización en tiempo real.
- **Difusión Masiva (*Broadcasting*)**: Cada vez que el anfitrión pausa, salta de pista o avanza la reproducción, el servidor serializa `RoomStateMessage` y lo difunde concurrentemente a todos los WebSockets de invitados conectados en la misma red Wi-Fi.
- **Cero Dependencia de Internet**: Funciona completamente en redes locales sin conexión a internet (ej. acampadas, coches o zonas sin cobertura con punto de acceso Wi-Fi local).

**Archivos fuente clave:**
- [`together/TogetherServer.kt:L25-190`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherServer.kt#L25-L190)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite escuchar música en grupo sin depender de servidores en la nube de terceros, con latencia ultra baja (<50ms en red Wi-Fi local).

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Servidor embebido en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El puerto `42117`: es el puerto estándar reservado por TSuki en la LAN.
- La detención limpia en `stop()` liberando el socket para no bloquear el puerto en futuros arranques.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Motor Ktor CIO configurado con corrutinas ligeras.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Protocolo Hibrido LAN y Cloud Relay]], [[13 - MusicTogetherRepository Fachada UI]].

---

## 7. Guía rápida para una IA nueva
- Para iniciar la sala local: `TogetherServer.start(port = 42117)`.
