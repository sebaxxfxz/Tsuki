# 04.06 — Paginación Exhaustiva de Continuaciones (100 a 148+ Items)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Implementa el bucle de paginación recursiva para extraer listas de reproducción completas sin truncamiento en `TSukiInnerTubeClient.kt`:
- **El Problema de los 100 Items**: La respuesta inicial de YouTube Music solo devuelve los primeros ~100 elementos de una playlist y un token de continuación.
- **Extracción de Tokens Multiformato**: El método `extractPlaylistContinuation` analiza exhaustivamente los 4 formatos de continuación que utiliza YouTube:
  1. `musicPlaylistShelfRenderer.continuations`
  2. `musicShelfContinuation`
  3. `sectionListContinuation`
  4. `appendContinuationItemsAction`
- **Bucle de Consulta**: Mientras exista un token de continuación, realiza llamadas a `/youtubei/v1/browse?continuation=...`, agrega los nuevos tracks a la lista acumulada y extrae el siguiente token hasta agotar la lista completa (ej. 148, 500 o 1000 canciones).

**Archivos fuente clave:**
- [`network/TSukiInnerTubeClient.kt:L1000-1080`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/TSukiInnerTubeClient.kt#L1000-L1080)

---

## 2. PARA QUÉ existe (problema que resuelve)
Sin este bucle exhaustivo, listas con más de 100 canciones quedaban incompletas, rompiendo la reproducción de listas largas de usuarios.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Lógica de análisis de respuestas JSON de InnerTube en `network/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La cobertura de los 4 tipos de renderers de continuación. YouTube cambia dinámicamente entre ellos según si la playlist es de usuario, álbum o mezcla automática.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Se probó exitosamente en listas de prueba con 148+ temas sin pérdida de pistas.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - TSukiInnerTubeClient y Autenticacion]], [[19 - PlaylistDetailScreen y Virtual LM]].

---

## 7. Guía rápida para una IA nueva
- Si una lista parece cortada a los 100 temas, verifica que el bucle de continuación no se haya interrumpido por una excepción no capturada.
