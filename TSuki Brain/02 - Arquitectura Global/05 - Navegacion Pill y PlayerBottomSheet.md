# 02.05 — Navegación Pill y PlayerBottomSheet

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Documenta el sistema de navegación de la aplicación:
- **Barra de Navegación Flotante (`TSukiPillNavBar.kt`)**: Píldora cilíndrica elevada (`CircleShape`, 56dp) que conmuta entre las secciones principales: Música (Home YTM), Explorar (Videos), Biblioteca (Playlists/Descargas) y Ajustes.
- **Contenedor Inferior de 3 Niveles (`PlayerBottomSheet.kt`)**:
  - `DISMISSED` (0): Oculto cuando no hay reproducción.
  - `COLLAPSED` (1): MiniPlayer visible sobre la barra de navegación.
  - `EXPANDED` (2): FullPlayer (`MusicPlayerScreenV9`) a pantalla completa.
- **Navegación sin Fragmentos**: Todo el árbol visual se monta dentro de `MainActivity` utilizando estado declarativo en Compose, simplificando la gestión de backstack y transiciones.

**Archivos fuente clave:**
- [`MainActivity.kt:L120-250`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/MainActivity.kt#L120-L250)
- [`ui/components/TSukiPillNavBar.kt:L1-161`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/TSukiPillNavBar.kt#L1-L161)
- [`ui/player/PlayerBottomSheet.kt:L1-298`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/PlayerBottomSheet.kt#L1-L298)

---

## 2. PARA QUÉ existe (problema que resuelve)
Provee una navegación moderna, fluida y gestual que mantiene la música siempre accesible mientras el usuario explora cualquier rincón de la app.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Diseño estructural en `02 - Arquitectura Global`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No romper la conexión `NestedScrollConnection` en `PlayerBottomSheet.kt`: permite que el scroll de las letras o la cola se transfiera de forma natural al cierre del sheet.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Transiciones fluidas gobernadas por resortes de `M3MotionTokens`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Arquitectura del Reproductor y MiniPlayer]], [[10 - PlayerBottomSheet Anclas 3 Niveles]].

---

## 7. Guía rápida para una IA nueva
- Para abrir una pantalla secundaria (ej. detalle de playlist), actualiza el estado de navegación en `MainActivity`.
