# 12.07 — Troubleshooting 403 416 Update Incompatible

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Manual de resolución inmediata de los errores técnicos más frecuentes durante el desarrollo y uso de TSuki:

### 1. Error `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
- **Causa**: Se intenta instalar una compilación de depuración (`debug.keystore`) sobre una versión previamente instalada que fue firmada con otra clave o generada en otro entorno.
- **Solución**: Desinstalar la aplicación antes de instalar la nueva:
  ```bash
  adb -s fb74ec96 uninstall com.example.tsuki
  adb -s fb74ec96 install -r app/build/outputs/apk/debug/app-debug.apk
  ```

### 2. Error HTTP 403 Forbidden en Streaming de Audio
- **Causa**: Las URLs de audio extraídas de YouTube Music expiran tras aproximadamente 6 horas o quedan invalidadas si cambia la dirección IP del dispositivo o los tokens de sesión.
- **Solución**:
  - `PlayerController` cuenta con un mecanismo que invalida automáticamente la entrada en la caché de URLs (`urlCache`) al recibir un error 403 y solicita una URL fresca a `YouTubeExtractor`.
  - Si persiste en una canción específica, vaciar la caché de reproducción en Ajustes o forzar la resolución mediante NewPipeExtractor como fallback.

### 3. Error HTTP 416 Range Not Satisfiable
- **Causa**: Ocurre en descargas segmentadas de `DownloadEngine` o al hacer seeking más allá del buffer descargado si el servidor de streaming de YouTube no reconoce el offset de bytes solicitado.
- **Solución**: Reiniciar la petición HTTP sin la cabecera `Range` o solicitar el rango comenzando desde el byte `0-`.

### 4. Error `TransactionTooLargeException` en Widgets de Escritorio
- **Causa**: Se intenta enviar un Bitmap de carátula de resolución completa a través de Binder hacia `RemoteViews` en Glance.
- **Solución**: Reescalar siempre el Bitmap a un máximo de 512x512 píxeles antes de pasarlo a `TSukiGlanceSync`.

---

## 2. PARA QUÉ existe (problema que resuelve)
Ahorra horas de depuración al proporcionar el diagnóstico exacto y la solución probada para los fallos más habituales del stack multimedia en Android.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `12 - Guia de Desarrollo y Comandos/` como manual de resolución de incidencias.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El manejo automático de reintento en caso de 403 en `PlayerController.kt` no debe ser desactivado; es vital para la continuidad de la música en sesiones prolongadas.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Revisión de logs con `adb logcat -d -s PlayerController:D TSukiInnerTube:D` para identificar si el error proviene de red o del decodificador.

---

## 6. Flujo y conexiones
- Complementa a: [[03 - Instalacion adb fb74ec96 y Logcat]], [[01 - PlayerController Central|03.01 - PlayerController]].

---

## 7. Guía rápida para una IA nueva
- Si la app no reproduce una canción y arroja error de reproducción, verifica si el logcat muestra `HttpDataSource$InvalidResponseCodeException: 403`.
