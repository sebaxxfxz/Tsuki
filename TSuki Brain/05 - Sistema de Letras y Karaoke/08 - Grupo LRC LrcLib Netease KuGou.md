# 05.08 — Grupo LRC de Sincronización por Línea (LrcLib, NetEase y KuGou)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Grupo de proveedores comunitarios especializados en formato LRC estándar sincronizado por línea:
- **`LrcLibLyricsProvider`**: Consulta la base de datos libre y abierta LrcLib (`lrclib.net/api/get`). Ofrece excelente cobertura para música occidental, rock e indie.
- **`KuGouLyricsProvider`**: Consulta la base de datos asiática KuGou. Es el proveedor de mayor tasa de acierto para música en japonés, anime, K-Pop y bandas sonoras orientales.
- **`NeteaseLyricsProvider`**: Consulta el servicio NetEase Cloud Music, líder en música asiática e internacional alternativa.
- Si devuelven líneas sincronizadas (`[mm:ss.xx]`), `LyricsHelper` guarda el resultado como "candidata de línea" y continúa buscando una versión palabra por palabra antes de rendirse.

**Archivos fuente clave:**
- [`lyrics/providers/LrcLibLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/LrcLibLyricsProvider.kt)
- [`lyrics/providers/KuGouLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/KuGouLyricsProvider.kt)
- [`lyrics/providers/NeteaseLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/NeteaseLyricsProvider.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Asegura que canciones que no disponen de letras oficiales de Apple Music o Spotify (canciones independientes, anime, música en vivo) cuenten al menos con letras sincronizadas por línea con auto-scroll.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Proveedores en `lyrics/providers/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El regex `LINE_REGEX` en `LyricsUtils.kt`: maneja líneas con múltiples marcas de tiempo agrupadas.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Peticiones con timeout de 4000ms para no demorar la cascada global.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Orquestador y 16 Proveedores]], [[02 - Parsers TTML y Enhanced LRC]].

---

## 7. Guía rápida para una IA nueva
- Si buscas música oriental y no aparece letra, verifica que `KuGouLyricsProvider` no esté bloqueado por red regional.
