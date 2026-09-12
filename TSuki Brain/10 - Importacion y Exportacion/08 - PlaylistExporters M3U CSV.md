# 10.08 — PlaylistExporters (Generación de Archivos M3U8 y CSV)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Genera archivos de listas de reproducción estándar a partir de las listas locales de TSuki en `playlistimport/PlaylistExporters.kt`:
1. **Exportación a M3U8 (`exportToM3u`)**:
   - Genera cabecera `#EXTM3U`.
   - Para cada pista: `#EXTINF:{durationSeconds},{artist} - {title}` seguido de la URL directa de YouTube Music (`https://music.youtube.com/watch?v={id}`) o ruta local.
2. **Exportación a CSV (`exportToCsv`)**:
   - Genera cabecera: `Title,Artist,Album,Duration (ms),Video ID`.
   - Aplica escape de comillas RFC 4180 (`csvEscape`) a textos con comas o saltos de línea.

**Archivos fuente clave:**
- [`playlistimport/PlaylistExporters.kt:L1-36`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/PlaylistExporters.kt#L1-L36)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza la portabilidad inversa: el usuario es dueño absoluto de sus listas y puede exportarlas en cualquier momento para usarlas en reproductores de escritorio o plataformas externas.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Generadores de formatos de exportación en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El formato de escape de comillas en CSV (`value.replace(""", """")`).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Accesible desde el menú de tres puntos en `PlaylistDetailScreen`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[19 - PlaylistDetailScreen y Virtual LM]], [[03 - PlaylistParsers CSV M3U ZIP]].

---

## 7. Guía rápida para una IA nueva
- Para exportar una lista a texto M3U: `PlaylistExporters.exportToM3u(tracks)`.
