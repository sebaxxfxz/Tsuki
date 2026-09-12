# 06.01 — Arquitectura de Persistencia: SQLite Nativo Sin Room

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Explica y justifica la decisión arquitectónica de implementar persistencia basada en SQLite nativo de Android (`android.database.sqlite.SQLiteOpenHelper` y `SQLiteDatabase`) prescindiendo totalmente de Room ORM:
- **Cero Procesamiento de Anotaciones (KSP/KAPT)**: Room requiere procesar `@Entity`, `@Dao` y `@Database` en cada compilación, lo que incrementa los tiempos de Gradle en 15-30 segundos. Sin Room, la compilación de Kotlin tarda ~2 segundos.
- **Consultas Directas Optimizadas**: Control granular de sentencias SQL crudas, transacciones manuales atómicas (`beginTransaction()`, `setTransactionSuccessful()`, `endTransaction()`), optimizaciones `VACUUM` y pragmas de base de datos.
- **Reducción del Tamaño de APK**: Ahorro en clases autogeneradas y código generado de intermediación.
- **Persistencia Reactiva Híbrida**: Se emiten actualizaciones a Compose combinando SQLite con `MutableStateFlow<Long>` (versiones de base de datos monotónicas) que invalidan las listas en UI de forma reactiva.

**Archivos fuente clave:**
- [`data/local/LocalPlaylistManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/LocalPlaylistManager.kt)
- [`data/local/WatchHistoryManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/WatchHistoryManager.kt)
- [`data/local/FavoritesManager.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/FavoritesManager.kt)
- [`data/local/LyricsDatabase.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/LyricsDatabase.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Mantiene un toolchain de compilación ultrarrápido y un consumo mínimo de memoria, crítico para la agilidad en sesiones de desarrollo e iteración interactiva con IAs.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Principio fundacional de almacenamiento en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO intentar migrar las bases de datos a Room. Violará el requerimiento de compilación en 2 segundos y romperá esquemas existentes.
- Toda operación de base de datos debe ejecutarse estrictamente en `Dispatchers.IO`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Métodos auxiliares de migración segura `migrateTablePreservingData` que consultan `PRAGMA table_info` antes de agregar columnas.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Esquema de Bases de Datos SQLite]], [[05 - LocalPlaylistManager tsuki_playlists.db]].

---

## 7. Guía rápida para una IA nueva
- Para crear una nueva tabla, añade la sentencia `CREATE TABLE IF NOT EXISTS` en el `onCreate` del helper respectivo.
