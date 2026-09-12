# 05.07 — Grupo Word-Sync (Paxsenix: Apple Music, Spotify y Musixmatch)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Grupo prioritario de proveedores en `lyrics/providers/PaxsenixSourceLyricsProviders.kt`:
- **Fuentes Soportadas**:
  - `PaxsenixAppleMusicLyricsProvider`: Extrae letras oficiales con formato TTML de Apple Music.
  - `PaxsenixSpotifyLyricsProvider`: Extrae letras oficiales de Musixmatch servidas a través del backend de Spotify.
  - `PaxsenixMusixmatchLyricsProvider`: Consulta directa a la API de Musixmatch.
  - `PaxsenixNeteaseLyricsProvider`: Letras asiáticas con temporización silábica.
- **Jerarquía de Formatos**:
  - Evalúa la respuesta en orden estricto: `richSync` > `ttml` > `lrcWordByWord` > `lrc` > `synced`.
  - Si encuentra TTML o marcas silábicas, activa de inmediato la vista de karaoke palabra por palabra (`KaraokeWordByWordView`).

**Archivos fuente clave:**
- [`lyrics/providers/PaxsenixSourceLyricsProviders.kt:L18-75`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/PaxsenixSourceLyricsProviders.kt#L18-L75)
- [`lyrics/LyricsUtils.kt:L65-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/LyricsUtils.kt#L65-L120)

---

## 2. PARA QUÉ existe (problema que resuelve)
Son la principal fuente de letras con precisión milimétrica por sílaba. Sin este grupo, TSuki solo podría ofrecer sincronización por línea completa, perdiendo la experiencia distintiva de karaoke estilo Apple Music.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Integraciones de red de proveedores específicos en `lyrics/providers/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El orden de prioridad en `baseProviders`: este grupo DEBE ejecutarse primero para asegurar la captura de TTML antes que alternativas de texto plano.
- La herencia de tiempos en `parseTtml`: si un `<span begin="...">` no declara `end`, se infiere como el inicio del span siguiente o `start + 4000L`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Respuestas HTTP parseadas con `org.json.JSONObject` nativo de Android para máxima velocidad.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Orquestador y 16 Proveedores]], [[02 - Parsers TTML y Enhanced LRC]].

---

## 7. Guía rápida para una IA nueva
- Para forzar la prueba de letras de Apple Music, selecciona "Paxsenix Apple Music" en `SettingsScreen > Letras > Proveedor preferido`.
