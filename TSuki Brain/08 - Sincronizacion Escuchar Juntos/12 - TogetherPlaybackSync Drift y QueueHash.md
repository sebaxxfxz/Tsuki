# 08.12 — TogetherPlaybackSync (Algoritmo de Drift y Hash de Cola)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Contiene los algoritmos matemáticos de sincronización temporal y verificación de consistencia en `together/TogetherPlaybackSync.kt`:
- **Umbrales de Deriva (*Drift Tolerance*)**:
  - `DriftPausedMs = 150L`: En estado de pausa, cualquier desfase superior a 150ms fuerza un seek correctivo.
  - `DriftLanMs = 700L`: En red local Wi-Fi, tolera hasta 700ms de fluctuación natural sin forzar saltos bruscos.
  - `DriftOnlineMs = 1200L`: En redes móviles a través de internet, tolera hasta 1200ms para evitar micro-cortes auditivos por jitter de antena.
- **Hash de Cola (`queueHash`)**:
  - Calcula un hash compacto de la lista de IDs de canciones en cola (`queue.map { it.id }.hashCode()`).
  - Si el hash del invitado no coincide con el del anfitrión, solicita automáticamente una resincronización completa de la cola (`QueueSyncMessage`).

**Archivos fuente clave:**
- [`together/TogetherPlaybackSync.kt:L5-47`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherPlaybackSync.kt#L5-L47)

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene el defecto más molesto del audio colaborativo: micro-seeks constantes que interrumpen el compás de la música ante pequeñas variaciones de ping.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Lógica de sincronización en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los umbrales de 150ms, 700ms y 1200ms. Reducirlos arruinará la experiencia musical en redes celulares.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Funciones puras fácilmente testeables.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[06 - TogetherClock EWMA Offset]], [[08 - TogetherClient Guest WS]].

---

## 7. Guía rápida para una IA nueva
- Para evaluar si se debe saltar: `TogetherPlaybackSync.shouldSeekForDrift(localPos, hostPos, isLan, isPlaying)`.
