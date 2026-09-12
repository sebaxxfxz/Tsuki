# 12.04 — Patrón PixelCopy Captura Dialog

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Documenta la implementación técnica exacta del patrón **PixelCopy en Diálogo Transparente**, diseñado para solventar los fallos endémicos de captura gráfica en Android Compose:
- **El Problema**: Las vistas Compose con aceleración de hardware, efectos de desenfoque (`blur`) y capas de mezcla (`blendMode`) producen mapas de bits negros o vacíos al intentar capturarlas mediante `GraphicsLayer.toImageBitmap()` o `View.draw(Canvas)`.
- **La Solución Arquitectural**:
  1. Desplegar un `Dialog(onDismissRequest = { ... }, properties = DialogProperties(usePlatformDefaultWidth = false))` con fondo transparente.
  2. Obtener la referencia a la ventana nativa (`Window`) mediante el proveedor de diálogo:
     ```kotlin
     val window = (LocalView.current.parent as? DialogWindowProvider)?.window
     ```
  3. Renderizar la tarjeta gráfica de alta resolución en el centro del diálogo.
  4. Obtener las coordenadas absolutas de la vista en pantalla (`view.getLocationInWindow(location)`).
  5. Crear un `Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)`.
  6. Disparar la sincronización de superficie con hardware:
     ```kotlin
     PixelCopy.request(window, srcRect, bitmap, { copyResult ->
         if (copyResult == PixelCopy.SUCCESS) {
             saveAndShareBitmap(context, bitmap)
         }
     }, Handler(Looper.getMainLooper()))
     ```
  7. Cerrar el diálogo solo tras recibir el callback de éxito.

**Archivos fuente clave:**
- [`ui/player/ShareCardView.kt:L200-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/ShareCardView.kt#L200-L260)
- [`ui/player/MusicPlayerScreenV9.kt:L240-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/MusicPlayerScreenV9.kt#L240-L260)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza que el 100% de las capturas de pantalla compartibles en redes sociales salgan nítidas, con colores vivos, sombras reales y sin parches negros en cualquier versión de Android (API 24 a API 36).

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `12 - Guia de Desarrollo y Comandos/` como documentación de una solución de ingeniería no trivial ganada en combate.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **`DialogWindowProvider` Cast**: No intentar obtener la ventana desde `LocalView.current.context as Activity`, ya que en Compose la ventana de la Activity no tiene el foco de renderizado del diálogo y capturará el fondo detrás del diálogo.
- **Sincronización con Handler Main**: El callback de `PixelCopy` debe ejecutarse en el `Handler(Looper.getMainLooper())` para poder manipular el estado de Compose o lanzar corrutinas de UI.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Esperar que Coil reporte estado `Success` en la carátula antes de invocar `PixelCopy.request()`.

---

## 6. Flujo y conexiones
- Resumen conceptual en: [[04 - Captura PixelCopy para Redes|11.04 - Captura PixelCopy]].
- Implementado en: [[14 - ShareCards PixelCopy Stats]].

---

## 7. Guía rápida para una IA nueva
- Si necesitas implementar una nueva tarjeta compartible, reutiliza la función helper de `PixelCopy` de `ShareCardView.kt`.
