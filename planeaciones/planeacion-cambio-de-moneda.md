# Planeación: Implementación de cambio de moneda (MXN/USD)

## 1) Contexto actual (lo que ya tenemos)
Con base en el archivo `app/src/main/assets/cambio_moneda.json`, ya existe una definición inicial de conversión:

- Acción: `convertir_moneda`
- Monedas soportadas: `MXN`, `USD`
- Tasas:
  - `MXN -> USD`: `0.0571`
  - `USD -> MXN`: `17.50`

Esto es suficiente para implementar una **primera versión funcional local (offline)** sin depender de API externa.

---

## 2) Objetivo funcional
Permitir que la app:

1. Defina una **moneda base** de operación (ej. MXN).
2. Defina una **moneda de visualización** (MXN o USD).
3. Convierta montos para mostrar precios, subtotales, total del carrito y reportes.
4. Mantenga cálculos internos consistentes (recomendado: operar siempre en una moneda base).
5. Persista la configuración elegida por el usuario en Configuración.

---

## 3) Decisión técnica recomendada
Para evitar errores y redondeos inconsistentes:

- Guardar y operar internamente en **moneda base del sistema** (recomendado: MXN).
- Convertir solo en capa de presentación cuando la moneda seleccionada sea distinta.
- Encapsular la lógica en un servicio dedicado (ej. `CurrencyManager` / `MonedaManager`).
- Evitar conversiones “ida y vuelta” repetidas sobre el mismo valor.

---

## 4) Arquitectura propuesta

## 4.1 Nuevos modelos
Crear un modelo para mapear `cambio_moneda.json`:

- `CambioMonedaConfig`
  - `String accion`
  - `List<String> monedas_soportadas`
  - `List<TipoCambio> tipos_de_cambio`
- `TipoCambio`
  - `String origen`
  - `String destino`
  - `double tasa`

## 4.2 Nueva configuración en `AppConfig`
Agregar a `AppConfig.Negocio` (o crear bloque `Moneda` si prefieres separar):

- `String moneda_base = "MXN"`
- `String moneda_visual = "MXN"`
- `boolean mostrar_codigo_moneda = true` (opcional)
- `int decimales_moneda = 2` (opcional)

## 4.3 Nuevo manager de moneda
Crear `MonedaManager` con responsabilidades:

- Cargar `cambio_moneda.json` desde `assets`.
- Validar monedas soportadas.
- Resolver tasa por par origen/destino.
- Convertir importes.
- Formatear monto + símbolo/código (`$`, `USD`, `MXN`).
- Exponer métodos seguros:
  - `double convertir(double monto, String origen, String destino)`
  - `String formatear(double monto, String moneda)`
  - `String getMonedaVisualActual()`

---

## 5) Fases de implementación

## Fase 1 — Preparación y base técnica
**Objetivo:** dejar lista la estructura para convertir y formatear moneda.

1. Crear modelos JSON para cambio de moneda.
2. Implementar carga de `cambio_moneda.json` desde `assets`.
3. Crear `MonedaManager` con cache en memoria.
4. Agregar validaciones:
   - Si falta tasa directa, intentar inversa.
   - Si no existe par, retornar error controlado + fallback (sin convertir).
5. Definir estrategia de redondeo (`HALF_UP` recomendado a 2 decimales en UI).

**Criterio de salida:** ya se puede convertir correctamente `MXN <-> USD` desde código.

---

## Fase 2 — Persistencia en configuración
**Objetivo:** que la moneda elegida sobreviva reinicios.

1. Ampliar `AppConfig` con campos de moneda.
2. Verificar que `ConfigManager.loadConfig()` y `saveConfig()` los serialicen automáticamente.
3. Definir defaults seguros:
   - base = `MXN`
   - visual = `MXN`
4. Si valor inválido en config, fallback a `MXN`.

**Criterio de salida:** al reiniciar app se conserva la moneda visual elegida.

---

## Fase 3 — Interfaz de Configuración (UI)
**Objetivo:** exponer ajuste de moneda al usuario en `ConfigActivity`/`activity_config.xml`.

1. Agregar nueva sección en Config:
   - Label “Moneda”
   - `Spinner` o `MaterialAutoCompleteTextView` para moneda visual (`MXN`, `USD`)
   - (Opcional) selector de moneda base (bloqueado en MXN inicialmente)
2. Cargar valor actual al abrir Config.
3. Guardar selección en `guardarCambios()`.
4. Aplicar traducciones para textos nuevos (`TraductorManager`).

**Criterio de salida:** el usuario puede elegir moneda desde Configuración y guardar.

