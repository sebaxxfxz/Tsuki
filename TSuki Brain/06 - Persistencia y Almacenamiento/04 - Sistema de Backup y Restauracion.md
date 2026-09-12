# 06.04 — Sistema de Respaldo y Restauración Unificada JSON

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa la copia de seguridad y migración integral de la biblioteca del usuario en `data/backup/BackupManager.kt`:
- **Formato Unificado JSON (`version: 1`)**:
  - Serializa en un único archivo JSON:
    - Todas las playlists locales y sus canciones en orden exacto.
    - Biblioteca de favoritos locales.
    - Historial agregado de reproducciones.
    - Tags y estados de ánimo asignados.
    - Historial de canciones reconocidas por Shazam.
- **Restauración Atómica con Fusión (*Merge Support*)**:
  - Permite al usuario elegir entre:
    - **Reemplazar Todo**: Borra las bases de datos previas e inserta el respaldo.
    - **Fusionar (*Merge*)**: Combina canciones y playlists sin duplicar IDs existentes.
- **Exportación Segura vía Storage Access Framework (SAF)**:
  - Genera el archivo mediante `Intent.ACTION_CREATE_DOCUMENT` para que el usuario guarde su respaldo en Google Drive, tarjeta SD o almacenamiento interno.

**Archivos fuente clave:**
- [`data/backup/BackupManager.kt:L39-294`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/backup/BackupManager.kt#L39-L294)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza la portabilidad completa de datos del usuario y la tranquilidad de no perder listas creadas a lo largo de los años al cambiar o formatear el teléfono.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Módulo de respaldo en `data/backup/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El campo `"version": 1` en la cabecera del JSON de respaldo.
- El uso de transacciones SQLite en la restauración: si el JSON está corrupto, la transacción hace rollback y no destruye la base de datos previa.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Notificación de progreso y resultado mediante Snackbar en `SettingsScreen`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Esquema de Bases de Datos SQLite]], [[20 - SettingsScreen y SettingsViewModel]].

---

## 7. Guía rápida para una IA nueva
- Para respaldar la biblioteca, llama a `BackupManager.getInstance(context).createBackupJson()`.
