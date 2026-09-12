# 05.12 — KaraokeWordByWord vs KaraokeLyricRow (Modos de Renderizado)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Compara y documenta las dos vistas de renderizado lírico en TSuki:
1. **`KaraokeWordByWordView.kt` (Palabra por Palabra)**:
   - Se activa cuando la letra contiene marcas silábicas (`hasWordSyncedLine() == true`).
   - Envuelve el componente `KaraokeLyricsView` de `lyrics-ui`.
   - Ilumina cada sílaba individualmente con un barrido suave de gradiente de color mientras el cantante vocaliza cada sonido.
   - Aplica efecto de desenfoque (*Blur*) a las líneas anteriores y posteriores en Android 12+ (`RenderEffect.createBlurEffect`).
2. **`KaraokeLyricRow.kt` / `SyncedLyricsView.kt` (Por Línea Estándar)**:
   - Se activa para letras LRC estándar.
   - Resalta la línea activa completa en color primario con aumento de escala y opacidad 1.0f.
   - Atenúa las líneas no activas al 40% de opacidad y tamaño reducido.
   - Permite tocar cualquier línea para hacer `seekTo` instantáneo al minuto exacto de esa estrofa.

**Archivos fuente clave:**
- [`ui/player/lyrics/KaraokeWordByWordView.kt:L1-94`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/lyrics/KaraokeWordByWordView.kt#L1-L94)
- [`ui/player/lyrics/KaraokeLyricRow.kt:L1-119`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/lyrics/KaraokeLyricRow.kt#L1-L119)
- [`ui/player/SyncedLyricsView.kt:L1-176`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/SyncedLyricsView.kt#L1-L176)

---

## 2. PARA QUÉ existe (problema que resuelve)
Provee una experiencia visual adaptativa: la máxima vistosidad posible cuando hay datos silábicos de estudio, y una navegación lírica por estrofas sólida cuando solo hay LRC.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Componentes de presentación visual en `ui/player/lyrics/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El condicional de enrutamiento en `SyncedLyricsView.kt:55`: `if (lyrics.hasWordSyncedLine()) { KaraokeWordByWordView(...) } else { ... }`.
- La suspensión de auto-scroll de 3 segundos ante toque del usuario para permitir lectura manual sin tirones.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Soporte para tamaño de fuente dinámico configurable desde los ajustes del reproductor.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[03 - Renderizado Visual y Smooth Position]], [[07 - Grupo Word-Sync Paxsenix Apple Spotify]].

---

## 7. Guía rápida para una IA nueva
- Para modificar la animación de las letras silábicas, revisa `KaraokeWordByWordView.kt`.
