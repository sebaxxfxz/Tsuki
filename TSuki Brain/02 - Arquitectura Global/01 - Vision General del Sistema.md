# 01 - Visión General de Arquitectura

> TSuki está diseñada bajo una arquitectura modular y pragmática, orientada a eventos unidireccionales (UDF - *Unidirectional Data Flow*), con un desacoplamiento estricto entre la interfaz de usuario de Jetpack Compose, el subsistema de reproducción Media3 en segundo plano, el cliente de red resiliente y la persistencia local en SQLite.

---

## 🏗️ Capas de la Aplicación

```mermaid
graph TB
    subgraph UI_PRESENTATION["1. Capa de Presentación (Jetpack Compose)"]
        direction TB
        Act[MainActivity] --> Nav[PillNavBar / PlayerBottomSheet]
        Nav --> ScrHome[MusicScreen & HomeScreen]
        Nav --> ScrLib[LibraryScreen & PlaylistDetail]
        Nav --> ScrPlayer[MusicPlayerScreenV9]
        Nav --> ScrRec[RecognitionScreen]
        Nav --> ScrTog[TogetherScreen]
    end

    subgraph CONTROLLER_ORCHESTRATOR["2. Orquestador Central (StateFlow UDF)"]
        direction TB
        PC[PlayerController
Singleton en Memoria]
        State[PlayerUiState
Immutable Data Class]
        Tick[PlaybackTick
Position / Duration Flow]
        PC --- State
        PC --- Tick
    end

    subgraph PLAYBACK_ENGINE["3. Motor de Reproducción (AndroidX Media3)"]
        direction TB
        Svc[TSukiPlaybackService
MediaLibraryService]
        Exo[ExoPlayer Principal
MergingMediaSource + 15s BackBuffer]
        CF[CrossfadeController
ExoPlayer Secundario + Potencias Iguales]
        DSP[AudioEqualizerHelper
Bands, BassBoost, Virtualizer]
        Svc --> Exo
        CF -.->|Handoff a| Exo
        Exo --> DSP
    end

    subgraph DATA_STORAGE["4. Datos, Caché y Persistencia"]
        direction TB
        DB_PL[LocalPlaylistManager
tsuki_playlists.db]
        DB_WH[WatchHistoryManager
tsuki_history.db]
        DB_LY[LyricsDatabase
lyrics.db]
        DB_TG[TrackTagsManager
tsuki_tags.db]
        PREFS[PlayerPreferences / HomePreferences
Jetpack DataStore]
        CACHE[PlayerCacheProvider
Disk LRU CacheDataSource]
    end

    subgraph NETWORK_INTEGRATION["5. Integración de Red y Servicios"]
        direction TB
        IT[TSukiInnerTubeClient
WEB_REMIX API + SAPISID]
        YE[YouTubeExtractor
NewPipe Core + Audio Streams]
        LH[LyricsHelper
16 Providers + TTML Parser]
        TG[TogetherServer / TogetherClient
Ktor WebSockets]
    end

    %% Conexiones entre capas
    UI_PRESENTATION <==>|Recolecta State / Emite Eventos| CONTROLLER_ORCHESTRATOR
    CONTROLLER_ORCHESTRATOR <==>|Comanda Sesión / Recibe Callbacks| PLAYBACK_ENGINE
    CONTROLLER_ORCHESTRATOR -->|Consulta Streams| YE
    CONTROLLER_ORCHESTRATOR -->|Solicita Letras| LH
    CONTROLLER_ORCHESTRATOR -->|Registra Escuchas| DB_WH
    
    UI_PRESENTATION -->|Carga Feeds y Búsquedas| IT
    UI_PRESENTATION -->|Consulta Playlists y Favoritos| DB_PL
    UI_PRESENTATION -->|Lee / Escribe Configuración| PREFS
    
    PLAYBACK_ENGINE -->|Almacena y Lee Fragmentos de Audio| CACHE
```

---

## 🔄 Flujo Unidireccional de Datos (UDF)

La arquitectura de reproducción de TSuki se fundamenta en la previsibilidad de estados inmutables:

1. **El Usuario Interactúa**: El usuario toca una pista en `MusicScreen` o pulsa reproducir en `MusicPlayerScreenV9`.
2. **Emisión de Intención**: La UI invoca un método de alto nivel en `PlayerController` (ej. `playQueue(tracks, startIndex)`).
3. **Resolución Asíncrona**:
   - `PlayerController` actualiza inmediatamente `_uiState` con `isBuffering = true` y los datos preliminares del track.
   - En paralelo, solicita la URL final de streaming a través de `YouTubeExtractor.getStreamUrlsDetailed(videoId)` (o lee la caché en memoria `urlCache`).
4. **Carga en el Servicio**: Con la URL obtenida, `PlayerController` crea un `MediaItem` inyectando metadatos en sus `extras` y lo despacha al `MediaController`.
5. **Buffer y Reproducción**: `TSukiPlaybackService` toma el `MediaItem`, lo canaliza por el `CacheDataSource` de `PlayerCacheProvider` y empieza la decodificación en `ExoPlayer`.
6. **Propagación del Estado**: Los listeners de ExoPlayer en el servicio informan de vuelta a `MediaController`. `PlayerController` escucha estos eventos y actualiza `PlayerUiState` (`isPlaying = true`, `isBuffering = false`).
7. **Recomposición Eficiente**: La UI observa `PlayerController.uiState` con `collectAsStateWithLifecycle()` y recombona únicamente las piezas visuales afectadas (carátula, botones, deslizador).

---

## 📦 Separación de Responsabilidades por Directorio

| Directorio | Propósito y Naturaleza |
| :--- | :--- |
| `ui/` | Pantallas puramente declarativas en Jetpack Compose, componentes reutilizables, temas de color y widgets Glance. No debe contener lógica de negocio ni llamadas directas de red bloqueantes. |
| `playback/` | El núcleo del audio: ciclo de vida de ExoPlayer, transiciones entre pistas, ecualización y servicio en primer plano. |
| `network/` | Clientes HTTP que interactúan con YouTube, YouTube Music, SponsorBlock y servicios de terceros. Serialización y deserialización JSON con `kotlinx.serialization`. |
| `lyrics/` | Gestión de proveedores de letras sincronizadas, parseo de formatos LRC y TTML, y resolución fonética silábica. |
| `data/` | Repositorios, persistencia en bases de datos SQLite nativas, DataStore de preferencias, exportación de backups y motor neuronal de recomendación local. |
| `domain/` | Clases de datos puras de dominio (`MediaTrack`, `LyricsEntry`) libres de dependencias de Android framework. |
| `together/` | Módulo de sincronización en tiempo real "Escuchar juntos", servidor HTTP/WebSocket CIO embebido en el dispositivo y sincronización de reloj. |
| `shazam/` | Implementación nativa del algoritmo de reconocimiento musical acústico mediante transformada rápida de Fourier (FFT) y picos de espectrograma. |
| `playlistimport/`| Parsers de listas de reproducción de Spotify, archivos M3U, CSV y backups ZIP de ArchiveTune con algoritmo de coincidencia difusa. |
