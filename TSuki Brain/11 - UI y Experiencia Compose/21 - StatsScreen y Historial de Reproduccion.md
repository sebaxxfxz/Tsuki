# 11.21 — StatsScreen y Historial de Reproducción

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla de análisis estadístico de consumo musical (`ui/screens/StatsScreen.kt`):
- **Métricas de Tiempo de Escucha**: Minutos totales reproducidos hoy, esta semana y en el histórico total.
- **Top Artistas y Canciones**: Rankings visuales calculados a partir de los datos registrados en SQLite por `WatchHistoryManager`.
- **Gráficos de Distribución de Géneros**: Visualización en barras proporcionales de los géneros musicales más reproducidos.
- **Resumen Semanal Interactivo (`WeeklyWrappedOverlay`)**: Experiencia visual tipo "Stories" que sintetiza la actividad musical semanal.
- **Exportación de Tarjeta Gráfica**: Botón para generar y compartir el resumen de estadísticas en redes sociales con `StatsShareCard`.

**Archivos fuente clave:**
- [`ui/screens/StatsScreen.kt:L1-380`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/StatsScreen.kt#L1-L380)
- [`data/local/WatchHistoryManager.kt:L1-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/WatchHistoryManager.kt#L1-L260)
- [`ui/player/StatsShareCard.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/StatsShareCard.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Brinda a los usuarios métricas detalladas y privadas sobre sus gustos musicales sin enviar datos a la nube, convirtiendo el historial pasivo en una experiencia reflexiva y compartible.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como una pantalla de valor añadido accesible desde el perfil o los ajustes.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Consulta de Sesiones de Escucha**: La consulta SQL para contabilizar sesiones de escucha únicas debe respetar la ventana de agrupación de 5 minutos: `COUNT(DISTINCT video_id || '_' || (timestamp/300000))`. No reemplazar por `COUNT(1)` simple porque inflaría artificialmente las estadísticas en saltos rápidos.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Carga asíncrona de datos pesados de la base de datos en `Dispatchers.IO` para evitar congelamientos en pantallas con miles de registros históricos.

---

## 6. Flujo y conexiones
- Consume datos de: [[06 - WatchHistoryManager Doble Tabla|06.02 - WatchHistoryManager]].
- Exporta mediante: [[14 - ShareCards PixelCopy Stats]].

---

## 7. Guía rápida para una IA nueva
- Para recalcular las estadísticas tras borrar el historial, invoca `refreshStats()`.
