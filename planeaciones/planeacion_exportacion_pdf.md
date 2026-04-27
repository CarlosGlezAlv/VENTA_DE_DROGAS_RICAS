# Planeación: Exportación de Ventas a PDF

**Fecha:** 2026-04-27  
**Proyecto:** Lumina Retail POS  
**Objetivo:** Implementar la generación de un documento PDF nativo (ticket/recibo) para una venta específica utilizando las ventas ya registradas en el "Historial de Compras", y basando su diseño visual y estructural en la configuración definida en el archivo `pdf.json`.

---

## 1. Análisis del requerimiento

El usuario desea poder exportar un registro de venta (historial) en formato PDF real, inspirándose en la estructura del asset `pdf.json`.
Actualmente el sistema:
- Ya guarda las ventas (`VentaRecord`) con folios, fechas, lista de productos y totales en el archivo JSON unificado que acabamos de refactorizar.
- En la interfaz del historial (`HistorialActivity`), el usuario ya puede visualizar cada venta, pero falta una acción para interactuar con ella.

El archivo `pdf.json` define:
- `apariencia`: Contiene `color_enfasis` y `tamano_texto`.
- `negocio`: Contiene `nombre_tienda`.
- `venta`: Es un bosquejo o plantilla de los datos que debe incluir el PDF (ID de venta, fecha, cliente, productos, subtotal, total).

## 2. Arquitectura de la Solución

Dado que el proyecto no usa librerías de terceros complejas como iText (y para mantener la app ligera), utilizaremos **`android.graphics.pdf.PdfDocument`**, que es nativo de Android y perfecto para dibujar tickets de compra.

### Modificaciones en la Interfaz (UI)
1. **`item_historial.xml`**:
   - Agregar un `ImageButton` o `Button` (ej. ícono de descarga/impresora) en la esquina superior derecha o inferior de la tarjeta.
2. **`HistorialAdapter.java`**:
   - Implementar un *ClickListener* para el botón de exportar.
   - Enviar la información de la `VentaRecord` seleccionada al Activity.

### Motor de PDF (`PdfManager.java`)
Crearemos una nueva clase llamada `PdfManager` encargada exclusivamente de dibujar y generar el archivo:
- Leerá la configuración desde `app/src/main/assets/pdf.json` para obtener el color de énfasis y el nombre de la tienda (o bien cruzará datos con `AppConfig` si el nombre de la tienda está vacío en `pdf.json`).
- Usará la API `Canvas` de Android para dibujar el texto línea por línea (Nombre de la tienda, Folio, Fecha, Encabezados de tabla de productos, y el total).
- Utilizará `MediaStore.Downloads` (igual que en Excel) para guardar el archivo resultante de manera que el usuario pueda encontrarlo fácilmente en su carpeta de "Descargas" del dispositivo Android.

## 3. Estructura de la Clase `PdfManager`

```java
public class PdfManager {
    // 1. Cargar pdf.json
    // 2. Definir brochas (Paint) con color_enfasis y tamaño de texto (pequeno, mediano)
    // 3. Crear PdfDocument y una página
    // 4. Dibujar cabecera (Negocio)
    // 5. Dibujar info Venta (Folio, Fecha)
    // 6. Dibujar Productos (Tabla)
    // 7. Dibujar Total
    // 8. Guardar archivo en Downloads vía MediaStore
}
```

## 4. Pasos de Ejecución

1. [ ] **Paso 1: Modelado de Configuración PDF**
   - Crear una clase `PdfConfig` para parsear `pdf.json` vía Gson.
2. [ ] **Paso 2: Creación del `PdfManager`**
   - Escribir la lógica de generación visual en un `Canvas`.
   - Implementar la lógica de escritura en almacenamiento (`MediaStore` para Android 10+ o `Environment.getExternalStoragePublicDirectory` para versiones antiguas).
3. [ ] **Paso 3: Actualizar la Interfaz**
   - Modificar `item_historial.xml` para añadir el botón de "Exportar a PDF".
   - Conectar el `HistorialAdapter` para capturar el click y ejecutar `PdfManager.exportarVenta(context, ventaRecord)`.
4. [ ] **Paso 4: Permisos**
   - Asegurarnos de que el `AndroidManifest.xml` cuenta con los permisos de lectura/escritura (o usar directamente Scoped Storage / MediaStore que no requiere permisos explícitos en Android 10+ para la carpeta Downloads).

## 5. Consideraciones
- **Tamaño de texto**: Si `pdf.json` dice "pequeno", usaremos por ejemplo `12f`.
- **Formato numérico**: Usaremos la moneda visual guardada en el `VentaRecord`.
