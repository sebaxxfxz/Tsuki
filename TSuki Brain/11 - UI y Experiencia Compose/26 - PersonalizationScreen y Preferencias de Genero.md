# 11.26 — PersonalizationScreen y Preferencias de Género

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Panel de ajuste fino del algoritmo de recomendación local (`ui/screens/PersonalizationScreen.kt`):
- **Control Deslizante Descubrimiento vs Familiaridad**: Permite al usuario decidir el balance entre canciones conocidas (alta afinidad) y canciones nuevas por descubrir (exploración de frontera vectorial).
- **Ajuste Manual de Pesos de Género**: Muestra las etiquetas temáticas aprendidas por el motor neuronal con barras de peso que el usuario puede aumentar, disminuir o bloquear completamente.
- **Lista Negra de Artistas y Tópicos**: Gestión de creadores o estilos que el usuario no desea escuchar bajo ninguna circunstancia.
- **Botón de Reinicio Algorítmico**: Opción para restablecer el vector de perfil a su estado original si los gustos del usuario cambian drásticamente.

**Archivos fuente clave:**
- [`ui/screens/PersonalizationScreen.kt:L1-310`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/PersonalizationScreen.kt#L1-L310)
- [`data/recommendation/TSukiTopicCatalog.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiTopicCatalog.kt)
- [`data/recommendation/TSukiNeuroEngine.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiNeuroEngine.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Otorga total soberanía al usuario sobre el algoritmo de IA, eliminando la frustración común de "quedar atrapado en una burbuja de recomendaciones" típica de plataformas comerciales.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` accesible desde la configuración o desde el encabezado de recomendaciones de Home.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Normalización de Pesos tras Modificación Manual**: Al guardar los cambios manuales del usuario, el vector completo debe volver a normalizarse con norma $L_2$ mediante `TSukiVectorMath.normalize` para mantener coherentes los cálculos de similitud coseno.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Sliders con retroalimentación háptica suave en cada escalón de cambio de peso.

---

## 6. Flujo y conexiones
- Modifica directamente: [[02 - Modelos y Algebra Vectorial|07.02 - Modelos Vectoriales]] en `TSukiNeuroEngine`.

---

## 7. Guía rápida para una IA nueva
- Para recalibrar el motor tras cambios en esta pantalla, llama a `TSukiNeuroEngine.getInstance(context).rebalanceProfile()`.
