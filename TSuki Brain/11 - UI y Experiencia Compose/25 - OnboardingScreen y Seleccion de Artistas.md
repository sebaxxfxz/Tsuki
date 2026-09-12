# 11.25 — OnboardingScreen y Selección de Artistas

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Experiencia de bienvenida para nuevos usuarios (`ui/screens/OnboardingScreen.kt`):
- **Inicialización del Perfil de Gustos**: Muestra una cuadrícula interactiva con artistas destacados de múltiples géneros (Rock, Pop, Urbana, Hip-Hop, Electrónica, Metal, Jazz).
- **Selección Interactiva con Fricción Positiva**:
  - Al tocar un artista, la burbuja o tarjeta se expande ligeramente con animación de resorte y revela subgéneros o artistas similares en tiempo real.
  - Requiere un mínimo de 3 selecciones para habilitar el botón "Comenzar".
- **Sembrado Inicial del Cerebro Neuronal**: Transforma los artistas seleccionados en vectores de afinidad temáticos y los inyecta en `TSukiNeuroEngine` (`seedPreferences`), garantizando que la pantalla de inicio ya ofrezca recomendaciones de alta relevancia desde el primer minuto.

**Archivos fuente clave:**
- [`ui/screens/OnboardingScreen.kt:L1-340`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/OnboardingScreen.kt#L1-L340)
- [`data/recommendation/TSukiNeuroEngine.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiNeuroEngine.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Resuelve de forma definitiva el problema del "arranque en frío" (*Cold Start Problem*) de los sistemas de recomendación, evitando mostrar pantallas vacías o sugerencias genéricas a un usuario nuevo.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` mostrada condicionalmente durante el primer arranque de la aplicación.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Flag de Primer Arranque**: Debe marcarse como completado en `AppearancePreferences.hasCompletedOnboarding` únicamente tras haber finalizado con éxito la escritura del vector en `TSukiNeuroEngine`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Animaciones fluidas de entrada y salida entre pasos del asistente utilizando transiciones de Compose.

---

## 6. Flujo y conexiones
- Vinculado con: [[01 - TSukiNeuroEngine y Aprendizaje Local|07.01 - TSukiNeuroEngine]], [[17 - HomeScreen y Secciones Personalizadas]].

---

## 7. Guía rápida para una IA nueva
- Para reiniciar el onboarding en pruebas de depuración, cambia `hasCompletedOnboarding` a falso en `AppearancePreferences`.
