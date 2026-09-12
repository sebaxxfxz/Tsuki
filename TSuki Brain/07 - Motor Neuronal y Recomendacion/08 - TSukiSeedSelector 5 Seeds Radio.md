# 07.08 — TSukiSeedSelector (Selección Dinámica de 5 Semillas para Radio)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Selecciona las pistas semilla (*Seeds*) óptimas para iniciar una estación de radio infinita personalizada:
- Analiza el historial reciente de `WatchHistoryManager` y las canciones favoritas de `FavoritesManager`.
- Filtra las pistas con mayor afinidad y selecciona hasta **5 semillas complementarias** que representen la vibra actual del usuario.
- Envía estas semillas a `TSukiInnerTubeClient.fetchRelatedTracks` o automix para generar una lista continua de canciones relacionadas.

**Archivos fuente clave:**
- [`data/recommendation/TSukiSeedSelector.kt:L10-90`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiSeedSelector.kt#L10-L90)
- [`playback/AutoQueueHelper.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/AutoQueueHelper.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Generar una radio basada en una sola canción produce colas monótonas. Mezclar hasta 5 semillas produce una mezcla fluida y diversa.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Lógica de selección de semillas en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El límite de 5 semillas: enviar más de 5 satura las llamadas automix de InnerTube.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Prioriza canciones escuchadas en las últimas 2 horas si están disponibles.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[04 - AutoQueue y Radio Automix]], [[01 - TSukiNeuroEngine y Aprendizaje Local]].

---

## 7. Guía rápida para una IA nueva
- Para obtener semillas automáticas: `TSukiSeedSelector.selectCurrentSeeds(context)`.
