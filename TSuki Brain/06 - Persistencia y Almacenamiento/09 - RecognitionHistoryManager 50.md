# 06.09 — RecognitionHistoryManager (Historial Shazam Top 50)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Guarda y gestiona el historial de canciones identificadas con el motor acústico estilo Shazam en `data/local/RecognitionHistoryManager.kt`:
- Base de datos `tsuki_recognition.db` (tabla `recognition_history`).
- Almacena: `shazam_id`, `title`, `artist`, `album`, `cover_url` y `matched_at`.
- **Tope Fijo de 50 Entradas**: Limita automáticamente el historial a los 50 descubrimientos más recientes eliminando los más antiguos mediante:
  ```sql
  DELETE FROM recognition_history WHERE id NOT IN (
      SELECT id FROM recognition_history ORDER BY matched_at DESC LIMIT 50
  )
  ```

**Archivos fuente clave:**
- [`data/local/RecognitionHistoryManager.kt:L22-139`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/RecognitionHistoryManager.kt#L22-L139)
- [`ui/screens/RecognitionScreen.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/RecognitionScreen.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite al usuario revisar qué canciones descubrió en una fiesta, radio o cafetería horas o días después de haberlas capturado.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Persistencia de descubrimientos de audio en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El límite de 50 entradas para mantener la base de datos ligera y ágil.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Integración fluida en `RecognitionScreen` con opción de reproducción inmediata.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[06 - RecognitionScreen y Flujo UX]], [[02 - MusicRecognizer AudioRecord 16kHz]].

---

## 7. Guía rápida para una IA nueva
- Para guardar un hallazgo, llama a `RecognitionHistoryManager.getInstance(context).saveRecognition(...)`.
