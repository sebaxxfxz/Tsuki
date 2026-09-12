# 08.05 — TogetherJson (Configuración del Serializador)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Configura la instancia única de `Json` de Kotlinx Serialization en `together/TogetherJson.kt`:
```kotlin
val TogetherJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    classDiscriminator = "type"
}
```
- **`classDiscriminator = "type"`**: Inyecta la propiedad `"type": "..."` en el JSON para identificar polimórficamente cada subtipo de mensaje de `TogetherMessages`.
- **`ignoreUnknownKeys = true`**: Garantiza que si una versión más reciente añade un nuevo campo, clientes anteriores no crasheen al recibir el mensaje.

**Archivos fuente clave:**
- [`together/TogetherJson.kt:L1-20`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/together/TogetherJson.kt#L1-L20)

---

## 2. PARA QUÉ existe (problema que resuelve)
Asegura la interoperabilidad y tolerancia a versiones mixtas en la red.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Serializador de red en `together/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- NO cambiar `classDiscriminator = "type"`. Si se cambia a `"class"` o `@type`, todos los mensajes entrantes fallarán con `SerializationException`.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Usado estrictamente para codificar y decodificar frames de WebSocket.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[03 - TogetherMessages Contrato v1]], [[08 - TogetherClient Guest WS]].

---

## 7. Guía rápida para una IA nueva
- Serializa siempre con `TogetherJson.encodeToString(message)`.
