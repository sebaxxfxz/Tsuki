# 07.07 — TogetherGuestPlaybackPlanner (Planificador de Permisos de Invitados)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la matriz de permisos de los oyentes conectados en `together/TogetherGuestPlaybackPlanner.kt`:
- Evalúa los permisos configurados por el anfitrión para la sala:
  - Modo "Solo Escucha": Los invitados no pueden pausar, saltar canciones ni alterar el orden.
  - Modo "Colaborativo con Aprobación": Los invitados pueden solicitar añadir canciones que el anfitrión debe aceptar.
  - Modo "Democrático": Todos los miembros tienen control total sobre transporte y cola.
- Traduce intenciones del usuario local en órdenes de red permitidas o muestra explicaciones claras en pantalla si la acción no está autorizada.

**Archivos fuente clave:**
- [`together/TogetherGuestPlaybackPlanner.kt:L1-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherGuestPlaybackPlanner.kt#L1-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene el caos en fiestas o sesiones públicas donde invitados no deseados puedan interrumpir constantemente la música.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Lógica de permisos en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La verificación de permisos en el servidor: el servidor anfitrión DEBE re-validar los permisos y nunca confiar ciegamente en el cliente.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Notificaciones amigables cuando una acción es denegada.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[03 - TogetherMessages Contrato v1]], [[09 - TogetherServer LAN CIO 42117]].

---

## 7. Guía rápida para una IA nueva
- Antes de ejecutar un seek en modo invitado, consulta `TogetherGuestPlaybackPlanner.canControl(role)`.
