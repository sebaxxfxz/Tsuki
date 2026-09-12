# 07.02 — Modelos de Datos y Álgebra Vectorial

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define las estructuras matemáticas y de datos del cerebro algorítmico:
- **`TSukiItemVector`**: Representación vectorial de una canción (vector disperso de tópicos TF-IDF, valor escalar de duración, pacing normalizado 0..1 y complejidad).
- **`TSukiProfile`**: Perfil acumulativo del usuario con pesos normalizados, matriz de correlación de estados de ánimo y mapa de fatiga de canciones.
- **`TSukiPersona`**: Clasificación psicográfica del comportamiento de escucha.

**Archivos fuente clave:**
- [`data/recommendation/TSukiModels.kt:L1-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiModels.kt#L1-L120)
- [`data/recommendation/TSukiVectorMath.kt:L1-80`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiVectorMath.kt#L1-L80)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite realizar comparaciones multidimensionales objetivas entre canciones y el gusto del usuario mediante operaciones de álgebra lineal.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Modelos inmutables en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La normalización euclidiana ($L_2$ norm) en `TSukiVectorMath.normalize`: garantiza que la similitud coseno esté siempre estrictamente en el rango $[-1.0, 1.0]$.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Operaciones matemáticas puras sin librerías externas para evitar consumo de memoria.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - TSukiNeuroEngine y Aprendizaje Local]], [[04 - TSukiVectorMath Coseno EMA]].

---

## 7. Guía rápida para una IA nueva
- Todas las estructuras en `TSukiModels.kt` son `@Serializable` para persistencia directa.
