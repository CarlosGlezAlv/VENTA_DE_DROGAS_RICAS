package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class Producto extends AppCompatActivity {

    private EditText etCodigo, etNombre, etDescripcion, etExistencia, etPrecio;
    private Button btnGuardar, btnLimpiar, btnIrInventario;
    private TextInputLayout tilCodigoP, tilNombreP;
    private ImageButton btnVolverProd;
    private RecyclerView rvProductos;
    private ProductoAdapter adapter;
    private BD_DrogsDataBase dbHelper;
    private List<ProductoModel> listaProductos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConfigManager manager = ConfigManager.getInstance(this);
        manager.aplicarConfiguracionBase(this);
        setContentView(R.layout.activity_producto);
        manager.aplicarEstilosVisuales(findViewById(android.R.id.content));

        dbHelper = new BD_DrogsDataBase(this);

        etCodigo = findViewById(R.id.etCodigo);
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        etExistencia = findViewById(R.id.etExistencia);
        etPrecio = findViewById(R.id.etPrecio);

        tilCodigoP = findViewById(R.id.tilCodigoP);
        tilNombreP = findViewById(R.id.tilNombreP);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnLimpiar = findViewById(R.id.btnLimpiar);
        btnIrInventario = findViewById(R.id.btnIrInventario);
        btnVolverProd = findViewById(R.id.btnVolverProd);
        rvProductos = findViewById(R.id.rvProductos);

        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter(listaProductos, new ProductoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ProductoModel producto) {
                cargarEnFormulario(producto);
            }

            @Override
            public void onEditClick(ProductoModel producto) {
                cargarEnFormulario(producto);
                etNombre.requestFocus();
            }

            @Override
            public void onDeleteClick(ProductoModel producto) {
                eliminar(producto.getId());
            }
        });
        rvProductos.setAdapter(adapter);

        // Búsqueda Exacta
        tilCodigoP.setEndIconOnClickListener(v -> buscarPorCodigo());

        // Búsqueda Amplia
        tilNombreP.setEndIconOnClickListener(v -> cargarListaProductos(etNombre.getText().toString()));

        btnGuardar.setOnClickListener(v -> guardarOActualizar());
        btnLimpiar.setOnClickListener(v -> limpiarFormulario());

        btnIrInventario.setOnClickListener(v -> {
            startActivity(new Intent(Producto.this, Inventario.class));
        });

        btnVolverProd.setOnClickListener(v -> finish());

        cargarListaProductos("");
    }

    private void cargarListaProductos(String filtroNombre) {
        listaProductos.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        if (filtroNombre.isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM productos", null);
        } else {
            cursor = db.rawQuery("SELECT * FROM productos WHERE nombre LIKE ?", new String[]{"%" + filtroNombre + "%"});
        }

        if (cursor.moveToFirst()) {
            do {
                listaProductos.add(new ProductoModel(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getFloat(3),
                        cursor.getFloat(4)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void buscarPorCodigo() {
        String codigo = etCodigo.getText().toString();
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese un código", Toast.LENGTH_SHORT).show();
            return;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM productos WHERE id = ?", new String[]{codigo});
        if (cursor.moveToFirst()) {
            ProductoModel p = new ProductoModel(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getFloat(3),
                    cursor.getFloat(4)
            );
            cargarEnFormulario(p);
            
            // Highlight in list
            for (ProductoModel item : listaProductos) {
                item.setSelected(item.getId().equals(codigo));
            }
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "No encontrado", Toast.LENGTH_SHORT).show();
            limpiarFormularioExceptoCodigo();
        }
        cursor.close();
    }

    private void cargarEnFormulario(ProductoModel p) {
        etCodigo.setText(p.getId());
        etNombre.setText(p.getNombre());
        etDescripcion.setText(p.getDescripcion());
        etExistencia.setText(String.valueOf(p.getCantidad()));
        etPrecio.setText(String.valueOf(p.getPrecio()));
    }

    private void guardarOActualizar() {
        String codigo = etCodigo.getText().toString();
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Debe ingresar un código", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", codigo);
        values.put("nombre", etNombre.getText().toString());
        values.put("descripcion", etDescripcion.getText().toString());
        values.put("cantidad", etExistencia.getText().toString());
        values.put("precio", etPrecio.getText().toString());

        long result = db.insertWithOnConflict("productos", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Toast.makeText(this, "Guardado exitosamente", Toast.LENGTH_SHORT).show();
            limpiarFormulario();
            cargarListaProductos("");
        } else {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminar(String codigo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete("productos", "id = ?", new String[]{codigo});
        if (count > 0) {
            Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show();
            limpiarFormulario();
            cargarListaProductos("");
        }
    }

    private void limpiarFormulario() {
        etCodigo.setText("");
        limpiarFormularioExceptoCodigo();
        for (ProductoModel p : listaProductos) p.setSelected(false);
        adapter.notifyDataSetChanged();
        etCodigo.requestFocus();
    }

    private void limpiarFormularioExceptoCodigo() {
        etNombre.setText("");
        etDescripcion.setText("");
        etExistencia.setText("");
        etPrecio.setText("");
    }
}
