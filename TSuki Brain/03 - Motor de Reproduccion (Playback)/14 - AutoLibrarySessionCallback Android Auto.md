# 03.14 — AutoLibrarySessionCallback (Soporte Android Auto)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa el callback `MediaLibrarySession.Callback` para interactuar con sistemas externos que consumen el MediaBrowser de Android (especialmente Android Auto, Android Automotive OS y relojes Wear OS):
- Expone el árbol jerárquico de navegación con nodos estables:
  - `tsuki_root`: Raíz del explorador.
  - `tsuki_favorites`: Canciones favoritas locales y remotas.
  - `tsuki_recent`: Historial de escucha reciente.
  - `tsuki_most_played`: Canciones más escuchadas.
  - `tsuki_playlists`: Listas de reproducción del usuario.
  - `tsuki_local`: Música almacenada físicamente en el dispositivo.
  - `tsuki_downloads`: Pistas descargadas offline.
- Maneja búsquedas por voz desde Google Assistant en el vehículo (`onSearch`).
- Resuelve URLs reproducibles (`onGetItem` / `resolveStreamUri`) priorizando archivos descargados locales.

**Archivos fuente clave:**
- [`playback/AutoLibrarySessionCallback.kt:L32-245`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playback/AutoLibrarySessionCallback.kt#L32-L245)
- [`res/xml/automotive_app_desc.xml`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/res/xml/automotive_app_desc.xml)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite controlar TSuki de forma segura desde la pantalla del salpicadero del coche con la interfaz estandarizada de Android Auto.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Es el backend de navegación de `TSukiPlaybackService` en `playback/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los identificadores de nodo `tsuki_root`, `tsuki_fav_`, `tsuki_recent_`. Android Auto los cachea internamente; cambiarlos causará que las carpetas del coche aparezcan vacías.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Pruebas mediante la herramienta oficial Android Auto Desktop Head Unit (DHU).

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[03 - Servicio de Fondo y Media3 Session]], [[07 - Manifiesto Android y Servicios]].

---

## 7. Guía rápida para una IA nueva
- Toda canción servida a Android Auto debe tener metadatos completos (`title`, `artist`, `artworkUri`).
