# 01.05 — Contratos que Nunca se Rompen (Lista Roja)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Cataloga las firmas públicas, identificadores de red, esquemas de bases de datos y claves de serialización que son **contratos inmutables**. Modificar cualquiera de estos elementos provocará roturas catastróficas en runtime o corrupción de datos:
- **`TogetherProtocolVersion = 1`** (`together/TogetherMessages.kt:7`): No alterar sin migración de protocolo; desconectará a todos los clientes.
- **Discriminador JSON en Together** (`together/TogetherJson.kt:6`): `classDiscriminator = "type"`, `ignoreUnknownKeys = true`. Los nombres `@SerialName` de los mensajes (`play`, `pause`, `seek_to`, `room_state`, `client_hello`) son fijos.
- **Rutas de WebSocket y API de Together**:
  - Servidor LAN: puerto `42117`, ruta `/together`.
  - Cloud Relay: ruta `/v1/together/ws`.
  - Enlaces: prefijo `tsuki://together?` o formato compacto `host|port|sid|key`.
- **Claves de DataStore**:
  - `player_preferences`: `pref_audio_quality`, `pref_crossfade_enabled`, `pref_crossfade_duration`, `pref_playback_speed`, `pref_lyrics_provider`, etc.
  - `appearance_preferences`: `pref_dark_theme`, `pref_pure_black_oled`, `pref_material_you`.
- **Nombres de Bases de Datos SQLite**:
  - `tsuki_playlists.db` (tablas `local_playlists`, `local_playlist_songs`).
  - `tsuki_history.db` (tablas `watch_history`, `play_events`).
  - `lyrics.db` (tabla `lyrics`).
  - `tsuki_tags.db` (tabla `track_tags`).
  - `tsuki_recognition.db` (tabla `recognition_history`).
- **Contrato de Proveedor de Letras**:
  - `suspend fun getLyrics(videoId: String, title: String, artist: String, durationSeconds: Int): Result<String>` (`lyrics/LyricsProvider.kt`).
- **Constantes de Crossfade**:
  - `PREPARE_AHEAD_MS = 8000L`, `END_GUARD_MS = 300L`, `FRAME_MS = 50L`, `MAX_ALLOWED_DRIFT_MS = 75L`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza la compatibilidad retroactiva entre versiones de la app, previene la pérdida de datos del usuario y asegura la interoperabilidad en sesiones multi-dispositivo.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Catálogo normativo de contratos rígidos en `01 - Reglas y Zona de Peligro`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Ninguno de los identificadores, nombres de columnas ni serializadores listados en esta ficha.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Si se necesita agregar un nuevo campo a un mensaje o base de datos, hacerlo de forma aditiva con valores por defecto (`null` o default), nunca renombrando ni eliminando campos existentes.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Invariantes Intocables y Quirks Criticos]], [[03 - TogetherMessages Contrato v1]], [[02 - Esquema de Bases de Datos SQLite]].

---

## 7. Guía rápida para una IA nueva
- Antes de refactorizar un modelo serializable, verifica si figura en esta lista roja.
