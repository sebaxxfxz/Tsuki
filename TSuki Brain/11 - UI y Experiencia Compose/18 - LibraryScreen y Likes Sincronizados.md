# 11.18 — LibraryScreen y Likes Sincronizados

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla de gestión integral de la biblioteca musical del usuario (`ui/screens/LibraryScreen.kt`):
- **Sección Superior de Me Gusta Sincronizados**:
  - Si `syncLikedEnabled` está activo en ajustes, posiciona en la cúspide de la biblioteca la tarjeta "Tus Me Gusta".
  - Muestra el conteo exacto de canciones unificado entre Me Gusta locales (`FavoritesManager`) y canciones marcadas con pulgar arriba en YouTube Music.
- **Categorías de Navegación Rápida**: Filtros por chips para alternar entre Listas de Reproducción, Artistas Seguidos, Álbumes Guardados, Canciones Descargadas y Archivos de Audio Locales del dispositivo.
- **Integración con SQLite Local**: Consume listas locales gestionadas por `LocalPlaylistManager` (`tsuki_playlists.db`) y listas de YouTube Music vinculadas a la cuenta.
- **Acceso Directo al Asistente de Importación**: Botón flotante para abrir `ImportPlaylistScreen` e importar listas desde Spotify o Takeout.

**Archivos fuente clave:**
- [`ui/screens/LibraryScreen.kt:L1-480`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/LibraryScreen.kt#L1-L480)
- [`data/local/LocalPlaylistManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/LocalPlaylistManager.kt)
- [`data/local/FavoritesManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/FavoritesManager.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Centraliza todo el patrimonio musical del usuario en una vista unificada y ordenada, permitiendo alternar de forma transparente entre archivos descargados en el teléfono y millones de pistas en la nube.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como pestaña fundamental de navegación de la barra inferior.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Posición de Likes arriba**: La tarjeta "Tus Me Gusta" debe permanecer en la parte superior si `syncLikedEnabled` es verdadero; los usuarios dependen de este acceso rápido para su reproducción diaria.
- **Lectura Reactiva de Playlists**: La lista debe recolectar los cambios mediante Flow desde `LocalPlaylistManager` para que cualquier lista creada o eliminada se actualice de inmediato sin recargar la pantalla.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Tarjetas de lista estilizadas con carátulas en cuadrícula de 4 miniaturas y texto con contraste adaptativo.

---

## 6. Flujo y conexiones
- Navegación hacia: [[19 - PlaylistDetailScreen y Virtual LM]], [[23 - ImportPlaylistScreen y Asistente de Migracion]].

---

## 7. Guía rápida para una IA nueva
- Para consultar las listas locales creadas por el usuario, inyecta `LocalPlaylistManager`.
