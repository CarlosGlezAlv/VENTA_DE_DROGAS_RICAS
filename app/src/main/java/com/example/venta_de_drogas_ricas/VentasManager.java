package com.example.venta_de_drogas_ricas;

import android.content.Context;
import com.google.gson.Gson;
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

public class VentasManager {

    private static final String FILE_NAME = "registro_ventas.json";
    private Context context;

    public static class ProductoVendido {

        public String codigo;
        public String nombre;
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

        public String fecha;
        public List<ProductoVendido> productos;
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

        registro.add(nuevaVenta);
        guardarRegistro(registro);
    }

    private List<VentaRecord> cargarRegistro() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<
                ArrayList<VentaRecord>
            >() {}.getType();
            List<VentaRecord> list = gson.fromJson(reader, listType);
            if (list == null) return new ArrayList<>();
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void guardarRegistro(List<VentaRecord> registro) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new Gson();
            gson.toJson(registro, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
