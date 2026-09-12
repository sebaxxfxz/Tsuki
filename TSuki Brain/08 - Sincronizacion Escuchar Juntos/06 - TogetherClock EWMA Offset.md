# 08.06 — TogetherClock (Estimación de Reloj y Filtro EWMA)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Calcula el desfase temporal (*Offset*) entre el reloj del invitado y el reloj del anfitrión utilizando un protocolo estilo NTP en `together/TogetherClock.kt`:
- Mide el tiempo de ida y vuelta de red (*RTT - Round Trip Time*):
  $$RTT = t_4 - t_1$$
- Calcula el offset instantáneo:
  $$Offset = rac{(t_2 - t_1) + (t_3 - t_4)}{2}$$
- **Filtro Adaptativo EWMA (*Exponential Weighted Moving Average*)**:
  - Amortigua el ruido y jitter de la red móvil:
    - Peso de RTT: `rttWeight = 0.15`.
    - Peso de Offset: `offsetWeight = if (rawOffset.absoluteValue > 1500) 0.6 else 0.2`.
- Permite proyectar en qué milisegundo exacto de la canción está el anfitrión en este instante.

**Archivos fuente clave:**
- [`together/TogetherClock.kt:L12-31`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherClock.kt#L12-L31)

---

## 2. PARA QUÉ existe (problema que resuelve)
Dos teléfonos Android nunca tienen su reloj del sistema sincronizado al mismo milisegundo. Sin este cálculo, la sincronización de audio tendría errores constantes de varios segundos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Sincronización temporal en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El uso de `SystemClock.elapsedRealtime()`: nunca usar `System.currentTimeMillis()` porque se ve afectado por cambios manuales de hora o sincronizaciones de red celular.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Actualizado continuamente con cada frame de pulso recibido.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Algoritmo de Sincronizacion y Drift]], [[12 - TogetherPlaybackSync Drift y QueueHash]].

---

## 7. Guía rápida para una IA nueva
- Para calcular el tiempo proyectado del host: `TogetherClock.estimateHostPosition(reportedPosition, reportedTime)`.
