package com.example.venta_de_drogas_ricas;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

public class VentasManager {

    private static final String FILE_NAME = "registro_ventas.json";
    private Context context;

    public static class ProductoVendido {

        public String codigo;
        public String nombre;
        @SerializedName("precio_unitario")
        public float precio;
        public float cantidad;
        public float subtotal;

        public ProductoVendido(
            String codigo,
            String nombre,
            float precio,
            float cantidad,
            float subtotal
        ) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.precio = precio;
            this.cantidad = cantidad;
            this.subtotal = subtotal;
        }
    }

    public static class VentaRecord {

        public String folio;
        public String fecha;
        public List<ProductoVendido> productos;
        @SerializedName("total")
        public float total_venta;
        public String moneda_base;
        public String moneda_visual;
        public double tasa_cambio;
        public float total_visual;

        public VentaRecord(
            String fecha,
            List<ProductoVendido> productos,
            float total_venta,
            String moneda_base,
            String moneda_visual,
            double tasa_cambio,
            float total_visual
        ) {
            this.fecha = fecha;
            this.productos = productos;
            this.total_venta = total_venta;
            this.moneda_base = moneda_base;
            this.moneda_visual = moneda_visual;
            this.tasa_cambio = tasa_cambio;
            this.total_visual = total_visual;
        }
    }

    public static class HistorialCompras {
        public List<VentaRecord> compras;
        
        public HistorialCompras(List<VentaRecord> compras) {
            this.compras = compras;
        }
    }

    public VentasManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void registrarVenta(
        List<ItemCarrito> itemsCarrito,
        float totalGeneral
    ) {
        List<VentaRecord> registro = cargarRegistro();

        List<ProductoVendido> productosVendidos = new ArrayList<>();
        for (ItemCarrito item : itemsCarrito) {
            productosVendidos.add(
                new ProductoVendido(
                    item.id,
                    item.nombre,
                    item.precio,
                    item.cantidad,
                    item.subtotal
                )
            );
        }

        AppConfig config = ConfigManager.getInstance(context).getConfig();
        String monedaBase = "MXN";
        String monedaVisual = "MXN";
        if (config != null && config.negocio != null) {
            if (config.negocio.moneda_base != null) monedaBase =
                config.negocio.moneda_base;
            if (config.negocio.moneda_visual != null) monedaVisual =
                config.negocio.moneda_visual;
        }

        MonedaManager monedaManager = MonedaManager.getInstance(context);
        BigDecimal totalVisualBD = monedaManager.convertir(
            BigDecimal.valueOf(totalGeneral),
            monedaBase,
            monedaVisual
        );
        float totalVisual = totalVisualBD.floatValue();

        double tasaCambio = 1.0;
        if (!monedaBase.equals(monedaVisual)) {
            BigDecimal tasaBD = monedaManager.convertir(
                BigDecimal.ONE,
                monedaBase,
                monedaVisual
            );
            tasaCambio = tasaBD.doubleValue();
        }

        String fechaActual = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(new Date());
        VentaRecord nuevaVenta = new VentaRecord(
            fechaActual,
            productosVendidos,
            totalGeneral,
            monedaBase,
            monedaVisual,
            tasaCambio,
            totalVisual
        );
        nuevaVenta.folio = "V-" + (registro.size() + 1);

        registro.add(nuevaVenta);
        guardarRegistro(registro);
    }

    public List<VentaRecord> cargarRegistro() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            JsonElement jsonElement = JsonParser.parseReader(reader);
            
            if (jsonElement.isJsonArray()) {
                Type listType = new TypeToken<ArrayList<VentaRecord>>() {}.getType();
                List<VentaRecord> list = gson.fromJson(jsonElement, listType);
                if (list != null) {
                    int count = 1;
                    for (VentaRecord v : list) {
                        if (v.folio == null) v.folio = "V-" + count;
                        count++;
                    }
                    return list;
                }
            } else if (jsonElement.isJsonObject()) {
                HistorialCompras historial = gson.fromJson(jsonElement, HistorialCompras.class);
                if (historial != null && historial.compras != null) return historial.compras;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void guardarRegistro(List<VentaRecord> registro) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            HistorialCompras historial = new HistorialCompras(registro);
            gson.toJson(historial, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
