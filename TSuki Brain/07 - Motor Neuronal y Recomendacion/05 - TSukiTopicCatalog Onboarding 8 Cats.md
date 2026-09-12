# 07.05 — TSukiTopicCatalog (Catálogo Maestro de 8 Categorías)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define la ontología musical maestra en `data/recommendation/TSukiTopicCatalog.kt`:
- **8 Macro-Categorías Musicales**:
  1. Rock & Metal (Classic Rock, Indie Rock, Alternative, Heavy Metal, Punk).
  2. Pop & Moderno (Electropop, Synthpop, Dance Pop, K-Pop).
  3. Hip-Hop & Urbano (Trap, Rap, Boom Bap, Reggaeton, Drill).
  4. Electrónica & Dance (House, Techno, Trance, Drum & Bass, Dubstep, Ambient).
  5. R&B, Soul & Jazz (Neo-Soul, Funk, Bebop, Smooth Jazz, Blues).
  6. Latina & Tradicional (Salsa, Bachata, Cumbia, Folclore).
  7. Clásica & Instrumental (Bandas Sonoras, Piano Solo, Orquestal, Neoclásica).
  8. Lo-Fi & Relax (Chillhop, Study Beats, Down-tempo).
- Utilizado en la pantalla de bienvenida (`OnboardingScreen`) para que el usuario seleccione sus géneros favoritos e inicializar su vector temático.

**Archivos fuente clave:**
- [`data/recommendation/TSukiTopicCatalog.kt:L10-150`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiTopicCatalog.kt#L10-L150)
- [`ui/screens/OnboardingScreen.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/OnboardingScreen.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Resuelve el problema del "Arranque en Frío" (*Cold Start Problem*): sin estas categorías iniciales, la app no sabría qué recomendar a un usuario recién instalado.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Catálogo taxonómico en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los identificadores string canónicos de cada categoría: `topic_rock`, `topic_pop`, etc.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Mapeo bidireccional entre nombres en español e identificadores internos.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[25 - OnboardingScreen y Seleccion de Artistas]], [[03 - TSukiTokenizer TF-IDF Lematizacion]].

---

## 7. Guía rápida para una IA nueva
- Para obtener la lista completa de categorías: `TSukiTopicCatalog.getAllCategories()`.
