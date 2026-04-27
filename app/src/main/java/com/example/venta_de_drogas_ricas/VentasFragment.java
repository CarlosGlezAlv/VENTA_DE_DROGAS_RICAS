package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.GsonBuilder;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.net.Uri;

public class VentasFragment extends Fragment {

    private EditText etCodigoInv, etCantidadVenta;
    private TextView tvInfoProd, tvSubtotalConsultado, tvTotalGeneral;
    private Button btnAgregarAlCarrito, btnTerminarVenta;
    private RecyclerView rvCarrito;

    private BD_DrogsDataBase dbHelper;
    private ArrayList<ItemCarrito> itemsCarrito = new ArrayList<>();
    private CarritoAdapter adapter;
    private MonedaManager monedaManager;

    private float totalGeneral = 0;
    private float stockDisponibleActual = 0;
    private String idActual = "";
    private String nombreActual = "";
    private float precioActual = 0;
    private String monedaBase = "MXN";
    private String monedaVisual = "MXN";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ventas, container, false);

        dbHelper = new BD_DrogsDataBase(requireContext());
        monedaManager = MonedaManager.getInstance(requireContext());

        etCodigoInv = view.findViewById(R.id.etCodigoInv);
        etCantidadVenta = view.findViewById(R.id.etCantidadVenta);
        tvInfoProd = view.findViewById(R.id.tvInfoProd);
        tvSubtotalConsultado = view.findViewById(R.id.tvSubtotalConsultado);
        tvTotalGeneral = view.findViewById(R.id.tvTotalGeneral);
        btnAgregarAlCarrito = view.findViewById(R.id.btnAgregarAlCarrito);
        btnTerminarVenta = view.findViewById(R.id.btnTerminarVenta);
        rvCarrito = view.findViewById(R.id.rvCarrito);

        rvCarrito.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CarritoAdapter(itemsCarrito, new CarritoAdapter.CarritoListener() {
            @Override public void onSumarCantidad(ItemCarrito item, int position) { item.cantidad++; item.subtotal = item.cantidad * item.precio; recalcularTotal(); adapter.notifyItemChanged(position); }
            @Override public void onRestarCantidad(ItemCarrito item, int position) { if (item.cantidad > 1) { item.cantidad--; item.subtotal = item.cantidad * item.precio; recalcularTotal(); adapter.notifyItemChanged(position); } else { onRemoverItem(item, position); } }
            @Override public void onRemoverItem(ItemCarrito item, int position) { itemsCarrito.remove(position); recalcularTotal(); adapter.notifyItemRemoved(position); adapter.notifyItemRangeChanged(position, itemsCarrito.size()); }
        });
        rvCarrito.setAdapter(adapter);

        etCodigoInv.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) buscarProducto(); });
        com.google.android.material.textfield.TextInputLayout tilCodigo = view.findViewById(R.id.tilCodigo);
        if (tilCodigo != null) tilCodigo.setStartIconOnClickListener(v -> buscarProducto());

        TraductorManager traductor = TraductorManager.getInstance(requireContext());
        TextView tvTituloPos = view.findViewById(R.id.tvTituloPos);
        if (tvTituloPos != null) tvTituloPos.setText(traductor.getString("inv_title"));
        TextView tvCarritoLabel = view.findViewById(R.id.tvCarritoLabel);
        if (tvCarritoLabel != null) tvCarritoLabel.setText(traductor.getString("inv_carrito_title"));
        etCodigoInv.setHint(traductor.getString("inv_codigo_hint"));
        etCantidadVenta.setHint(traductor.getString("inv_cantidad_hint"));
        btnAgregarAlCarrito.setText(traductor.getString("inv_btn_anadir"));
        btnTerminarVenta.setText(traductor.getString("inv_btn_finalizar"));

        btnAgregarAlCarrito.setOnClickListener(v -> agregarAlCarrito());
        btnTerminarVenta.setOnClickListener(v -> terminarVenta());

        return view;
    }

    private void recalcularTotal() {
        totalGeneral = 0;
        for (ItemCarrito item : itemsCarrito) totalGeneral += item.subtotal;
        BigDecimal totalVisual = monedaManager.convertir(BigDecimal.valueOf(totalGeneral), monedaBase, monedaVisual);
        tvTotalGeneral.setText("TOTAL: " + monedaManager.formatear(totalVisual, monedaVisual));
    }

    private void buscarProducto() {
        String codigo = etCodigoInv.getText().toString();
        if (codigo.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM productos WHERE id = ?", new String[]{codigo});
        if (cursor.moveToFirst()) {
            idActual = cursor.getString(0); nombreActual = cursor.getString(1);
            stockDisponibleActual = cursor.getFloat(3); precioActual = cursor.getFloat(4);
            tvInfoProd.setText(nombreActual);
            BigDecimal precioVisual = monedaManager.convertir(BigDecimal.valueOf(precioActual), monedaBase, monedaVisual);
            tvSubtotalConsultado.setText(TraductorManager.getInstance(requireContext()).getString("msg_stock_precio", String.valueOf(stockDisponibleActual), monedaManager.formatear(precioVisual, monedaVisual)));
            etCantidadVenta.setText("1");
        } else {
            Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_producto_no_encontrado"), Toast.LENGTH_SHORT).show();
            limpiarConsulta();
        }
        cursor.close();
    }

    private void agregarAlCarrito() {
        String cantStr = etCantidadVenta.getText().toString();
        if (idActual.isEmpty() || cantStr.isEmpty()) { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_busque_producto"), Toast.LENGTH_SHORT).show(); return; }
        float cantidadAVender = Float.parseFloat(cantStr);
        ConfiguracionAlertas configAlertas = ConfigManager.getInstance(requireContext()).getConfigAlertas();
        if (configAlertas.stock.bloquear_sin_stock && cantidadAVender > stockDisponibleActual) { Toast.makeText(requireContext(), configAlertas.alertas.mensaje_sin_stock + "\nStock Disponible: " + stockDisponibleActual, Toast.LENGTH_LONG).show(); return; }
        if (cantidadAVender <= 0) { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_cantidad_mayor_cero"), Toast.LENGTH_SHORT).show(); return; }
        boolean existe = false;
        for (ItemCarrito item : itemsCarrito) { if (item.id.equals(idActual)) { item.cantidad += cantidadAVender; item.subtotal = item.cantidad * item.precio; existe = true; break; } }
        if (!existe) itemsCarrito.add(new ItemCarrito(idActual, nombreActual, cantidadAVender, precioActual));
        adapter.notifyDataSetChanged(); recalcularTotal(); limpiarConsulta();
    }

    private void terminarVenta() {
        if (itemsCarrito.isEmpty()) { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_carrito_vacio"), Toast.LENGTH_SHORT).show(); return; }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ItemCarrito item : itemsCarrito) {
                Cursor c = db.rawQuery("SELECT cantidad FROM productos WHERE id = ?", new String[]{item.id});
                if (c.moveToFirst()) {
                    float nuevoStock = c.getFloat(0) - item.cantidad;
                    ContentValues values = new ContentValues(); values.put("cantidad", nuevoStock);
                    db.update("productos", values, "id = ?", new String[]{item.id});
                    verificarAlertasDeStock(item.nombre, nuevoStock);
                }
                c.close();
            }
            db.setTransactionSuccessful();
            new VentasManager(requireContext()).registrarVenta(itemsCarrito, totalGeneral);
            mostrarDialogoExito();
        } catch (Exception e) {
            Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_error_venta"), Toast.LENGTH_SHORT).show();
        } finally { db.endTransaction(); }
    }

    private void mostrarDialogoExito() {
        new AlertDialog.Builder(requireContext())
            .setTitle(TraductorManager.getInstance(requireContext()).getString("msg_venta_exitosa"))
            .setMessage("La venta se ha procesado correctamente.")
            .setCancelable(false)
            .setPositiveButton("Cerrar", (d, i) -> limpiarTodo())
            .setNeutralButton("Exportar a Excel", (d, i) -> exportarAExcel())
            .show();
    }

    private void exportarAExcel() {
        try {
            if (itemsCarrito.isEmpty()) return;
            Inventario.ExportarExcelData exportData = new Inventario.ExportarExcelData();
            exportData.fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            exportData.total_venta = totalGeneral;
            exportData.productos = new ArrayList<>();
            for (ItemCarrito item : itemsCarrito) {
                Inventario.ExportarExcelData.ProductoItem p = new Inventario.ExportarExcelData.ProductoItem();
                p.codigo = item.id; p.nombre = item.nombre; p.precio = item.precio; p.cantidad = item.cantidad; p.subtotal = item.subtotal;
                exportData.productos.add(p);
            }
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "Venta_" + timestamp + ".json";
            String jsonOutput = new GsonBuilder().setPrettyPrinting().create().toJson(exportData);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName); values.put(MediaStore.Downloads.MIME_TYPE, "application/json"); values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) { if (os != null) { os.write(jsonOutput.getBytes()); os.flush(); Toast.makeText(requireContext(), "Guardado en Descargas: " + fileName, Toast.LENGTH_LONG).show(); } }
            }
            limpiarTodo();
        } catch (Exception e) { Toast.makeText(requireContext(), "ERROR AL GUARDAR: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    private void limpiarConsulta() {
        idActual = ""; nombreActual = ""; precioActual = 0; stockDisponibleActual = 0;
        etCodigoInv.setText(""); etCantidadVenta.setText("");
        tvInfoProd.setText(TraductorManager.getInstance(requireContext()).getString("msg_esperando_codigo"));
        tvSubtotalConsultado.setText(TraductorManager.getInstance(requireContext()).getString("msg_stock_precio_vacio"));
    }

    private void limpiarTodo() { limpiarConsulta(); itemsCarrito.clear(); adapter.notifyDataSetChanged(); recalcularTotal(); }

    private void verificarAlertasDeStock(String nombreProducto, float nuevoStock) {
        ConfiguracionAlertas configAlertas = ConfigManager.getInstance(requireContext()).getConfigAlertas();
        String mensaje = null; int colorFinal = android.graphics.Color.BLACK;
        if (nuevoStock <= 0) { mensaje = configAlertas.alertas.mensaje_sin_stock; colorFinal = android.graphics.Color.RED; }
        else if (nuevoStock <= configAlertas.stock.critico) { mensaje = configAlertas.alertas.mensaje_critico; colorFinal = android.graphics.Color.RED; }
        else if (nuevoStock <= configAlertas.stock.minimo_alerta) { mensaje = configAlertas.alertas.mensaje_bajo; colorFinal = android.graphics.Color.parseColor("#FFA500"); }
        if (mensaje != null) {
            String mensajeFinal = TraductorManager.getInstance(requireContext()).getString("alerta_stock_producto", nombreProducto, mensaje, String.valueOf(nuevoStock));
            if (configAlertas.alertas.mostrar_popup) {
                final int fc = colorFinal;
                requireActivity().runOnUiThread(() -> {
                    android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(requireContext());
                    android.widget.TextView tv = new android.widget.TextView(requireContext());
                    tv.setText(TraductorManager.getInstance(requireContext()).getString("alerta_stock_titulo")); tv.setPadding(40,40,40,40); tv.setTextSize(20);
                    if (configAlertas.alertas.usar_color) tv.setTextColor(fc);
                    b.setCustomTitle(tv); b.setMessage(mensajeFinal); b.setPositiveButton(TraductorManager.getInstance(requireContext()).getString("alerta_entendido"), null); b.show();
                });
            }
            mostrarNotificacionSistema(nombreProducto, mensajeFinal);
        }
    }

    private void mostrarNotificacionSistema(String titulo, String mensaje) {
        android.app.NotificationManager nm = (android.app.NotificationManager) requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String channelId = "stock_alerts_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel ch = new android.app.NotificationChannel(channelId, "Alertas de Stock", android.app.NotificationManager.IMPORTANCE_HIGH);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        androidx.core.app.NotificationCompat.Builder b = new androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Alerta: " + titulo).setContentText(mensaje)
            .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(mensaje)).setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);
        if (nm != null) nm.notify((int) System.currentTimeMillis(), b.build());
    }
}
