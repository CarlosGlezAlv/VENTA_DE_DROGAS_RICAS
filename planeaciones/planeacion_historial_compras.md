# Planeación: Sistema e Historial de Compras (Ventas)

**Fecha:** 2026-04-27  
**Proyecto:** Lumina Retail POS  
**Objetivo:** Implementar la interfaz del "Historial de Compras" (registro de las ventas realizadas) y reestructurar el modelo de datos JSON para alinearlo con el diseño original propuesto en `historial_compras.json`, pero manteniendo las funcionalidades de múltiples monedas que el sistema ya posee.

---

## 1. Análisis del Sistema Actual de Compras (Ventas)

Actualmente, cuando un cliente hace una "compra" (desde la perspectiva del sistema, es una **venta** procesada en `Inventario.java`), el sistema hace lo siguiente:
1. Resta el stock de la base de datos local SQLite (`BD_DrogsDataBase`).
2. Delega en `VentasManager.java` la persistencia del registro en un archivo llamado **`registro_ventas.json`** en el almacenamiento interno de la app (`getFilesDir()`).

### Estructura actual de `registro_ventas.json`:
Es un Arreglo JSON directo (`[ { ... }, { ... } ]`) donde cada objeto (`VentaRecord`) tiene:
- `fecha`
- `productos` (Arreglo de `ProductoVendido` con `codigo`, `nombre`, `precio`, `cantidad`, `subtotal`)
- `total_venta`, `moneda_base`, `moneda_visual`, `tasa_cambio`, `total_visual`

### Estructura propuesta en el asset `historial_compras.json`:
- Utiliza un objeto envoltorio con la clave `"compras": [...]`.
- Cada objeto tiene un identificador único: **`folio`** (ej. "V-1", "V-2").
- Utiliza el término `total` en lugar de `total_venta`.
- Utiliza `precio_unitario` en lugar de `precio`.

---

## 2. Plan de Reestructuración del JSON

Para unificar ambos enfoques, se modificará `VentasManager.java` para que genere una estructura que combine el requerimiento de "Folios" de `historial_compras.json` con la gestión de monedas actual.

**Nueva Estructura (Modelo de Datos Unificado):**
```json
{
  "compras": [
    {
      "folio": "V-1",
      "fecha": "2026-04-27 15:30:00",
      "total_venta": 150.0,
      "moneda_base": "MXN",
      "moneda_visual": "USD",
      "tasa_cambio": 0.05,
      "total_visual": 7.5,
      "productos": [
        {
          "codigo": "100",
          "nombre": "Producto Ejemplo",
          "precio_unitario": 50.0,
          "cantidad": 3.0,
          "subtotal": 150.0
        }
      ]
    }
  ]
}
```

### Cambios a realizar en el código:
1. **En `VentasManager.java`**:
   - Crear una clase envoltorio `RegistroCompras` que contenga `List<VentaRecord> compras;`.
   - Modificar `VentaRecord` para incluir el campo `public String folio;`.
   - Modificar `ProductoVendido` para renombrar `precio` a `precio_unitario`.
   - En el método `registrarVenta()`, antes de guardar, calcular el nuevo folio basándose en el tamaño de la lista (ej: `"V-" + (compras.size() + 1)`).
2. **Migración de datos**:
   - Si ya existe un archivo `registro_ventas.json` con el formato de arreglo puro, crear un método de compatibilidad que lo lea como un arreglo y lo reescriba con el nuevo envoltorio y asigne folios a las ventas antiguas.

---

## 3. Plan de Interfaz: Historial de Compras

Se creará la interfaz visual para que el usuario pueda consultar las ventas. Como se estableció en la planeación de la *Bottom Navigation Bar*, esto se implementará como `HistorialFragment` (o como un `Activity` temporal que posteriormente será un fragment).

### Componentes de la Interfaz (`fragment_historial.xml` o `activity_historial.xml`):
1. **RecyclerView (`rvHistorial`)**: Lista deslizable para mostrar las tarjetas de las ventas.
2. **CardView para cada registro (Adapter)**:
   - Encabezado: Folio (Ej. **V-1**) y Fecha.
   - Cuerpo: Lista de productos comprados, cantidades y subtotales (se puede usar un `LinearLayout` dinámico o un texto resumido).
   - Pie de tarjeta: Total pagado (en la `moneda_visual` utilizada en ese momento).

### Lógica del Controlador (`HistorialFragment.java` / `HistorialActivity.java`):
- `VentasManager` tendrá un nuevo método público: `public List<VentaRecord> obtenerHistorial()` que leerá el JSON y lo retornará.
- El adaptador se alimentará de esta lista y se mostrará en orden cronológico inverso (las ventas más recientes arriba).

---

## 4. Pasos de Ejecución

1. [ ] **Fase 1: Refactorización de Datos**
   - Actualizar las clases modelo (`VentaRecord`, `ProductoVendido`) en `VentasManager.java`.
   - Implementar la lógica autogeneradora de Folios.
   - Renombrar archivo resultante a `historial_compras.json` en el directorio interno de la app (opcional, o mantener `registro_ventas.json` pero con la nueva estructura).
2. [ ] **Fase 2: Interfaz de Usuario**
   - Crear `item_historial.xml` (Diseño de la tarjeta).
   - Crear el Adaptador `HistorialAdapter.java`.
   - Crear el layout `activity_historial.xml` (o `fragment_historial.xml`).
3. [ ] **Fase 3: Integración**
   - Integrar la vista con `VentasManager`.
   - Probar creando una venta en la sección de inventario y verificar que aparece en el historial con el folio correcto.
