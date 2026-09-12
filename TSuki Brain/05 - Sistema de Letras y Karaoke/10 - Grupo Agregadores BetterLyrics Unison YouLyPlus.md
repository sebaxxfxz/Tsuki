# 05.10 — Grupo de Agregadores (BetterLyrics, Unison y YouLyPlus)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Proveedores agregadores comunitarios de alto nivel:
- **`BetterLyricsProvider` & `BetterLyricsPortatoProvider`**: Servidores que compilan bases de datos de letras avanzadas con soporte para Enhanced LRC y marcas silábicas. Portato actúa como nodo espejo de baja latencia.
- **`UnisonLyricsProvider`**: Servicio de sincronización lírica de alta precisión.
- **`YouLyPlusLyricsProvider` & `SimpMusicLyricsProvider`**: Agregadores de metadatos líricos de proyectos de código abierto para YouTube Music.

**Archivos fuente clave:**
- [`lyrics/providers/BetterLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/BetterLyricsProvider.kt)
- [`lyrics/providers/UnisonLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/UnisonLyricsProvider.kt)
- [`lyrics/providers/YouLyPlusLyricsProvider.kt`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/lyrics/providers/YouLyPlusLyricsProvider.kt)

---

## 2. PARA QUÉ existe (problema que resuelve)
Multiplica la redundancia: si las APIs de Paxsenix experimentan cortes o límites de tasa, estos agregadores entregan letras idénticas sin degradar la experiencia de usuario.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Proveedores en `lyrics/providers/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- Los endpoints de API configurados en sus respectivos companion objects.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Detección automática de TTML vs LRC en la respuesta recibida.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Orquestador y 16 Proveedores]], [[07 - Grupo Word-Sync Paxsenix Apple Spotify]].

---

## 7. Guía rápida para una IA nueva
- Para comprobar la respuesta de un agregador específico, puedes aislar la llamada en una prueba unitaria.
