# 05.06 — LyricsDatabase (Caché Local SQLite de 1,500 Letras)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Base de datos SQLite persistente (`lyrics.db`) dedicada exclusivamente al almacenamiento en disco de letras descargadas:
- **Tabla `lyrics`**:
  - `video_id` (TEXT PRIMARY KEY): Identificador de 11 caracteres de YouTube.
  - `raw_lyrics` (TEXT): Contenido completo de la letra en formato TTML enriquecido o LRC estándar.
  - `has_word_sync` (INTEGER): Booleano (1/0) que indica si contiene marcas silábicas para karaoke.
  - `cached_timestamp` (INTEGER): Timestamp epoch de la última consulta o inserción.
- **Auto-Poda LRU (*Auto-Pruning*)**:
  - Limita el tamaño de la base de datos a los **1,500 registros más recientes**:
    ```sql
    DELETE FROM lyrics WHERE video_id NOT IN (
        SELECT video_id FROM lyrics ORDER BY cached_timestamp DESC LIMIT 1500
    )
    ```
- **Sentinel de Fallo**: Si ningún proveedor tiene letra para una pista, almacena el centinela `LYRICS_NOT_FOUND` para no reintentar inútilmente durante 24 horas.

**Archivos fuente clave:**
- [`data/local/LyricsDatabase.kt:L8-83`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/LyricsDatabase.kt#L8-L83)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite que las letras funcionen en modo offline para canciones previamente reproducidas y ahorra millones de peticiones HTTP innecesarias a APIs comunitarias.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Persistencia de la capa de letras en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La consulta de auto-poda: previene que la base de datos crezca indefinidamente tras meses de uso continuo.
- El almacenamiento del centinela `LYRICS_NOT_FOUND`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Consultas protegidas en `Dispatchers.IO` mediante métodos suspendidos.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Arquitectura SQLite Sin Room]], [[01 - Orquestador y 16 Proveedores]].

---

## 7. Guía rápida para una IA nueva
- Para consultar la letra guardada, usa `LyricsDatabase.getInstance(context).getLyrics(videoId)`.
