# 05.09 — Grupo Fallback: Subtítulos y Transcripciones de YouTube

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Último recurso de rescate cuando ningún proveedor externo tiene la letra:
1. **`YouTubeSubtitleLyricsProvider`**:
   - Extrae los subtítulos cerrados (*Closed Captions / Transcripts*) del video de YouTube mediante NewPipeExtractor o InnerTube.
   - Analiza los bloques temporales de subtítulos automáticos o manuales y los convierte dinámicamente en formato lírico sincronizado por línea.
2. **`YouTubeLyricsProvider`**:
   - Consulta la pestaña oficial "Letras" de YouTube Music a través de `/youtubei/v1/browse` con el token `browseId` de lyrics.
   - Provee el texto oficial de la canción (habitualmente texto plano no sincronizado provisto por LyricFind / Musixmatch).

**Archivos fuente clave:**
- [`lyrics/providers/YouTubeSubtitleLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/YouTubeSubtitleLyricsProvider.kt)
- [`lyrics/providers/YouTubeLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/YouTubeLyricsProvider.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que incluso canciones extremadamente raras, maquetas, remixes de aficionados o covers de YouTube muestren la letra en pantalla en lugar de un mensaje de "Letra no encontrada".

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Proveedores de contingencia en `lyrics/providers/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Su posición final en la cadena (posiciones 15 y 16): nunca deben ejecutarse antes que los proveedores especializados porque sus transcripciones suelen carecer de saltos de estrofa o marcas silábicas.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Extracción transparente mediante el `videoId` sin requerir búsqueda por título.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Orquestador y 16 Proveedores]], [[01 - TSukiInnerTubeClient y Autenticacion]].

---

## 7. Guía rápida para una IA nueva
- Si una canción de YouTube tiene subtítulos en el reproductor de video pero no en letras, revisa el parser de `YouTubeSubtitleLyricsProvider`.
