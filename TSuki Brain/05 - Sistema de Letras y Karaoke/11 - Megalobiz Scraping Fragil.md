# 05.11 — Megalobiz (Scraping de Respaldo Frágil)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Proveedor de scraping web sobre el portal comunitario Megalobiz (`megalobiz.com/search?qry=...`):
- Realiza una búsqueda web simulada y extrae los enlaces de resultados mediante expresiones regulares HTML sobre `<a class="entity_name">`.
- Descarga el contenido del archivo `.lrc` almacenado en la página de detalle.
- Debido a su naturaleza de web scraper HTML, es vulnerable a cambios de diseño del sitio web.

**Archivos fuente clave:**
- [`lyrics/providers/MegalobizLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/MegalobizLyricsProvider.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Actúa como salvavidas de contingencia para canciones hispanas y de géneros populares de los años 90 y 2000 que solo existen catalogadas en foros antiguos de karaoke.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Proveedor en `lyrics/providers/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Su posición tardía en la jerarquía (posición 14): nunca debe adelantarse a APIs JSON formales debido a la lentitud del scraping HTML.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Timeout estricto de 4 segundos con control de excepciones `try-catch` para no bloquear la cadena si el sitio cae.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Orquestador y 16 Proveedores]], [[08 - Grupo LRC LrcLib Netease KuGou]].

---

## 7. Guía rápida para una IA nueva
- Si Megalobiz cambia su HTML, actualiza los selectores regex en `MegalobizLyricsProvider.kt`.
