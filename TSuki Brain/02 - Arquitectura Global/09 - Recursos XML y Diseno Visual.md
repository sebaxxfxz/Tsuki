# 02.09 — Recursos XML y Diseño Visual

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Inventario de recursos nativos en `app/src/main/res/`:
- **Icono de Notificación**: `drawable/media3_notification_small_icon.xml` (vector blanco monocromo sin transparencias intermedias).
- **Atajos Dinámicos (`shortcuts.xml`)**: Atajos de acceso directo en el launcher para "Buscar", "Mis Me Gusta" y "Reconocer Canción".
- **Widgets Clásicos y Glance**: Diseños XML de vista previa (`widget_preview_*.xml`) y metadatos de configuración de widgets (`tsuki_glance_widget_info*.xml`).
- **Valores del Sistema**: `res/values/strings.xml` para cadenas localizadas, `res/values/themes.xml` para el tema base de la ventana antes de inicializar Compose.

---

## 2. PARA QUÉ existe (problema que resuelve)
Alberga los recursos obligatorios que Android requiere a nivel de sistema antes de renderizar la UI de Jetpack Compose.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Capa de recursos Android en `02 - Arquitectura Global`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- `media3_notification_small_icon.xml`: Debe mantenerse monocromo puro; en Android 12+ los iconos de notificación con color o degradados se renderizan como cuadrados grises.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Modificaciones mínimas y quirúrgicas en XML; la mayor parte del diseño reside en composables declarativos.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Invariantes Intocables y Quirks Criticos]], [[34 - Widgets Glance y TSukiGlanceReceiver (Keys, Sync, Actions, Theme)]].

---

## 7. Guía rápida para una IA nueva
- Si necesitas un nuevo string visible en widgets clásicos o notificaciones, agrégalo a `res/values/strings.xml`.
