# 04.08 — Feed Personalizado y Secciones de Home

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Extrae y estructura la pantalla de inicio de música desde YouTube Music (`/youtubei/v1/browse` con `browseId = "FEmusic_home"`):
- **Extracción de Carruseles**: Analiza `sectionListRenderer` y extrae estantes horizontales (`musicCarouselShelfRenderer`): "Escuchar de nuevo", "Novedades para ti", "Éxitos del momento", etc.
- **Chips de Estado de Ánimo (*Mood Chips*)**: Extrae los botones de filtro superior ("Energía", "Relax", "Fiesta", "Entrenamiento", "Concentración") y sus tokens de navegación para filtrar la pantalla.
- **Caché Híbrida en Disco y RAM (`MusicHomeMemory`)**:
  - Almacena las secciones parseadas en RAM y en un archivo JSON local `tsuki_music_personalized.json`.
  - Aplica un tiempo de vida (TTL) de 6 horas.
  - Permite abrir la app instantáneamente sin esperar respuesta de red.

**Archivos fuente clave:**
- [`network/TSukiInnerTubeClient.kt:L230-360`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L230-L360)
- [`ui/screens/MusicScreen.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/MusicScreen.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Provee una pantalla de inicio rica y personalizada con arranque instantáneo gracias a la persistencia local.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Integración del feed de inicio en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El identificador `FEmusic_home`.
- La clave de caché `tsuki_music_personalized.json`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Refresco silencioso en segundo plano mientras se muestra la versión cacheada.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[16 - MusicScreen y Pestanas Virtuales]], [[09 - Playlist LM Likes Virtuales]].

---

## 7. Guía rápida para una IA nueva
- Para forzar actualización del feed, limpia la memoria de `MusicHomeMemory` y llama a `fetchHomePersonalized(bypassCache = true)`.
