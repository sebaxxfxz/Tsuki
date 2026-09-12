# 04.05 — SAPISIDHASH y Autenticación con YouTube

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa el algoritmo criptográfico de autenticación de Google para llamadas a InnerTube:
- Extrae la cookie `SAPISID` (o `__Secure-3PAPISID`) almacenada en `YouTubeAuthManager`.
- Genera la cabecera `Authorization: SAPISIDHASH {timestamp}_{hash}` donde:
  - `timestamp`: Segundos transcurridos desde Epoch (`System.currentTimeMillis() / 1000`).
  - `hash`: Resumen SHA-1 en formato hexadecimal de la concatenación: `"$timestamp $sapisid $origin"`, donde `origin` es `https://music.youtube.com`.
- Permite realizar peticiones privadas en nombre del usuario autenticado para consultar "Tus Me Gusta", playlists personales y modificar suscripciones.

**Archivos fuente clave:**
- [`auth/YouTubeAuthManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/auth/YouTubeAuthManager.kt)
- [`network/TSukiInnerTubeClient.kt:L110-140`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L110-L140)

---

## 2. PARA QUÉ existe (problema que resuelve)
Google rechaza peticiones que envíen cookies de sesión sin la firma criptográfica `SAPISIDHASH` correspondiente, devolviendo HTTP 401 Unauthorized.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Capa de autenticación y red en `network/` y `auth/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La fórmula de hash: `"$timestamp $sapisid $origin"` con espacio simple de separación.
- El origen `https://music.youtube.com` para peticiones de YouTube Music.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Si el usuario no ha iniciado sesión, las llamadas omiten `Authorization` y operan en modo anónimo público.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - TSukiInnerTubeClient y Autenticacion]], [[09 - Playlist LM Likes Virtuales]].

---

## 7. Guía rápida para una IA nueva
- Para realizar una llamada autenticada, inyecta `authManager.getSapisidHash()` en las cabeceras de OkHttp.
