# 04.04 — Arquitectura Doble Cliente: WEB_REMIX vs WEB

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Explica por qué `network/TSukiInnerTubeClient.kt` implementa un cliente dual que alterna entre dos identidades de YouTube en sus peticiones:
1. **Cliente `WEB_REMIX`**:
   - `clientName = "WEB_REMIX"`, `clientVersion = "1.20260213.01.00"`.
   - Simula el cliente oficial de YouTube Music Web.
   - Utilizado para: catálogo musical, álbumes, artistas, playlists de estado de ánimo, lista de favoritos ("LM") y exploración musical.
2. **Cliente `WEB`**:
   - `clientName = "WEB"`, `clientVersion = "2.20260710.06.00"`.
   - Simula el cliente oficial de YouTube Web tradicional.
   - Utilizado para: feed de videos generales, búsqueda de videos no musicales, YouTube Shorts y comentarios.

**Archivos fuente clave:**
- [`network/TSukiInnerTubeClient.kt:L53-145`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L53-L145)

---

## 2. PARA QUÉ existe (problema que resuelve)
YouTube segrega internamente sus APIs. Si se consultan Shorts o feeds generales de video con `WEB_REMIX`, la API devuelve errores de shelf no soportado. A la inversa, si se consultan playlists de estados de ánimo musicales con `WEB`, los metadatos de audio especializado no están disponibles.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Estrategia de red central en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Las cadenas de versión de los clientes. Deben mantenerse sincronizadas con versiones activas de YouTube para evitar bloqueos por cliente desactualizado.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Encapsulación automática: cada método público de `TSukiInnerTubeClient` selecciona internamente el cliente adecuado según el tipo de recurso solicitado.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - TSukiInnerTubeClient y Autenticacion]], [[08 - Feed Personalizado y Home]].

---

## 7. Guía rápida para una IA nueva
- Si agregas una función para extraer contenido exclusivo de YouTube Music, configúrala con `clientName = "WEB_REMIX"`.
