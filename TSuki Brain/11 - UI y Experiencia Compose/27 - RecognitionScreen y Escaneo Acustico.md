# 11.27 — RecognitionScreen y Escaneo Acústico

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla de reconocimiento musical en tiempo real mediante huella acústica (`ui/screens/RecognitionScreen.kt`):
- **Animación de Radar Pulsante**: Esfera central animada con ondas concéntricas que reaccionan a la captura de audio del micrófono.
- **Gestión Automática de Permisos**: Hoja de explicación de permisos (`PermissionRationaleSheet`) si el permiso `RECORD_AUDIO` no ha sido otorgado.
- **Integración con Motor Shazam**:
  - Captura 3 a 5 segundos de audio en PCM a 16kHz mono.
  - Genera la huella espectral mediante `ShazamSignatureGenerator`.
  - Consulta la API de reconocimiento a través de `MusicRecognizer`.
- **Historial de Canciones Identificadas**: Muestra la lista de temas reconocidos previamente con fecha y opción de reproducción inmediata o guardado en favoritos.

**Archivos fuente clave:**
- [`ui/screens/RecognitionScreen.kt:L1-370`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/RecognitionScreen.kt#L1-L370)
- [`shazam/MusicRecognizer.kt:L1-160`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/shazam/MusicRecognizer.kt#L1-L160)
- [`data/local/RecognitionHistoryManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/RecognitionHistoryManager.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite identificar cualquier canción que esté sonando en el entorno físico (en la radio, una fiesta, un bar) y reproducirla o añadirla a la biblioteca de TSuki con un solo toque.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` con acceso directo desde el buscador principal o desde la barra superior.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Liberación del `AudioRecord`**: El objeto `AudioRecord` del sistema debe cerrarse y liberarse inmediatamente tras finalizar la grabación o si el usuario sale de la pantalla, evitando fugas del micrófono que drenan la batería y muestran el indicador verde de privacidad indefinidamente.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Transición inmediata con vibración de confirmación cuando una canción es reconocida con éxito.

---

## 6. Flujo y conexiones
- Alimentado por todo el subsistema: [[01 - Generador de Firmas Acusticas FFT|09.01 - Shazam]], [[02 - MusicRecognizer AudioRecord 16kHz|09.02 - Generador de Huellas]].

---

## 7. Guía rápida para una IA nueva
- Para iniciar el reconocimiento programáticamente, llama a `recognitionViewModel.startListening()`.
