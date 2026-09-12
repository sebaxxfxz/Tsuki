# 04.07 — Búsqueda searchMusic y Filtros Protobuf

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa el motor de búsqueda en el catálogo de YouTube Music:
- **Llamadas a `/youtubei/v1/search`**:
  - Parámetro `query`: Texto de búsqueda introducido por el usuario.
  - Parámetro `params`: Tokens binarios codificados en Base64 URL-safe requeridos por InnerTube para filtrar por categorías:
    - Canciones (`SONGS`): `EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D`
    - Álbumes (`ALBUMS`): `EgWKAQIBAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D`
    - Artistas (`ARTISTS`): `EgWKAQIgAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D`
    - Playlists (`PLAYLISTS`): `EgWKAQIoAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D`
    - Todos (`ALL`): Sin parámetro `params`.
- **Sugerencias de Búsqueda Instantáneas**: Consulta el endpoint de autocompletado en tiempo real mientras el usuario escribe.

**Archivos fuente clave:**
- [`network/TSukiInnerTubeClient.kt:L130-220`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L130-L220)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite realizar búsquedas segmentadas por categoría exacta, evitando que búsquedas de canciones devuelvan videos de fans o podcasts irrelevantes.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Extracción de datos de búsqueda en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los valores Base64 de `params`. Son estructuras Protobuf rígidas compiladas por Google.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Mapeo directo a objetos `MediaTrack` listos para ser consumidos por Compose.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[16 - MusicScreen y Pestanas Virtuales]], [[02 - Resolucion y Matching Difuso]].

---

## 7. Guía rápida para una IA nueva
- Para buscar canciones específicamente, invoca `TSukiInnerTubeClient.searchMusic(query, MusicSearchFilter.SONGS)`.
