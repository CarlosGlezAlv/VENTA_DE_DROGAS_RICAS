package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class Producto extends AppCompatActivity {

    private EditText etCodigo, etNombre, etDescripcion, etExistencia, etPrecio;
    private Button btnGuardar, btnConsultar, btnModificar, btnEliminar, btnIrInventario;
    private ImageButton btnVolverProd;
    private BD_DrogsDataBase dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producto);

        dbHelper = new BD_DrogsDataBase(this);

        etCodigo = findViewById(R.id.etCodigo);
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        etExistencia = findViewById(R.id.etExistencia);
        etPrecio = findViewById(R.id.etPrecio);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnConsultar = findViewById(R.id.btnConsultar);
        btnModificar = findViewById(R.id.btnModificar);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnIrInventario = findViewById(R.id.btnIrInventario);
        btnVolverProd = findViewById(R.id.btnVolverProd);

        btnGuardar.setOnClickListener(v -> guardar());
        btnConsultar.setOnClickListener(v -> consultar());
        btnModificar.setOnClickListener(v -> modificar());
        btnEliminar.setOnClickListener(v -> eliminar());
        
        btnIrInventario.setOnClickListener(v -> {
            Intent intent = new Intent(Producto.this, Inventario.class);
            startActivity(intent);
        });

        btnVolverProd.setOnClickListener(v -> finish());
    }

    private void guardar() {
        if (etCodigo.getText().toString().isEmpty()) {
            Toast.makeText(this, "Debe ingresar un código", Toast.LENGTH_SHORT).show();
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", etCodigo.getText().toString());
        values.put("nombre", etNombre.getText().toString());
        values.put("descripcion", etDescripcion.getText().toString());
        values.put("cantidad", etExistencia.getText().toString());
        values.put("precio", etPrecio.getText().toString());

        long newRowId = db.insert("productos", null, values);
        if (newRowId != -1) {
            Toast.makeText(this, "Producto guardado con éxito", Toast.LENGTH_SHORT).show();
            limpiar();
        } else {
            Toast.makeText(this, "Error: El código ya existe o datos inválidos", Toast.LENGTH_SHORT).show();
        }
    }

    private void consultar() {
        String codigo = etCodigo.getText().toString();
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese código para buscar", Toast.LENGTH_SHORT).show();
            return;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM productos WHERE id = ?", new String[]{codigo});

        if (cursor.moveToFirst()) {
            etNombre.setText(cursor.getString(1));
            etDescripcion.setText(cursor.getString(2));
            etExistencia.setText(cursor.getString(3));
            etPrecio.setText(cursor.getString(4));
            Toast.makeText(this, "Producto encontrado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Producto no registrado", Toast.LENGTH_SHORT).show();
            limpiarExceptoCodigo();
        }
        cursor.close();
    }

    private void modificar() {
        String codigo = etCodigo.getText().toString();
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese código para modificar", Toast.LENGTH_SHORT).show();
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", etNombre.getText().toString());
        values.put("descripcion", etDescripcion.getText().toString());
        values.put("cantidad", etExistencia.getText().toString());
        values.put("precio", etPrecio.getText().toString());

        int count = db.update("productos", values, "id = ?", new String[]{codigo});
        if (count > 0) {
            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No se pudo actualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminar() {
        String codigo = etCodigo.getText().toString();
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese código para eliminar", Toast.LENGTH_SHORT).show();
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete("productos", "id = ?", new String[]{codigo});
        if (count > 0) {
            Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show();
            limpiar();
        } else {
            Toast.makeText(this, "No se encontró el producto", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiar() {
        etCodigo.setText("");
        etNombre.setText("");
        etDescripcion.setText("");
        etExistencia.setText("");
        etPrecio.setText("");
    }

    private void limpiarExceptoCodigo() {
        etNombre.setText("");
        etDescripcion.setText("");
        etExistencia.setText("");
        etPrecio.setText("");
    }
}
