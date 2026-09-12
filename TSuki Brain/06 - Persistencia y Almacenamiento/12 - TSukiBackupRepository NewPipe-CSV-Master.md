# 06.12 — TSukiBackupRepository (Importación/Exportación NewPipe y OPML)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Especialista en la interoperabilidad de suscripciones con el ecosistema de código abierto:
- **Importación/Exportación NewPipe**: Parsea y genera archivos JSON compatibles con la copia de seguridad de NewPipe (`{"app_version": "...", "subscriptions": [...]}`).
- **Importación/Exportación OPML**: Parsea y genera esquemas OPML XML estándar utilizados por lectores de feeds RSS y clientes de podcasts.
- Permite migrar cientos de canales suscritos desde NewPipe o LibreTube a TSuki con un solo clic.

**Archivos fuente clave:**
- [`data/local/TSukiBackupRepository.kt:L20-218`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/local/TSukiBackupRepository.kt#L20-L218)

---

## 2. PARA QUÉ existe (problema que resuelve)
Garantiza la libertad de datos del usuario, facilitando la transición desde otras aplicaciones libres sin tener que buscar y suscribirse manualmente a cada artista de nuevo.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Operaciones de importación/exportación de suscripciones en `data/local/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- La estructura de tags XML en OPML (`<outline type="rss" xmlUrl="...">`).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Ejecutado en `Dispatchers.IO` para procesar archivos grandes sin congelar la UI.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[11 - TSukiSubscriptionRepository Canonica]], [[04 - Sistema de Backup y Restauracion]].

---

## 7. Guía rápida para una IA nueva
- Para importar suscripciones de NewPipe, invoca `TSukiBackupRepository.importNewPipeSubscriptions(jsonString)`.
