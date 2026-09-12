# 07.03 — TSukiTokenizer (Tokenización, Bi-gramas y TF-IDF)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Procesador de lenguaje natural en `data/recommendation/TSukiTokenizer.kt`:
- Toma el título, nombre de artista, álbum y descripción de una canción o video.
- Limpia signos ortográficos, convierte a minúsculas y elimina palabras vacías (*stopwords*) en español e inglés.
- Genera tokens individuales y pares de términos adyacentes (*bi-gramas*) para capturar contexto musical (ej. `"post rock"`, `"heavy metal"`, `"lo fi"`).
- Calcula la frecuencia de término ponderada por frecuencia inversa de documento (TF-IDF) para extraer las etiquetas más distintivas de la pista.

**Archivos fuente clave:**
- [`data/recommendation/TSukiTokenizer.kt:L15-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiTokenizer.kt#L15-L180)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite extraer el ADN semántico y temático de cualquier canción de YouTube Music automáticamente sin requerir metadatos manuales de discográfica.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Módulo de NLP en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La lista de stopwords en español e inglés: previene que palabras neutras como "the", "de", "and", "la" dominen los vectores de afinidad.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Ejecutado en corrutinas `Dispatchers.Default`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[05 - TSukiTopicCatalog Onboarding 8 Cats]], [[01 - TSukiNeuroEngine y Aprendizaje Local]].

---

## 7. Guía rápida para una IA nueva
- Para vectorizar un título, usa `TSukiTokenizer.extractFeatureTokens(title, artist, album)`.
