# 09.06 — RecognitionScreen y Experiencia de Reconocimiento

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa la interfaz de usuario de reconocimiento acústico en `ui/screens/RecognitionScreen.kt`:
- **Animación de Radar Visual**: Ondas circulares animadas con `rememberInfiniteTransition` que reaccionan a la amplitud real del micrófono capturada por `MusicRecognizer`.
- **Estados de Flujo**:
  - `IDLE`: Botón grande central invitando a pulsar para escuchar.
  - `LISTENING`: Micrófono activo y ondas vibrando.
  - `MATCHED`: Tarjeta de la canción encontrada con carátula grande, botón de reproducir de inmediato, añadir a favoritos o ver en YouTube.
  - `NO_MATCH`: Mensaje animado invitando a reintentar.
- **Pestaña de Historial**: Lista deslizable con los últimos 50 reconocimientos guardados en `tsuki_recognition.db`.

**Archivos fuente clave:**
- [`ui/screens/RecognitionScreen.kt:L1-903`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/RecognitionScreen.kt#L1-L903)
- [`data/local/RecognitionHistoryManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/RecognitionHistoryManager.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Provee una experiencia de usuario instantánea y estética para cazar canciones en cualquier lugar sin salir de TSuki.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Pantalla de usuario en `ui/screens/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La gestión del ciclo de vida: si el usuario sale de la pantalla, la grabación del micrófono DEBE cancelarse de inmediato.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Microinteracciones hápticas al detectar coincidencia.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - MusicRecognizer AudioRecord 16kHz]], [[09 - RecognitionHistoryManager 50]].

---

## 7. Guía rápida para una IA nueva
- Para probar la pantalla de reconocimiento, navega desde el atajo del launcher o desde la biblioteca.
