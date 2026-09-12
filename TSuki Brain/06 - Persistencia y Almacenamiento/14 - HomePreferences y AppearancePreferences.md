# 06.14 — HomePreferences y AppearancePreferences (Ajustes Visuales y Home)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la personalización estética y la disposición visual de la app:
1. **`AppearancePreferences.kt`**:
   - `darkThemeMode`: `FOLLOW_SYSTEM`, `ALWAYS_DARK`, `ALWAYS_LIGHT`.
   - `pureBlackOled`: Booleano para fondos AMOLED `#000000` puro.
   - `materialYouEnabled`: Booleano para usar la paleta de colores dinámicos del fondo de pantalla de Android 12+.
   - `cornerRoundingDp`: Radio de redondeo de carátulas (8dp a 28dp).
2. **`HomePreferences.kt`**:
   - `layoutMode`: `FULL_WIDTH` (tarjetas grandes), `COMPACT` (filas condensadas), `GRID` (rejilla 2 columnas).
   - `blockedChannels`: Conjunto de IDs de canales que el usuario no desea ver en sus recomendaciones.
   - `hiddenHomeSections`: Filtro de secciones del feed de inicio.

**Archivos fuente clave:**
- [`data/local/AppearancePreferences.kt:L29-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/AppearancePreferences.kt#L29-L120)
- [`data/local/HomePreferences.kt:L26-110`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/HomePreferences.kt#L26-L110)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite al usuario adaptar la estética de la app a sus gustos personales y ahorrar energía en pantallas OLED.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Ajustes visuales en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Las claves de DataStore `pref_pure_black_oled` y `pref_material_you`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Inyectadas en el tema global `TSukiTheme` en `ui/theme/Theme.kt`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[33 - Theme Color Shape Type PlayerColorExtractor]], [[17 - HomeScreen y Secciones Personalizadas]].

---

## 7. Guía rápida para una IA nueva
- Para aplicar el tema oscuro, observa `appearancePreferences.darkThemeMode`.
