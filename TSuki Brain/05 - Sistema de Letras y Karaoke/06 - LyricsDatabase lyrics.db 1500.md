# 05.06 — LyricsDatabase (`lyrics.db`, prune 1500)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

## 1. QUÉ hace
SQLite crudo sin Room. `getCachedLyrics` retorna `raw+source` (source valida `preferredProvider` en Helper:85-87). `saveLyrics` ignora blank/`LYRICS_NOT_FOUND`. Prune a 1500 más recientes.

**Archivos fuente que documenta:**
- `data/local/LyricsDatabase.kt:8 83L, 13 schema video_id PK/title/artist/raw/source/timestamp, 32 getCachedLyrics, 49 saveLyrics, 60 CONFLICT_REPLACE, 62 prune 1500, 77 singleton`

## 2. PARA QUÉ existe (problema que resuelve)
Sin caché persistente cada repeat gasta red y rompe offline.

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `data/local` junto a `tsuki_playlists.db`/`tsuki_history.db`; `lyrics/` lo consume pero no lo posee.

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO cambiar schema/nombre `lyrics.db` sin migración.
- NO guardar `LYRICS_NOT_FOUND`.
- Singleton `applicationContext`: no pasar Activity.

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
Crudo `SQLiteOpenHelper`, sin Room por control y tamaño. Probar prune con 1501 inserts.

## 6. Flujo y conexiones
Helper: RAM(24) → SQLite → red 16 providers (timeout 4s, ranking word-sync>line>plain) → save → UI `parseLyrics`.

## 7. Guía rápida para una IA nueva
- Lee primero el `QUÉ` y el `Flujo`, luego abre solo los rangos `archivo:línea` citados.
- No reescribas más de 100 líneas sin razón; usa `oldString` mínimo.
- Cero comentarios `//` o `/** */` en `app/src/**/*.kt`. La explicación vive aquí.
- Verifica con `JAVA_HOME=/home/sebaxxfxz/.jdks/openjdk-26.0.2 ./gradlew :app:compileDebugKotlin`.
- Si tocas quirks, prueba en dispositivo `fb74ec96` antes de afirmar.


