package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class Inventario extends AppCompatActivity {

    private EditText etCodigoInv, etCantidadVenta;
    private TextView tvInfoProd, tvSubtotalConsultado, tvTotalGeneral;
    private Button btnAgregarAlCarrito, btnTerminarVenta;
    private ListView lvCarrito;
    private ImageButton btnVolverInv;
    
    private BD_DrogsDataBase dbHelper;
    private AppConfig config;
    private ArrayList<String> listaCarritoStr = new ArrayList<>();
    private ArrayList<ItemCarrito> itemsCarrito = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    
    private float totalGeneral = 0;
    private float precioActual = 0;
    private float stockDisponibleActual = 0;
    private String nombreActual = "";
    private String idActual = "";

    private static class ItemCarrito {
        String id;
        String nombre;
        float cantidad;
        float precio;
        float subtotal;

        ItemCarrito(String id, String nombre, float cantidad, float precio) {
            this.id = id;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precio = precio;
            this.subtotal = cantidad * precio;
        }
    }

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
        
        // Aplicar estilos visuales (Color y Tamaño de letra) recursivamente
        manager.aplicarEstilosVisuales(findViewById(android.R.id.content));

        dbHelper = new BD_DrogsDataBase(this);

        etCodigoInv = findViewById(R.id.etCodigoInv);
        etCantidadVenta = findViewById(R.id.etCantidadVenta);
        tvInfoProd = findViewById(R.id.tvInfoProd);
        tvSubtotalConsultado = findViewById(R.id.tvSubtotalConsultado);
        tvTotalGeneral = findViewById(R.id.tvTotalGeneral);
        btnAgregarAlCarrito = findViewById(R.id.btnAgregarAlCarrito);
        btnTerminarVenta = findViewById(R.id.btnTerminarVenta);
        lvCarrito = findViewById(R.id.lvCarrito);
        btnVolverInv = findViewById(R.id.btnVolverInv);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaCarritoStr);
        lvCarrito.setAdapter(adapter);

        etCodigoInv.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) buscarProducto();
        });

        btnAgregarAlCarrito.setOnClickListener(v -> agregarAlCarrito());
        btnTerminarVenta.setOnClickListener(v -> terminarVenta());
        btnVolverInv.setOnClickListener(v -> finish());
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

            String moneda = "$";
            tvInfoProd.setText("Prod: " + nombreActual + " | Stock: " + stockDisponibleActual + " | Precio: " + moneda + precioActual);
            etCantidadVenta.setText("1");
            actualizarSubtotal();
        } else {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
            limpiarConsulta();
        }
        cursor.close();
    }

    private void actualizarSubtotal() {
        try {
            float cant = Float.parseFloat(etCantidadVenta.getText().toString());
            float sub = cant * precioActual;
            tvSubtotalConsultado.setText("Subtotal Item: $" + sub);
        } catch (Exception e) {
            tvSubtotalConsultado.setText("Subtotal Item: $0.00");
        }
    }

    private void agregarAlCarrito() {
        String cantStr = etCantidadVenta.getText().toString();
        if (idActual.isEmpty() || cantStr.isEmpty()) {
            Toast.makeText(this, "Busque un producto y asigne cantidad", Toast.LENGTH_SHORT).show();
            return;
        }

        float cantidadAVender = Float.parseFloat(cantStr);

        ConfiguracionAlertas configAlertas = ConfigManager.getInstance(this).getConfigAlertas();

        // Bloqueo de stock
        if (configAlertas.stock.bloquear_sin_stock && cantidadAVender > stockDisponibleActual) {
            Toast.makeText(this, configAlertas.alertas.mensaje_sin_stock + "\nStock Disponible: " + stockDisponibleActual, Toast.LENGTH_LONG).show();
            return;
        }

        if (cantidadAVender <= 0) {
            Toast.makeText(this, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            return;
        }

        ItemCarrito nuevoItem = new ItemCarrito(idActual, nombreActual, cantidadAVender, precioActual);
        itemsCarrito.add(nuevoItem);
        
        String moneda = "$";
        String displayStr = nuevoItem.nombre + " x" + nuevoItem.cantidad + " = " + moneda + nuevoItem.subtotal;
        listaCarritoStr.add(displayStr);
        adapter.notifyDataSetChanged();

        totalGeneral += nuevoItem.subtotal;
        
        tvTotalGeneral.setText("TOTAL: " + moneda + totalGeneral);

        limpiarConsulta();
    }

    private void terminarVenta() {
        if (itemsCarrito.isEmpty()) {
            Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show();
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
                    
                    // Verificar alertas
                    verificarAlertasDeStock(item.nombre, nuevoStock);
                }
                c.close();
            }
            db.setTransactionSuccessful();
            Toast.makeText(this, "VENTA EXITOSA. Stock actualizado.", Toast.LENGTH_LONG).show();
            limpiarTodo();
        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar venta", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    private void limpiarConsulta() {
        idActual = "";
        nombreActual = "";
        precioActual = 0;
        stockDisponibleActual = 0;
        etCodigoInv.setText("");
        etCantidadVenta.setText("");
        tvInfoProd.setText("Producto: --- | Precio: $0.00");
        tvSubtotalConsultado.setText("Subtotal Item: $0.00");
    }

    private void limpiarTodo() {
        limpiarConsulta();
        itemsCarrito.clear();
        listaCarritoStr.clear();
        adapter.notifyDataSetChanged();
        totalGeneral = 0;
        tvTotalGeneral.setText("TOTAL: $0.00");
    }

    private void verificarAlertasDeStock(String nombreProducto, float nuevoStock) {
        ConfiguracionAlertas configAlertas = ConfigManager.getInstance(this).getConfigAlertas();
        
        String mensaje = null;
        String titulo = "Alerta de Stock";
        int colorTemp = android.graphics.Color.BLACK;

        if (nuevoStock <= 0) {
            mensaje = configAlertas.alertas.mensaje_sin_stock;
            colorTemp = android.graphics.Color.RED;
        } else if (nuevoStock <= configAlertas.stock.critico) {
            mensaje = configAlertas.alertas.mensaje_critico;
            colorTemp = android.graphics.Color.RED;
        } else if (nuevoStock <= configAlertas.stock.minimo_alerta) {
            mensaje = configAlertas.alertas.mensaje_bajo;
            colorTemp = android.graphics.Color.parseColor("#FFA500"); // Naranja
        }

        final int colorFinal = colorTemp;

        if (mensaje != null) {
            String mensajeFinal = "Producto: " + nombreProducto + "\n" + mensaje + "\nStock actual: " + nuevoStock;
            
            // 1. Mostrar Popup si está configurado
            if (configAlertas.alertas.mostrar_popup) {
                runOnUiThread(() -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    
                    android.widget.TextView titleView = new android.widget.TextView(this);
                    titleView.setText(titulo);
                    titleView.setPadding(40, 40, 40, 40);
                    titleView.setTextSize(20);
                    if (configAlertas.alertas.usar_color) {
                        titleView.setTextColor(colorFinal);
                    }
                    builder.setCustomTitle(titleView);
                    
                    builder.setMessage(mensajeFinal);
                    builder.setPositiveButton("Entendido", null);
                    builder.show();
                });
            }

            // 2. Mostrar Notificación del Sistema
            mostrarNotificacionSistema(nombreProducto, mensajeFinal);
        }
    }
    
    private void mostrarNotificacionSistema(String titulo, String mensaje) {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String channelId = "stock_alerts_channel";
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                channelId, "Alertas de Stock", android.app.NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Alerta: " + titulo)
            .setContentText(mensaje)
            .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
            
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
