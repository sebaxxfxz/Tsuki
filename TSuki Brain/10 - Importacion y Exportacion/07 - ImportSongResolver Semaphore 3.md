# 10.07 — ImportSongResolver (Concurrencia con Semáforo de 3 Permisos)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Coordina el proceso asíncrono de resolución masiva de canciones en `playlistimport/ImportSongResolver.kt`:
- **Semáforo de Concurrencia (`Semaphore(3)`)**:
  - Limita a un máximo de **3 peticiones simultáneas** de búsqueda en YouTube Music.
  - Previene que importar una playlist de 200 canciones dispare 200 llamadas HTTP paralelas que provocarían un baneo temporal por IP (HTTP 429 Too Many Requests) de YouTube.
- **Emisión Reactiva de Progreso**:
  - Emite el progreso canción a canción mediante un callback o flujo para actualizar la barra de progreso en `ImportPlaylistScreen`.
  - Agrupa las pistas resueltas en una nueva `LocalPlaylist` en `tsuki_playlists.db`.

**Archivos fuente clave:**
- [`playlistimport/ImportSongResolver.kt:L20-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/ImportSongResolver.kt#L20-L140)
- [`ui/screens/ImportPlaylistScreen.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/ImportPlaylistScreen.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Equilibra velocidad de importación con seguridad ante bloqueos de red de Google.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Coordinador de resolución en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El límite de 3 permisos en el semáforo. Subirlo a 10 o más provocará bloqueos inmediatos por captcha en redes domésticas.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Soporte para pausar y cancelar la importación si el usuario sale de la pantalla.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Resolucion y Matching Difuso]], [[23 - ImportPlaylistScreen y Asistente de Migracion]].

---

## 7. Guía rápida para una IA nueva
- Para resolver un lote de canciones: `ImportSongResolver.resolveBatch(songs, onProgress)`.
