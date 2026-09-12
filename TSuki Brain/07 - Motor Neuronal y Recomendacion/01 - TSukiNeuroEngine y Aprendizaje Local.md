# 07.01 — TSukiNeuroEngine y Aprendizaje Local On-Device

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Motor de inteligencia artificial y recomendación musical ejecutado 100% en el dispositivo (`data/recommendation/TSukiNeuroEngine.kt`):
- **Vector de Afinidad del Usuario**: Modela el perfil musical del usuario como un vector multidimensional que pondera géneros, subgéneros, duración preferida, tempo/ritmo y complejidad instrumental.
- **Aprendizaje Continuo en Tiempo Real**:
  - Reproducción completa (>= 85%): Refuerza los tópicos de la canción mediante media móvil exponencial (EMA).
  - Me Gusta (Favorito): Aplica un impulso de momento (*Momentum Boost*) multiplicando por 2.5x los pesos temáticos.
  - Salto Prematuro (*Skip* < 30s): Penaliza el vector temático y añade fatiga temporal al artista.
  - Descarte Explicito ("No me gusta este artista"): Reduce a 0 la afinidad del canal y lo excluye del ranking.
- **Predicción de Persona Musical (`TSukiPersona`)**: Determina la identidad auditiva del usuario ("Explorador Ecléctico", "Purista de Género", "Nostálgico Nocturno", etc.).

**Archivos fuente clave:**
- [`data/recommendation/TSukiNeuroEngine.kt:L20-781`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiNeuroEngine.kt#L20-L781)
- [`data/recommendation/TSukiBrainStorage.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiBrainStorage.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Provee recomendaciones musicales hiper-personalizadas con la misma calidad algorítmica de Spotify o TikTok sin enviar un solo dato de telemetría a servidores corporativos y sin requerir cuenta.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Núcleo algorítmico en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El archivo de persistencia `tsuki_brain_v1.json`.
- La tasa de decaimiento temporal: si el usuario deja de escuchar un género, su peso decae suavemente sin borrarse abruptamente.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Ejecución asíncrona en `Dispatchers.Default` para no impactar la fluidez visual de Compose.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Modelos y Algebra Vectorial]], [[07 - TSukiNeuroEngine Ranking 781 Lineas]].

---

## 7. Guía rápida para una IA nueva
- Para registrar el consumo de una pista en el motor neuronal, llama a `TSukiNeuroEngine.getInstance(context).onPlaybackCompleted(track, percentPlayed)`.
