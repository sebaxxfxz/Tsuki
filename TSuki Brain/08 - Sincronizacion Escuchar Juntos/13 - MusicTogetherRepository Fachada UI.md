# 08.13 — MusicTogetherRepository (Fachada Reactiva para Compose)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Actúa como la fachada única de acceso para la interfaz de usuario en `together/MusicTogetherRepository.kt`:
- Expone un `StateFlow<TogetherSessionState>` reactivo que describe:
  - Estado de conexión (`DISCONNECTED`, `CONNECTING`, `HOSTING`, `GUEST`).
  - Identificador de sala y enlace para compartir.
  - Lista de miembros presentes con nombres y avatares.
  - Rol asignado y permisos.
- Simplifica los comandos para la UI: `createLanRoom()`, `createOnlineRoom()`, `joinRoom(link)`, `leaveSession()`.

**Archivos fuente clave:**
- [`together/MusicTogetherRepository.kt:L20-110`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/MusicTogetherRepository.kt#L20-L110)
- [`ui/screens/TogetherScreen.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/TogetherScreen.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Aísla a los componentes de Jetpack Compose de la complejidad de bajo nivel de los WebSockets de Ktor y la gestión de sockets de red.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Capa de repositorio en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La emisión inmutable de `TogetherSessionState`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Consumido directamente por `TogetherScreen.kt`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[28 - SubscriptionsScreen y RSS de Canales]], [[01 - Protocolo Hibrido LAN y Cloud Relay]].

---

## 7. Guía rápida para una IA nueva
- Para observar el estado de la sala en Compose, recolecta `MusicTogetherRepository.getInstance(context).sessionState`.
