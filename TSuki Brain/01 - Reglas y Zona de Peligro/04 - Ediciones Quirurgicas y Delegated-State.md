# 01.04 — Ediciones Quirúrgicas y Trampas de Compose

> Ficha cerebro TSuki — fuente única de verdad para cualquier IA o ingeniero.
> Responde: QUÉ hace, PARA QUÉ, POR QUÉ está ahí, QUÉ NO TOCAR, CÓMO se trabaja.

---

## 1. QUÉ hace
Describe las directrices de edición de código y los errores comunes de Compose que deben evitarse:
1. **Delegated-State y Smart Cast**:
   - En Compose: `val cookieImport by authManager.cookie.collectAsState(null)` produce una variable delegada. Kotlin NO permite hacer smart-cast directo sobre variables delegadas (`if (cookieImport != null) { ... cookieImport.length }` falla).
   - Solución obligatoria: Capturar localmente en una variable inmutable antes del chequeo:
     ```kotlin
     val cookieVal = cookieImport ?: ""
     if (cookieVal.isNotEmpty()) { ... }
     ```
2. **Tipos Explícitos en `async` dentro de `coroutineScope`**:
   - En bucles concurrentes con `async { }`, especificar siempre la inferencia de tipos para evitar que el compilador infiera `Deferred<Any>`:
     ```kotlin
     val jobs: List<Deferred<Unit>> = items.map { item ->
         async {
             process(item)
             Unit
         }
     }
     jobs.awaitAll()
     ```
3. **Uso Correcto de `derivedStateOf`**:
   - Emplear `val derivedList by remember { derivedStateOf { ... } }`.
   - NUNCA declarar `var x = remember { derivedStateOf { ... } }` con mutación directa.
4. **Verificación de Consumo de Estado**:
   - Todo nuevo `mutableStateOf` o flag de diálogo insertado en un Composable DEBE tener su respectivo consumidor en el árbol visual (verificar con `grep` para evitar botones muertos).

---

## 2. PARA QUÉ existe (problema que resuelve)
Previene errores de compilación crípticos del frontend de Kotlin 2.2.10 y estados huérfanos que dejan elementos de UI no funcionales.

---

## 3. POR QUÉ está en ese lugar (justificación de paquete)
Guía técnica de buenas prácticas en `01 - Reglas y Zona de Peligro`.

---

## 4. QUÉ NO SE DEBE TOCAR JAMÁS (zona roja)
- No reemplazar bloques enteros de más de 100 líneas sin una justificación arquitectónica explicada previamente al usuario.

---

## 5. CÓMO se ha estado trabajando aquí (convenciones reales)
- Ediciones basadas en bloques contextuales pequeños con líneas de anclaje unívocas.

---

## 6. Flujo y conexiones
- Enlaces del cerebro: [[01 - Invariantes Intocables y Quirks Criticos]], [[03 - Capas UDF y Estado Reactivo]].

---

## 7. Guía rápida para una IA nueva
- Si vas a usar `by collectAsState`, extrae el valor a un `val` local antes de cualquier rama condicional.
