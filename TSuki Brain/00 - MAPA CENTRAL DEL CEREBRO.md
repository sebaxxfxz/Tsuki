# 🗺️ MAPA CENTRAL DEL CEREBRO TSUKI (INDEX)

> **TSuki Brain** — Base de conocimiento canónica y memoria arquitectónica viva.
> Este repositorio contiene la documentación exhaustiva de cada componente, invariante, quirk y zona roja de la aplicación.
> Diseñado para que cualquier Inteligencia Artificial o ingeniero humano comprenda al 100% el sistema sin necesidad de inferir o adivinar.

---

## 📌 Índice Rápido de Secciones

- [🗺️ Mapa Central y Navegación](#00---mapa-central) `(5 documentos)`
- [🚨 Reglas Inmutables y Zona de Peligro](#01---reglas-y-zona-de-peligro) `(8 documentos)`
- [🏛️ Arquitectura Global del Sistema](#02---arquitectura-global) `(18 documentos)`
- [🎵 Motor de Reproducción y Audio (Playback)](#03---motor-de-reproduccion-playback) `(20 documentos)`
- [🌐 Red, Extracción y APIs (Network)](#04---red-y-extraccion-network) `(13 documentos)`
- [🎤 Sistema de Letras, TTML y Karaoke](#05---sistema-de-letras-y-karaoke) `(14 documentos)`
- [💾 Persistencia, SQLite y DataStore](#06---persistencia-y-almacenamiento) `(14 documentos)`
- [🧠 Motor Neuronal y Recomendación Local](#07---motor-neuronal-y-recomendacion) `(10 documentos)`
- [👥 Sincronización Social Escuchar Juntos](#08---sincronizacion-escuchar-juntos) `(13 documentos)`
- [⚡ Reconocimiento Acústico Shazam](#09---reconocimiento-acustico-shazam) `(6 documentos)`
- [📦 Importación y Migración de Playlists](#10---importacion-y-exportacion) `(9 documentos)`
- [🎨 UI, Jetpack Compose y Material 3](#11---ui-y-experiencia-compose) `(35 documentos)`
- [🛠️ Guía de Desarrollo, Toolchain y Depuración](#12---guia-de-desarrollo-y-comandos) `(7 documentos)`

**Total de fichas canónicas documentadas en el cerebro:** `172 documentos`.

---

## ⚡ Los 7 Invariantes Sagrados (Resumen Ejecutivo)

1. **Cero Comentarios en Código Kotlin (`app/src/`)**: Prohibido agregar `//` o `/** */`. La explicación arquitectónica vive exclusivamente en esta bóveda.
2. **Compilación de 2 Segundos**: Ejecuta `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin` tras cada edición para validar sintaxis y tipos.
3. **Contrato `M3WavySlider`**: El parámetro `value` **exige estrictamente una fracción `0.0f..1.0f`**. Nunca pasar milisegundos brutos.
4. **Progreso de MiniPlayer en Draw Phase**: `progressProvider = () -> Float` debe evaluarse dentro de `Canvas { ... }` para evitar recomposiciones que rompan los 60/120 FPS.
5. **Claves Compuestas en `LazyColumn`**: Usar `key = { index, item -> "${item.id}_#$index" }` para tolerar pistas duplicadas sin crashes.
6. **Captura PixelCopy en Diálogos**: Nunca usar `GraphicsLayer.toImageBitmap()` (produce imágenes negras). Usar `PixelCopy.request(window, ...)` sobre `DialogWindowProvider.window`.
7. **Desactivar Hardware Bitmaps para Paleta**: En Coil, configurar obligatoriamente `allowHardware(false)` al extraer paletas con `Palette` para evitar crashes nativos de GPU.

---

## <a id="00---mapa-central"></a>🗺️ Mapa Central y Navegación

- [[01 - Glosario Ubicuo y Mapa de Dependencias]]
- [[02 - Flujo de Datos End-to-End]]
- [[03 - Onboarding para IAs Nuevas]]
- [[04 - Entry Points y Grafo de Arranque]]
- [[05 - Threading Coroutines y Dispatchers]]

## <a id="01---reglas-y-zona-de-peligro"></a>🚨 Reglas Inmutables y Zona de Peligro

- [[01 - Invariantes Intocables y Quirks Criticos]]
- [[02 - Metodologia de Trabajo y Convenciones]]
- [[03 - Cero Comentarios en Kotlin]]
- [[04 - Ediciones Quirurgicas y Delegated-State]]
- [[05 - Contratos que Nunca se Rompen]]
- [[06 - Privacy-Safe y Prohibicion sp_dc TOTP]]
- [[07 - Checklist Pre-Commit y Verificacion]]
- [[08 - Bugs Ya Pagados Historial]]

## <a id="02---arquitectura-global"></a>🏛️ Arquitectura Global del Sistema

- [[01 - Vision General del Sistema]]
- [[02 - Diccionario Completo de Archivos]]
- [[03 - Capas UDF y Estado Reactivo]]
- [[03 - Capas y Flujo UDF]]
- [[04 - Inyeccion Manual y Singletons Sin Hilt]]
- [[04 - Singletons Manuales sin Hilt]]
- [[05 - Navegacion Pill y BottomSheet Player]]
- [[05 - Navegacion Pill y PlayerBottomSheet]]
- [[06 - Deep Links Intents y Compatibilidad]]
- [[06 - Deep-Links YouTube y Together]]
- [[07 - AndroidManifest Permisos y Servicios]]
- [[07 - Manifiesto Android y Servicios]]
- [[08 - Gradle AGP 9.3.1 Kotlin 2.2.10]]
- [[08 - Gradle Toolchain y Repos]]
- [[09 - Recursos XML y Diseno Visual]]
- [[09 - Recursos res y Drawables]]
- [[10 - Modelos de Dominio MediaTrack y Lyrics]]
- [[10 - Modelos de Dominio MediaTrack y LyricsEntry]]

## <a id="03---motor-de-reproduccion-playback"></a>🎵 Motor de Reproducción y Audio (Playback)

- [[01 - PlayerController Central]]
- [[02 - Dual-ExoPlayer Crossfade y Handoff]]
- [[03 - Servicio de Fondo y Media3 Session]]
- [[04 - AutoQueue y Radio Automix]]
- [[05 - Ecualizador DSP y Audio Effects]]
- [[06 - Cache de Streaming y Descargas]]
- [[07 - PlayerUiState y PlaybackTick]]
- [[08 - Politica de Reintentos y Generaciones]]
- [[09 - Operaciones de Cola Shuffle Repeat]]
- [[10 - Sleep Timer y Private Mode]]
- [[10 - SleepTimer y Modo Privado]]
- [[11 - NextTrackPrecacher 14MB]]
- [[11 - NextTrackPrecacher Buffer 14MB]]
- [[12 - YouTubeHttpDataSource Headers 403]]
- [[13 - TogetherManager Interceptores]]
- [[14 - AutoLibrarySessionCallback Android Auto]]
- [[15 - DownloadEngine Paralelo y Mux]]
- [[16 - CrossfadeHandoffPolicy Matematica]]
- [[17 - PlayerCacheProvider SimpleCache]]
- [[18 - Modos Audio-Video y Calidad]]

## <a id="04---red-y-extraccion-network"></a>🌐 Red, Extracción y APIs (Network)

- [[01 - TSukiInnerTubeClient y Autenticacion]]
- [[02 - YouTubeExtractor y Ciphers]]
- [[03 - SponsorBlock Dislikes y RSS]]
- [[04 - WEB_REMIX vs WEB Doble Cliente]]
- [[05 - SAPISIDHASH y Autenticacion]]
- [[06 - Paginacion Continuations 100-148 Items]]
- [[07 - Busqueda searchMusic y Filtros]]
- [[08 - Feed Personalizado y Home]]
- [[09 - Playlist LM Likes Virtuales]]
- [[10 - Comentarios con Fallback NewPipe]]
- [[11 - AudioQualityPolicy y MeteredNetwork]]
- [[12 - RSS Canales Sin Cuota]]
- [[13 - NewPipeDownloader y ConnectivityObserver]]

## <a id="05---sistema-de-letras-y-karaoke"></a>🎤 Sistema de Letras, TTML y Karaoke

- [[01 - Orquestador y 16 Proveedores]]
- [[02 - Parsers TTML y Enhanced LRC]]
- [[03 - Renderizado Visual y Smooth Position]]
- [[04 - LyricsSanitizer Ruido y Feat]]
- [[05 - LyricsPreloadManager 2 Tracks]]
- [[06 - LyricsDatabase Cache SQLite 1500]]
- [[06 - LyricsDatabase lyrics.db 1500]]
- [[07 - Grupo Word-Sync Paxsenix Apple Spotify]]
- [[08 - Grupo LRC LrcLib Netease KuGou]]
- [[09 - Grupo Fallback YouTube Transcripts]]
- [[10 - Grupo Agregadores BetterLyrics Unison YouLyPlus]]
- [[11 - Megalobiz Scraping Fragil]]
- [[12 - KaraokeWordByWord vs KaraokeLyricRow]]
- [[13 - LyricsPaneV9 y Volumen Sistema]]

## <a id="06---persistencia-y-almacenamiento"></a>💾 Persistencia, SQLite y DataStore

- [[01 - Arquitectura SQLite Sin Room]]
- [[02 - Esquema de Bases de Datos SQLite]]
- [[03 - DataStore de Preferencias y Estados]]
- [[04 - Sistema de Backup y Restauracion]]
- [[05 - LocalPlaylistManager tsuki_playlists.db]]
- [[06 - WatchHistoryManager Doble Tabla]]
- [[07 - FavoritesManager Corazon Offline]]
- [[08 - TrackTagsManager Moods]]
- [[09 - RecognitionHistoryManager 50]]
- [[10 - LocalAudioScanner MediaStore]]
- [[11 - TSukiSubscriptionRepository Canonica]]
- [[12 - TSukiBackupRepository NewPipe-CSV-Master]]
- [[13 - PlayerPreferences DataStore]]
- [[14 - HomePreferences y AppearancePreferences]]

## <a id="07---motor-neuronal-y-recomendacion"></a>🧠 Motor Neuronal y Recomendación Local

- [[01 - TSukiNeuroEngine y Aprendizaje Local]]
- [[02 - Modelos y Algebra Vectorial]]
- [[03 - TSukiTokenizer TF-IDF Lematizacion]]
- [[04 - TSukiVectorMath Coseno EMA]]
- [[05 - TSukiTopicCatalog Onboarding 8 Cats]]
- [[06 - TSukiBrainStorage Proto-DataStore]]
- [[07 - TSukiNeuroEngine Ranking 781 Lineas]]
- [[08 - TSukiSeedSelector 5 Seeds Radio]]
- [[09 - Shorts Classifier Discovery Repository]]
- [[10 - Subscriptions Feed y NewVideosWorker]]

## <a id="08---sincronizacion-escuchar-juntos"></a>👥 Sincronización Social Escuchar Juntos

- [[01 - Protocolo Hibrido LAN y Cloud Relay]]
- [[02 - Algoritmo de Sincronizacion y Drift]]
- [[03 - TogetherMessages Contrato v1]]
- [[04 - TogetherLink Codec tsuki-together]]
- [[05 - TogetherJson Discriminador Type]]
- [[06 - TogetherClock EWMA Offset]]
- [[07 - TogetherGuestPlaybackPlanner Permisos]]
- [[08 - TogetherClient Guest WS]]
- [[09 - TogetherServer LAN CIO 42117]]
- [[10 - TogetherOnlineHost Relay Privilegiado]]
- [[11 - TogetherOnlineApi y Endpoint Bearer]]
- [[12 - TogetherPlaybackSync Drift y QueueHash]]
- [[13 - MusicTogetherRepository Fachada UI]]

## <a id="09---reconocimiento-acustico-shazam"></a>⚡ Reconocimiento Acústico Shazam

- [[01 - Generador de Firmas Acusticas FFT]]
- [[02 - MusicRecognizer AudioRecord 16kHz]]
- [[03 - ShazamSignatureGenerator FFT 2048]]
- [[04 - Shazam Cliente Anti-Ban Cola]]
- [[05 - ShazamModels DTOs SerialName]]
- [[06 - RecognitionScreen y Flujo UX]]

## <a id="10---importacion-y-exportacion"></a>📦 Importación y Migración de Playlists

- [[01 - Parsers Multiplataforma y ArchiveTune]]
- [[02 - Resolucion y Matching Difuso]]
- [[03 - PlaylistParsers CSV M3U ZIP]]
- [[04 - SpotifyPlaylistParser Exportify Oficial Web]]
- [[05 - SpotifyTrackMatcher Bigrama 0.60]]
- [[06 - FuzzyMatcher Fallback 0.50]]
- [[07 - ImportSongResolver Semaphore 3]]
- [[08 - PlaylistExporters M3U CSV]]
- [[09 - ArchiveTune ZIP song.db Detalle]]

## <a id="11---ui-y-experiencia-compose"></a>🎨 UI, Jetpack Compose y Material 3

- [[01 - Sistema de Diseno Material 3 Expressive]]
- [[02 - Arquitectura del Reproductor y MiniPlayer]]
- [[03 - Widgets de Pantalla de Inicio Glance]]
- [[04 - Captura PixelCopy para Redes]]
- [[05 - MusicPlayerScreenV9 Orquestador 1466L]]
- [[06 - PlayerControlsV9 Pastillas y Slider]]
- [[07 - ArtworkPagerV9 Gestos y Canvas]]
- [[08 - PlayerBackgroundV9 Gradiente]]
- [[09 - QueueListV9 Reorder Swipe Undo]]
- [[10 - PlayerBottomSheet Anclas 3 Niveles]]
- [[11 - AodPlayerScreen Ambiente y Slide-Lock]]
- [[12 - Canvas Trio Provider Resolver DiskCache]]
- [[13 - EqualizerDialog 5 Bandas]]
- [[14 - ShareCards PixelCopy Stats]]
- [[15 - VideoPlayerScreen 2001L Gestos]]
- [[16 - MusicScreen y Pestanas Virtuales]]
- [[17 - HomeScreen y Secciones Personalizadas]]
- [[18 - LibraryScreen y Likes Sincronizados]]
- [[19 - PlaylistDetailScreen y Virtual LM]]
- [[20 - SettingsScreen y SettingsViewModel]]
- [[21 - StatsScreen y Historial de Reproduccion]]
- [[22 - ChannelScreen y Exploracion de Artistas]]
- [[23 - ImportPlaylistScreen y Asistente de Migracion]]
- [[24 - LoginScreen y Autenticacion OAuth InnerTube]]
- [[25 - OnboardingScreen y Seleccion de Artistas]]
- [[26 - PersonalizationScreen y Preferencias de Genero]]
- [[27 - RecognitionScreen y Escaneo Acustico]]
- [[28 - SubscriptionsScreen y RSS de Canales]]
- [[29 - TogetherScreen y Salas Escuchar Juntos]]
- [[30 - TSukiShortsScreen y Feed Vertical]]
- [[31 - Componentes Settings (SettingsGroup, Sliders, Toggles, Actions)]]
- [[32 - M3Motion Tokens Springs Sliders]]
- [[33 - Theme Color Shape Type PlayerColorExtractor]]
- [[34 - Widgets Glance y TSukiGlanceReceiver (Keys, Sync, Actions, Theme)]]
- [[35 - Componentes Core (MiniPlayer, TSukiPillNavBar, FastScrollBox, Sheets)]]

## <a id="12---guia-de-desarrollo-y-comandos"></a>🛠️ Guía de Desarrollo, Toolchain y Depuración

- [[01 - Toolchain Comandos y Depuracion]]
- [[02 - Compilacion Rapida vs APK]]
- [[03 - Instalacion adb fb74ec96 y Logcat]]
- [[04 - Patron PixelCopy Captura Dialog]]
- [[05 - Patrones UI Ganados M3]]
- [[06 - Testing Unitario y Sin CI]]
- [[07 - Troubleshooting 403 416 Update Incompatible]]
