# 07.07 — Algoritmo de Ranking y Penalización por Fatiga (781 Líneas)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa el pipeline completo de puntuación, filtrado y ordenamiento de candidatos en `TSukiNeuroEngine.kt`:
1. **Extracción de Candidatos**: Toma canciones de feeds de YouTube Music, suscripciones y biblioteca local.
2. **Puntuación Base (*Base Score*)**: Calcula la similitud compuesta entre el candidato y el perfil del usuario.
3. **Penalización por Fatiga (*Skip & Overplay Fatigue*)**:
   - `WATCHED_PENALTY_FULL = 0.02`: Si la canción se escuchó al 100% recientemente, su puntuación se reduce temporalmente al 2% para forzar variedad.
   - `WATCHED_PENALTY_HALF = 0.30`: Si se escuchó al 50%, reduce al 30%.
   - `IMPRESSION_PENALTY_HEAVY = 0.05`: Si la canción se mostró varias veces en el feed y el usuario no la reprodujo, se penaliza para no aburrir.
4. **Impulso de Diversidad (*Serendipity Injection*)**:
   - Inserta intencionalmente un 10-15% de canciones de géneros adyacentes no explorados para fomentar nuevos descubrimientos.

**Archivos fuente clave:**
- [`data/recommendation/TSukiNeuroEngine.kt:L30-150`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiNeuroEngine.kt#L30-L150)

---

## 2. PARA QUÉ existe (problema que resuelve)
Evita la "burbuja de eco" (*filter bubble*) donde los algoritmos repiten obsesivamente las mismas 20 canciones hasta quemarlas.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Corazón de la recomendación en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los coeficientes de penalización por fatiga (`0.02`, `0.30`, `0.05`). Están balanceados para que una canción vuelva a aparecer gradualmente tras varios días.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Evaluación de candidatos en lotes paralelos con `Dispatchers.Default`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - TSukiNeuroEngine y Aprendizaje Local]], [[08 - TSukiSeedSelector 5 Seeds Radio]].

---

## 7. Guía rápida para una IA nueva
- Para rankear una lista de canciones: `TSukiNeuroEngine.getInstance(context).rankTracks(candidates)`.
