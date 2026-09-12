# 01.08 — Bugs Ya Pagados (Historial de Lecciones)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Documenta el catálogo retrospectivo de bugs severos sufridos en el proyecto, su causa raíz y la solución definitiva adoptada:

1. **Bug del WavySlider Congelado al Final**:
   - *Causa*: Se pasaban milisegundos (`positionMs`) al parámetro `value` de `M3WavySlider`. Al esperar un valor 0..1, cualquier canción de más de 1 segundo llenaba la barra al máximo.
   - *Solución*: Pasar siempre la fracción calculada `(positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)`.
2. **Bug de Estadísticas Infladas (COUNT 1 vs Sesiones)**:
   - *Causa*: `WatchHistoryManager` emite eventos de escucha periódicos cada 30 segundos mientras la pista suena. Un `COUNT(1)` contaba los pings en vez de las canciones escuchadas.
   - *Solución*: Agrupar por bloques de 5 minutos mediante `COUNT(DISTINCT video_id || '_' || (timestamp/300000))`.
3. **Bug de Listas Truncadas en YouTube Music**:
   - *Causa*: La API de InnerTube devuelve únicamente los primeros ~100 elementos en la primera respuesta.
   - *Solución*: Implementar el bucle de continuaciones exhaustivo en `TSukiInnerTubeClient.kt`.
4. **Bug de Capturas Negras con GraphicsLayer**:
   - *Causa*: En Compose con Compose BOM 2026 y RenderNode acelerado por hardware, `GraphicsLayer.toImageBitmap()` produce un mapa de bits negro.
   - *Solución*: Usar `PixelCopy.request` sobre la ventana nativa de un `Dialog`.
5. **Bug de Claves Duplicadas en la Cola (`QueueListV9`)**:
   - *Causa*: Usar `key = { it.id }` en `LazyColumn` crasheaba si una canción estaba repetida en la lista.
   - *Solución*: Calcular claves compuestas únicas con el índice de ocurrencia: `"${item.id}_#$count"`.
6. **Bug de Bitmaps de Hardware en Palette**:
   - *Causa*: Coil carga por defecto imágenes en memoria GPU (`HardwareBitmap`), lo que causa un crash cuando `Palette.from(bitmap)` intenta leer píxeles en CPU.
   - *Solución*: Forzar `allowHardware(false)` en toda petición de carátula destinada a extracción de color.
7. **Bug de Desconexión de Audio en Video DASH**:
   - *Causa*: ExoPlayer cargaba streams DASH de video sin la pista de audio Opus sincronizada.
   - *Solución*: Implementación de `MergingMediaSource` en `TSukiPlaybackService.kt`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Evita el ciclo destructivo donde un desarrollador o IA reintroduce un error antiguo por desconocimiento del histórico.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Memoria histórica de estabilidad en `01 - Reglas y Zona de Peligro`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No revertir ninguna de las 7 soluciones descritas.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Si se detecta un nuevo bug complejo, se documenta de inmediato en esta ficha.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Invariantes Intocables y Quirks Criticos]], [[06 - PlayerControlsV9 Pastillas y Slider]], [[04 - Captura PixelCopy para Redes]].

---

## 7. Guía rápida para una IA nueva
- Consulta esta lista antes de tocar el scrubber, la cola o la extracción de carátulas.
