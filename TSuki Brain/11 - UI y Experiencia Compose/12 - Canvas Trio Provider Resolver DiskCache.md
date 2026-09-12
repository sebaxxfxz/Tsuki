# 11.12 — Canvas Trio Provider Resolver DiskCache

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pipeline completo para la resolución, descarga y almacenamiento en caché de videos animados de fondo (Spotify Canvas) para las canciones en reproducción:
- **`CanvasProvider.kt`**: Cliente HTTP ligero que consulta servicios puente (mirrors de Canvas) utilizando el código ISRC o el título y artista de la pista.
- **`CanvasResolver.kt`**: Orquestador que decide si una canción tiene Canvas disponible, coordina la consulta de red y emite el estado reactivo (`Idle`, `Loading`, `Available(uri)`, `Unavailable`).
- **`CanvasDiskCache.kt`**: Gestor de almacenamiento en disco con política LRU que guarda los clips de video en formato MP4 en el directorio de caché (`cacheDir/canvas_cache/`), evitando descargas repetitivas y permitiendo reproducción instantánea offline.
- **`CanvasArtworkPlayer.kt`**: Vista de Compose que renderiza el video MP4 en bucle infinito con escalado vertical centrado y volumen cero.

**Archivos fuente clave:**
- [`ui/player/canvas/CanvasProvider.kt:L1-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/canvas/CanvasProvider.kt#L1-L120)
- [`ui/player/canvas/CanvasResolver.kt:L1-150`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/canvas/CanvasResolver.kt#L1-L150)
- [`ui/player/canvas/CanvasDiskCache.kt:L1-110`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/canvas/CanvasDiskCache.kt#L1-L110)
- [`ui/player/CanvasArtworkPlayer.kt:L1-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/CanvasArtworkPlayer.kt#L1-L180)

---

## 2. PARA QUÉ existe (problema que resuelve)
Dota a TSuki de la experiencia visual moderna de videos en bucle característica de Spotify, sin acoplarse directamente a librerías propietarias y con soporte robusto de caché offline.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/player/canvas/` manteniendo aislada la lógica de resolución y caché multimedia del Canvas.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Cero Pistas de Audio en el Reproductor de Canvas**: El reproductor de Canvas debe silenciar completamente su salida de audio (`player.volume = 0f`). Si no se silencia, se producirá un conflicto de enfoque de audio (Audio Focus) con `ExoPlayer` principal, pausando la música.
- **Límite de Espacio en Caché**: `CanvasDiskCache` tiene un límite de 200MB con eliminación automática de los archivos más antiguos.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Descarga en segundo plano mediante `Dispatchers.IO` y comprobación de conectividad antes de iniciar la solicitud de video.

---

## 6. Flujo y conexiones
- Consumido por: [[07 - ArtworkPagerV9 Gestos y Canvas]] dentro de [[05 - MusicPlayerScreenV9 Orquestador 1466L]].

---

## 7. Guía rápida para una IA nueva
- Para habilitar o deshabilitar Canvas según las preferencias del usuario, consulta el valor de `showCanvas` en `PlayerPreferences`.
