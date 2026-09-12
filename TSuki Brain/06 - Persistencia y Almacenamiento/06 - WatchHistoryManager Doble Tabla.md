# 06.06 — WatchHistoryManager (Arquitectura de Doble Tabla)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Administra el historial de visualización y escucha en `data/local/WatchHistoryManager.kt`:
- **Doble Tabla Arquitectónica**:
  1. `watch_history`: Tabla agregada donde cada canción es única (`video_id PRIMARY KEY`). Almacena el número total de reproducciones (`play_count`) y la última fecha de escucha (`last_watched`).
  2. `play_events`: Tabla cronológica de eventos atómicos. Registra cada ping de escucha con su timestamp exacto y la duración escuchada en milisegundos.
- **Consultas Estadísticas Avanzadas**:
  - Alimenta la pantalla de estadísticas (`StatsScreen`), generando resúmenes de minutos totales escuchados por día, semana y mes, así como el mapa de calor por horas.

**Archivos fuente clave:**
- [`data/local/WatchHistoryManager.kt:L32-100`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/WatchHistoryManager.kt#L32-L100)
- [`ui/screens/StatsScreen.kt:L70-130`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/StatsScreen.kt#L70-L130)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite generar resúmenes anuales tipo Spotify Wrapped y análisis de hábitos de escucha con fidelidad estadística real.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Gestor de historial en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Cálculo de escuchas**: En `play_events`, calcular siempre agrupando por bloques de 5 minutos: `COUNT(DISTINCT video_id || '_' || (timestamp/300000))`. Nunca usar `COUNT(1)`.
- El respeto a la bandera `privateMode`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Vaciado selectivo de historial o borrado completo desde Ajustes.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[21 - StatsScreen y Historial de Reproduccion]], [[10 - SleepTimer y Modo Privado]].

---

## 7. Guía rápida para una IA nueva
- Para registrar una reproducción, llama a `WatchHistoryManager.getInstance(context).recordPlay(track)`.
