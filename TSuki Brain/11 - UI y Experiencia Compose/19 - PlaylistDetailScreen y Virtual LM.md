# 11.19 — PlaylistDetailScreen y Virtual LM

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla de detalle para visualizar y reproducir cualquier lista de reproducción o álbum (`ui/screens/PlaylistDetailScreen.kt`):
- **Ruta Crítica Virtual `"LM"`**:
  - Si el parámetro `playlistId == "LM"`, redirige la obtención de pistas hacia `TSukiInnerTubeClient.fetchLikedMusicTracks("LM")`.
  - Ejecuta el bucle de paginación exhaustivo para traer todas las páginas de canciones sin detenerse en los primeros 100 elementos.
- **Cabecera Colapsable con Desenfoque Dinámico**:
  - Al hacer scroll vertical, la carátula grande y los títulos se encogen suavemente transformándose en una barra superior compacta (`LargeTopAppBar` colapsable).
- **Acciones Rápidas Masivas**:
  - Botón "Reproducir Todo" y "Modo Aleatorio".
  - Botón "Descargar Todo": Pasa la lista completa a `DownloadEngine` en segundo plano.
  - Opciones de compartir enlace de lista o duplicar como lista local editable.
- **Buscador Interno**: Barra de filtro rápido para buscar canciones dentro de la misma lista por título o artista.

**Archivos fuente clave:**
- [`ui/screens/PlaylistDetailScreen.kt:L1-580`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/PlaylistDetailScreen.kt#L1-L580)
- [`network/TSukiInnerTubeClient.kt:L600-750`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L600-L750)
- [`playback/DownloadEngine.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/DownloadEngine.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite inspeccionar a fondo el contenido de álbumes y listas tanto locales como de YouTube Music, resolviendo el problema de truncamiento de listas grandes gracias a la paginación de continuaciones.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como pantalla de destino al presionar cualquier tarjeta de álbum o playlist.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Ruta `"LM"`**: Nunca intentar invocar `fetchPlaylistTracks("LM")` usando el endpoint estándar de navegación general de InnerTube. Fallará con error 404 o lista vacía; la lista `"LM"` requiere cookies de autenticación activas y la ruta especializada de Me Gusta.
- **Descarga en Lote**: No disparar más de 3 descargas simultáneas en paralelo para evitar saturar el CPU y la red del dispositivo.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Elementos de lista renderizados con `TrackComponents.kt` usando claves únicas basadas en el índice y el ID.

---

## 6. Flujo y conexiones
- Conectado a: [[01 - TSukiInnerTubeClient y Autenticacion|04.01 - TSukiInnerTubeClient]], [[15 - DownloadEngine Paralelo y Mux|03.08 - DownloadEngine]].

---

## 7. Guía rápida para una IA nueva
- Para cargar las canciones de una lista, llama a `loadTracks(playlistId)` y observa el estado en `uiState`.
