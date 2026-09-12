# 12.03 — Instalación ADB fb74ec96 y Logcat

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Documenta el procedimiento oficial de despliegue, instalación y captura de logs en el dispositivo físico de pruebas:
- **Dispositivo Físico Objetivo**: Dispositivo Android con identificador serial ADB `fb74ec96`.
- **Instalación y Lanzamiento Atómico**:
  ```bash
  adb -s fb74ec96 install -r app/build/outputs/apk/debug/app-debug.apk && adb -s fb74ec96 shell monkey -p com.example.tsuki -c android.intent.category.LAUNCHER 1
  ```
  - El flag `-r` preserva la base de datos SQLite (`tsuki_playlists.db`, `lyrics.db`), las canciones descargadas y las preferencias en DataStore.
  - El comando `monkey` dispara el intent `LAUNCHER` oficial para abrir la aplicación de inmediato sin requerir desbloqueo manual.
- **Resolución de Firma Incompatible (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`)**:
  - Si el dispositivo tenía una versión previa firmada con otro certificado de depuración o de release, Android rechazará la actualización.
  - Solución:
    ```bash
    adb -s fb74ec96 uninstall com.example.tsuki
    ```
- **Filtros de Logcat de Alta Prioridad**:
  - Errores y Crashes Fatales:
    ```bash
    adb -s fb74ec96 logcat -d -s AndroidRuntime:E
    ```
  - Trazas de Reproducción y Red:
    ```bash
    adb -s fb74ec96 logcat -d -s TSukiPlaybackService:D PlayerController:D TSukiInnerTube:D
    ```

---

## 2. PARA QUÉ existe (problema que resuelve)
Proporciona el canal de verificación física en hardware real, esencial para diagnosticar problemas de audio hardware, renderizado SurfaceView, PixelCopy y notificaciones del sistema.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `12 - Guia de Desarrollo y Comandos/` como guía de despliegue y hardware.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Serial `-s fb74ec96`**: Especificar siempre el serial en todos los comandos ADB para evitar colisiones si hay emuladores u otros teléfonos conectados.
- **No desinstalar sin avisar**: Desinstalar la app borra la base de datos local y las descargas del usuario; usar `uninstall` únicamente cuando aparezca `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Revisar `logcat -d -s AndroidRuntime:E` de inmediato si la app se cierra inesperadamente durante una prueba.

---

## 6. Flujo y conexiones
- Conectado a: [[07 - Troubleshooting 403 416 Update Incompatible]].

---

## 7. Guía rápida para una IA nueva
- Para instalar y abrir la app: compila con `assembleDebug` y ejecuta la línea encadenada de `install -r` y `monkey`.
