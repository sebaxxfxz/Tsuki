# 11.17 — HomeScreen y Secciones Personalizadas

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla de inicio y centro de descubrimientos impulsado por el motor neuronal local (`ui/screens/HomeScreen.kt` y `ui/viewmodels/TSukiHomeViewModel.kt`):
- **Carrusel de Escuchar de Nuevo (Listen Again)**: Carrusel horizontal con las canciones reproducidas frecuentemente por el usuario durante la última semana.
- **Mixes Personalizados Diarios**: Secciones automáticas generadas por `TSukiNeuroEngine` agrupadas por afinidad temática:
  - "Tu Mix de Energía"
  - "Descubrimiento Semanal"
  - "Noche Melancólica"
  - "Artistas que te podrían encantar"
- **Tarjeta de Reanudar Reproducción (`ResumeWatchingCard`)**: Tarjeta destacada superior que recuerda la última pista o podcast no finalizado con su barra de progreso relativa.
- **Feed Inteligente con Shimmer**: Estados de carga esqueléticos elegantes (`ShimmerLoading.kt`) mientras se calculan los rankings vectoriales en background.

**Archivos fuente clave:**
- [`ui/screens/HomeScreen.kt:L1-390`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/HomeScreen.kt#L1-L390)
- [`ui/viewmodels/TSukiHomeViewModel.kt:L1-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/viewmodels/TSukiHomeViewModel.kt#L1-L260)
- [`data/recommendation/TSukiNeuroEngine.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiNeuroEngine.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Ofrece al usuario una experiencia de inicio instantánea y altamente personalizada basada en sus propios hábitos locales de escucha, sin depender de un algoritmo externo ni requerir inicio de sesión obligatorio.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` junto a su ViewModel en `ui/viewmodels/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Cálculo de Secciones en Hilos de Fondo**: Las operaciones de inferencia y cálculo de similitud coseno de `TSukiNeuroEngine` deben ejecutarse siempre en `viewModelScope.launch(Dispatchers.Default)`. Jamás bloquear el hilo principal de Compose.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Manejo de estado UDF mediante `StateFlow<HomeUiState>` con clases selladas (`Loading`, `Success`, `Empty`).

---

## 6. Flujo y conexiones
- Conectado a: [[01 - TSukiNeuroEngine y Aprendizaje Local|07.01 - TSukiNeuroEngine]], [[35 - Componentes Core (MiniPlayer, TSukiPillNavBar, FastScrollBox, Sheets)]].

---

## 7. Guía rápida para una IA nueva
- Para forzar la actualización de las recomendaciones de Home, invoca `homeViewModel.refreshRecommendations()`.
