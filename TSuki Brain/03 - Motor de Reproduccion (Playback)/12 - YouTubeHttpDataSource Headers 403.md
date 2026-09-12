# 03.12 — YouTubeHttpDataSource (Inyección Dinámica Anti-403)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Fábrica de fuentes de datos HTTP (`HttpDataSource.Factory`) para Media3 que intercepta cada solicitud hacia los servidores de GoogleVideo:
- Inspecciona el parámetro de cliente `?c=` en la URL del stream (ej. `IOS`, `ANDROID`, `WEB_REMIX`, `TVHTML5`, `VISIONOS`).
- Inyecta dinámicamente el `User-Agent` exacto que corresponde a ese cliente oficial:
  - `IOS`: `com.google.ios.youtube/21.03.1 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X; en_US)`
  - `ANDROID`: `com.google.android.youtube/19.49.34 (Linux; U; Android 14; en_US; Pixel 8 Pro)`
  - `WEB_REMIX`: Navegador Firefox moderno.
- Inyecta cabeceras HTTP obligatorias de validación:
  - `Origin: https://www.youtube.com`
  - `Referer: https://www.youtube.com/`
  - `Accept-Encoding: identity` (para que el servidor no comprima con gzip streams de audio).

**Archivos fuente clave:**
- [`playback/YouTubeHttpDataSource.kt:L23-172`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/YouTubeHttpDataSource.kt#L23-L172)

---

## 2. PARA QUÉ existe (problema que resuelve)
Los servidores de streaming de YouTube verifican que la firma criptográfica del enlace concuerde con el User-Agent del cliente. Si una petición firmada para iOS se solicita con un User-Agent genérico de OkHttp o Android, el servidor corta la conexión de inmediato con `HTTP 403 Forbidden`.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Es el adaptador de transporte de red inyectado en ExoPlayer en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La tabla de mapeo de User-Agents en `resolveUserAgentForUrl` (L152-161).
- La cabecera `Accept-Encoding: identity`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Monitoreo de respuestas HTTP mediante listeners de Media3.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[03 - Servicio de Fondo y Media3 Session]], [[02 - YouTubeExtractor y Ciphers]].

---

## 7. Guía rápida para una IA nueva
- Si experimentas errores HTTP 403 repentinos en streams específicos, revisa la tabla de User-Agents en este archivo.
