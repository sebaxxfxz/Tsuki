# 11.04 — Captura PixelCopy para Redes

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Proporciona el mecanismo infalible para exportar tarjetas visuales de alta fidelidad (para compartir en Instagram Stories, WhatsApp, Twitter/X y Telegram):
- **Problema de `GraphicsLayer.toImageBitmap()`**: En dispositivos con aceleración por hardware activa y vistas complejas con shaders y desenfoques, la API nativa de Compose `GraphicsLayer.toImageBitmap()` frecuentemente retorna un mapa de bits negro o transparente.
- **Solución Robusta con `PixelCopy`**:
  - Renderiza la tarjeta dentro de un `Dialog` sin bordes y con fondo transparente anclado al `Window` nativo.
  - Obtiene la referencia al `window` a través de `(LocalView.current.parent as? DialogWindowProvider)?.window`.
  - Ejecuta `PixelCopy.request(window, srcRect, destBitmap, callback, handler)`.
  - Guarda el Bitmap como PNG temporal en `cacheDir/shares/` y genera un URI seguro mediante `FileProvider`.
  - Despacha el intent `Intent.ACTION_SEND` con `image/png`.

**Archivos fuente clave:**
- [`ui/player/ShareCardView.kt:L180-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/ShareCardView.kt#L180-L260)
- [`ui/player/StatsShareCard.kt:L120-190`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/StatsShareCard.kt#L120-L190)
- [`ui/player/MusicPlayerScreenV9.kt:L203-260`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/player/MusicPlayerScreenV9.kt#L203-L260)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a los usuarios compartir canciones y estadísticas auditivas con una estética impecable (carátula con sombra 3D, ondas de audio, código de barras/QR de la pista y branding de TSuki) sin sufrir errores de renderizado ni imágenes negras.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Integrado en `ui/player/` donde se originan las solicitudes de compartir tanto la canción actual como las estadísticas de reproducción.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Cierre Prematuro del Diálogo**: No cerrar el diálogo `showShareCard = false` antes de que el callback de `PixelCopy` haya finalizado con `PixelCopy.SUCCESS`. Si la ventana se destruye mientras el GPU copia la superficie, la captura fallará con `ERROR_SOURCE_INVALID`.
- **Cálculo de Coordenadas de Rectángulo**: El `srcRect` pasado a `PixelCopy` debe calcularse usando `view.getLocationInWindow()` en coordenadas absolutas de pantalla para no capturar los bordes de la barra de estado.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Esperar a que la imagen de la carátula haya terminado de cargar en Coil (`AsyncImagePainter.State.Success`) antes de activar el disparador de captura automática.
- Otorgar permisos de lectura al URI compartido (`Intent.FLAG_GRANT_READ_URI_PERMISSION`).

---

## 6. Flujo y conexiones
- Documento complementario: [[14 - ShareCards PixelCopy Stats]].
- Guía de desarrollo: [[04 - Patron PixelCopy Captura Dialog|12.04 - Patrón PixelCopy]].

---

## 7. Guía rápida para una IA nueva
- Para generar una tarjeta compartible, muestra el composable dentro de un `Dialog` y pasa el callback `onCaptureReady` que invoque el helper de `PixelCopy`.