---

## Fase 4 — Integración en pantallas de negocio
**Objetivo:** mostrar montos en moneda seleccionada en toda la app.

Aplicar en:

- Lista de productos (`ProductoAdapter`)
- Detalle de producto (`Producto`)
- Carrito (`ItemCarrito`, `CarritoAdapter`)
- Pantalla principal (`MainActivity`) para subtotal/total
- Inventario y/o reportes (`Inventario`, `VentasManager`) donde haya montos visibles

Regla:

- Si el precio fuente está en base (MXN), convertir solo para mostrar.
- Guardar cálculos finales de venta en base para consistencia histórica.

**Criterio de salida:** todos los importes visibles respetan moneda configurada.

---

## Fase 5 — Consistencia de cálculos y almacenamiento
**Objetivo:** asegurar integridad numérica y evitar discrepancias.

1. Definir contrato: BD y lógica de venta en moneda base.
2. Al cerrar venta:
   - Guardar total en base (MXN).
   - Opcional: guardar también moneda visual y tasa usada (auditoría).
3. Evitar usar `double` para persistencia de dinero si es posible:
   - Preferir `BigDecimal` en cálculos críticos.
4. Unificar redondeo en un único punto (`MonedaManager`).

**Criterio de salida:** no hay diferencias entre total mostrado y total guardado (dentro de política de redondeo).

---

## Fase 6 — QA y pruebas
**Objetivo:** validar funcionalidad y prevenir regresiones.

### Casos mínimos
1. Conversión MXN->USD y USD->MXN correcta según JSON.
2. Cambio de moneda en Config se refleja al instante (o al reabrir pantalla).
3. Persistencia de preferencia tras cerrar/reabrir app.
4. Totales de carrito correctos con redondeo.
5. Fallback ante configuración inválida (moneda no soportada).
6. Verificar UI en tema claro/oscuro y traducciones.

### Pruebas recomendadas
- Unit tests para `MonedaManager`.
- Tests de integración para flujo de compra con moneda visual `USD`.

**Criterio de salida:** cobertura de escenarios críticos y sin errores de formato/cálculo.

---

## Fase 7 — Mejora opcional (futuro)
**Objetivo:** evolucionar de tasas fijas locales a tasas dinámicas.

1. Crear proveedor de tasas remoto (API externa).
2. Guardar fecha/hora de última actualización.
3. Modo offline con fallback a JSON local.
4. Pantalla de estado de tasa (“actualizada hace X horas”).
5. Reintentos y manejo de error de red.

> Nota: esta fase requerirá API key si eliges proveedor externo.

---

## 6) Cambios puntuales sugeridos por archivo (guía)

- `app/src/main/assets/cambio_moneda.json`
  - Mantener como fuente inicial de tasas.
- `app/src/main/java/.../AppConfig.java`
  - Agregar campos de moneda.
- `app/src/main/java/.../ConfigManager.java`
  - Validaciones/defaults de moneda al cargar.
- `app/src/main/java/.../ConfigActivity.java`
  - Leer/guardar selección de moneda.
- `app/src/main/res/layout/activity_config.xml`
  - Añadir controles UI de moneda.
- `app/src/main/java/.../ProductoAdapter.java`, `MainActivity.java`, `CarritoAdapter.java`, etc.
  - Reemplazar render de precio por `MonedaManager.formatear(...)`.

---

## 7) Riesgos y mitigaciones

1. **Riesgo:** redondeos distintos en cada pantalla.  
   **Mitigación:** centralizar redondeo/formato en `MonedaManager`.

2. **Riesgo:** inconsistencia entre monto visual y monto guardado.  
   **Mitigación:** persistir en moneda base; convertir solo en UI.

3. **Riesgo:** par de monedas faltante en JSON.  
   **Mitigación:** fallback controlado + log + no bloquear flujo.

4. **Riesgo:** mezcla de símbolo `$` ambiguo entre MXN/USD.  
   **Mitigación:** mostrar código explícito (`MXN 120.00`, `USD 6.85`) o símbolo + código.

---

## 8) Entregable incremental recomendado (orden real de trabajo)

1. `MonedaManager` + modelos + test unitario.
2. Campos de moneda en `AppConfig`.
3. UI de Configuración + guardado.
4. Integración en pantallas de precios.
5. Pruebas de flujo completo y ajuste fino de formato.

---

## 9) Resultado esperado al finalizar
La app permitirá seleccionar moneda (MXN/USD) desde Configuración y mostrará todos los montos convertidos de forma consistente, persistente y estable, manteniendo cálculos internos confiables para ventas e inventario.