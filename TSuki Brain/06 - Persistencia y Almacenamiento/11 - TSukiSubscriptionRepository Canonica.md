# 06.11 — TSukiSubscriptionRepository (Suscripciones Canónicas)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona los canales a los que el usuario se ha suscrito localmente sin necesidad de cuenta de Google:
- Persistencia en DataStore (`tsuki_subscriptions`).
- Modelo `SubscriptionItem`: `channelId`, `channelName`, `avatarUrl`, `subscriberCount`, `subscribedAt`.
- **Formato de Serialización con Delimitador**:
  - Serializa los campos en cadenas separadas por el carácter tubería `|`.
  - Escapa preventivamente cualquier carácter `|` presente en el nombre del canal convirtiéndolo a `%7C`.
- Expone un `Flow<List<SubscriptionItem>>` reactivo consumido por `SubscriptionsScreen`.

**Archivos fuente clave:**
- [`data/local/TSukiSubscriptionRepository.kt:L15-107`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/TSukiSubscriptionRepository.kt#L15-L107)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a los usuarios seguir canales de artistas y creadores de contenido de forma 100% privada sin vincular su cuenta personal de Google.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Repositorio de suscripciones en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El delimitador `|` y su escape `%7C`. Alterarlo corromperá la lectura de todas las suscripciones previas guardadas en el DataStore del usuario.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Operaciones `subscribe`, `unsubscribe` y `isSubscribed` totalmente reactivas.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[28 - SubscriptionsScreen y RSS de Canales]], [[12 - TSukiBackupRepository NewPipe-CSV-Master]].

---

## 7. Guía rápida para una IA nueva
- Para consultar si un canal está suscrito, observa `repository.isSubscribed(channelId)`.
