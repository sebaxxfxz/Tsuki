# 10.04 — SpotifyPlaylistParser (Exportify y Spotify Takeout JSON)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Especialista en importar volcados de biblioteca de Spotify en `playlistimport/SpotifyPlaylistParser.kt`:
- **Formato CSV de Exportify**:
  - Parsea las columnas estándar: `Track Name`, `Artist Name(s)`, `Album Name`, `Duration (ms)`.
  - Separa múltiples artistas separados por coma manteniendo al artista principal al inicio.
- **Formato JSON de Spotify Takeout**:
  - Parsea el volcado oficial GDPR de datos de usuario de Spotify (`{"items": [{"track": {"name": ..., "artists": [...]}}]}`).
  - Extrae el nombre de la playlist, descripción y carátula si están presentes en el JSON.

**Archivos fuente clave:**
- [`playlistimport/SpotifyPlaylistParser.kt:L1-120`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/playlistimport/SpotifyPlaylistParser.kt#L1-L120)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a los usuarios que abandonan Spotify migrar sus listas completas a TSuki de forma 100% legal, privada y sin proporcionar credenciales de acceso.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Parser de Spotify en `playlistimport/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Prohibición estricta de scraping de cookies `sp_dc` o TOTP. Toda la importación de Spotify se mantiene estrictamente basada en archivos.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Pruebas unitarias automatizadas en `SpotifyImportTest.kt`.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[06 - Privacy-Safe y Prohibicion sp_dc TOTP]], [[05 - SpotifyTrackMatcher Bigrama 0.60]].

---

## 7. Guía rápida para una IA nueva
- Para parsear un archivo de Exportify: `SpotifyPlaylistParser.parseExportifyCsv(csvText)`.
