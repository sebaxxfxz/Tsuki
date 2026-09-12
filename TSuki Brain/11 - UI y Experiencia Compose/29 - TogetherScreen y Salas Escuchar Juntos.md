# 11.29 — TogetherScreen y Salas Escuchar Juntos

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Interfaz para la experiencia social y sincronización de música en tiempo real entre múltiples dispositivos (`ui/screens/TogetherScreen.kt`):
- **Creación de Sala Anfitrión (Host)**:
  - Genera un código de sala aleatorio de 6 caracteres alfanuméricos.
  - Muestra un código QR generado localmente para que amigos cercanos puedan escanearlo y unirse al instante.
  - Control de permisos de sala: solo el anfitrión puede cambiar de canción o permitir que los invitados agreguen canciones a la cola.
- **Unirse como Invitado (Client)**:
  - Campo de texto estilizado para introducir el código de sala o lector de código QR con la cámara.
  - Sincronización automática de reproducción a nivel de milisegundos mediante el algoritmo de reloj de Berkeley.
- **Lista de Oyentes Activos**: Muestra los avatares, nombres y estado de sincronización acústica de todos los participantes conectados en la sala.

**Archivos fuente clave:**
- [`ui/screens/TogetherScreen.kt:L1-430`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/TogetherScreen.kt#L1-L430)
- [`together/TogetherServer.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherServer.kt)
- [`together/TogetherClient.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherClient.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a grupos de amigos o parejas escuchar exactamente la misma canción al mismo tiempo sin importar la distancia, o conectar múltiples teléfonos en una misma habitación para amplificar el sonido.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como módulo social de primer nivel en la app.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Token de Autenticación de Servidor**: La conexión al servidor público de señalización utiliza la clave `TOGETHER_BEARER_TOKEN` definida en `local.properties`. No hardcodear tokens temporales en el código fuente.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Desconexión limpia y liberación de sockets WebSocket al salir de la pantalla o al pausar la sesión.

---

## 6. Flujo y conexiones
- Orquestado por todo el subsistema: [[01 - Protocolo Hibrido LAN y Cloud Relay|08.01 - Escuchar Juntos]], [[02 - Algoritmo de Sincronizacion y Drift|08.04 - Reloj de Berkeley]].

---

## 7. Guía rápida para una IA nueva
- Para unirse a una sesión desde la UI, invoca `togetherManager.joinRoom(roomCode)`.
