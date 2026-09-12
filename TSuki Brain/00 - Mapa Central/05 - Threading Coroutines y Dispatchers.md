# 00.05 — Threading Coroutines y Dispatchers

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define la política estricta de hilos y concurrencia con Kotlin Coroutines en TSuki:
- **`Dispatchers.Main`**: Exclusivo para mutaciones de estado de Compose, recolección de `StateFlow` en UI y comandos inmediatos al `MediaController`.
- **`Dispatchers.IO`**: Para todas las operaciones de red (InnerTube, OkHttp, Ktor, Genius, SponsorBlock), consultas a bases de datos SQLite (`tsuki_playlists.db`, `tsuki_history.db`), lectura/escritura en DataStore y operaciones en disco (descargas offline, cache de audio).
- **`Dispatchers.Default`**: Cálculos intensivos de CPU:
  - Transformada Rápida de Fourier (FFT 2048) en `ShazamSignatureGenerator.kt`.
  - Tokenización, TF-IDF y similitud coseno en `TSukiNeuroEngine.kt`.
  - Parseo de XML y cálculo silábico en `LyricsUtils.kt`.
  - Algoritmo de distancia Levenshtein y coincidencias difusas en `FuzzyMatcher.kt` y `SpotifyTrackMatcher.kt`.
- **`Mutex` y Sincronización**:
  - `FavoritesManager.toggleMutex`: Evita condiciones de carrera en pulsación rápida de favoritos.
  - `DownloadEngine.downloadJobs`: Mapa concurrente `ConcurrentHashMap` para deduplicar descargas activas.
  - `PlayerCacheProvider`: Doble verificación sincronizada (`synchronized`) en la creación del singleton `SimpleCache`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene congelamientos de pantalla (*ANR - Application Not Responding*), caídas de frames por debajo de 60/120 FPS y bloqueos por contención de hilos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Directriz transversal en `00 - Mapa Central`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Prohibido `runBlocking` en hilo principal**: No utilizar `runBlocking` en Composables ni en métodos del ciclo de vida de la Activity.
- **Cálculo de progreso en draw phase**: Las animaciones continuas de progreso (como el indicador circular del MiniPlayer) deben calcular su valor en la fase de dibujo (`Canvas`), no en la composición.
- **Inferencia explícita en `coroutineScope`**: Dentro de `coroutineScope`, al usar `async`, especificar siempre el tipo: `val jobs: List<Deferred<Unit>> = ...`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Uso de `viewModelScope` y `rememberCoroutineScope()`.
- Flujos recolectados en Compose con `collectAsStateWithLifecycle()`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Flujo de Datos End-to-End]], [[01 - PlayerController Central]], [[01 - Generador de Firmas Acusticas FFT]].

---

## 7. Guía rápida para una IA nueva
- Si vas a ejecutar parsing pesado o matemáticas vectoriales, envuélvelo siempre en `withContext(Dispatchers.Default)`.
