# 02 - Algoritmo de Sincronización y Drift

> **Ubicación:** `app/src/main/java/com/example/tsuki/together/TogetherPlaybackSync.kt` y `TogetherClock.kt`

---

## ⏱️ Sincronización de Reloj Estilo NTP

Los dispositivos nunca tienen sus relojes internos exactamente a la misma hora. Si el anfitrión dice "estoy en el milisegundo 45,000", el oyente debe compensar el tiempo que tardó el paquete en viajar por la red.

`TogetherClock` calcula continuamente:
1. **RTT (*Round Trip Time*):** Tiempo de ida y vuelta de los paquetes de pulso (*ping/pong*).
2. **Desfase Estimado (*Estimated Offset*):**
   $$	ext{Offset} = rac{(t_1 - t_0) + (t_2 - t_3)}{2}$$
3. **Posición Objetivo Real:**
   $$	ext{TargetPosition} = 	ext{State.positionMs} + (	ext{CurrentTime} - 	ext{SentTime} + 	ext{Offset})$$

---

## 📏 Umbrales de Tolerancia y Corrección de Deriva (*Drift*)

Si el reproductor intentara corregir la posición por desajustes de 10 milisegundos, el audio saltaría constantemente. `TogetherPlaybackSync` define umbrales de histéresis estrictos:

```kotlin
fun shouldSeekForDrift(
    currentPositionMs: Long, 
    targetPositionMs: Long, 
    isPlaying: Boolean, 
    isOnlineSession: Boolean
): Boolean {
    val limit = when {
        !isPlaying -> 150L      // Si está en pausa, máxima precisión (150ms)
        isOnlineSession -> 1200L // En red celular, tolerancia a jitter (1.2s)
        else -> 700L             // En red Wi-Fi local, tolerancia media (700ms)
    }
    return abs(currentPositionMs - targetPositionMs) > limit
}
```

### Supresión de Eco (*Echo Window*)
Cuando el anfitrión ejecuta un comando local (ej. pausa), se activa una ventana de supresión de eco de **700 ms** (`EchoWindowMs = 700L`). Cualquier paquete que regrese rebotado del servidor durante este intervalo es ignorado para evitar que el anfitrión des-haga su propia acción.
