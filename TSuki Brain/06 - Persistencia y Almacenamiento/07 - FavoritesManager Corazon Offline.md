# 06.07 — FavoritesManager (Colección Local de Canciones con Corazón)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona las pistas favoritas almacenadas localmente en `data/local/FavoritesManager.kt`:
- Base de datos `tsuki_favorites.db` (tabla `favorites`).
- Ofrece comprobaciones instantáneas en memoria (`isFavorite(videoId): Boolean`).
- Emite `_favoritesVersion` (`StateFlow<Long>`) para que todos los iconos de corazón de la app se iluminen o apaguen de forma sincronizada.
- Incluye un `toggleMutex` (`Mutex`) que protege las operaciones de guardado/eliminado rápido contra condiciones de carrera causadas por dobles toques accidentales del usuario.

**Archivos fuente clave:**
- [`data/local/FavoritesManager.kt:L18-91`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/FavoritesManager.kt#L18-L91)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que el usuario pueda tener una colección de favoritos permanente y offline incluso sin iniciar sesión con cuenta de Google.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Gestión de favoritos en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El `toggleMutex`: previene desincronizaciones entre el estado visual y la base de datos ante clics repetidos.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Sincronización híbrida: si el usuario tiene activada la sincronización con YouTube Music, se envía además el Me Gusta a InnerTube.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[05 - MusicPlayerScreenV9 Orquestador 1466L]], [[18 - LibraryScreen y Likes Sincronizados]].

---

## 7. Guía rápida para una IA nueva
- Para alternar el favorito de una canción, invoca `FavoritesManager.getInstance(context).toggleFavorite(track)`.
