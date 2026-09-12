# 06.10 — LocalAudioScanner (Escáner de Audio Local en MediaStore)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Explora la biblioteca multimedia física del dispositivo utilizando el proveedor de contenidos `MediaStore` de Android en `data/local/LocalAudioScanner.kt`:
- Consulta `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
- **Filtros Estrictos de Música**:
  - `IS_MUSIC != 0`
  - `IS_NOTIFICATION = 0`, `IS_ALARM = 0`, `IS_RINGTONE = 0`, `IS_PODCAST = 0`, `IS_RECORDING = 0`.
  - Duración mínima: `MIN_MUSIC_DURATION_MS = 30_000L` (ignora efectos de sonido cortos de menos de 30s).
  - Exclusión de carpetas de mensajería: regex `Regex("/(WhatsApp|Telegram|com\\.whatsapp|Media/WhatsApp[A-Za-z ]*)/", RegexOption.IGNORE_CASE)`.
- Mapea los archivos a instancias de `MediaTrack` con `isLocal = true` y `mediaType = MediaType.LOCAL_AUDIO`.

**Archivos fuente clave:**
- [`data/local/LocalAudioScanner.kt:L12-124`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/LocalAudioScanner.kt#L12-L124)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite usar TSuki como reproductor de música local completo para MP3, FLAC, M4A o WAV almacenados en el teléfono sin que aparezcan notas de voz personales o audios de grupos de WhatsApp.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Acceso a archivos del dispositivo en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El regex de exclusión de chats y la duración mínima de 30 segundos.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Agrupación opcional por carpetas mediante `listAudioFolders()`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[18 - LibraryScreen y Likes Sincronizados]], [[10 - Modelos de Dominio MediaTrack y Lyrics]].

---

## 7. Guía rápida para una IA nueva
- Para obtener la lista de pistas locales, ejecuta `LocalAudioScanner(context).scanLocalTracks()`.
