# 11.23 — ImportPlaylistScreen y Asistente de Migración

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Asistente integral para importar y migrar listas de reproducción desde múltiples plataformas externas (`ui/screens/ImportPlaylistScreen.kt`):
- **Soporte Multiformato de Importación**:
  - Archivos ZIP de copia de seguridad de ArchiveTune (extracción y parseo de `song.db` SQLite).
  - Archivos CSV y JSON de Spotify (generados mediante Exportify o Google Takeout).
  - Listas de reproducción estándar en formato `.M3U` y `.M3U8`.
  - Base de datos local SQLite directa.
- **Motor de Resolución Musical (`ImportSongResolver`)**:
  - Busca cada pista en YouTube Music mediante coincidencia difusa (*Fuzzy Matching*) evaluando similitud de título, artista y duración.
- **Barra de Progreso y Reporte de Coincidencias**: Muestra el porcentaje completado, el total de canciones emparejadas con éxito y la lista de canciones que no pudieron ser encontradas para revisión manual.
- **Destino Dual**: Permite guardar la lista resultante en la base de datos local de TSuki o clonarla directamente como una lista privada en la cuenta de YouTube Music del usuario.

**Archivos fuente clave:**
- [`ui/screens/ImportPlaylistScreen.kt:L1-530`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/ImportPlaylistScreen.kt#L1-L530)
- [`playlistimport/ImportSongResolver.kt:L1-210`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/ImportSongResolver.kt#L1-L210)
- [`playlistimport/ArchiveTuneBackupParser.kt:L1-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/ArchiveTuneBackupParser.kt#L1-L180)

---

## 2. PARA QUÉ existe (problema que resuelve)
Rompe el efecto "jardín vallado" de Spotify y otras plataformas privativas, permitiendo a los usuarios migrar años de listas personales a TSuki en cuestión de minutos sin perder sus canciones favoritas.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` ofreciendo un flujo guiado paso a paso para la selección y resolución de archivos.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Umbral de Coincidencia Difusa**: El umbral de similitud en `FuzzyMatcher` debe mantenerse por encima de `0.72`. Bajar este umbral provoca que canciones incorrectas (covers o versiones en vivo) se asignen a temas de estudio.
- **Chunking al Crear en YTM**: Al sincronizar con YouTube Music, respetar los lotes de 12 canciones con retardo de 60ms entre llamadas para evitar bloqueos por tasa de peticiones (Rate Limiting).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Ejecución en corrutinas ligadas al ciclo de vida con notificación flotante de advertencia si el usuario intenta cerrar la app durante una importación masiva.

---

## 6. Flujo y conexiones
- Respaldado por todo el módulo: [[01 - Parsers Multiplataforma y ArchiveTune|10.01 - Parsers]], [[07 - ImportSongResolver Semaphore 3|10.03 - ImportSongResolver]].

---

## 7. Guía rápida para una IA nueva
- Para procesar un archivo seleccionado por el usuario mediante el selector del sistema (`ActivityResultContracts.GetContent()`), pasa el URI a `ImportSongResolver.processUri(uri)`.
