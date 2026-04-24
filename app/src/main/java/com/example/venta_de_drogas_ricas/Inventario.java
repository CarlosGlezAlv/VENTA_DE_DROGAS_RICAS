package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Inventario extends AppCompatActivity {

    private EditText etCodigoInv, etCantidadVenta;
    private TextView tvInfoProd, tvSubtotalConsultado, tvTotalGeneral;
    private Button btnAgregarAlCarrito, btnTerminarVenta;
    private RecyclerView rvCarrito;
    private ImageButton btnVolverInv;

    private BD_DrogsDataBase dbHelper;
    private AppConfig config;
    private ArrayList<ItemCarrito> itemsCarrito = new ArrayList<>();
    private CarritoAdapter adapter;

    private float totalGeneral = 0;
    private float precioActual = 0;
    private MonedaManager monedaManager;
    private String monedaBase = "MXN";
    private String monedaVisual = "MXN";
    private float stockDisponibleActual = 0;
    private String nombreActual = "";
    private String idActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        ConfigManager manager = ConfigManager.getInstance(this);
        manager.aplicarConfiguracionBase(this);
        config = manager.getConfig();

        setContentView(R.layout.activity_inventario);
        manager.aplicarEstilosVisuales(findViewById(android.R.id.content));

        dbHelper = new BD_DrogsDataBase(this);
        monedaManager = MonedaManager.getInstance(this);
        if (config != null && config.negocio != null) {
            if (config.negocio.moneda_base != null && !config.negocio.moneda_base.trim().isEmpty()) {
                monedaBase = config.negocio.moneda_base.trim().toUpperCase();
            }
            if (config.negocio.moneda_visual != null && !config.negocio.moneda_visual.trim().isEmpty()) {
                monedaVisual = config.negocio.moneda_visual.trim().toUpperCase();
            }
        }

        etCodigoInv = findViewById(R.id.etCodigoInv);
        etCantidadVenta = findViewById(R.id.etCantidadVenta);
        tvInfoProd = findViewById(R.id.tvInfoProd);
        tvSubtotalConsultado = findViewById(R.id.tvSubtotalConsultado);
        tvTotalGeneral = findViewById(R.id.tvTotalGeneral);
        btnAgregarAlCarrito = findViewById(R.id.btnAgregarAlCarrito);
        btnTerminarVenta = findViewById(R.id.btnTerminarVenta);
        rvCarrito = findViewById(R.id.rvCarrito);
        btnVolverInv = findViewById(R.id.btnVolverInv);

        rvCarrito.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CarritoAdapter(itemsCarrito, new CarritoAdapter.CarritoListener() {
            @Override
            public void onSumarCantidad(ItemCarrito item, int position) {
                float nuevoTotal = item.cantidad + 1;
                item.cantidad = nuevoTotal;
                item.subtotal = item.cantidad * item.precio;
                recalcularTotal();
                adapter.notifyItemChanged(position);
            }

            @Override
            public void onRestarCantidad(ItemCarrito item, int position) {
                if (item.cantidad > 1) {
                    item.cantidad -= 1;
                    item.subtotal = item.cantidad * item.precio;
                    recalcularTotal();
                    adapter.notifyItemChanged(position);
                } else {
                    onRemoverItem(item, position);
                }
            }

            @Override
            public void onRemoverItem(ItemCarrito item, int position) {
                itemsCarrito.remove(position);
                recalcularTotal();
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, itemsCarrito.size());
            }
        });
        rvCarrito.setAdapter(adapter);

        etCodigoInv.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) buscarProducto();
        });

        com.google.android.material.textfield.TextInputLayout tilCodigo = findViewById(R.id.tilCodigo);
        tilCodigo.setStartIconOnClickListener(v -> buscarProducto());

        TraductorManager traductor = TraductorManager.getInstance(this);

        TextView tvTituloPos = findViewById(R.id.tvTituloPos);
        if (tvTituloPos != null) {
            tvTituloPos.setText(traductor.getString("inv_title"));
        }

        TextView tvCarritoLabel = findViewById(R.id.tvCarritoLabel);
        if (tvCarritoLabel != null) {
            tvCarritoLabel.setText(traductor.getString("inv_carrito_title"));
        }

        etCodigoInv.setHint(traductor.getString("inv_codigo_hint"));
        etCantidadVenta.setHint(traductor.getString("inv_cantidad_hint"));
        btnAgregarAlCarrito.setText(traductor.getString("inv_btn_anadir"));
        btnTerminarVenta.setText(traductor.getString("inv_btn_finalizar"));

        btnAgregarAlCarrito.setOnClickListener(v -> agregarAlCarrito());
        btnTerminarVenta.setOnClickListener(v -> terminarVenta());
        btnVolverInv.setOnClickListener(v -> finish());
    }

    private void recalcularTotal() {
        totalGeneral = 0;
        for (ItemCarrito item : itemsCarrito) {
            totalGeneral += item.subtotal;
        }
        BigDecimal totalVisual = monedaManager.convertir(BigDecimal.valueOf(totalGeneral), monedaBase, monedaVisual);
        tvTotalGeneral.setText("TOTAL: " + monedaManager.formatear(totalVisual, monedaVisual));
    }

    private void buscarProducto() {
        String codigo = etCodigoInv.getText().toString();
        if (codigo.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM productos WHERE id = ?", new String[]{codigo});

        if (cursor.moveToFirst()) {
            idActual = cursor.getString(0);
            nombreActual = cursor.getString(1);
            stockDisponibleActual = cursor.getFloat(3);
            precioActual = cursor.getFloat(4);

            tvInfoProd.setText(nombreActual);
            BigDecimal precioVisual = monedaManager.convertir(BigDecimal.valueOf(precioActual), monedaBase, monedaVisual);
            tvSubtotalConsultado.setText(TraductorManager.getInstance(this).getString("msg_stock_precio", String.valueOf(stockDisponibleActual), monedaManager.formatear(precioVisual, monedaVisual)));
            etCantidadVenta.setText("1");
        } else {
            Toast.makeText(this, TraductorManager.getInstance(this).getString("msg_producto_no_encontrado"), Toast.LENGTH_SHORT).show();
            limpiarConsulta();
        }
        cursor.close();
    }

    private void agregarAlCarrito() {
        String cantStr = etCantidadVenta.getText().toString();
        if (idActual.isEmpty() || cantStr.isEmpty()) {
            Toast.makeText(this, TraductorManager.getInstance(this).getString("msg_busque_producto"), Toast.LENGTH_SHORT).show();
            return;
        }

        float cantidadAVender = Float.parseFloat(cantStr);
        ConfiguracionAlertas configAlertas = ConfigManager.getInstance(this).getConfigAlertas();

        if (configAlertas.stock.bloquear_sin_stock && cantidadAVender > stockDisponibleActual) {
            Toast.makeText(this, configAlertas.alertas.mensaje_sin_stock + "\nStock Disponible: " + stockDisponibleActual, Toast.LENGTH_LONG).show();
            return;
        }

        if (cantidadAVender <= 0) {
            Toast.makeText(this, TraductorManager.getInstance(this).getString("msg_cantidad_mayor_cero"), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean existe = false;
        for (ItemCarrito item : itemsCarrito) {
            if (item.id.equals(idActual)) {
                item.cantidad += cantidadAVender;
                item.subtotal = item.cantidad * item.precio;
                existe = true;
                break;
            }
        }

        if (!existe) {
            ItemCarrito nuevoItem = new ItemCarrito(idActual, nombreActual, cantidadAVender, precioActual);
            itemsCarrito.add(nuevoItem);
        }

        adapter.notifyDataSetChanged();
        recalcularTotal();
        limpiarConsulta();
    }

    private void terminarVenta() {
        if (itemsCarrito.isEmpty()) {
            Toast.makeText(this, TraductorManager.getInstance(this).getString("msg_carrito_vacio"), Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ItemCarrito item : itemsCarrito) {
                Cursor c = db.rawQuery("SELECT cantidad FROM productos WHERE id = ?", new String[]{item.id});
                if (c.moveToFirst()) {
                    float stockActualBD = c.getFloat(0);
                    float nuevoStock = stockActualBD - item.cantidad;
                    ContentValues values = new ContentValues();
                    values.put("cantidad", nuevoStock);
                    db.update("productos", values, "id = ?", new String[]{item.id});
                    verificarAlertasDeStock(item.nombre, nuevoStock);
                }
                c.close();
            }
            db.setTransactionSuccessful();

            VentasManager ventasManager = new VentasManager(this);
            ventasManager.registrarVenta(itemsCarrito, totalGeneral);

            mostrarDialogoExito();
            
        } catch (Exception e) {
            Toast.makeText(this, TraductorManager.getInstance(this).getString("msg_error_venta"), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    private void mostrarDialogoExito() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder.setTitle(TraductorManager.getInstance(this).getString("msg_venta_exitosa"))
               .setMessage("La venta se ha procesado correctamente.")
               .setCancelable(false)
               .setPositiveButton("Cerrar", (dialog, id) -> {
                   limpiarTodo();
               })
               .setNeutralButton("Exportar a Excel", (dialog, id) -> {
                   exportarAExcel();
               });
        
        builder.create().show();
    }

    private void exportarAExcel() {
        try {
            if (itemsCarrito.isEmpty()) {
                Toast.makeText(this, "No hay productos para exportar", Toast.LENGTH_SHORT).show();
                return;
            }

            ExportarExcelData exportData = new ExportarExcelData();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            exportData.fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            exportData.total_venta = totalGeneral;
            exportData.productos = new ArrayList<>();

            for (ItemCarrito item : itemsCarrito) {
                ExportarExcelData.ProductoItem p = new ExportarExcelData.ProductoItem();
                p.codigo = item.id;
                p.nombre = item.nombre;
                p.precio = item.precio;
                p.cantidad = item.cantidad;
                p.subtotal = item.subtotal;
                exportData.productos.add(p);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(exportData);

            String fileName = "Venta_" + timestamp + ".json";
            
            // --- MÉTODO COMPATIBLE PARA GUARDAR EN LA CARPETA DOWNLOADS PÚBLICA ---
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            
            if (uri != null) {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os != null) {
                        os.write(jsonOutput.getBytes());
                        os.flush();
                        Toast.makeText(this, "¡VENTA GUARDADA! Búscala en la carpeta principal de 'Descargas' (Downloads) de tu teléfono.", Toast.LENGTH_LONG).show();
                    }
                }
                limpiarTodo();
            } else {
                // Fallback para versiones que no soportan MediaStore Downloads o error de inserción
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(path, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(jsonOutput.getBytes());
                    Toast.makeText(this, "Guardado en Descargas: " + fileName, Toast.LENGTH_LONG).show();
                }
                limpiarTodo();
            }

        } catch (Exception e) {
            android.util.Log.e("EXPORT_ERROR", "Error: ", e);
            Toast.makeText(this, "ERROR AL GUARDAR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static class ExportarExcelData {
        String fecha;
        float total_venta;
        ArrayList<ProductoItem> productos;

        static class ProductoItem {
            String codigo;
            String nombre;
            float precio;
            float cantidad;
            float subtotal;
        }
    }

    private void limpiarConsulta() {
        idActual = "";
        nombreActual = "";
        precioActual = 0;
        stockDisponibleActual = 0;
        etCodigoInv.setText("");
        etCantidadVenta.setText("");
        tvInfoProd.setText(TraductorManager.getInstance(this).getString("msg_esperando_codigo"));
        tvSubtotalConsultado.setText(TraductorManager.getInstance(this).getString("msg_stock_precio_vacio"));
    }

    private void limpiarTodo() {
        limpiarConsulta();
        itemsCarrito.clear();
        adapter.notifyDataSetChanged();
        recalcularTotal();
    }

    private void verificarAlertasDeStock(String nombreProducto, float nuevoStock) {
        ConfiguracionAlertas configAlertas = ConfigManager.getInstance(this).getConfigAlertas();
        String mensaje = null;
        String titulo = TraductorManager.getInstance(this).getString("alerta_stock_titulo");
        int colorFinal = android.graphics.Color.BLACK;

        if (nuevoStock <= 0) {
            mensaje = configAlertas.alertas.mensaje_sin_stock;
            colorFinal = android.graphics.Color.RED;
        } else if (nuevoStock <= configAlertas.stock.critico) {
            mensaje = configAlertas.alertas.mensaje_critico;
            colorFinal = android.graphics.Color.RED;
        } else if (nuevoStock <= configAlertas.stock.minimo_alerta) {
            mensaje = configAlertas.alertas.mensaje_bajo;
            colorFinal = android.graphics.Color.parseColor("#FFA500");
        }

        if (mensaje != null) {
            String mensajeFinal = TraductorManager.getInstance(this).getString("alerta_stock_producto", nombreProducto, mensaje, String.valueOf(nuevoStock));
            if (configAlertas.alertas.mostrar_popup) {
                final int finalColor = colorFinal;
                runOnUiThread(() -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    android.widget.TextView titleView = new android.widget.TextView(this);
                    titleView.setText(titulo);
                    titleView.setPadding(40, 40, 40, 40);
                    titleView.setTextSize(20);
                    if (configAlertas.alertas.usar_color) titleView.setTextColor(finalColor);
                    builder.setCustomTitle(titleView);
                    builder.setMessage(mensajeFinal);
                    builder.setPositiveButton(TraductorManager.getInstance(this).getString("alerta_entendido"), null);
                    builder.show();
                });
            }
            mostrarNotificacionSistema(nombreProducto, mensajeFinal);
        }
    }

    private void mostrarNotificacionSistema(String titulo, String mensaje) {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String channelId = "stock_alerts_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Alertas de Stock", android.app.NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);
        }
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Alerta: " + titulo)
                .setContentText(mensaje)
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (notificationManager != null) notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
