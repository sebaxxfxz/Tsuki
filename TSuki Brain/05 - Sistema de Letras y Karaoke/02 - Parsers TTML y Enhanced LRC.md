# 02 - Parsers TTML y Enhanced LRC

> **Ubicación:** `app/src/main/java/com/example/tsuki/lyrics/LyricsUtils.kt`
> **Formatos soportados:** TTML (Timed Text Markup Language), Enhanced LRC (`<mm:ss.xx>`), Standard LRC (`[mm:ss.xx]`), Texto plano.

---

## 📜 Formato 1: Enhanced LRC

El formato Enhanced LRC utiliza marcas angulares para delimitar el instante exacto en el que cada palabra o sílaba debe iluminarse:

```text
[00:14.20] <00:14.20> Me <00:14.45> canse <00:14.90> de <00:15.10> rogarle
```

El regex `ENHANCED_LRC_WORD_TIME_REGEX` extrae minutos, segundos y centésimas:
```kotlin
val ENHANCED_LRC_WORD_TIME_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{2,3}))?>""")
```

---

## 📐 Formato 2: TTML (Apple Music / Musixmatch)

TTML es un estándar XML con estructura jerárquica:
- `<p begin="00:14.20" end="00:18.50">`: Párrafo correspondiente a una línea lírica completa.
- `<span begin="00:14.20" end="00:14.45">Me</span>`: Intervalo temporal de una sílaba individual.

```xml
<p begin="00:14.20" end="00:18.50">
    <span begin="00:14.20" end="00:14.45">Me</span>
    <span begin="00:14.45" end="00:14.90">can</span>
    <span begin="00:14.90" end="00:15.10">se </span>
</p>
```

### Reglas Críticas del Parser de TSuki:
1. **Espacios Inter-Spans:** El texto entre `</span>` y `<span` a menudo contiene espacios vitales. Si se ignoran, las palabras se pegan.
2. **Entidades HTML:** Convierte automáticamente caracteres especiales (`&quot;`, `&#39;`, `&amp;`) mediante `unescapeHtml()`.
3. **Manejo de Duraciones Ausentes:** Si un span no tiene atributo `end`, el parser infiere que termina cuando empieza el span siguiente o cuando finaliza el párrafo padre.
