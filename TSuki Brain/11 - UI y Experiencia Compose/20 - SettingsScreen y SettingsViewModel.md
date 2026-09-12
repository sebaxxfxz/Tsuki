# 11.20 — SettingsScreen y SettingsViewModel

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Panel de configuración y preferencias avanzadas de TSuki (`ui/screens/SettingsScreen.kt` y `ui/viewmodels/SettingsViewModel.kt`):
- **Categorías de Configuración Estructuradas**:
  - **Audio y Reproducción**: Selección de calidad (Baja 64k, Media 128k, Alta 160k Opus/256k AAC, Lossless FLAC), duración de crossfade (0 a 12s), normalización de volumen ReplayGain, salto de silencios.
  - **Apariencia y Tema**: Modo Oscuro forzado / Claro / Sistema, tema Negro Puro OLED, colores dinámicos Monet (Android 12+), personalización de estilo de navbar.
  - **Letras y Karaoke**: Proveedores de letras (Musixmatch, NetEase, LRCLIB, TTML), tamaño de fuente de letras, visualización palabra por palabra.
  - **Caché y Almacenamiento**: Limpieza de caché de audio, caché de carátulas Coil, caché de videos Canvas y base de datos de letras.
  - **Cuenta y Sincronización**: Estado de inicio de sesión en YouTube Music, alternador de sincronización de Me Gusta, token de salas "Escuchar Juntos".
  - **Acerca de**: Versión de la app, enlaces a repositorio y comprobación de actualizaciones.

**Archivos fuente clave:**
- [`ui/screens/SettingsScreen.kt:L1-520`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/SettingsScreen.kt#L1-L520)
- [`ui/viewmodels/SettingsViewModel.kt:L1-310`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/viewmodels/SettingsViewModel.kt#L1-L310)
- [`ui/components/settings/SettingsGroup.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/settings/SettingsGroup.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Ofrece al usuario control absoluto sobre el comportamiento acústico, visual y de red de la aplicación mediante una interfaz organizada y respetuosa de Material Design 3.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Pantalla en `ui/screens/` y lógica de estado en `ui/viewmodels/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Persistencia en DataStore**: Todas las mutaciones deben pasar por `SettingsViewModel` que escribe en los repositorios de DataStore correspondientes (`PlayerPreferences`, `AppearancePreferences`, `HomePreferences`). No acceder a DataStore de forma síncrona o bloqueante.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Elementos agrupados visualmente con `SettingsGroup` utilizando esquinas redondeadas de `20dp` y separadores sutiles entre filas.

---

## 6. Flujo y conexiones
- Utiliza componentes de: [[31 - Componentes Settings (SettingsGroup, Sliders, Toggles, Actions)]].
- Almacena en: [[13 - PlayerPreferences DataStore|06.06 - Preferencias DataStore]].

---

## 7. Guía rápida para una IA nueva
- Para añadir una nueva preferencia, agrégala en el DataStore correspondiente, expón el Flow en `SettingsViewModel` y añade un `TogglePreference` o `SliderPreference` en `SettingsScreen.kt`.
