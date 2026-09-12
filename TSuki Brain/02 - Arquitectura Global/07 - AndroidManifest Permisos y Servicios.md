# 02.07 — AndroidManifest: Permisos y Servicios

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
Declara INTERNET/NETWORK/WIFI (InnerTube/NewPipe/RSS), FOREGROUND+MEDIA_PLAYBACK+WAKE (fondo), POST_NOTIF (player+updates), REQUEST_INSTALL (auto-update), READ_MEDIA_AUDIO/VIDEO + READ_EXTERNAL max32 (local), RECORD_AUDIO (Shazam), IGNORE_BATTERY. `overrideLibrary lyrics-ui` permite minSdk 24 con lib minSdk 29. Servicio `mediaPlayback exported=true` para fondo/lockscreen/Auto. FileProvider `${applicationId}.fileprovider` para PixelCopy share.

**Archivos fuente que documenta:**
- `app/src/main/AndroidManifest.xml:179L`
- `.TSukiApp Theme.TSuki`, `.playback.TSukiPlaybackService`, `.util.UpdateDownloadReceiver`, 6 widgets Glance

## 2. PARA QUÉ existe (problema que resuelve)
Sin esto no hay red, fondo, Auto, updates ni deeplinks aunque el Kotlin sea perfecto.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En 02 porque es contrato con el sistema, no código.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO quitar `overrideLibrary`, `foregroundServiceType`, `exported`, schemes/hosts, authorities.
- NO subir `minSdk` ni quitar override sin probar `fb74ec96`.
- Media3 1.5.1 sin `setSmallIcon`: usar `drawable/media3_notification_small_icon` (vector blanco).

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Cambios de manifest se prueban instalando y abriendo desde launcher + share + Auto + widget.

## 6. Flujo y conexiones
Servicio START_STICKY, mata solo si `!playWhenReady` en `onTaskRemoved`. Wake PARTIAL. Canal `tsuki_playback LOW+noBadge`.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


