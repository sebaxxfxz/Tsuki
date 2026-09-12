# 07.06 — TSukiBrainStorage (Persistencia del Cerebro Algorítmico)

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Gestiona el guardado atómico del modelo adaptativo del usuario en disco en `data/recommendation/TSukiBrainStorage.kt`:
- Archivo de almacenamiento: `tsuki_brain_v1.json` en `context.filesDir`.
- Serializa el objeto `TSukiProfile` mediante `kotlinx.serialization.json.Json`.
- Escribe primero a un archivo temporal `.tmp` y ejecuta un renombrado atómico sobre el archivo destino para prevenir la corrupción del perfil si la app se cierra abruptamente en plena escritura.
- Mantiene una copia en memoria RAM con bloqueo de lectura/escritura thread-safe.

**Archivos fuente clave:**
- [`data/recommendation/TSukiBrainStorage.kt:L15-95`](file:///home/sebaxxfxz/Documentos/TSuki/app/src/main/java/com/example/tsuki/data/recommendation/TSukiBrainStorage.kt#L15-L95)

---

## 2. PARA QUÉ existe (problema que resuelve)
Asegura que el perfil musical aprendido a lo largo de semanas o meses sobreviva a reinicios del teléfono y actualizaciones del sistema sin riesgo de corromperse.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Persistencia del motor neuronal en `data/recommendation/`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- El patrón de escritura atómica con archivo `.tmp` y `renameTo`.
- La clave `"version": 1` del perfil.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Auto-guardado con debounce de 3 segundos para agrupar múltiples actualizaciones de reproducción.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - TSukiNeuroEngine y Aprendizaje Local]], [[04 - Sistema de Backup y Restauracion]].

---

## 7. Guía rápida para una IA nueva
- Para guardar el cerebro forzadamente: `TSukiBrainStorage.getInstance(context).saveProfileSync()`.
