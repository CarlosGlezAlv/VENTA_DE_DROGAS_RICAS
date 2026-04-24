# Estado de Implementación de Archivos JSON (Assets)

Este documento detalla qué archivos `.json` ubicados en la carpeta `app/src/main/assets/` han sido implementados en el código de la aplicación y cuáles aún están pendientes de uso.

## ✅ Archivos JSON Implementados

Los siguientes archivos están siendo referenciados y utilizados en el código Java (`app/src/main/java/com/example/venta_de_drogas_ricas/`):

- **`appconfig.json`**: Implementado en `ConfigManager.java`.
- **`cambio_moneda.json`**: Implementado en `MonedaManager.java`.
- **`theme_config.json`**: Implementado en `ConfigManager.java` a través de `ThemeConfig.java`.
- **`configuracion_alertas.json`**: Implementado en `ConfigManager.java`.
- **`traductor.json`**: Implementado en `TraductorManager.java`.

## ⏳ Archivos JSON Pendientes (No Implementados)

Los siguientes archivos se encuentran en la carpeta de *assets*, pero actualmente no tienen referencias en el código fuente de la aplicación:

- **`exportar_excel.json`**
- **`historial_compras.json`**
- **`pdf.json`**
---
*Nota: Este reporte fue generado en base a la búsqueda de referencias exactas de los nombres de archivo en el código fuente de la carpeta `app/src/main/java/`.*