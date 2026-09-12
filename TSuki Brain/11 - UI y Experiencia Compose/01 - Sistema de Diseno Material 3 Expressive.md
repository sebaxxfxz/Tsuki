# 11.01 — Sistema de Diseño Material 3 Expressive

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Establece las directrices y tokens del sistema visual de TSuki bajo los principios de **Material 3 Expressive**:
- **Tokens de Color Semánticos (48 Roles M3)**: `MaterialTheme.colorScheme.primary`, `surfaceContainer`, `surfaceContainerHigh`, `secondaryContainer`, `onSurfaceVariant`, etc. Prohíbe totalmente el uso de colores hexadecimales hardcodeados (`Color(0xFF...)`) en layouts de pantalla.
- **Jerarquía de Formas Adaptativas**: Esquinas redondeadas orgánicas (`Shape.kt`) que varían según el contexto (tarjetas `16dp` / `24dp`, pastillas `50.dp` / `CircleShape`, hojas modales con esquinas superiores `28dp`).
- **Sistema de Tipografía Dinámica**: Escala tipográfica estricta definida en `Type.kt` (`headlineLarge`, `titleMedium`, `bodyMedium`, `labelSmall`) con legibilidad garantizada para pantallas OLED y modo exterior.
- **Física de Movimiento con Springs**: Ausencia de transiciones lineales mecánicas. Toda animación de interacción usa resortes físicos configurados en `M3Motion.kt` con amortiguamiento natural (`DampingRatioNoBouncy` o `DampingRatioLowBouncy`).

**Archivos fuente clave:**
- [`ui/theme/Theme.kt:L1-150`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Theme.kt#L1-L150)
- [`ui/theme/Color.kt:L1-100`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Color.kt#L1-L100)
- [`ui/theme/Shape.kt:L1-50`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Shape.kt#L1-L50)
- [`ui/theme/Type.kt:L1-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Type.kt#L1-L60)
- [`ui/components/M3Motion.kt:L1-85`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/M3Motion.kt#L1-L85)

---

## 2. PARA QUÉ existe (problema que resuelve)
Evita la fragmentación visual y la apariencia de "UI amateur/genérica". Dota a TSuki de una identidad visual de nivel premium comparable a las mejores aplicaciones de música globales (Spotify, Apple Music, Tidal), manteniendo soporte nativo para temas dinámicos (Android 12+ Monet) y modo negro puro OLED.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Se ubica en `ui/theme/` y `ui/components/` como el cimiento estructural sobre el cual se construyen todas las pantallas y diálogos de la aplicación.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Cero colores directos en Composables**: Nunca escribas `Color.White`, `Color.Black`, o `Color(0xFF121212)` en pantallas. Debes usar siempre `MaterialTheme.colorScheme.*`.
- **Modo OLED Negro Puro**: Cuando `pureBlackOled` está habilitado en `AppearancePreferences`, los contenedores de fondo deben forzar `#000000` absoluto para apagar los píxeles OLED; no sobrescribir esto con grises oscuros.
- **MotionScheme Springs**: Prohibido usar `tween(durationMillis = 300, easing = LinearEasing)` para gestos interactivos. Las animaciones deben ser interrumpibles mediante resortes físicos.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- **Tokens M3 de Contenedores**: Usar `surfaceContainerLow`, `surfaceContainer`, y `surfaceContainerHigh` para crear elevación tonal sin sombras artificiales.
- **Acciones Táctiles**: Usar `FilledTonalIconButton` o `IconButton` envuelto en pastillas tonales de 36-44dp para garantizar áreas táctiles accesibles según el estándar WCAG (mínimo 48x48dp de toque efectivo).
- **Cero comentarios**: Prohibido añadir comentarios `//` o `/** */` en `Theme.kt`, `Color.kt`, etc.

---

## 6. Flujo y conexiones
- Dependencias directas: [[33 - Theme Color Shape Type PlayerColorExtractor]], [[32 - M3Motion Tokens Springs Sliders]].
- Aplicación transversal en todo el árbol de Compose: [[05 - MusicPlayerScreenV9 Orquestador 1466L]], [[16 - MusicScreen y Pestanas Virtuales]], [[35 - Componentes Core (MiniPlayer, TSukiPillNavBar, FastScrollBox, Sheets)]].

---

## 7. Guía rápida para una IA nueva
- Para obtener el color de acento actual de la canción en reproducción, lee `dominantColor` o `accentColor` extraído por `PlayerColorExtractor` y combínalo con `MaterialTheme.colorScheme.onSurface`.
- Si necesitas un resorte suave para una tarjeta que se expande, usa `M3Motion.SpringStandard`.
