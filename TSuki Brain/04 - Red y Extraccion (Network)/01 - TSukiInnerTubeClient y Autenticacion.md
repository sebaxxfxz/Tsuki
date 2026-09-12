# 01 - TSukiInnerTubeClient y Autenticación SAPISID

> **Ubicación:** `app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt`
> **Líneas de código:** ~3,320 líneas
> **Propósito:** Cliente API nativo y sin cuotas para YouTube Music e InnerTube.

---

## 🌐 ¿Qué es InnerTube?

InnerTube es la API interna y privada que utilizan las aplicaciones web y móviles de Google para comunicarse con YouTube y YouTube Music (`https://music.youtube.com/youtubei/v1/`).
No tiene los límites de cuota asfixiantes de la API oficial de YouTube Data v3 (la cual solo permite unas pocas miles de operaciones al día por aplicación).

TSuki implementa dos clientes principales dentro de las cabeceras de contexto:
1. **`WEB_REMIX`:** El cliente web oficial de YouTube Music. Se usa para Home (`FEmusic_home`), listas de éxitos, mezclas personalizadas, continuaciones y playlists musicales.
2. **`WEB`:** El cliente de escritorio de YouTube estándar. Se utiliza para búsquedas generales, canales y metadatos de video.

---

## 🔐 Algoritmo de Autenticación SAPISIDHASH

Para acceder a datos privados (tus playlists, canales suscritos o canciones con "Me Gusta"), YouTube Music requiere una cabecera `Authorization: SAPISIDHASH <timestamp>_<hash>`.

TSuki implementa el algoritmo criptográfico nativo en `buildSapisidHash`:

```kotlin
private fun buildSapisidHash(cookie: String, origin: String = YTM_ORIGIN): String? {
    val sapisid = parseCookieMap(cookie)["SAPISID"] 
        ?: parseCookieMap(cookie)["__Secure-3PAPISID"] 
        ?: return null
    val timestamp = System.currentTimeMillis() / 1000
    val raw = "$timestamp $sapisid $origin"
    val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray(Charsets.UTF_8))
    val hash = digest.joinToString("") { "%02x".format(it) }
    return "${timestamp}_${hash}"
}
```

### Cabeceras Inyectadas en Peticiones Autenticadas
* `Authorization: SAPISIDHASH <timestamp>_<hash>`
* `X-Origin: https://music.youtube.com`
* `Cookie: <cookieString>`
* `X-Goog-Visitor-Id: <visitorData>` (mantiene la consistencia de sesión)

---

## 🔄 Arquitectura de Continuaciones (Paginación Infinita)

Las respuestas de YouTube devuelven listas parciales con un `continuationToken`. Para recuperar catálogos completos (por ejemplo, una playlist de 500 canciones):

1. **Primera Solicitud:** `postBrowse("VL<playlistId>")`. Devuelve las primeras 100 canciones y un token dentro de `musicPlaylistShelfRenderer.continuations`.
2. **Solicitudes Subsecuentes:** `postBrowseContinuation(continuationToken)`.
3. **Parseo Resiliente:** Los tokens de continuación cambian de ubicación según el tipo de respuesta (en `musicShelfContinuation`, `sectionListContinuation` o `appendContinuationItemsAction`). TSuki cuenta con extractores recursivos que analizan el árbol JSON hasta encontrar el token terminal.

---

## 📦 Cacheo Local en Memoria: `MusicHomeMemory`

El feed de inicio de YouTube Music es denso y pesado. Para no descargar megabytes de JSON en cada inicio de la app:
- Los datos se almacenan en `MusicHomeMemory` en RAM.
- Se respaldan en disco en `tsuki_music_personalized.json`.
- **TTL de 6 Horas:** Durante 6 horas, abrir la app presenta el Home de inmediato sin gastar datos. El usuario siempre puede hacer *Pull-to-Refresh* para forzar una recarga.
