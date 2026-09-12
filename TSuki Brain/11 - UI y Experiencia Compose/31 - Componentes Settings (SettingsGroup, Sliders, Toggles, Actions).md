# 11.31 — Componentes Settings (SettingsGroup, Sliders, Toggles, Actions)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Kit de componentes reutilizables y estandarizados para construir todas las pantallas de opciones y menús de la aplicación (`ui/components/settings/`):
- **`SettingsGroup`**: Contenedor visual que agrupa un conjunto de preferencias bajo una tarjeta con esquinas redondeadas de `20dp`, fondo `surfaceContainerLow` y título de cabecera con tipografía `labelLarge`.
- **`TogglePreference`**: Fila con icono leading, título, descripción y un `Switch` de Material 3 Expressive que anima su estado y emite cambios booleanos.
- **`SliderPreference`**: Fila interactiva para ajustes numéricos continuos o por pasos (ej. segundos de crossfade, tamaño de caché), con valor numérico visible y formato personalizado.
- **`ListPreference`**: Fila que al tocarse despliega un diálogo modal de selección de opción única con botones de radio (`RadioButton`) y persistencia del índice elegido.
- **`ActionPreference`**: Fila para disparar acciones inmediatas (ej. "Limpiar caché", "Cerrar sesión", "Buscar actualizaciones") con icono trailing de flecha o confirmación.

**Archivos fuente clave:**
- [`ui/components/settings/SettingsGroup.kt:L1-80`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/settings/SettingsGroup.kt#L1-L80)
- [`ui/components/settings/TogglePreference.kt:L1-95`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/settings/TogglePreference.kt#L1-L95)
- [`ui/components/settings/SliderPreference.kt:L1-110`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/settings/SliderPreference.kt#L1-L110)
- [`ui/components/settings/ListPreference.kt:L1-130`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/settings/ListPreference.kt#L1-L130)
- [`ui/components/settings/ActionPreference.kt:L1-85`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/components/settings/ActionPreference.kt#L1-L85)

---

## 2. PARA QUÉ existe (problema que resuelve)
Elimina la duplicación de código en menús de ajustes y garantiza una consistencia milimétrica en márgenes, padding, colores y comportamiento táctil en toda la aplicación.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Aislado en `ui/components/settings/` como subsistema de componentes especializados de configuración.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Áreas Táctiles Mínimas**: No reducir el padding vertical de las filas por debajo de `12.dp` ni la altura total por debajo de `56.dp` para garantizar el cumplimiento de las pautas de accesibilidad táctil de Google.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Todos los componentes reciben un parámetro `modifier: Modifier = Modifier` como primer argumento opcional para respetar las directrices oficiales de Compose.

---

## 6. Flujo y conexiones
- Consumido por: [[20 - SettingsScreen y SettingsViewModel]], [[26 - PersonalizationScreen y Preferencias de Genero]].

---

## 7. Guía rápida para una IA nueva
- Para añadir un nuevo ajuste a cualquier pantalla, envuélvelo dentro de un `SettingsGroup` existente utilizando el componente de preferencia adecuado.
