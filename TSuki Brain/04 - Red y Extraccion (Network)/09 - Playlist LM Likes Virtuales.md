# 04.09 — Playlist Virtual "LM" (Tus Me Gusta)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona la lista de canciones favoritas de YouTube Music:
- En YouTube Music, la lista de canciones a las que el usuario dio "Me Gusta" se representa internamente con el identificador especial `"LM"` (*Liked Music*).
- `TSukiInnerTubeClient.fetchLikedMusicTracks("LM")` realiza llamadas autenticadas con las cookies del usuario para extraer esta lista completa mediante paginación.
- En la interfaz de usuario, TSuki sintetiza un objeto virtual `TSukiPlaylist(id = "LM", title = "Tus Me Gusta", ...)` que aparece fijado como primera lista en Home y Biblioteca cuando la sincronización está activa.

**Archivos fuente clave:**
- [`network/TSukiInnerTubeClient.kt:L380-450`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L380-L450)
- [`ui/screens/PlaylistDetailScreen.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/PlaylistDetailScreen.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a los usuarios que inician sesión acceder a su biblioteca de canciones marcadas con corazón de YouTube Music directamente desde la app.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Manejo de colecciones de usuario en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NUNCA enviar `"LM"` al endpoint genérico de playlists `/youtubei/v1/browse` sin la ruta de canciones favoritas. YouTube devuelve error 400. Se debe usar siempre el método especializado `fetchLikedMusicTracks`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Integración dual: sincroniza con el servidor si hay cuenta, o recurre a `FavoritesManager` si no hay sesión.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Invariantes Intocables y Quirks Criticos]], [[19 - PlaylistDetailScreen y Virtual LM]].

---

## 7. Guía rápida para una IA nueva
- En `PlaylistDetailScreen`, verifica siempre `if (playlistId == "LM")` para activar la ruta de carga especializada.
