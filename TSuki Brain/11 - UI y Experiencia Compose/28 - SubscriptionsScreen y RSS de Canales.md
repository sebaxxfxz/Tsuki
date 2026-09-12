# 11.28 — SubscriptionsScreen y RSS de Canales

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Feed cronológico de los últimos lanzamientos de los canales y artistas seguidos por el usuario (`ui/screens/SubscriptionsScreen.kt`):
- **Lanzamientos Cronológicos Recientes**: Muestra videos, álbumes y canciones ordenados estrictamente por fecha de publicación.
- **Motor Ultrarrápido de Feeds RSS**:
  - Lee las actualizaciones a través de `ChannelRssClient` (`https://www.youtube.com/feeds/videos.xml?channel_id=...`).
  - Obtiene los estrenos en menos de 300ms sin necesidad de tokens de autenticación ni llamadas pesadas a la API de YouTube.
- **Filtros por Creador**: Carrusel horizontal superior con los avatares circulares de todos los canales suscritos para filtrar el feed por un artista individual.
- **Indicador de No Leídos / Nuevos**: Insignia tonal en las pistas lanzadas en las últimas 24 horas.

**Archivos fuente clave:**
- [`ui/screens/SubscriptionsScreen.kt:L1-360`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/SubscriptionsScreen.kt#L1-L360)
- [`network/ChannelRssClient.kt:L1-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/ChannelRssClient.kt#L1-L140)
- [`data/local/TSukiSubscriptionRepository.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/TSukiSubscriptionRepository.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que el usuario nunca se pierda un estreno de sus artistas favoritos, proporcionando un muro limpio y sin algoritmos de distracción.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como destino de la pestaña de suscripciones.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Formato del Repositorio de Suscripciones**: El almacenamiento de canales en DataStore utiliza un formato delimitado por barras verticales `|` con escape estricto de caracteres `%7C`. No alterar la función de serialización/deserialización para no corromper la lista de creadores guardados.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Soporte para refresco mediante arrastre (`PullToRefreshBox`) que consulta en paralelo los canales seguidos mediante `async/awaitAll`.

---

## 6. Flujo y conexiones
- Depende de: `network/ChannelRssClient.kt` y `data/local/TSukiSubscriptionRepository.kt`.
- Enlaza hacia: [[22 - ChannelScreen y Exploracion de Artistas]].

---

## 7. Guía rápida para una IA nueva
- Para consultar los últimos estrenos de un canal, llama a `ChannelRssClient.fetchChannelFeed(channelId)`.
