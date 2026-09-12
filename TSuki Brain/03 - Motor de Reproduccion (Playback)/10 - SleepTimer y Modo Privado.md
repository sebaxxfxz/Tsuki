# 03.10 — Temporizador de Apagado y Modo Privado

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa dos funciones de conveniencia y privacidad para el usuario en `PlayerController.kt`:
1. **Temporizador de Apagado (*Sleep Timer*)**:
   - Permite programar la detención automática de la música en un intervalo de tiempo (ej. 15, 30, 45, 60 minutos) o al finalizar la canción actual (*End of Track*).
   - Emite el tiempo restante `sleepTimerRemainingMs` en `PlayerUiState`.
   - Al expirar, ejecuta una pausa suave y cancela los jobs asociados.
2. **Modo Privado (*Private Mode*)**:
   - Alterna una bandera que deshabilita inmediatamente el registro de reproducciones en `WatchHistoryManager` (`tsuki_history.db`).
   - Bloquea el ajuste del vector de preferencias en `TSukiNeuroEngine`.
   - Permite al usuario escuchar música ocasional sin alterar sus estadísticas ni sus recomendaciones personalizadas.

**Archivos fuente clave:**
- [`playback/PlayerController.kt:L710-790`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/PlayerController.kt#L710-L790)
- [`data/local/WatchHistoryManager.kt:L80-95`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/WatchHistoryManager.kt#L80-L95)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite dormir escuchando música sin agotar la batería del teléfono toda la noche y protege el algoritmo de recomendaciones frente a escuchas temporales o de terceros.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Son modificadores directos de la lógica del reproductor en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La verificación de `privateMode` en `recordPlay()`: debe evaluarse antes de cualquier inserción en SQLite.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Jobs de corrutina de cuenta regresiva gestionados con cancelación limpia en `setSleepTimer(0)`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - PlayerController Central]], [[06 - WatchHistoryManager Doble Tabla]].

---

## 7. Guía rápida para una IA nueva
- Para activar el temporizador, invoca `PlayerController.setSleepTimer(minutes)`.
