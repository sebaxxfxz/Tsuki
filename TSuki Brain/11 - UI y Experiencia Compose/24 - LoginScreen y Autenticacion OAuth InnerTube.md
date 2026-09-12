# 11.24 — LoginScreen y Autenticación OAuth InnerTube

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Pantalla de inicio de sesión segura para conectar la cuenta de Google / YouTube Music (`ui/screens/LoginScreen.kt`):
- **WebView Aislado Oficial**: Abre la página oficial de autenticación de Google (`accounts.google.com`) dentro de un componente `AndroidView` seguro.
- **Extractor Silencioso de Cookies**:
  - Intercepta las cabeceras de respuesta y la cookie jar tras el inicio de sesión exitoso.
  - Extrae los tokens criptográficos esenciales de InnerTube: `SAPISID`, `SSID`, `HSID`, `SID`, `__Secure-3PSID` y `VISITOR_INFO1_LIVE`.
- **Cálculo de Cabecera `SAPISIDHASH`**: Genera la firma SHA-1 requerida para autenticar peticiones a la API privada `WEB_REMIX`.
- **Almacenamiento Seguro**: Persiste los tokens cifrados localmente en `YouTubeAuthManager` para mantener la sesión activa de forma indefinida.

**Archivos fuente clave:**
- [`ui/screens/LoginScreen.kt:L1-280`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/ui/screens/LoginScreen.kt#L1-L280)
- [`network/YouTubeAuthManager.kt:L1-190`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/network/YouTubeAuthManager.kt#L1-L190)

---

## 2. PARA QUÉ existe (problema que resuelve)
Permite a TSuki acceder a las listas privadas, historial de reproducción, suscripciones y canciones favoritas del usuario sin vulnerar sus credenciales ni violar políticas de seguridad.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
En `ui/screens/` como pantalla modal de configuración de cuenta.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- **Privacidad Total de Credenciales**: Nunca registrar ni almacenar el usuario o contraseña en texto plano ni en logs de depuración (`Log.d`). Solo se capturan las cookies de sesión resultantes devueltas por el servidor de Google.
- **User-Agent del WebView**: Debe mantenerse configurado con el User-Agent estándar de Chrome para evitar que Google bloquee el inicio de sesión con el mensaje "Este navegador no es compatible".

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Cierre automático de la pantalla al detectar la cookie `SAPISID` y redirección inmediata con mensaje de confirmación al usuario.

---

## 6. Flujo y conexiones
- Alimenta a: `network/YouTubeAuthManager.kt` y [[01 - TSukiInnerTubeClient y Autenticacion|04.01 - TSukiInnerTubeClient]].

---

## 7. Guía rápida para una IA nueva
- Para comprobar si el usuario está autenticado en la UI, observa `YouTubeAuthManager.isLoggedInFlow`.
