# 02.07 — Manifiesto Android y Servicios

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Analiza la configuración central en `app/src/main/AndroidManifest.xml`:
- **Permisos Declarados**: `INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO` (para reconocimiento Shazam), `POST_NOTIFICATIONS` (Android 13+), `FOREGROUND_SERVICE` con tipos específicos `mediaPlayback` y `dataSync` (Android 14+), `WAKE_LOCK`, `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`.
- **Servicio de Reproducción**: `TSukiPlaybackService` declarado con `android:foregroundServiceType="mediaPlayback"` y filtro de intención `androidx.media3.session.MediaLibraryService`.
- **Compatibilidad con Android Auto**: Referencia al descriptor de aplicación vehicular `res/xml/automotive_app_desc.xml`.
- **Reglas de Backup**: `res/xml/backup_rules.xml` y `res/xml/data_extraction_rules.xml` para proteger credenciales y bases de datos.
- **Proveedores de Archivos (`FileProvider`)**: Configurado con `res/xml/file_paths.xml` para compartir capturas de estadísticas y tarjetas a través de la caché privada.

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza el cumplimiento normativo con Android 14 y 15 (Target SDK 35 / Compile SDK 36), evitando que el sistema operativo mate el servicio de fondo o bloquee la ejecución por falta de permisos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Descriptor maestro del paquete Android en `02 - Arquitectura Global`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No remover `<uses-sdk tools:overrideLibrary="com.mocharealm.accompanist.lyrics.ui" />`.
- No alterar los tipos de servicio en primer plano `mediaPlayback` y `dataSync`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Mantener los componentes exportados estrictamente necesarios con `android:exported="true"` protegidos por filtros de intención válidos.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[04 - Entry Points y Grafo de Arranque]], [[03 - Servicio de Fondo y Media3 Session]].

---

## 7. Guía rápida para una IA nueva
- Si añades un `BroadcastReceiver` para widgets o acciones del sistema, regístralo explícitamente en el manifiesto.
