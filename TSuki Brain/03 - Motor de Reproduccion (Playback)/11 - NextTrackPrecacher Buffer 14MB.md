# 03.11 — NextTrackPrecacher (Precarga de Buffer 14MB)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Descarga de forma preventiva los primeros megabytes del siguiente tema musical de la cola en `PlayerCacheProvider`:
- `MAX_PRECACHE_BYTES = 14L * 1024L * 1024L` (14 MB).
- `MIN_USEFUL_BYTES = 512L * 1024L` (512 KB).
- Resuelve la URL del próximo track en segundo plano y abre un `CacheWriter` sobre `SimpleCache`.
- Al saltar a la siguiente pista, ExoPlayer encuentra los primeros segundos ya presentes en disco e inicia la reproducción en menos de 50ms sin esperar respuesta de red.

**Archivos fuente clave:**
- [`playback/NextTrackPrecacher.kt:L14-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/NextTrackPrecacher.kt#L14-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
Elimina por completo el tiempo de buffering percibido por el usuario entre canciones.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Optimización del pipeline de reproducción en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El formato de clave de caché: `val key = "${track.id}_$qualityLabel"`. Debe concordar byte a byte con la clave generada por ExoPlayer en `TSukiPlaybackService.kt`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Se ejecuta en `Dispatchers.IO` con prioridad reducida para no competir con el stream actual.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[06 - Cache de Streaming y Descargas]], [[17 - PlayerCacheProvider SimpleCache]].

---

## 7. Guía rápida para una IA nueva
- `NextTrackPrecacher` se dispara automáticamente desde `PlayerController` al transcurrir el 30% de la pista actual.
