# 05.05 — LyricsPreloadManager (Precarga de Letras)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona la descarga anticipada de letras en segundo plano:
- Inspecciona la cola de reproducción activa (`queue`) y toma las siguientes **2 canciones** a partir del índice actual.
- Verifica primero en la base de datos local `LyricsDatabase` si la letra ya está guardada.
- Si no existe en la base de datos local, lanza una corrutina en `Dispatchers.IO` para consultar la cascada de `LyricsHelper` y guardar el resultado en disco.
- Cuando el usuario avanza de canción y pulsa la pestaña de letras, el texto y las marcas de sincronización ya están disponibles de inmediato en memoria.

**Archivos fuente clave:**
- [`lyrics/LyricsPreloadManager.kt:L16-49`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/LyricsPreloadManager.kt#L16-L49)
- [`playback/PlayerController.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Elimina los círculos de carga (*spinners*) y el retraso de varios segundos al abrir el panel de letras durante la reproducción.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Optimizador de carga de la capa de letras en `lyrics/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La verificación previa en base de datos: `if (db.getLyrics(videoId) != null) continue`. Evita saturar las APIs de letras con solicitudes innecesarias.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Se cancela y reconfigura limpiamente cada vez que el usuario reordena la cola o salta a una pista arbitraria.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Orquestador y 16 Proveedores]], [[06 - LyricsDatabase Cache SQLite 1500]].

---

## 7. Guía rápida para una IA nueva
- Para activar la precarga ante un cambio de cola, llama a `LyricsPreloadManager.preloadUpcoming(queue, currentIndex)`.
