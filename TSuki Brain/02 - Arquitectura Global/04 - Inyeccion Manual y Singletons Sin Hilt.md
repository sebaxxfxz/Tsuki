# 02.04 — Inyección Manual y Singletons Sin Hilt

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Explica por qué TSuki prescinde de frameworks de inyección de dependencias pesados como Hilt o Dagger y adopta un patrón de inyección manual pragmático:
- **Singletons con Contexto de Aplicación**: Clases como `PlayerController`, `YouTubeAuthManager`, `FavoritesManager`, `WatchHistoryManager` y `LocalPlaylistManager` se instancian mediante el patrón:
  ```kotlin
  companion object {
      @Volatile private var instance: PlayerController? = null
      fun getInstance(context: Context): PlayerController =
          instance ?: synchronized(this) {
              instance ?: PlayerController(context.applicationContext).also { instance = it }
          }
  }
  ```
- **Retención de Contexto Seguro**: Todos los singletons capturan estrictamente `context.applicationContext`. Esto garantiza que nunca se retenga el contexto de una `Activity`, eliminando el riesgo de fugas de memoria (*Memory Leaks*).
- **Ventajas Técnicas**:
  - Tiempos de compilación ultrarrápidos (~2 segundos para compilar Kotlin) al no requerir procesamiento de anotaciones KSP/KAPT para inyección.
  - Arranque en frío instantáneo sin inicializadores de grafo en runtime.
  - Total interoperabilidad con el ciclo de vida de `MediaLibraryService`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Mantiene el proyecto ligero, previene la lentitud en la compilación de Gradle y asegura un control total sobre el orden de inicialización de los componentes de audio.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Decisión de arquitectura fundamental en `02 - Arquitectura Global`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NUNCA guardar una referencia directa a una `Activity` en un singleton o companion object. Usar siempre `applicationContext`.
- No intentar migrar el proyecto a Hilt sin una discusión y aprobación previa.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Acceso directo vía `ClassName.getInstance(context)`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Vision General del Sistema]], [[04 - Entry Points y Grafo de Arranque]].

---

## 7. Guía rápida para una IA nueva
- Para consumir un repositorio en un Composable o Servicio, obtén la instancia mediante `Manager.getInstance(context)`.
