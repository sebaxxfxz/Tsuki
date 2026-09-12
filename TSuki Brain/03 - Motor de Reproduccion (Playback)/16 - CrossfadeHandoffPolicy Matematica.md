# 03.16 — CrossfadeHandoffPolicy (Matemática de Potencia Constante)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Aísla la matemática pura y los umbrales de sincronización acústica del crossfade para permitir pruebas unitarias limpias sin dependencias del framework de Android:
- **Curva de Igual Potencia (*Equal-Power Crossfade*)**:
  - Ganancia saliente: $G_{out} = \cos(p \cdot rac{\pi}{2})$
  - Ganancia entrante: $G_{in} = \sin(p \cdot rac{\pi}{2})$
  - Donde $p \in [0.0, 1.0]$ es el progreso normalizado del fundido.
  - Satisface la identidad acústica: $G_{out}^2 + G_{in}^2 = 1.0$, manteniendo el volumen acústico percibido perfectamente uniforme.
- **Constantes Nucleares**:
  - `PREPARE_AHEAD_MS = 8000L`: Tiempo previo al fin de la pista donde se prepara el reproductor secundario.
  - `END_GUARD_MS = 300L`: Margen de seguridad antes del final absoluto del archivo.
  - `FRAME_MS = 50L`: Intervalo de ajuste de volumen (20 pasos por segundo).
  - `MAX_ALLOWED_DRIFT_MS = 75L`: Tolerancia máxima de desfase antes de forzar un seek correctivo.

**Archivos fuente clave:**
- [`playback/CrossfadeHandoffPolicy.kt:L6-43`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/CrossfadeHandoffPolicy.kt#L6-L43)
- [`app/src/test/java/com/example/tsuki/playback/CrossfadeHandoffPolicyTest.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/test/java/com/example/tsuki/playback/CrossfadeHandoffPolicyTest.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Un fundido lineal tradicional ($1 - p$ y $p$) provoca una caída de volumen de -3dB a mitad de la transición que se percibe como un bache de silencio. Esta política garantiza transiciones con calidad de estudio de grabación.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Módulo matemático desacoplado en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La fórmula trigonométrica de `equalPowerGains` y la constante $rac{\pi}{2}$.
- Las pruebas unitarias en `CrossfadeHandoffPolicyTest` deben pasar siempre al 100%.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Verificación mediante tests unitarios rápidos de JUnit sin necesidad de emulador.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[02 - Dual-ExoPlayer Crossfade y Handoff]], [[06 - Testing Unitario y Sin CI]].

---

## 7. Guía rápida para una IA nueva
- Ejecuta `./gradlew testDebugUnitTest` tras modificar cualquier parámetro de esta política.
