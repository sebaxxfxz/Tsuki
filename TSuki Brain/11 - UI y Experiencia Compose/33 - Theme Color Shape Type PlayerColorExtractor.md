# 11.33 — Theme Color Shape Type PlayerColorExtractor

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Paquete fundamental de tematización y estilización visual de la aplicación (`ui/theme/`):
- **`Theme.kt`**: Composable raíz `TSukiTheme` que aplica el esquema de colores dinámicos (Android 12+ Monet vía `dynamicDarkColorScheme`), modo oscuro permanente o modo negro puro OLED según las preferencias.
- **`Color.kt`**: Paleta maestra de colores primarios, secundarios, terciarios, contenedores y tonos tonales neutros.
- **`Shape.kt`**: Definición de radios de curvatura para esquinas (pequeñas 8dp, medianas 16dp, grandes 24dp, extra grandes 28dp y circulares).
- **`Type.kt`**: Escala tipográfica completa basada en Google Fonts con soporte para pesos Regular, Medium, SemiBold y Bold.
- **`PlayerColorExtractor.kt`**: Extractor de color dominante y color de acento a partir del Bitmap de la carátula utilizando la librería `Palette` de AndroidX con optimización de memoria.

**Archivos fuente clave:**
- [`ui/theme/Theme.kt:L1-150`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Theme.kt#L1-L150)
- [`ui/theme/Color.kt:L1-100`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Color.kt#L1-L100)
- [`ui/theme/Shape.kt:L1-50`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Shape.kt#L1-L50)
- [`ui/theme/Type.kt:L1-60`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/Type.kt#L1-L60)
- [`ui/theme/PlayerColorExtractor.kt:L1-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/theme/PlayerColorExtractor.kt#L1-L120)

---

## 2. PARA QUÉ existe (problema que resuelve)
Provee la base de diseño responsiva, accesible y expresiva requerida por todas las vistas de la aplicación.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/theme/` siguiendo la convención estándar de Jetpack Compose en Android.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **`allowHardware(false)` en Carga de Coil para Paleta**: **INVARIANTE CRÍTICO**: Al cargar una imagen en Coil con el propósito de extraer la paleta de colores con `PlayerColorExtractor`, la petición **DEBE configurar obligatoriamente `allowHardware(false)`**. Si se pasa un `HardwareBitmap` de GPU a la librería `Palette`, Android lanzará un `IllegalArgumentException: Software rendering doesn't support hardware bitmaps` que provocará un crash instantáneo.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Caché en memoria de los colores extraídos de las últimas 50 canciones para no recalcular la paleta al volver a reproducir un tema reciente.

---

## 6. Flujo y conexiones
- Alimenta a: [[08 - PlayerBackgroundV9 Gradiente]], [[05 - MusicPlayerScreenV9 Orquestador 1466L]], [[01 - Sistema de Diseno Material 3 Expressive]].

---

## 7. Guía rápida para una IA nueva
- Si necesitas extraer colores de una URL de carátula, usa `PlayerColorExtractor.extractColors(context, imageUrl)`.
