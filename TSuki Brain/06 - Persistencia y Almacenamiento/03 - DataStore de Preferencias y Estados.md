# 06.03 — DataStore de Preferencias y Estados Reactivos

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la persistencia de configuraciones reactivas mediante Jetpack Preferences DataStore:
- **`player_preferences` (`PlayerPreferences.kt`)**:
  - Tasa de bits de audio (`LOW`, `MEDIUM`, `HIGH`, `AUTO`).
  - Crossfade habilitado y duración (0.5s a 12.0s).
  - Velocidad de reproducción (0.25x a 2.0x).
  - Tamaño de caché en disco (MB) y Audio Offload.
  - Normalización de volumen y presets de ecualizador.
  - Proveedor de letras preferido y offset de sincronización (-2000ms a +2000ms).
- **`appearance_preferences` (`AppearancePreferences.kt`)**:
  - Modo de tema (Sistema, Claro, Oscuro).
  - Modo Negro Puro OLED (True Black).
  - Soporte Material You (Dynamic Color).
  - Radio de esquinas de carátula.
- **`home_preferences` (`HomePreferences.kt`)**:
  - Disposición de tarjetas en Home (`FULL_WIDTH`, `COMPACT`, `GRID`).
  - Canales bloqueados y categorías visibles.
- **`tsuki_subscriptions` (`TSukiSubscriptionRepository.kt`)**:
  - Lista de canales de YouTube suscritos localmente.

---

## 2. PARA QUÉ existe (problema que resuelve)
Reemplaza `SharedPreferences` tradicional por una API asíncrona, segura ante caídas y totalmente compatible con flujos reactivos `Flow<T>` de Kotlin Coroutines.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Persistencia de ajustes en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los nombres de archivo de DataStore y las claves tipadas (`preferencesKey`).
- No utilizar `runBlocking` para leer preferencias en el arranque; usar flujos con valores por defecto.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Mutaciones mediante `dataStore.edit { preferences -> preferences[KEY] = value }`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[13 - PlayerPreferences DataStore]], [[14 - HomePreferences y AppearancePreferences]].

---

## 7. Guía rápida para una IA nueva
- Para exponer un nuevo ajuste a Compose, agrégalo a la clase de DataStore respectiva como un `Flow<T>`.
