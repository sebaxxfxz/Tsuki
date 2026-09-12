# 01.06 — Privacy-Safe y Prohibición `sp_dc/TOTP`

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Establece las directrices éticas y legales sobre la integración con servicios de terceros (especialmente Spotify y ArchiveTune):
1. **Prohibición Total de `sp_dc` y TOTP**:
   - El uso de cookies de sesión no oficiales de Spotify (`sp_dc`) y generadores de contraseñas de un solo uso (TOTP) viola flagrantemente los Términos de Servicio de Spotify y pone en riesgo las cuentas de los usuarios.
   - En TSuki, la importación de Spotify se realiza **exclusivamente mediante archivos locales**:
     - Volcados CSV de herramientas web legítimas (como Exportify).
     - Archivos JSON de copias de seguridad de datos oficiales (Spotify Takeout).
     - Listas M3U exportadas.
   - Si en el futuro se implementa sincronización en vivo con Spotify, se deberá utilizar la API oficial con flujo **OAuth 2.0 PKCE**, jamás scraping de cookies.
2. **Prohibición de Copiar Código de ArchiveTune**:
   - El código de ArchiveTune solo debe servir de referencia conceptual o inspiración funcional.
   - Toda implementación en TSuki debe escribirse desde cero utilizando las APIs modernas nativas de AndroidX, Ktor y SQLiteDatabase.

---

## 2. PARA QUÉ existe (problema que resuelve)
Protege la privacidad del usuario, evita baneos de cuentas en plataformas de streaming y mantiene el proyecto protegido frente a reclamos de propiedad intelectual o infracciones de ToS.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Frontera ética y de seguridad en `01 - Reglas y Zona de Peligro`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No incorporar scripts ni dependencias que soliciten contraseñas o cookies privadas de Spotify.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- El importador parsea archivos estáticos y resuelve las canciones contra el catálogo público de YouTube Music mediante `ImportSongResolver` y `FuzzyMatcher`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Parsers Multiplataforma y ArchiveTune]], [[04 - SpotifyPlaylistParser Exportify Oficial Web]].

---

## 7. Guía rápida para una IA nueva
- Si el usuario te pide conectar con Spotify, utiliza únicamente los parsers de archivos locales existentes en `playlistimport/`.
