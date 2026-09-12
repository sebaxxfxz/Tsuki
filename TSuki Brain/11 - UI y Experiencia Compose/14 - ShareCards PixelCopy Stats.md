# 11.14 — ShareCards PixelCopy Stats

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Genera las tarjetas gráficas de alta definición diseñadas para compartirse en redes sociales (`ui/player/ShareCardView.kt` y `ui/player/StatsShareCard.kt`):
- **`ShareCardView` (Canción Actual)**:
  - Formato vertical optimizado para Instagram Stories (relación 9:16).
  - Fondo desenfocado con gradiente generado a partir de la carátula de la pista.
  - Carátula central con bordes redondeados y sombra 3D realista.
  - Título del tema, artista, barra de reproducción estética y logotipo distintivo de TSuki.
- **`StatsShareCard` (Estadísticas y Resumen)**:
  - Tarjeta de resumen de minutos escuchados, artista #1, top 5 pistas y género favorito.
  - Formato estilo "Wrapped" anual o semanal (`WeeklyWrappedOverlay`).
- **Pipeline de Exportación**: Renderizado fuera de pantalla, captura mediante `PixelCopy` y envío mediante el selector estándar de compartir de Android.

**Archivos fuente clave:**
- [`ui/player/ShareCardView.kt:L1-280`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/ShareCardView.kt#L1-L280)
- [`ui/player/StatsShareCard.kt:L1-220`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/StatsShareCard.kt#L1-L220)
- [`ui/components/WeeklyWrappedOverlay.kt:L1-190`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/WeeklyWrappedOverlay.kt#L1-L190)

---

## 2. PARA QUÉ existe (problema que resuelve)
Brinda una herramienta de marketing viral y expresión comunitaria, permitiendo a los usuarios compartir su música favorita con una calidad visual inigualable.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/player/` y `ui/components/` estrechamente ligado a las pantallas de reproducción y estadísticas.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Prohibido `GraphicsLayer.toImageBitmap()`**: No sustituir el flujo de `PixelCopy` por la API de captura interna de Compose, ya que falla en la mayoría de terminales produciendo imágenes negras.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Guardado en archivo temporal mediante `Bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)` y URI generado con `FileProvider.getUriForFile`.

---

## 6. Flujo y conexiones
- Explicación técnica de captura en: [[04 - Captura PixelCopy para Redes]].
- Orquestado desde: [[05 - MusicPlayerScreenV9 Orquestador 1466L]] y [[21 - StatsScreen y Historial de Reproduccion]].

---

## 7. Guía rápida para una IA nueva
- Para añadir nuevos datos a la tarjeta de estadísticas, edita `StatsShareCard.kt` manteniendo la relación de aspecto 9:16.
