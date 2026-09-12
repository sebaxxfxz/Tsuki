# 04.13 — NewPipeDownloader y ConnectivityObserver

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa los puentes de red de bajo nivel:
1. **`NewPipeDownloader.kt`**:
   - Implementa la interfaz `Downloader` requerida por NewPipeExtractor.
   - Utiliza una instancia compartida de `OkHttpClient` optimizada con compresión Brotli y Gzip, timeout de conexión de 15 segundos y pool de conexiones de 32 sockets reutilizables durante 5 minutos.
   - Inyecta cabeceras de navegador de escritorio Firefox para evadir bloqueos de bots.
2. **`ConnectivityObserver.kt` (`util/ConnectivityObserver.kt`)**:
   - Emite un `Flow<Boolean>` reactivo indicando si el dispositivo cuenta con conexión a internet activa.
   - Monitorea transiciones entre Wi-Fi, datos móviles y pérdida de red.
   - Alimenta las alertas visuales (`OfflineBanner`) en la interfaz de usuario.

**Archivos fuente clave:**
- [`network/NewPipeDownloader.kt:L15-82`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/NewPipeDownloader.kt#L15-L82)
- [`util/ConnectivityObserver.kt:L1-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/util/ConnectivityObserver.kt#L1-L60)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza un transporte HTTP robusto para la extracción multimedia y provee a Compose un flujo reactivo para adaptarse al estado de conectividad sin bloquear hilos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Infraestructura de transporte y conectividad en `network/` y `util/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- `NewPipeDownloader.init(NewPipeDownloader)` debe registrarse en el arranque antes de cualquier llamada a NewPipe.
- Manejo de HTTP 429 (Too Many Requests) con detección de ReCaptcha.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Cliente HTTP optimizado para bajo consumo de batería.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[04 - Entry Points y Grafo de Arranque]], [[02 - YouTubeExtractor y Ciphers]].

---

## 7. Guía rápida para una IA nueva
- Para observar el estado de red en un Composable, recolecta `ConnectivityObserver.observe()`.
