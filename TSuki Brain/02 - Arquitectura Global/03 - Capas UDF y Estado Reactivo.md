# 02.03 — Capas UDF y Estado Reactivo

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Explica el patrón de Flujo Unidireccional de Datos (UDF) implementado entre la interfaz de usuario en Jetpack Compose y las capas de negocio:
- **Estado Inmutable**: Los estados son emitidos exclusivamente como `StateFlow<T>` con `data class` inmutables anotadas con `@Immutable` (ej. `PlayerUiState`, `RoomState`).
- **Emisión de Eventos**: Los composables nunca mutan variables directamente; invocan métodos de intención en controladores o ViewModels (ej. `PlayerController.togglePlayPause()`, `SettingsViewModel.setAudioQuality(...)`).
- **Ciclo de Actualización**:
  1. La intención muta el estado interno privado `MutableStateFlow<T>`.
  2. El nuevo estado inmutable se emite a los observadores.
  3. Los composables que recolectan mediante `collectAsStateWithLifecycle()` recomponen únicamente los nodos suscritos a las propiedades modificadas.

**Archivos fuente clave:**
- [`playback/PlayerController.kt:L128-215`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt#L128-L215)
- [`ui/viewmodels/SettingsViewModel.kt:L20-75`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/viewmodels/SettingsViewModel.kt#L20-L75)

---

## 2. PARA QUÉ existe (problema que resuelve)
Elimina las inconsistencias de sincronización y los estados intermedios inválidos frecuentes en arquitecturas imperativas tradicionales.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Base arquitectónica en `02 - Arquitectura Global`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No exponer `MutableStateFlow` públicamente desde ningún controlador o repositorio.
- No mutar objetos de estado internamente: utilizar siempre `.update { it.copy(...) }`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Estado de Compose gestionado de manera predecible y desacoplada del ciclo de vida de la Activity.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Vision General del Sistema]], [[01 - PlayerController Central]].

---

## 7. Guía rápida para una IA nueva
- Para agregar una nueva propiedad visual al reproductor, añádela a `PlayerUiState` con un valor por defecto y actualízala mediante `_uiState.update { it.copy(...) }`.
