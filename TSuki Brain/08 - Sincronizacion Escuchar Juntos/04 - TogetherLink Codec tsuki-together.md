# 08.04 — TogetherLink (Codec de Enlaces y Códigos de Sala)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Codifica y decodifica enlaces y credenciales de unión a salas en `together/TogetherLink.kt`:
- **Formato 1: Deep Link Oficial (`tsuki://together`)**:
  - `tsuki://together?host=192.168.1.50&port=42117&sid=ROOM123&key=PASS`
  - Permite unirse con un solo toque desde WhatsApp, Telegram o SMS.
- **Formato 2: Código Compacto Pipe**:
  - Formato: `host|port|roomId|secretKey`.
  - Diseñado para ser dictado o copiado rápidamente como texto corto.
- **Formato 3: URL Directa WebSocket**:
  - `ws://...` o `wss://...` para conexiones en la nube o depuración directa.

**Archivos fuente clave:**
- [`together/TogetherLink.kt:L8-89`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherLink.kt#L8-L89)

---

## 2. PARA QUÉ existe (problema que resuelve)
Simplifica radicalmente la experiencia de unión a salas para los usuarios, soportando tanto enlaces web como códigos alfanuméricos cortos.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Utilidad de codificación en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El esquema `tsuki://together` y los nombres de parámetros de consulta (`host`, `port`, `sid`, `key`).

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Parsea tolerando espacios accidentales en los extremos (`trim()`).

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[06 - Deep Links Intents y Compatibilidad]], [[28 - SubscriptionsScreen y RSS de Canales]].

---

## 7. Guía rápida para una IA nueva
- Para generar el enlace para compartir: `TogetherLink.encodeLink(sessionParams)`.
