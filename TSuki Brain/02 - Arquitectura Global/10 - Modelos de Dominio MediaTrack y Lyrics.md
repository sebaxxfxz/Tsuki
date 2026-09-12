# 02.10 — Modelos de Dominio MediaTrack y Lyrics

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define las entidades centrales del dominio musical en `domain/model/`:
- **`MediaTrack`**:
  - `id`: String único.
  - `title`: Título de la pista.
  - `artist`: Nombre del artista o canal.
  - `album`: Nombre del álbum o playlist (opcional).
  - `durationMs`: Duración en milisegundos.
  - `streamUrl`: URL resuelta de streaming o ruta local (`file://` o `content://`).
  - `artworkUrl`: URL de la carátula en alta resolución.
  - `isLocal`: Boolean que indica si es un archivo físico en el almacenamiento.
  - `mediaType`: `MediaType.STREAM_AUDIO`, `STREAM_VIDEO` o `LOCAL_AUDIO`.
  - `isVideoItem`: Indica si la pista debe reproducirse con visor de video por defecto.
- **`LyricsEntry`**:
  - `timestampMs`: Marca de tiempo de la línea completa en milisegundos.
  - `text`: Texto de la línea.
  - `words`: Lista opcional de `WordTimestamp` para karaoke silábico palabra por palabra.
- **`WordTimestamp`**:
  - `word`: Texto de la palabra o sílaba individual.
  - `startSeconds`: Tiempo de inicio en segundos (Float).
  - `endSeconds`: Tiempo de fin en segundos (Float).

---

## 2. PARA QUÉ existe (problema que resuelve)
Establece un contrato inmutable común para todas las capas. Garantiza que la UI, el servicio de audio y la base de datos hablen exactamente el mismo idioma sin acoplamiento a librerías externas.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Capa de Dominio puro (`domain/model/`), libre de dependencias de Android framework.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No mutar las propiedades de `MediaTrack`. Si se necesita modificar un track, usar `.copy(...)`.
- `WordTimestamp.startSeconds` DEBE mantenerse en segundos (`Float`) para compatibilidad con `lyrics-ui`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Entidades inmutables y serializables para intercambio de datos sin efectos secundarios.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Glosario Ubicuo y Mapa de Dependencias]], [[01 - PlayerController Central]], [[02 - Parsers TTML y Enhanced LRC]].

---

## 7. Guía rápida para una IA nueva
- Toda nueva característica que represente contenido musical debe construirse sobre `MediaTrack`.
