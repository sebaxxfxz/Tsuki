# 07.04 — TSukiVectorMath (Similitud Coseno, Pesos y Decaimiento)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Librería de cálculo matemático de recomendaciones en `data/recommendation/TSukiVectorMath.kt`:
- **Similitud Coseno Compuesta**:
  - Pondera cuatro dimensiones con pesos calibrados:
    - `TOPIC_SIMILARITY_WEIGHT = 0.70` (70% afinidad temática y de género).
    - `DURATION_SIMILARITY_WEIGHT = 0.10` (10% duración de la pista).
    - `PACING_SIMILARITY_WEIGHT = 0.10` (10% ritmo y tempo).
    - `COMPLEXITY_SIMILARITY_WEIGHT = 0.10` (10% complejidad acústica).
- **Tasas de Decaimiento Temporal**:
  - `ESTABLISHED_DECAY_RATE = 0.998`: Géneros consolidados decaen muy lentamente.
  - `DEVELOPING_DECAY_RATE = 0.993`: Géneros en desarrollo decaen a ritmo medio.
  - `EMERGING_DECAY_RATE = 0.97`: Géneros recién explorados decaen rápido si no se consolidan.

**Archivos fuente clave:**
- [`data/recommendation/TSukiVectorMath.kt:L6-50`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiVectorMath.kt#L6-L50)

---

## 2. PARA QUÉ existe (problema que resuelve)
Define el equilibrio acústico entre lo que el usuario ama desde hace años y las novedades recientes, previniendo que una tarde escuchando un género nuevo destruya su perfil histórico.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Matemática del motor de recomendación en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Las constantes de pesos `0.70`, `0.10`, `0.10`, `0.10` (su suma es estrictamente 1.0).
- Las tasas de decaimiento `0.998`, `0.993`, `0.97`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Funciones matemáticas puras sin efectos secundarios.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Modelos y Algebra Vectorial]], [[07 - TSukiNeuroEngine Ranking 781 Lineas]].

---

## 7. Guía rápida para una IA nueva
- Para calcular afinidad: `TSukiVectorMath.compositeSimilarity(userVector, itemVector)`.
