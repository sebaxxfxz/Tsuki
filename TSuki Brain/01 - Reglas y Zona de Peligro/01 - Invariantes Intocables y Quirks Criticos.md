# 01.01 — Invariantes Intocables y Quirks Críticos

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Compila la lista negra de invariantes técnicos y soluciones a problemas no documentados ("quirks") que fueron descubiertos y pagados con horas de depuración en el hardware real:

1. **`overrideLibrary` para `lyrics-ui`**:
   - En `app/src/main/AndroidManifest.xml`: `<uses-sdk tools:overrideLibrary="com.mocharealm.accompanist.lyrics.ui" />`.
   - La librería declara internamente `minSdk 29`. TSuki soporta `minSdk 24`. No remover este override ni aumentar `minSdk` del proyecto sin pruebas en `fb74ec96`.
2. **Icono Pequeño de Notificación Media3**:
   - Media3 1.5.1 `DefaultMediaNotificationProvider` no expone `setSmallIcon(...)` en su builder.
   - La solución probada consiste en sobreescribir el recurso del sistema en `app/src/main/res/drawable/media3_notification_small_icon.xml` con un vector monocromo blanco. No intentar usar builders inexistentes.
3. **Paginación en YouTube Music (InnerTube)**:
   - `TSukiInnerTubeClient.fetchPlaylistTracks` debe ejecutar el bucle de continuación sobre `musicPlaylistShelfRenderer.continuations`, `musicShelfContinuation`, `sectionListContinuation` y `appendContinuationItemsAction`. Sin este bucle, listas de más de 100 elementos fallan silenciosamente truncando resultados.
4. **Creación de Playlists en YouTube Music**:
   - `createYTMPlaylist` requiere crear la playlist como `PRIVATE` con hasta 50 canciones iniciales, y añadir las restantes en lotes de 12 temas con `delay(60ms)` entre chunks para evitar el baneo por abuso de API. Si el usuario sale de la app, el bucle se cancela.
5. **Captura PixelCopy en Diálogos**:
   - `GraphicsLayer.toImageBitmap()` genera imágenes totalmente negras en este stack de Compose. La captura gráfica (para compartir estadísticas o reproductor a redes) DEBE ejecutarse mediante `PixelCopy.request` extrayendo la ventana nativa `DialogWindowProvider.window` antes de cerrar el diálogo.
6. **M3WavySlider Fracción 0..1**:
   - El parámetro `value` de `M3WavySlider` recibe obligatoriamente una fracción normalizada entre 0.0f y 1.0f. Pasar milisegundos llena la barra al 100% y bloquea el desplazamiento.
7. **Conteo de Sesiones en Historial**:
   - `WatchHistoryManager` emite ticks de escucha cada 30 segundos. Para contar reproducciones reales, se DEBE calcular `COUNT(DISTINCT video_id || '_' || (timestamp/300000))`. Hacer `COUNT(1)` inflará las estadísticas de forma fraudulenta.
8. **Playlist Virtual "LM" (Tus Me Gusta)**:
   - "LM" es un identificador virtual interno. Enviar "LM" a los endpoints generales de browse de YouTube Music provoca error 400. Se debe interceptar por prefijo y llamar a `fetchLikedMusicTracks("LM")`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Evita que cualquier desarrollador o IA revierta soluciones técnicas maduras creyendo erróneamente que son errores de sintaxis o código redundante.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `01 - Reglas y Zona de Peligro` como documento de máxima autoridad preventiva.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No modificar ninguno de los 8 quirks listados arriba sin autorización explícita y pruebas directas en el terminal físico `fb74ec96`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Revisar esta ficha antes de alterar cualquier archivo en `playback/`, `network/`, `lyrics/` o `ui/`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[05 - Contratos que Nunca se Rompen]], [[08 - Bugs Ya Pagados Historial]], [[05 - MusicPlayerScreenV9 Orquestador 1466L]].

---

## 7. Guía rápida para una IA nueva
- Si encuentras un bloque de código inusual en los archivos citados, asume que resuelve uno de estos 8 quirks antes de refactorizar.
