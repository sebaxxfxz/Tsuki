# 07.10 — Feed de Suscripciones y Worker Periódico en Segundo Plano

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Coordina la sincronización de novedades de los artistas suscritos:
1. **`TSukiSubscriptionFeedRepository.kt`**:
   - Agrega los canales suscritos de `TSukiSubscriptionRepository`.
   - Consulta concurrentemente los feeds RSS mediante `ChannelRssClient` con caché TTL de 5 minutos.
   - Ordena cronológicamente los últimos lanzamientos musicales.
2. **`NewVideosWorker.kt` (Android WorkManager)**:
   - Tarea periódica en segundo plano programada cada 6 horas (`PeriodicWorkRequestBuilder(6, TimeUnit.HOURS)`).
   - Requiere restricción de red conectada (`NetworkType.CONNECTED`).
   - Si detecta videos subidos en las últimas horas que el usuario no ha visto, emite una notificación nativa en el canal `tsuki_new_videos`.

**Archivos fuente clave:**
- [`data/subscriptions/TSukiSubscriptionFeedRepository.kt:L17-74`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/subscriptions/TSukiSubscriptionFeedRepository.kt#L17-L74)
- [`data/subscriptions/NewVideosWorker.kt:L27-104`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/subscriptions/NewVideosWorker.kt#L27-L104)

---

## 2. PARA QUÉ existe (problema que resuelve)
Mantiene al usuario informado sobre nuevos temas y videoclips de sus bandas favoritas sin requerir cuenta de Google ni mantener la app abierta.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Sincronización en segundo plano en `data/subscriptions/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El canal de notificación `tsuki_new_videos` con el icono `media3_notification_small_icon`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Encolamiento único mediante `WorkManager.enqueueUniquePeriodicWork` para no duplicar workers.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[12 - RSS Canales Sin Cuota]], [[28 - SubscriptionsScreen y RSS de Canales]].

---

## 7. Guía rápida para una IA nueva
- Para forzar la comprobación inmediata de nuevos videos, encola un `OneTimeWorkRequest` de `NewVideosWorker`.
