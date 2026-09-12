# 03.09 — Operaciones de Cola: Shuffle, Repeat y Reordenamiento

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la manipulación de la lista de reproducción activa (`queue: List<MediaTrack>` y `queueIndex: Int`):
- **Modo Aleatorio (`Shuffle`)**:
  - Al activarse, reordena aleatoriamente la cola pero mantiene la canción sonando actualmente en la posición inicial (índice 0) para que la música no se corte.
  - Conserva una copia de la cola original (`originalQueue`) para poder restaurar el orden previo exacto al desactivar el shuffle.
- **Modos de Repetición (`RepeatMode`)**:
  - `REPEAT_MODE_OFF`: Reproduce la cola hasta el final y se detiene (o activa AutoQueue).
  - `REPEAT_MODE_ONE`: Repite la canción actual indefinidamente en bucle.
  - `REPEAT_MODE_ALL`: Al terminar la última canción, regresa automáticamente a la primera.
- **Reordenamiento y Eliminación**:
  - Soporta mover pistas de posición (`moveTrack(from, to)`) y eliminar pistas individuales (`removeTrack(index)`) actualizando el `queueIndex` de forma transparente si la pista modificada estaba antes de la activa.

**Archivos fuente clave:**
- [`playback/PlayerController.kt:L550-680`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt#L550-L680)
- [`ui/player/QueueListV9.kt:L100-180`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/QueueListV9.kt#L100-L180)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite al usuario un control total sobre su flujo de escucha sin causar saltos inesperados en la reproducción.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Lógica de colas en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La sincronización entre `queueIndex` y el `currentTrack`: si se elimina una canción previa, `queueIndex` DEBE decrementarse en 1 para mantener apuntando al track sonando.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Modificaciones en listas inmutables utilizando `toMutableList()`, modificación de índices y emisión de una nueva lista inmutable `toList()`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - PlayerController Central]], [[09 - QueueListV9 Reorder Swipe Undo]].

---

## 7. Guía rápida para una IA nueva
- Para añadir canciones a la cola sin interrumpir la reproducción actual, utiliza `PlayerController.addToQueue(tracks)`.
