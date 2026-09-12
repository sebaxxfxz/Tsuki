# 02.06 — Deep Links Intents y Compatibilidad

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona los puntos de entrada externos de la aplicación mediante Android Intents:
1. **Enlaces de YouTube y YouTube Music**:
   - Captura URLs: `https://youtube.com/watch?v=...`, `https://youtu.be/...`, `https://music.youtube.com/watch?v=...`, shorts y transmisiones en vivo.
   - Extrae el ID de 11 caracteres mediante la expresión regular `youtubeVideoIdRegex` en `MainActivity.handleExternalYouTubeIntent`.
   - Inicia la reproducción directa y activa la radio automática basada en esa pista.
2. **Deep Links de "Escuchar Juntos" (`tsuki://together`)**:
   - Procesa esquemas `tsuki://together?host=...&port=...&sid=...&key=...`.
   - Conecta automáticamente a la sala compartida como oyente invitado.
3. **Archivos Locales Compartidos (`ACTION_SEND` / `ACTION_VIEW`)**:
   - Abre archivos de audio locales (`audio/*`) enviados desde exploradores de archivos o WhatsApp y los reproduce de inmediato.

**Archivos fuente clave:**
- [`MainActivity.kt:L260-350`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/MainActivity.kt#L260-L350)
- [`app/src/main/AndroidManifest.xml`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/AndroidManifest.xml)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a TSuki integrarse perfectamente con el sistema operativo Android, navegadores web y aplicaciones de mensajería para reproducir enlaces compartidos al instante.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Puente con el sistema operativo en `02 - Arquitectura Global`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La regex de extracción de ID de YouTube: `(v=|youtu\.be/|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})`. Debe soportar todos los formatos de enlace de Google.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Verificación con comandos adb: `adb shell am start -a android.intent.action.VIEW -d "https://youtu.be/..."`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[04 - Entry Points y Grafo de Arranque]], [[01 - PlayerController Central]].

---

## 7. Guía rápida para una IA nueva
- Si agregas soporte para un nuevo esquema de enlace, decláralo primero en el `AndroidManifest.xml` y manéjalo en `MainActivity.handleIntent()`.
