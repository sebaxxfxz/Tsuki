# 05.13 — LyricsPaneV9 y Slider de Volumen de Hardware

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa el panel completo de la pestaña de letras en el reproductor `MusicPlayerScreenV9`:
- Aloja el visor de letras sincronizadas (`SyncedLyricsView`) en el área superior.
- En la base inferior, integra:
  - Mini controles de reproducción (Anterior, Play/Pausa, Siguiente) para no tener que volver a la carátula para pausar.
  - Barra de progreso ondulada compacta.
  - **Control Deslizante de Volumen de Hardware**: Observa el volumen del sistema mediante un `ContentObserver` registrado sobre `Settings.System.CONTENT_URI` y el `AudioManager.STREAM_MUSIC`. Permite subir y bajar el volumen real del teléfono con arrastre táctil en pantalla y refleja instantáneamente los toques de los botones físicos de volumen.

**Archivos fuente clave:**
- [`ui/player/LyricsPaneV9.kt:L1-184`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/LyricsPaneV9.kt#L1-L184)
- [`ui/player/MusicPlayerScreenV9.kt:L600-720`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/MusicPlayerScreenV9.kt#L600-L720)

---

## 2. PARA QUÉ existe (problema que resuelve)
Ofrece una experiencia inmersiva de canto donde el usuario puede leer la letra, controlar la canción y calibrar el volumen del audio sin salir de la vista de letras ni tapar la pantalla con el diálogo flotante del sistema Android.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Componente de UI del reproductor en `ui/player/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El registro y desregistro del `ContentObserver` en el bloque `DisposableEffect`: olvidar desregistrar el observer causa una fuga de memoria (`Memory Leak`) del contexto de Compose.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Slider de volumen estilizado con tokens de `MaterialTheme.colorScheme.secondaryContainer`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[05 - MusicPlayerScreenV9 Orquestador 1466L]], [[12 - KaraokeWordByWord vs KaraokeLyricRow]].

---

## 7. Guía rápida para una IA nueva
- Para probar el slider de volumen, usa `adb shell media volume --stream 3 --set 10` y verifica que el deslizador se mueva en pantalla.
