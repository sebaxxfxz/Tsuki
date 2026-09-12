# 03.17 — PlayerCacheProvider (Instancia Única SimpleCache)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Provee el singleton de `SimpleCache` para ExoPlayer:
- Directorio de almacenamiento: `context.cacheDir.resolve("exoplayer")`.
- Proveedor de base de datos: `StandaloneDatabaseProvider(context)`.
- Expulsor LRU: `LeastRecentlyUsedCacheEvictor(cacheSizeBytes)`.
- Si el tamaño configurado en ajustes es menor o igual a 0, utiliza `NoOpCacheEvictor`.
- Ofrece métodos para consultar espacio ocupado (`usedBytes`) y vaciar el contenido de la caché (`clear()`).

**Archivos fuente clave:**
- [`playback/PlayerCacheProvider.kt:L15-52`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerCacheProvider.kt#L15-L52)

---

## 2. PARA QUÉ existe (problema que resuelve)
Centraliza el cacheo en disco para que múltiples componentes (`TSukiPlaybackService`, `CrossfadeController`, `NextTrackPrecacher`) compartan el mismo almacenamiento sin colisiones de base de datos SQLite.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Proveedor de infraestructura de almacenamiento para el reproductor en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El bloqueo `synchronized(this)` de doble verificación.
- Usar siempre `context.applicationContext` para inicializar el singleton.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Tamaño gestionado a través de `PlayerPreferences.cacheSizeMb`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[06 - Cache de Streaming y Descargas]], [[11 - NextTrackPrecacher Buffer 14MB]].

---

## 7. Guía rápida para una IA nueva
- Para obtener la caché, llama a `PlayerCacheProvider.get(context)`.
