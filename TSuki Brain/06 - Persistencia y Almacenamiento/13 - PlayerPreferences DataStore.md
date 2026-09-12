# 06.13 — PlayerPreferences (Ajustes de Audio y Reproductor)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Define las claves y los valores por defecto para el reproductor en `data/local/PlayerPreferences.kt`:
- `audioQuality`: `AUDIO_QUALITY_AUTO`, `AUDIO_QUALITY_HIGH`, `AUDIO_QUALITY_MEDIUM`, `AUDIO_QUALITY_LOW`.
- `crossfadeEnabled`: Booleano (false por defecto).
- `crossfadeDurationSeconds`: Float de 0.5f a 12.0f (5.0f por defecto).
- `cacheSizeMb`: Entero (128 a 2048 MB, 1024 MB por defecto; -1 para ilimitado).
- `playbackSpeed`: Float de 0.25f a 2.0f (1.0f por defecto).
- `lyricsProvider`: String con el nombre del proveedor preferido ("Auto" por defecto).
- `lyricsSyncOffsetMs`: Offset de calibración manual (-2000ms a +2000ms, 0ms por defecto).
- `audioOffloadEnabled`: Booleano para ahorro de batería mediante DSP de hardware.

**Archivos fuente clave:**
- [`data/local/PlayerPreferences.kt:L26-150`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/PlayerPreferences.kt#L26-L150)

---

## 2. PARA QUÉ existe (problema que resuelve)
Mantiene todas las preferencias de audio accesibles reactivamente mediante `Flow` inmutables.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Preferencias del reproductor en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los nombres literales de las claves de preferencias.
- Los rangos de validación con `coerceIn`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Consumido directamente por `PlayerController` y `SettingsViewModel`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - PlayerController Central]], [[20 - SettingsScreen y SettingsViewModel]].

---

## 7. Guía rápida para una IA nueva
- Para actualizar el crossfade: `playerPreferences.setCrossfadeDuration(seconds)`.
