# 04.11 — AudioQualityPolicy y Monitoreo de Red Medida

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gobierna la selección inteligente de calidad de audio para streaming:
- **`AudioQualityPolicy.kt`**:
  - `AUDIO_QUALITY_HIGH`: Prefiere streams Opus a 160 kbps (la máxima fidelidad ofrecida por YouTube Music).
  - `AUDIO_QUALITY_MEDIUM`: Selecciona streams Opus o AAC a ~128 kbps.
  - `AUDIO_QUALITY_LOW`: Selecciona streams AAC a 48-64 kbps para ahorro extremo.
  - `AUDIO_QUALITY_AUTO`: Evalúa la red mediante `MeteredNetworkMonitor`. Si la conexión es de coste medido (datos móviles), conmuta a calidad Media; si es Wi-Fi ilimitada, selecciona calidad Alta.
- **`MeteredNetworkMonitor.kt`**:
  - Escucha eventos del sistema mediante `ConnectivityManager.registerDefaultNetworkCallback`.
  - Detecta si la red actual carece de la capacidad `NET_CAPABILITY_NOT_METERED`.

**Archivos fuente clave:**
- [`network/AudioQualityPolicy.kt:L5-33`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/AudioQualityPolicy.kt#L5-L33)
- [`network/MeteredNetworkMonitor.kt:L11-46`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/MeteredNetworkMonitor.kt#L11-L46)

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene el consumo excesivo de la tarifa de datos móviles del usuario mientras garantiza la mejor experiencia acústica en redes Wi-Fi.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Políticas de consumo de red en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El orden de preferencia en la selección de códecs: Opus sobre AAC para la misma tasa de bits (Opus ofrece mejor compresión y calidad acústica).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Configuración editable por el usuario en `SettingsScreen > Reproductor > Calidad de audio`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - YouTubeExtractor y Ciphers]], [[18 - Modos Audio-Video y Calidad]].

---

## 7. Guía rápida para una IA nueva
- Para resolver la calidad efectiva a solicitar, usa `AudioQualityPolicy.resolveEffectiveQuality(pref, dataSaver, isMetered)`.
