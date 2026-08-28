<div align="center">
  <h1>TSuki 🌙</h1>

  <p><strong>Un reproductor de música moderno para Android con streaming sin anuncios, letras karaoke palabra por palabra, crossfade real, ecualizador integrado y reconocimiento de canciones.</strong></p>

  [![GitHub Release](https://img.shields.io/github/v/release/sebaxxfxz/TSuki?style=for-the-badge&color=6f42c1)](https://github.com/sebaxxfxz/TSuki/releases)
  [![GitHub Downloads](https://img.shields.io/github/downloads/sebaxxfxz/TSuki/total?style=for-the-badge&color=007ec6)](https://github.com/sebaxxfxz/TSuki/releases)
  [![GitHub Stars](https://img.shields.io/github/stars/sebaxxfxz/TSuki?style=for-the-badge&color=e3b341)](https://github.com/sebaxxfxz/TSuki/stargazers)
  [![GitHub Issues](https://img.shields.io/github/issues/sebaxxfxz/TSuki?style=for-the-badge&color=d9534f)](https://github.com/sebaxxfxz/TSuki/issues)
  [![License](https://img.shields.io/github/license/sebaxxfxz/TSuki?style=for-the-badge&color=28a745)](LICENSE)

</div>

---

## 📌 Tabla de Contenidos

- [Visión General](#-visión-general)
- [Características Principales](#-características-principales)
  - [Reproducción & Streaming](#reproducción--streaming)
  - [Letras Sincronizadas](#letras-sincronizadas)
  - [Audio & Ecualización](#audio--ecualización)
  - [Descubrimiento & Biblioteca](#descubrimiento--biblioteca)
  - [Interfaz & Personalización](#interfaz--personalización)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Compilación e Instalación](#-compilación-e-instalación)
- [Especiales Agradecimientos](#-especiales-agradecimientos)
- [Estadísticas del Repositorio](#-estadísticas-del-repositorio)

---

## 🔍 Visión General

**TSuki** es una aplicación nativa para Android construida en **Kotlin** y **Jetpack Compose** que transforma el streaming musical en una experiencia premium: reproducción continua sin anuncios, letras sincronizadas con animación karaoke palabra por palabra, transiciones crossfade reales entre pistas y herramientas de descubrimiento como reconocimiento de audio e importación de playlists de Spotify.

---

## 🚀 Características Principales

### Reproducción & Streaming
- 🚫 **Sin Anuncios**: Streaming limpio y continuo vía InnerTube (YouTube Music) con fallback a NewPipeExtractor.
- 🎚️ **Crossfade Real**: Mezcla equal-power entre pistas con handoff de doble motor ExoPlayer, modo gapless opcional.
- ♾️ **Cola Infinita**: Radio automática basada en la pista actual para que la música nunca se detenga.
- 📋 **Cola Editable**: Reordenamiento por arrastrar y soltar con gestos táctiles.
- 🔗 **Deep-Links de YouTube**: Comparte cualquier video o canción desde YouTube y TSuki lo reproduce junto a su radio.
- 🌙 **Segundo Plano**: Reproducción activa con la pantalla apagada, controles en notificación y widget de inicio.
- ⏲️ **Temporizador de Apagado**: Detén la reproducción al terminar la canción o tras minutos configurados.

### Letras Sincronizadas
- 🎤 **Karaoke Palabra por Palabra**: Soporte Enhanced LRC y TTML con resaltado dinámico por sílaba.
- 📜 **16 Proveedores de Letras**: Búsqueda resiliente multi-fuente (LrcLib, KuGou, Musixmatch-like, BetterLyrics y más).
- 🎨 **Animaciones Fluidas**: Autoscroll suave con corrección de deriva y efectos visuales sincronizados al tempo.

### Audio & Ecualización
- 🎛️ **Ecualizador Gráfico**: Bandas personalizables con preajustes.
- 🔊 **Bass Boost & Virtualizador**: Refuerzo de graves y audio envolvente con ganancia de salida ajustable.
- 💾 **Perfiles Persistentes**: Tu configuración de audio se restaura automáticamente en cada sesión.

### Descubrimiento & Biblioteca
- 🎵 **Reconocimiento de Música**: Identifica canciones del entorno con firma acústica estilo Shazam.
- 📥 **Importar Playlists de Spotify**: Parser nativo con resolución difusa de coincidencias canción por canción.
- 📶 **Modo Sin Conexión**: Descarga canciones para escucharlas sin internet.
- 👥 **Feed de Suscripciones**: Sigue canales, mira Shorts y accede a tu cuenta de YouTube Music.
- 📊 **Estadísticas e Historial**: Registro automático de escuchas y recomendaciones adaptativas.

### Interfaz & Personalización
- 🎨 **Material 3 Expressive**: Colores dinámicos extraídos de la carátula y motion físico basado en springs.
- 🌙 **Modo Oscuro & Claro**: Visualización óptima en cualquier entorno.
- 🧩 **Widget de Inicio**: Control rápido de reproducción desde la pantalla principal.
- 📱 **Mini Reproductor Táctil**: Gestos para expandir, colapsar y deslizar entre pistas.

---

## 🧱 Estructura del Proyecto

Código fuente organizado en paquetes con separación clara de responsabilidades:

| Paquete | Descripción |
| :--- | :--- |
| **`ui/`** | Pantallas Compose, reproductor completo, letras karaoke, temas y widget. |
| **`playback/`** | `PlayerController` central, servicio Media3, motor de crossfade y cola automática. |
| **`lyrics/`** | 16 proveedores de letras y parser Enhanced LRC / TTML. |
| **`network/`** | Cliente InnerTube (YouTube Music) y extractor NewPipe de respaldo. |
| **`data/`** | Repositorios, caché local, historial, suscripciones y descargas. |
| **`domain/`** | Modelos de dominio (`MediaTrack`, `LyricsEntry`) y contratos. |
| **`shazam/`** | Generador de firmas acústicas y reconocimiento de canciones. |
| **`playlistimport/`** | Parsers de playlists de Spotify y resolución difusa de pistas. |

---

## 🛠️ Compilación e Instalación

### Requisitos Previos
- Android Studio con soporte AGP 9+
- JDK 17+
- Android SDK 35+

### Compilación desde Consola

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/sebaxxfxz/TSuki.git
   cd TSuki
   ```

2. **Compilar el APK de depuración:**
   ```bash
   ./gradlew :app:assembleDebug
   ```

El ejecutable estará disponible en: `app/build/outputs/apk/debug/app-debug.apk`

---

## Especiales Agradecimientos

TSuki stands on the shoulders of several excellent open-source projects. Sincere thanks to:

| Project | Description |
| :--- | :--- |
| [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) | YouTube extraction engine and fallback pipeline |
| [Better Lyrics](https://github.com/better-lyrics/better-lyrics) | Lyrics enhancement and synchronization |
| [LrcLib](https://lrclib.net) | Open synchronized lyrics database |
| [InnerTune](https://github.com/z-huang/InnerTune) | Foundational inspiration and architecture reference |

---

## 📈 Estadísticas del Repositorio

### Historial de Estrellas (Star History)

[[![Star History Chart](https://api.star-history.com/svg?repos=sebaxxfxz/TSuki&type=Date)](https://star-history.com/#sebaxxfxz/TSuki&Date)]
### Métricas del Proyecto

<div align="center">
  <img src="https://github-readme-stats.vercel.app/api/pin/?username=sebaxxfxz&repo=TSuki&theme=dark" alt="Estadísticas de TSuki"/>
</div>

---

<div align="center">
  <sub>Licenciado bajo la Licencia Abierta GPL-3.0. Desarrollado con ❤️ para los entusiastas de la música.</sub>
</div>
