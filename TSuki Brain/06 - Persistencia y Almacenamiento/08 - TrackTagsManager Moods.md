# 06.08 — TrackTagsManager (Etiquetas de Estado de Ánimo)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Permite asociar estados de ánimo (*moods*) y etiquetas arbitrarias a canciones en `data/local/TrackTagsManager.kt`:
- Base de datos `tsuki_tags.db` (tabla `track_tags`).
- Clave primaria compuesta: `(video_id, mood)`.
- Normaliza las etiquetas a minúsculas (`mood.trim().lowercase()`) para evitar duplicados como "Chill" y "chill".
- Permite generar dinámicamente listas de reproducción filtradas por un estado de ánimo seleccionado.

**Archivos fuente clave:**
- [`data/local/TrackTagsManager.kt:L21-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/TrackTagsManager.kt#L21-L140)
- [`ui/components/TagSongSheet.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/TagSongSheet.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite al usuario clasificar su música por la energía o vibra que le transmite para escuchar fácilmente mezclas adaptadas a su momento del día.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Metadatos personalizados en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La normalización en minúsculas en inserción y consulta.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Sheet interactivo `TagSongSheet` accesible desde los menús de tres puntos de cada canción.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[35 - Componentes Core (MiniPlayer, TSukiPillNavBar, FastScrollBox, Sheets)]], [[18 - LibraryScreen y Likes Sincronizados]].

---

## 7. Guía rápida para una IA nueva
- Para obtener todas las canciones con un tag específico, usa `TrackTagsManager.getInstance(context).getTracksForMood(mood)`.
