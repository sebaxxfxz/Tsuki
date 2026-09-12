# 11.22 — ChannelScreen y Exploración de Artistas

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Página de perfil detallado de artistas y canales musicales (`ui/screens/ChannelScreen.kt`):
- **Hero Banner con Imagen de Cabecera**: Cabecera visual inmersiva con imagen panorámica del artista, avatar circular y conteo de suscriptores.
- **Botón de Suscripción Reactivo**: Permite suscribirse al canal guardando el seguimiento localmente en `TSukiSubscriptionRepository` y activando la monitorización de lanzamientos RSS.
- **Secciones de Discografía Organizadas**:
  - Carrusel de Canciones Principales (Top Songs).
  - Carrusel de Álbumes de Estudio Oficiales.
  - Carrusel de Sencillos y EPs.
  - Videos Musicales oficiales y presentaciones en vivo.
  - Artistas Relacionados recomendados.

**Archivos fuente clave:**
- [`ui/screens/ChannelScreen.kt:L1-460`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/ChannelScreen.kt#L1-L460)
- [`network/TSukiInnerTubeClient.kt:L450-550`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L450-L550)
- [`data/local/TSukiSubscriptionRepository.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/TSukiSubscriptionRepository.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite profundizar en la obra completa de un músico o creador, descubrir su catálogo íntegro y recibir alertas de sus nuevos estrenos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como destino de navegación al tocar el nombre de un artista en el reproductor o en las listas.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **ID de Canal de YouTube**: El parámetro `channelId` debe comenzar por `UC` (ej. `UC_x5XG1OV2P6uZZ5FSM9Ttw`). Si se pasa un identificador de búsqueda o vanity URL, debe resolverse primero a su `channelId` canónico.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Peticiones a InnerTube mediante `browse(channelId)` extrayendo `musicShelfRenderer` y `musicCarouselShelfRenderer`.

---

## 6. Flujo y conexiones
- Sincronizado con: [[28 - SubscriptionsScreen y RSS de Canales]], [[01 - TSukiInnerTubeClient y Autenticacion|04.01 - TSukiInnerTubeClient]].

---

## 7. Guía rápida para una IA nueva
- Para verificar si el usuario sigue al artista actual, consulta `TSukiSubscriptionRepository.isSubscribed(channelId)`.
