# 02 - Diccionario Completo de Archivos y Responsabilidades

> Este documento es el inventario maestro y exhaustivo de todos los archivos Kotlin en `app/src/main/java/com/example/tsuki/`. Para cada archivo se define **qué hace**, **para qué existe**, **por qué está ubicado en ese paquete**, y qué precauciones deben mantenerse.

---

## 🧭 Índice por Paquetes

1. [[#1. Raíz de la Aplicación (Root)]]
2. [[#2. Autenticación (auth)]]
3. [[#3. Modelos de Dominio (domain.model)]]
4. [[#4. Motor de Reproducción y Audio (playback)]]
5. [[#5. Red y Extracción de Contenido (network)]]
6. [[#6. Letras y Sincronización Karaoke (lyrics & lyrics.providers)]]
7. [[#7. Persistencia, Bases de Datos y Preferencias (data.local & data.backup)]]
8. [[#8. Inteligencia y Recomendaciones (data.recommendation)]]
9. [[#9. Shorts y Suscripciones (data.shorts & data.subscriptions)]]
10. [[#10. Escuchar Juntos en Tiempo Real (together)]]
11. [[#11. Reconocimiento Acústico (shazam)]]
12. [[#12. Importación y Exportación de Playlists (playlistimport)]]
13. [[#13. Interfaz de Usuario: Reproductor (ui.player)]]
14. [[#14. Interfaz de Usuario: Pantallas Principales (ui.screens)]]
15. [[#15. Interfaz de Usuario: Componentes Reutilizables (ui.components)]]
16. [[#16. Interfaz de Usuario: Temas y Diseño (ui.theme)]]
17. [[#17. Widgets para Pantalla de Inicio (ui.widget & ui.widget.glance)]]
18. [[#18. Utilidades y Sistema (util)]]

---

## 1. Raíz de la Aplicación (Root)

### `TSukiApp.kt`
* **Qué hace:** Es la clase `Application` de Android. Inicializa componentes globales en el arranque: NewPipeExtractor con `NewPipeDownloader`, ImageLoader de Coil optimizado para cacheo en disco/memoria, y canales de notificación.
* **Para qué existe:** Punto de entrada de inicialización del proceso antes de cualquier Activity o Service.
* **Por qué está aquí:** Debe estar en el paquete raíz `com.example.tsuki` al ser referenciada directamente por el `AndroidManifest.xml` (`android:name=".TSukiApp"`).

### `MainActivity.kt`
* **Qué hace:** Actividad única (*Single-Activity*) de la app. Gestiona el ciclo de vida, soporte Edge-to-Edge nativo, contenedor de navegación en Compose, persistencia del `PlayerBottomSheet`, soporte Picture-in-Picture (PiP) para videos, recepción de Intents externos (enlaces de YouTube, "Compartir a TSuki", deep links `tsuki://together`) y solicitud de permisos en tiempo de ejecución.
* **Para qué existe:** Servir de host visual e interactivo principal para todo el árbol de Composables.
* **Por qué está aquí:** Actividad declarada en el manifiesto con `android.intent.action.MAIN`.

---

## 2. Autenticación (auth)

### `YouTubeAuthManager.kt`
* **Qué hace:** Administra las cookies de sesión de YouTube Music, el identificador de visitante (`visitorData`), el token `SAPISID` y la persistencia segura de las credenciales del usuario en DataStore / SharedPreferences.
* **Para qué existe:** Permite a la app identificarse ante los servidores de YouTube Music para acceder a playlists privadas, historial de reproducción y la lista de "Tus Me Gusta" (`LM`).
* **Por qué está en `auth/`:** Aísla la responsabilidad criptográfica y de almacenamiento de tokens de sesión del resto de la lógica de red.

---

## 3. Modelos de Dominio (domain.model)

### `MediaTrack.kt`
* **Qué hace:** Modela la entidad fundamental de una pista de audio/video: identificador único, título, artista, álbum, URL de carátula, duración en segundos, tipo de medio (`MediaType.STREAM_AUDIO`, `STREAM_VIDEO`, `LOCAL_AUDIO`), si es Short, URL de stream resuelta y metadatos complementarios.
* **Para qué existe:** Garantiza un contrato inmutable común consumido por la UI, la base de datos, el reproductor y los extractores.
* **Por qué está en `domain/model/`:** Principio de Clean Architecture; las entidades del núcleo no dependen de capas externas.

### `LyricsEntry.kt`
* **Qué hace:** Modela una línea de letra sincronizada con marca de tiempo en milisegundos (`timestampMs`), texto completo de la línea, y una lista opcional de segmentos silábicos (`WordTimestamp`) con marcas de inicio y fin en segundos para el karaoke palabra por palabra.
* **Para qué existe:** Estructura tipada que alimenta los renderizadores de letras sincronizadas.
* **Por qué está en `domain/model/`:** Modelo de dominio puro consumido tanto por el parser como por la vista en Compose.

---

## 4. Motor de Reproducción y Audio (playback)

### `PlayerController.kt`
* **Qué hace:** Es el controlador maestro y cerebro operativo de la reproducción musical. Conecta con el `MediaController` de Media3, gestiona la cola de pistas (`queue`, `queueIndex`), emite el estado reactivo inmutable `PlayerUiState`, gobierna la barra de progreso (`playbackTick`), cachea URLs de streaming con un LRU de 50 elementos, salta automáticamente segmentos de anuncios/patrocinio con SponsorBlock, consulta votos de Return YouTube Dislike, administra el temporizador de apagado (*Sleep Timer*), la velocidad de reproducción y orquesta las extensiones automáticas de cola (*AutoQueue*).
* **Para qué existe:** Centraliza toda la lógica de negocio y estado del reproductor en un único punto observable, desacoplando completamente la UI del servicio en segundo plano.
* **Por qué está en `playback/`:** Pertenece al núcleo del motor de audio de la aplicación.

### `TSukiPlaybackService.kt`
* **Qué hace:** Servicio en primer plano (*Foreground Service*) que hereda de `MediaLibraryService` de Media3. Inicializa el `ExoPlayer` principal con `MergingMediaSource` (para combinar video DASH con audio Opus), configura el `DefaultLoadControl` con 15 segundos de buffer hacia atrás (*backBuffer*), adquiere el `WakeLock` de red y CPU durante la reproducción, publica la notificación multimedia del sistema (con el vector monocromo blanco) e implementa la compatibilidad con Android Auto y el MediaBrowser del sistema.
* **Para qué existe:** Mantener la reproducción viva de forma ininterrumpida cuando la pantalla está apagada o la aplicación se encuentra en segundo plano.
* **Por qué está en `playback/`:** Es el componente de servicio Android del motor de reproducción.

### `CrossfadeController.kt`
* **Qué hace:** Implementa la mezcla continua y fluida (*Crossfade*) entre canciones sin interrupciones. Crea un `secondary: ExoPlayer` independiente, pre-descarga la pista siguiente, ejecuta una curva trigonométrica de igual potencia (coseno/seno) para fundir la pista saliente y la entrante de 0 a 100%, y realiza un intercambio silencioso (*silent swap*) al reproductor principal.
* **Para qué existe:** Proveer transiciones de audio con calidad de estudio idénticas a Spotify o Apple Music, algo que ExoPlayer estándar no soporta de forma nativa en un solo reproductor.
* **Por qué está en `playback/`:** Modifica directamente el comportamiento y volumen del motor de reproducción.

### `CrossfadeHandoffPolicy.kt`
* **Qué hace:** Define la matemática pura del crossfade: cálculo de ganancias de igual potencia (`equalPowerGains`), márgenes temporales de preparación (`PREPARE_AHEAD_MS = 8000L`), cuadros por segundo de volumen (`FRAME_MS = 50L`) y detección de deriva entre reproductores (`needsCorrectiveCrossfadeSeek`).
* **Para qué existe:** Separa la matemática de cálculo acústico del ciclo de vida del reproductor para permitir pruebas unitarias limpias.
* **Por qué está en `playback/`:** Es una política técnica íntimamente ligada a `CrossfadeController`.

### `AutoQueueHelper.kt`
* **Qué hace:** Detecta cuándo la cola de reproducción actual está cerca de agotarse (menos de 2 pistas restantes) y consulta de forma transparente pistas similares recomendadas a través de `TSukiInnerTubeClient.fetchRelatedTracks` (o automix por defecto), extendiendo la cola para que la música nunca se detenga.
* **Para qué existe:** Ofrecer la funcionalidad de "Radio Infinita" sin requerir intervención manual del usuario.
* **Por qué está en `playback/`:** Gobierna la extensión dinámica de la cola de reproducción.

### `AudioEqualizerHelper.kt`
* **Qué hace:** Envuelve y expone los efectos nativos del DSP de audio de Android: `Equalizer` (bandas de frecuencia ajustables), `BassBoost` (refuerzo de graves por milésimas de fuerza), `Virtualizer` (efecto envolvente 3D) y `LoudnessEnhancer` (ganancia de salida en mB para normalización de volumen).
* **Para qué existe:** Brindar control acústico avanzado al usuario sobre la salida de audio de ExoPlayer.
* **Por qué está en `playback/`:** Opera sobre el `audioSessionId` generado por el reproductor.

### `PlayerCacheProvider.kt`
* **Qué hace:** Provee la instancia Singleton de `SimpleCache` de Media3 respaldada por una base de datos `StandaloneDatabaseProvider` y un almacenamiento en disco con política LRU (*Least Recently Used*).
* **Para qué existe:** Almacenar en caché fragmentos descargados de streams de YouTube para que el avance/retroceso rápido y la repetición de canciones consuman cero datos de red adicionales.
* **Por qué está en `playback/`:** Provee la fuente de datos en caché consumida por `TSukiPlaybackService`.

### `DownloadEngine.kt`
* **Qué hace:** Administra las descargas persistentes completas de pistas de audio y video al almacenamiento local, gestionando el progreso, estados de pausa/reanudación y registro en base de datos.
* **Para qué existe:** Permitir al usuario reproducir sus listas y álbumes favoritos en modo 100% sin conexión a internet (*Offline*).
* **Por qué está en `playback/`:** Trabaja estrechamente con el sistema de archivos multimedia y el reproductor local.

### `YouTubeHttpDataSource.kt`
* **Qué hace:** Fábrica personalizada de fuentes de datos HTTP (`HttpDataSource.Factory`) que inyecta las cabeceras HTTP necesarias (User-Agent, Range, Origin) para evitar que los servidores de streaming de YouTube corten la conexión prematuramente.
* **Para qué existe:** Evitar errores `HttpDataSourceException` y bloqueos de red al solicitar fragmentos de audio de alta tasa de bits.
* **Por qué está en `playback/`:** Es el adaptador de red directo inyectado en la factoría de fuentes de ExoPlayer.

### `NextTrackPrecacher.kt`
* **Qué hace:** Pre-descarga de forma preventiva los primeros megabytes del siguiente stream de audio mientras la pista actual se está reproduciendo.
* **Para qué existe:** Lograr un arranque instantáneo (*Zero Buffering Latency*) al saltar a la siguiente canción.
* **Por qué está en `playback/`:** Optimiza el flujo de precarga de ExoPlayer.

### `AutoLibrarySessionCallback.kt`
* **Qué hace:** Implementación del callback `MediaLibrarySession.Callback` para interactuar con clientes externos, tales como Android Auto o el subsistema multimedia de Android.
* **Para qué existe:** Responder a solicitudes de navegación en listas, comandos de reproducción remota y comandos por voz de Google Assistant.
* **Por qué está en `playback/`:** Es el puente MediaSession del servicio de reproducción.

### `TogetherManager.kt`
* **Qué hace:** Conecta el estado de `PlayerController` con el subsistema `together/`, coordinando la emisión y recepción de comandos remotos (play, pause, seek, sincronización de cola).
* **Para qué existe:** Aislar las dependencias de la sesión "Escuchar juntos" del código principal del reproductor.
* **Por qué está en `playback/`:** Actúa como adaptador entre el reproductor y el módulo colaborativo.

---

## 5. Red y Extracción de Contenido (network)

### `TSukiInnerTubeClient.kt`
* **Qué hace:** Cliente HTTP de bajo nivel masivo que habla directamente con los endpoints de YouTube Music (`WEB_REMIX`) y YouTube Web (`WEB`). Gestiona llamadas a `/browse`, `/search`, `/next`, `/player`, creación y edición de playlists, cálculo de SAPISIDHASH criptográfico, paginación exhaustiva mediante tokens de continuación, y extracción de chips de estado de ánimo.
* **Para qué existe:** Obtener catálogo musical completo, carátulas, playlists personalizadas y recomendaciones sin recurrir a la API oficial de YouTube v3 (que impone cuotas severas e inutilizables).
* **Por qué está en `network/`:** Es el motor de red principal para toda la capa de descubrimiento de contenido.

### `YouTubeExtractor.kt`
* **Qué hace:** Extrae URLs directas de streams de audio (Opus, AAC) y video a partir del ID de un video (`videoId`). Utiliza `NewPipeExtractor` como núcleo, analiza streams progresivos y adaptativos, extrae avatares de canal, títulos, carátulas y resuelve descargas directas.
* **Para qué existe:** Resolver el archivo reproducible real (`.googlevideo.com/...`) a partir del identificador de YouTube.
* **Por qué está en `network/`:** Especializado en extracción y resolución de medios de streaming.

### `NewPipeDownloader.kt`
* **Qué hace:** Implementa la interfaz `Downloader` de NewPipeExtractor utilizando un cliente `OkHttpClient` optimizado con compresión Brotli, cabeceras personalizadas y cookies de sesión.
* **Para qué existe:** Conectar NewPipeExtractor con el stack de red moderno de TSuki.
* **Por qué está en `network/`:** Adaptador de red directo para la librería externa de extracción.

### `AudioQualityPolicy.kt`
* **Qué hace:** Aplica la política de calidad de audio elegida por el usuario (Alta: Opus 160kbps, Media: 128kbps, Baja: 64kbps/AAC) considerando el estado de ahorro de datos y la naturaleza de la conexión (Wi-Fi vs Datos móviles).
* **Para qué existe:** Garantizar un consumo racional de datos móviles y la mejor fidelidad en conexiones de alta velocidad.
* **Por qué está en `network/`:** Gobierna la selección de streams dentro de la capa de red.

### `MeteredNetworkMonitor.kt`
* **Qué hace:** Monitorea de forma reactiva si el dispositivo está conectado a una red con coste medido (datos móviles o punto de acceso Wi-Fi compartido).
* **Para qué existe:** Alimentar a `AudioQualityPolicy` para activar automáticamente el modo de ahorro de datos.
* **Por qué está en `network/`:** Es un observador de conectividad de red de bajo nivel.

### `ReturnYouTubeDislikeClient.kt`
* **Qué hace:** Consulta la API pública de Return YouTube Dislike (RYD) para obtener el conteo estimado de dislikes y votos de un video.
* **Para qué existe:** Restaurar la visibilidad de la opinión de la comunidad sobre las canciones y videos.
* **Por qué está en `network/`:** Cliente HTTP para un servicio de terceros.

### `SponsorBlockClient.kt`
* **Qué hace:** Consulta la API de SponsorBlock para obtener los segmentos de tiempo en los que un video contiene intros no musicales, patrocinios, autopromoción o silencios.
* **Para qué existe:** Permitir a `PlayerController` saltar automáticamente partes habladas en videos musicales, ofreciendo una experiencia auditiva limpia.
* **Por qué está en `network/`:** Cliente HTTP de la API de SponsorBlock.

### `ChannelRssClient.kt`
* **Qué hace:** Consulta y parsea los feeds RSS nativos de los canales de YouTube (`https://www.youtube.com/feeds/videos.xml?channel_id=...`).
* **Para qué existe:** Obtener de manera ultrarrápida y sin consumo de cuota los últimos videos subidos por canales suscritos.
* **Por qué está en `network/`:** Cliente de lectura de sindicación RSS.

---

## 6. Letras y Sincronización Karaoke (lyrics & lyrics.providers)

### `LyricsHelper.kt`
* **Qué hace:** Orquesta la cascada de búsqueda de letras en 16 proveedores diferentes. Aplica una jerarquía estricta: si encuentra letras con marcas silábicas (*Word-Synced* / TTML), las almacena de inmediato en caché y las retorna; si solo encuentra letras por línea (*Line-Synced*), las guarda como candidatas y sigue buscando una versión palabra por palabra antes de rendirse.
* **Para qué existe:** Ofrecer la tasa de éxito de letras karaoke más alta posible en la aplicación.
* **Por qué está en `lyrics/`:** Es el coordinador central del subsistema de letras.

### `LyricsUtils.kt`
* **Qué hace:** Caja de herramientas de análisis sintáctico de letras: detecta y parsea XML de TTML (`<p begin>` y `<span begin>`), analiza formato LRC enriquecido con marcas `<mm:ss.xx>`, decodifica entidades HTML y calcula offsets de tiempo.
* **Para qué existe:** Convertir texto crudo devuelto por las APIs en listas de `LyricsEntry` utilizables por la interfaz.
* **Por qué está en `lyrics/`:** Lógica pura de parsing y validación de letras.

### `LyricsPreloadManager.kt`
* **Qué hace:** Precarga asíncronamente en segundo plano las letras de la pista que sigue en la cola de reproducción.
* **Para qué existe:** Eliminar cualquier retraso o spinner de carga cuando el usuario abre la pestaña de letras al cambiar de canción.
* **Por qué está en `lyrics/`:** Optimiza la experiencia de usuario del reproductor de letras.

### `LyricsSanitizer.kt`
* **Qué hace:** Limpia títulos y nombres de artistas eliminando texto basura como "(Official Video)", "[Audio]", "feat. X", etc., antes de consultar los proveedores de letras.
* **Para qué existe:** Mejorar drásticamente el porcentaje de coincidencias exactas en las bases de datos de letras.
* **Por qué está en `lyrics/`:** Es un paso de normalización de datos previo a la búsqueda de letras.

### `LyricsProvider.kt`
* **Qué hace:** Interfaz que define el contrato `suspend fun getLyrics(videoId, title, artist, durationSeconds): Result<String>`.
* **Para qué existe:** Polimorfismo limpio para conectar nuevos proveedores de letras de forma desacoplada.
* **Por qué está en `lyrics/`:** Define el contrato base de la capa.

### Proveedores en `lyrics.providers.*`
* **`BetterLyricsProvider.kt` & `BetterLyricsPortatoProvider.kt`:** Consulta a la API de BetterLyrics con soporte para TTML silábico de Apple Music y Spotify.
* **`LrcLibLyricsProvider.kt`:** Cliente para la base de datos libre y comunitaria LrcLib.
* **`KuGouLyricsProvider.kt`:** Acceso a la base de datos asiática KuGou (ideal para música oriental y anime).
* **`NeteaseLyricsProvider.kt`:** Acceso al servicio Netease Cloud Music.
* **`Paxsenix*.kt`:** Suite de proveedores de Paxsenix (Spotify, Apple Music, Musixmatch, Netease) con letras oficiales sincronizadas por palabra.
* **`SimpMusicLyricsProvider.kt` & `YouLyPlusLyricsProvider.kt`:** Proveedores de respaldo comunitarios.
* **`UnisonLyricsProvider.kt`:** Proveedor de letras sincronizadas de alta precisión.
* **`YouTubeLyricsProvider.kt` & `YouTubeSubtitleLyricsProvider.kt`:** Extracción directa de subtítulos cerrados de YouTube convertidos a formato lírico.
* **Por qué están en `lyrics/providers/`:** Organización modular; cada archivo representa una integración HTTP aislada.

---

## 7. Persistencia, Bases de Datos y Preferencias (data.local & data.backup)

### `LocalPlaylistManager.kt`
* **Qué hace:** Gestiona la base de datos SQLite cruda `tsuki_playlists.db`. Crea y administra tablas de listas de reproducción y canciones asociadas (`playlist_songs` guardando el JSON serializado de cada pista con su posición de ordenamiento).
* **Para qué existe:** Proveer listas de reproducción locales creadas por el usuario sin depender de una cuenta de Google ni de internet.
* **Por qué está en `data/local/`:** Es el gestor de almacenamiento de listas de reproducción locales.

### `WatchHistoryManager.kt`
* **Qué hace:** Gestiona `tsuki_history.db`. Registra el historial agregado de reproducciones (`watch_history`) y los eventos atómicos de tiempo escuchado (`play_events`), permitiendo computar estadísticas de uso, canciones más escuchadas y tiempo total de escucha.
* **Para qué existe:** Respaldar la pantalla de estadísticas del usuario y alimentar el motor de recomendaciones.
* **Por qué está en `data/local/`:** Persistencia del historial de consumo multimedia.

### `LyricsDatabase.kt`
* **Qué hace:** Gestiona la base de datos `lyrics.db` que almacena en disco las letras descargadas, asociadas a su `videoId`, con auto-poda (*pruning*) a los 1,500 registros más recientes.
* **Para qué existe:** Evitar descargas repetitivas de letras en canciones ya reproducidas y habilitar letras en modo offline.
* **Por qué está en `data/local/`:** Caché persistente de letras.

### `TrackTagsManager.kt`
* **Qué hace:** Gestiona `tsuki_tags.db`. Permite asociar estados de ánimo (*moods*) y etiquetas personalizadas a cada canción.
* **Para qué existe:** Permitir al usuario filtrar y organizar su música por vibras o estados anímicos.
* **Por qué está en `data/local/`:** Persistencia de metadatos personalizados del usuario.

### `FavoritesManager.kt`
* **Qué hace:** Gestiona las pistas marcadas como favoritas de forma local, ofreciendo consultas rápidas `isFavorite(videoId)` y flujos reactivos de pistas preferidas.
* **Para qué existe:** Biblioteca de canciones que le gustan al usuario independientemente de si ha iniciado sesión en YouTube Music.
* **Por qué está en `data/local/`:** Gestión de favoritos locales.

### `RecognitionHistoryManager.kt`
* **Qué hace:** Almacena el historial de canciones reconocidas mediante el motor acústico estilo Shazam.
* **Para qué existe:** Guardar un registro histórico para que el usuario pueda consultar qué canciones identificó en el pasado.
* **Por qué está en `data/local/`:** Persistencia de descubrimientos acústicos.

### `TSukiSubscriptionRepository.kt`
* **Qué hace:** Almacena los canales de YouTube a los que el usuario se ha suscrito dentro de la app sin necesidad de iniciar sesión con una cuenta de Google.
* **Para qué existe:** Ofrecer una experiencia de suscripción privada e independiente.
* **Por qué está en `data/local/`:** Almacenamiento local de suscripciones.

### `LocalAudioScanner.kt`
* **Qué hace:** Escanea el almacenamiento interno del teléfono mediante el `MediaStore` de Android para descubrir canciones en formato MP3, FLAC, M4A, etc., filtrando notas de voz de WhatsApp/Telegram y sonidos de sistema.
* **Para qué existe:** Permitir reproducir los archivos de música almacenados físicamente en el teléfono.
* **Por qué está en `data/local/`:** Escaneo de archivos multimedia del dispositivo.

### `PlayerPreferences.kt`, `HomePreferences.kt`, `AppearancePreferences.kt`
* **Qué hacen:** Implementan la persistencia atómica y reactiva de ajustes mediante Jetpack DataStore (calidad de audio, crossfade, velocidad, tema visual, proveedor de letras preferido, etc.).
* **Para qué existen:** Mantener la configuración del usuario intacta entre reinicios de la aplicación.
* **Por qué están en `data/local/`:** Configuración de preferencias persistentes locales.

### `BackupManager.kt` & `TSukiBackupRepository.kt`
* **Qué hacen:** Serializan y deserializan toda la información local de la app (playlists, historial, favoritos, tags, reconocimientos) en un único archivo JSON unificado con soporte de mezcla (*Merge*).
* **Para qué existen:** Permitir al usuario migrar su biblioteca musical a un nuevo dispositivo o restaurarla tras un formateo.
* **Por qué están en `data/backup/`:** Módulo de respaldo y restauración de datos.

---

## 8. Inteligencia y Recomendaciones (data.recommendation)

### `TSukiNeuroEngine.kt`
* **Qué hace:** Motor neuronal de recomendación musical local (*On-Device*). Construye y actualiza vectores de afinidad del usuario mediante medias móviles exponenciales (EMA), penaliza canciones sobre-escuchadas o rechazadas (*skip fatigue*), aplica aumentos de momento (*momentum boost*) y perfila la personalidad musical del usuario (*TSukiPersona*).
* **Para qué existe:** Ofrecer recomendaciones musicales hiper-personalizadas sin enviar los hábitos del usuario a ningún servidor externo.
* **Por qué está en `data/recommendation/`:** Núcleo de la lógica de recomendación inteligente.

### `TSukiVectorMath.kt`, `TSukiTokenizer.kt`, `TSukiTopicCatalog.kt`, `TSukiModels.kt`
* **Qué hacen:** Proveen el álgebra lineal (similitud coseno, normalización euclidiana), análisis de texto por n-gramas, catálogo maestro de géneros/estilos y modelos de datos inmutables del cerebro algorítmico.
* **Para qué existen:** Respaldar matemáticamente el funcionamiento de `TSukiNeuroEngine`.
* **Por qué están en `data/recommendation/`:** Librería matemática y modelos del motor de recomendación.

### `TSukiBrainStorage.kt`
* **Qué hace:** Almacena los pesos y vectores del cerebro algorítmico en el almacenamiento privado de la aplicación.
* **Para qué existe:** Persistir el perfil musical adaptativo del usuario.
* **Por qué está en `data/recommendation/`:** Almacenamiento específico del motor neuronal.

---

## 9. Shorts y Suscripciones (data.shorts & data.subscriptions)

### `TSukiShortsRepository.kt`, `TSukiShortsDiscoveryEngine.kt`, `TSukiShortsClassifier.kt`
* **Qué hacen:** Descubren, clasifican y preparan el feed vertical de videos cortos (Shorts de YouTube) con filtrado por duración (menor a 65 segundos) y etiquetas (`#shorts`).
* **Para qué existen:** Soportar la pantalla de visualización continua de videos cortos musicales.
* **Por qué están en `data/shorts/`:** Lógica de datos de la funcionalidad de Shorts.

### `TSukiSubscriptionFeedRepository.kt` & `NewVideosWorker.kt`
* **Qué hacen:** Consultan los videos más recientes de los canales seguidos utilizando RSS y `YouTubeExtractor`, y ejecutan tareas periódicas en segundo plano con WorkManager para notificar al usuario sobre nuevos lanzamientos.
* **Para qué existen:** Mantener al usuario al día con el contenido de sus artistas predilectos.
* **Por qué están en `data/subscriptions/`:** Gestión del feed y sincronización en segundo plano de canales.

---

## 10. Escuchar Juntos en Tiempo Real (together)

### `TogetherServer.kt`
* **Qué hace:** Levanta un servidor HTTP y WebSocket local en el dispositivo utilizando el motor Ktor CIO. Administra la lista de participantes conectados, procesa solicitudes de unión y difunde el estado del reproductor.
* **Para qué existe:** Permitir que otros dispositivos en la misma red local (LAN / Wi-Fi) se sincronicen con la reproducción del anfitrión sin necesidad de un servidor intermedio en la nube.
* **Por qué está en `together/`:** Componente de servidor de la funcionalidad colaborativa.

### `TogetherClient.kt`
* **Qué hace:** Cliente WebSocket que se conecta a un `TogetherServer` (local o en la nube), enviando paquetes de pulso y recibiendo cambios de estado.
* **Para qué existe:** Permitir a un dispositivo unirse como oyente a una sesión existente.
* **Por qué está en `together/`:** Componente de cliente del protocolo.

### `TogetherPlaybackSync.kt` & `TogetherClock.kt`
* **Qué hace:** Implementa un protocolo de sincronización de reloj similar a NTP: calcula el desfase temporal (*offset*), latencia de ida y vuelta (*RTT*), suprime ecos de comandos locales y determina cuándo es necesario ejecutar un salto correctivo (*seek*) si la deriva supera los umbrales (700ms en LAN, 1200ms en Cloud).
* **Para qué existe:** Garantizar que todos los usuarios escuchen la misma nota musical en el mismo milisegundo exacto.
* **Por qué están en `together/`:** Lógica matemática de sincronización temporal.

### `TogetherOnlineHost.kt`, `TogetherOnlineApi.kt`, `TogetherOnlineEndpoint.kt`
* **Qué hacen:** Gestionan la conexión con el servidor en la nube de "Escuchar juntos" cuando los participantes no están en la misma red Wi-Fi, utilizando el token `TOGETHER_BEARER_TOKEN`.
* **Para qué existen:** Permitir sesiones compartidas a través de internet móvil.
* **Por qué están en `together/`:** Capa de red remota del subsistema colaborativo.

---

## 11. Reconocimiento Acústico (shazam)

### `ShazamSignatureGenerator.kt`
* **Qué hace:** Captura audio mono a 16kHz en bloques de 128 muestras, aplica una ventana de Hann sobre un búfer circular de 2048 muestras, ejecuta la transformada rápida de Fourier (FFT), propaga los picos espectrales (*peak spreading*), los agrupa por bandas de frecuencia y los empaqueta en un payload binario con suma de verificación CRC32 y codificación Base64.
* **Para qué existe:** Generar la firma acústica exacta requerida por la API de Shazam sin recurrir a librerías propietarias externas.
* **Por qué está en `shazam/`:** Es el núcleo del algoritmo acústico.

### `Shazam.kt` & `MusicRecognizer.kt`
* **Qué hace:** Envía la firma generada a los endpoints de Shazam y parsea la respuesta para obtener el título, artista, carátula y metadatos de la canción sonando en el ambiente.
* **Para qué existe:** Ofrecer reconocimiento instantáneo de canciones del entorno.
* **Por qué están en `shazam/`:** Coordinadores de reconocimiento musical.

---

## 12. Importación y Exportación de Playlists (playlistimport)

### `SpotifyPlaylistParser.kt` & `PlaylistParsers.kt`
* **Qué hace:** Parsea volcados de listas de reproducción de Spotify en formato JSON (Takeout), CSV (Exportify) y M3U genérico.
* **Para qué existe:** Permitir a usuarios de otras plataformas migrar sus bibliotecas a TSuki.
* **Por qué están en `playlistimport/`:** Parsers de formatos externos.

### `ImportSongResolver.kt` & `FuzzyMatcher.kt`
* **Qué hace:** Toma el título y artista de una canción importada, realiza una búsqueda en YouTube Music y calcula una puntuación de coincidencia difusa utilizando el ratio de ordenamiento de tokens (*Token Sort Ratio*) de Levenshtein y tolerancia de duración temporal.
* **Para qué existe:** Encontrar la versión exacta en YouTube Music de canciones provenientes de Spotify o archivos locales.
* **Por qué están en `playlistimport/`:** Motor de resolución e inteligencia de coincidencia.

### `PlaylistExporters.kt`
* **Qué hace:** Exporta las listas de reproducción locales a archivos universales `.m3u8` y `.csv`.
* **Para qué existe:** Asegurar que los datos del usuario nunca queden atrapados dentro de la app (*Data Portability*).
* **Por qué está en `playlistimport/`:** Generación de formatos exportables.

---

## 13. Interfaz de Usuario: Reproductor (ui.player)

### `MusicPlayerScreenV9.kt`
* **Qué hace:** Pantalla principal del reproductor a pantalla completa. Coordina el paginador de carátulas (`ArtworkPagerV9`), controles de transporte (`PlayerControlsV9`), fondo dinámico difuminado (`PlayerBackgroundV9`), panel de letras en tiempo real (`LyricsPaneV9`), visualizador de video en bucle (`CanvasArtworkPlayer`), y sheets modulares para ecualizador, temporizador, velocidad de reproducción y tarjeta para compartir.
* **Para qué existe:** Es la interfaz visual insignia donde el usuario interactúa mientras escucha música.
* **Por qué está en `ui/player/`:** Núcleo de la UI del reproductor.

### `QueueListV9.kt`
* **Qué hace:** Lista reordenable mediante gestos táctiles de arrastrar y soltar (*drag and drop*) que muestra la cola actual de reproducción, pista activa y opciones para eliminar o reordenar elementos.
* **Para qué existe:** Visualizar y modificar dinámicamente el orden de reproducción.
* **Por qué está en `ui/player/`:** Componente de cola del reproductor.

### `SyncedLyricsView.kt`, `KaraokeLyricRow.kt`, `KaraokeWordByWordView.kt`
* **Qué hacen:** Renderizan las letras de las canciones con autodesplazamiento suave a 60/120 FPS y resaltado palabra por palabra mediante shaders y gradientes de Compose.
* **Para qué existen:** Ofrecer una experiencia de karaoke interactiva y estéticamente superior.
* **Por qué están en `ui/player/lyrics/`:** Vistas especializadas de renderizado de letras.

### `EqualizerDialog.kt`
* **Qué hace:** Diálogo interactivo con controles deslizantes para ajustar las bandas de frecuencia del ecualizador gráfico, preajustes (Rock, Pop, Jazz, etc.), refuerzo de graves y virtualizador.
* **Para qué existe:** Interfaz de usuario para configurar `AudioEqualizerHelper`.
* **Por qué está en `ui/player/`:** Accesible directamente desde el reproductor.

### `AodPlayerScreen.kt`
* **Qué hace:** Pantalla de reproducción optimizada para pantallas Always-On Display (AOD) y modo noche con consumo ultrabajo de batería (fondo negro puro AMOLED, reloj minimalista y controles táctiles de alto contraste).
* **Para qué existe:** Controlar la música mientras el teléfono reposa en una base de noche o escritorio.
* **Por qué está en `ui/player/`:** Variante especializada del reproductor.

### `ShareCardView.kt` & `StatsShareCard.kt`
* **Qué hace:** Diseña composables con estilo de tarjeta estética (con carátula, tipografía estilizada y paleta de colores extraída) para ser convertidos a imagen con `PixelCopy` y compartidos en redes sociales.
* **Para qué existe:** Permitir al usuario compartir lo que está escuchando en Instagram Stories, WhatsApp, etc.
* **Por qué están en `ui/player/`:** Generadores de tarjetas visuales para compartir.

---

## 14. Interfaz de Usuario: Pantallas Principales (ui.screens)

* **`MusicScreen.kt`:** Pantalla principal de descubrimiento de YouTube Music (canciones populares, lanzamientos recientes, carruseles por estado de ánimo y la playlist virtual `LM`).
* **`HomeScreen.kt`:** Pantalla unificada que coordina las pestañas principales de la aplicación.
* **`LibraryScreen.kt`:** Biblioteca personal del usuario: playlists locales, álbumes descargados, favoritos, pistas locales del dispositivo y acceso a importar listas.
* **`PlaylistDetailScreen.kt`:** Detalle completo de una playlist o álbum: cabecera con arte grande, duración total, opciones de edición, reordenamiento y reproducción aleatoria.
* **`StatsScreen.kt`:** Panel de estadísticas de escucha: minutos totales escuchados, top artistas, top canciones, desglose por días y resumen semanal/mensual (*Wrapped*).
* **`RecognitionScreen.kt`:** Pantalla del reconocedor acústico con animación de radar de audio y lista de capturas históricas.
* **`TogetherScreen.kt`:** Interfaz para crear o unirse a salas de escucha compartida (código de sala, participantes activos, chat de estado).
* **`SubscriptionsScreen.kt` & `ChannelScreen.kt`:** Feed de canales suscritos y vista detallada del perfil de un artista o canal de YouTube.
* **`TSukiShortsScreen.kt`:** Reproductor de video vertical continuo de videos musicales cortos.
* **`SettingsScreen.kt`:** Pantalla completa de ajustes organizada en categorías (Audio, Apariencia, Descargas, Caché, Cuenta, Acerca de).
* **`PersonalizationScreen.kt` & `OnboardingScreen.kt`:** Pantallas para configurar las preferencias neuronales iniciales y temas de interés.
* **`LoginScreen.kt`:** WebView seguro para iniciar sesión en YouTube Music y extraer las cookies necesarias.
* **`ImportPlaylistScreen.kt`:** Asistente paso a paso para importar archivos de playlists desde Spotify o almacenamiento.

---

## 15. Interfaz de Usuario: Componentes Reutilizables (ui.components)

* **`MiniPlayer.kt`:** El reproductor flotante minimalista que permanece visible en la parte inferior sobre la barra de navegación; soporta gestos de deslizamiento para pausar o saltar canciones, y expansión fluida al reproductor completo.
* **`TSukiPillNavBar.kt`:** Barra de navegación flotante inferior con forma de píldora que utiliza tokens de Material 3 Expressive.
* **`M3Motion.kt`:** Biblioteca de físicas de movimiento: física de rebote táctil (`m3PressBounce`), transiciones de contenedor compartido y el deslizador ondulado (`M3WavySlider`).
* **`CornerPipPlayer.kt`:** Ventana flotante en miniatura tipo Picture-in-Picture dentro de la propia app mientras se visualizan videos.
* **`ShimmerLoading.kt`:** Animaciones de carga con efecto de brillo metálico (*Shimmer*) para estados mientras se obtienen datos de red.
* **`WeeklyWrappedOverlay.kt`:** Presentación animada estilo historias del resumen musical semanal.
* **`TagSongSheet.kt` & `AddToPlaylistSheet.kt`:** Hojas inferiores modulares para etiquetar canciones por estado de ánimo o agregarlas a listas de reproducción.
* **`TrackComponents.kt`:** Tarjetas de canción estandarizadas con opciones de menú contextual.

---

## 16. Interfaz de Usuario: Temas y Diseño (ui.theme)

* **`Theme.kt`:** Proveedor del tema general `TSukiTheme` configurando colores dinámicos de Android 12+ o paletas personalizadas AMOLED, soporte para tema claro/oscuro e inyección de tokens de diseño.
* **`Color.kt`, `Type.kt`, `Shape.kt`:** Definición de la tipografía y esquemas de forma redondeados según las pautas de Material 3 Expressive.
* **`PlayerColorExtractor.kt`:** Algoritmo que analiza la carátula de la canción actual utilizando `androidx.palette` y Coil, extrayendo los colores dominante y de acento, ajustando su contraste y luminosidad para garantizar legibilidad óptima en el fondo del reproductor.

---

## 17. Widgets para Pantalla de Inicio (ui.widget & ui.widget.glance)

* **`TSukiGlanceWidget.kt`:** Implementación declarativa de los widgets de inicio construidos con Jetpack Glance y Material 3. Ofrece 5 tamaños adaptables (Estándar, Mini, Cuadrado, Reanudación Rápida y Hero).
* **`TSukiGlanceSync.kt`:** Sincronizador de alto rendimiento que empuja el estado del reproductor (título, artista, carátula en bitmap, reproducción, favorito) a los widgets activos en la pantalla de inicio.
* **`TSukiGlanceActions.kt`, `TSukiGlanceKeys.kt`, `TSukiGlanceTheme.kt`:** Acciones de interacción (play/pause, next, prev, like), claves de preferencias de Glance y temas visuales para los widgets.
* **`TSukiWidgetProvider.kt`:** Receptor clásico para compatibilidad con sistemas que no soportan Glance.

---

## 18. Utilidades y Sistema (util)

* **`ConnectivityObserver.kt`:** Flujo reactivo que monitorea la pérdida o recuperación de la conexión a internet.
* **`UpdateChecker.kt`:** Comprueba si existe una nueva versión de TSuki publicada en los lanzamientos de GitHub.
* **`UpdateNotificationHelper.kt`:** Gestiona la descarga en segundo plano e instalación del nuevo paquete APK actualizado.
