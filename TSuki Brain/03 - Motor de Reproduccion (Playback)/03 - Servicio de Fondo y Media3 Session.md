# 03 - Servicio de Fondo y Media3 Session

> **Ubicación:** `app/src/main/java/com/example/tsuki/playback/TSukiPlaybackService.kt`
> **Tipo:** `MediaLibraryService` de AndroidX Media3
> **Manifiesto:** Declarado con `android:foregroundServiceType="mediaPlayback"` y exportado para integración con el sistema.

---

## 🚀 Responsabilidad del Servicio

`TSukiPlaybackService` es el ancla que mantiene el proceso de TSuki vivo en el sistema operativo Android. Sus responsabilidades son:
1. Crear y ser dueño de la instancia física de `ExoPlayer`.
2. Alojar la `MediaLibrarySession` que expone controles a auriculares Bluetooth, mandos de volante, Android Auto y notificaciones.
3. Administrar el `WakeLock` de CPU y el modo de suspensión de red (`C.WAKE_MODE_NETWORK`).
4. Configurar la factoría de fuentes de datos combinadas (`MergingMediaSource`) y el almacenamiento en caché (`CacheDataSource`).
5. Emitir periódicamente pulsos de sincronización a los widgets Glance en pantalla de inicio.

---

## ⚙️ Configuración Especial de ExoPlayer

El reproductor se instancia en `onCreate()` con parámetros altamente afinados para streaming musical sobre redes celulares variables:

```kotlin
val loadControl = DefaultLoadControl.Builder()
    .setAllocator(DefaultAllocator(true, 64 * 1024))
    .setBufferDurationsMs(
        1_000,    // Mínimo buffer antes de permitir reproducción (1s)
        50_000,   // Buffer máximo en memoria (50s)
        200,      // Buffer necesario para arrancar tras un seek (200ms)
        600       // Buffer para reanudar tras congelamiento (600ms)
    )
    .setBackBuffer(15_000, true) // 15 segundos de buffer hacia atrás
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()
```

### ¿Por qué 15 segundos de `backBuffer`?
Cuando un usuario pulsa el botón "Retroceder 10 segundos" o arrastra el deslizador ligeramente hacia atrás, un reproductor común descartaría el audio anterior y volvería a solicitar los bytes por red. Con `setBackBuffer(15_000, true)`, ExoPlayer mantiene en RAM los últimos 15 segundos reproducidos: **el salto hacia atrás es 100% instantáneo y consume 0 bytes de internet**.

---

## 🔀 MergingMediaSource: Video + Audio Separados

En YouTube, las mejores pistas de audio (Opus a 160 kbps) vienen en streams independientes sin video. Si el usuario conmuta al modo video (`PlayerMode.VideoExpanded`), el reproductor no recurre a un stream MP4 de baja calidad de 360p.

En su lugar, `TSukiPlaybackService` utiliza `MergingMediaSource`:
* **Pista 1:** Video DASH a 1080p o 720p.
* **Pista 2:** Audio Opus de máxima fidelidad.
* Ambas fuentes se sincronizan en el mismo reloj de ExoPlayer.

---

## 🔋 WakeLock y Eficiencia Energética

Para evitar que los sistemas agresivos de ahorro de batería de fabricantes como Xiaomi, Samsung o Huawei maten el reproductor con la pantalla apagada:
1. `serviceWakeLock`: Se adquiere un `PARTIAL_WAKE_LOCK` únicamente mientras `player.playWhenReady == true` y la pista no haya terminado.
2. `updateServiceWakeLock()`: En cuanto el usuario pausa o la música termina, el WakeLock se libera de inmediato para permitir que el procesador entre en modo de reposo profundo (*deep sleep*).
3. `setHandleAudioBecomingNoisy(true)`: Si el usuario desconecta los auriculares o se apaga el altavoz Bluetooth, ExoPlayer pausa la reproducción de forma automática para evitar que la música suene a todo volumen por el altavoz del teléfono.

---

## ⚠️ Zonas Prohibidas en este archivo

1. **No instanciar un segundo servicio:** Media3 exige una única sesión activa por proceso.
2. **No remover el override de `media3_notification_small_icon.xml`:** Rompería el ícono en la barra de estado.
3. **No ejecutar `runBlocking` dentro de callbacks de MediaLibrarySession:** Bloquearía el hilo principal de IPC de Android.
