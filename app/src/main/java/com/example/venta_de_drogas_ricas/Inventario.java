package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

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

        btnAgregarAlCarrito.setOnClickListener(v -> agregarAlCarrito());
        btnTerminarVenta.setOnClickListener(v -> terminarVenta());
        btnVolverInv.setOnClickListener(v -> finish());
    }

    private void recalcularTotal() {
        totalGeneral = 0;
        for (ItemCarrito item : itemsCarrito) {
            totalGeneral += item.subtotal;
        }
        tvTotalGeneral.setText(String.format("TOTAL: $%.2f", totalGeneral));
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
            tvSubtotalConsultado.setText("Stock: " + stockDisponibleActual + " | Precio: $" + precioActual);
            etCantidadVenta.setText("1");
        } else {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
            limpiarConsulta();
        }
        cursor.close();
    }

    private void agregarAlCarrito() {
        String cantStr = etCantidadVenta.getText().toString();
        if (idActual.isEmpty() || cantStr.isEmpty()) {
            Toast.makeText(this, "Busque un producto y asigne cantidad", Toast.LENGTH_SHORT).show();
            return;
        }

        float cantidadAVender = Float.parseFloat(cantStr);
        ConfiguracionAlertas configAlertas = ConfigManager.getInstance(this).getConfigAlertas();

        if (configAlertas.stock.bloquear_sin_stock && cantidadAVender > stockDisponibleActual) {
            Toast.makeText(this, configAlertas.alertas.mensaje_sin_stock + "\nStock Disponible: " + stockDisponibleActual, Toast.LENGTH_LONG).show();
            return;
        }

        if (cantidadAVender <= 0) {
            Toast.makeText(this, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
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
                    
                    verificarAlertasDeStock(item.nombre, nuevoStock);
                }
                c.close();
            }
            db.setTransactionSuccessful();

            // Guardar registro de la venta en JSON
            VentasManager ventasManager = new VentasManager(this);
            ventasManager.registrarVenta(itemsCarrito, totalGeneral);

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
        tvInfoProd.setText("Esperando código...");
        tvSubtotalConsultado.setText("Stock: -- | Precio: $0.00");
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
            colorTemp = android.graphics.Color.parseColor("#FFA500");
        }

        final int colorFinal = colorTemp;

        if (mensaje != null) {
            String mensajeFinal = "Producto: " + nombreProducto + "\n" + mensaje + "\nStock actual: " + nuevoStock;
            
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
