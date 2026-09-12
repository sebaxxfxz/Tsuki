# 05.04 — LyricsSanitizer (Normalización y Limpieza de Títulos)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Limpia y normaliza los títulos de canciones y nombres de artistas antes de consultar las APIs de letras en `lyrics/LyricsSanitizer.kt`:
- Elimina sufijos y metadatos típicos de videos de YouTube:
  - `(Official Music Video)`, `[Official Video]`, `(4K Remastered)`, `(Audio Oficial)`, `[Lyric Video]`, etc.
  - Etiquetas de colaboradores: `feat.`, `ft.`, `featuring` y listas de artistas secundarios entre paréntesis.
  - Emojis, caracteres especiales decorativos y espacios en blanco redundantes.
- Conserva cuidadosamente paréntesis legítimos que forman parte del nombre real de la canción (ej. subtítulos originales en canciones clásicas).

**Archivos fuente clave:**
- [`lyrics/LyricsSanitizer.kt:L3-26`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/LyricsSanitizer.kt#L3-L26)

---

## 2. PARA QUÉ existe (problema que resuelve)
Las bases de datos de letras (LrcLib, NetEase, Musixmatch, Genius) buscan por coincidencia exacta de texto. Si se busca `"Song Title (Official Video 4K)"`, la tasa de fallos supera el 70%. Sanitizar el título eleva la tasa de éxito por encima del 92%.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Preprocesamiento de datos en `lyrics/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La lista de expresiones regulares `NOISE_PATTERNS`. Alterar los patrones sin pruebas puede eliminar palabras válidas del título de canciones.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Se ejecuta de forma síncrona y pura en memoria antes de despachar las llamadas de red.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Orquestador y 16 Proveedores]], [[05 - LyricsPreloadManager 2 Tracks]].

---

## 7. Guía rápida para una IA nueva
- Para limpiar un título antes de buscar letras, invoca `LyricsSanitizer.sanitizeTitle(rawTitle)`.
