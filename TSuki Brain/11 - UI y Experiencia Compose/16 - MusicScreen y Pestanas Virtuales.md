# 11.16 — MusicScreen y Pestañas Virtuales

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla principal de exploración musical basada en YouTube Music (`ui/screens/MusicScreen.kt`):
- **Pastillas de Estado de Ánimo (Mood Chips)**: Filtros superiores interactivos que modifican la cuadrícula de contenido:
  - "Energía" (Workout / Upbeat)
  - "Relax" (Chill / Ambient)
  - "Concentración" (Focus / Lo-Fi / Instrumental)
  - "Fiesta" (Party / Dance)
  - "Triste" (Melancholy / Acoustic)
- **Playlist Virtual `"LM"` (Tus Me Gusta)**:
  - Aparece anclada como la primera tarjeta destacada en la parte superior cuando el usuario ha conectado su cuenta de YouTube Music.
  - Subtítulo dinámico "YouTube Music • N canciones" calculado mediante paginación continua.
- **Caché Persistente en Disco (`MusicHomeMemory`)**:
  - Almacena en memoria RAM y en `tsuki_music_personalized.json` las secciones y listas personalizadas con un tiempo de vida (TTL) de 6 horas.
  - Si el usuario abre la app sin conexión a internet, la pantalla se carga instantáneamente sin pantallas de error ni placeholders vacíos.

**Archivos fuente clave:**
- [`ui/screens/MusicScreen.kt:L1-450`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/MusicScreen.kt#L1-L450)
- [`data/local/MusicHomeMemory.kt:L1-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/MusicHomeMemory.kt#L1-L180)
- [`network/TSukiInnerTubeClient.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Proporciona el punto de entrada diario al universo de YouTube Music, permitiendo descubrir novedades y acceder a las canciones favoritas sin demoras de red.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como una de las cuatro pestañas principales de navegación de la barra inferior.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **ID Virtual `"LM"`**: Jamás enviar el ID literal `"LM"` directamente a la API de navegación estándar `browse` de YouTube. `"LM"` es un identificador virtual reservado para la playlist de Me Gusta (`VLLM`), que requiere la ruta especial `fetchLikedMusicTracks`.
- **TTL de 6 Horas en Caché**: No forzar recargas de red completas en cada cambio de pestaña. Respeta la caché de `MusicHomeMemory` para no saturar la cuota de InnerTube.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Soporte para Pull-to-Refresh (`PullToRefreshBox`) que invalida la caché local y solicita datos frescos de red.

---

## 6. Flujo y conexiones
- Navegación hacia: [[19 - PlaylistDetailScreen y Virtual LM]], [[22 - ChannelScreen y Exploracion de Artistas]].
- Caché respaldada por: `data/local/MusicHomeMemory.kt`.

---

## 7. Guía rápida para una IA nueva
- Para obtener la lista de canciones favoritas en la UI, consulta `MusicHomeMemory.cachedLikedPlaylist`.
