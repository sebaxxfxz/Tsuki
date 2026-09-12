# 11.09 — QueueListV9 Reorder Swipe Undo

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona la cola dinámica de reproducción en curso (`ui/player/QueueListV9.kt`):
- **Lista Reactiva de Pistas**: Muestra las canciones en orden de ejecución, destacando la pista actual con un indicador de ecualizador animado y tipografía destacada.
- **Reordenamiento por Arrastre (Drag-and-Drop)**: Permite a los usuarios reorganizar el orden de las canciones manteniendo presionado el tirador lateral de cada elemento.
- **Eliminación Rápida con Deslizamiento (Swipe-to-Dismiss)**: Implementado mediante `SwipeToDismissBox`, permitiendo descartar canciones hacia la izquierda con retroalimentación háptica.
- **Acción de Deshacer (Undo)**: Despliega inmediatamente un `Snackbar` flotante con la opción "Deshacer" para restaurar una canción eliminada por error en su posición original.
- **Limpieza y Modo Aleatorio**: Botones superiores para vaciar la cola o reorganizar aleatoriamente las canciones pendientes.

**Archivos fuente clave:**
- [`ui/player/QueueListV9.kt:L1-420`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/QueueListV9.kt#L1-L420)
- [`playback/PlayerController.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Otorga control absoluto sobre el orden de reproducción futuro sin detener la pista actual.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Ubicado en `ui/player/` para ser consumido directamente por la hoja inferior de la cola en `MusicPlayerScreenV9`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Clave Compuesta en `LazyColumn`**: **INVARIANTE CRÍTICO**: La clave de cada elemento de la lista debe construirse obligatoriamente como clave compuesta: `key = { index, item -> "${item.id}_#$index" }`. **Nunca uses solo `item.id` como clave**. Si una canción aparece duplicada en la cola (ej. el usuario agregó la misma canción dos veces), usar solo `item.id` provocará un `IllegalArgumentException: Key was already used` que crasheará la aplicación de inmediato.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Los cambios de posición en el reordenamiento se actualizan localmente en la lista temporal de Compose para una respuesta visual a 120 FPS y luego se sincronizan con `PlayerController.reorderQueue(fromIndex, toIndex)`.

---

## 6. Flujo y conexiones
- Orquestado desde: [[05 - MusicPlayerScreenV9 Orquestador 1466L]].
- Acciones despachadas hacia: `playback/PlayerController.kt`.

---

## 7. Guía rápida para una IA nueva
- Si agregas una acción a los items de la cola, asegúrate de mantener la clave compuesta `"${item.id}_#$index"`.
