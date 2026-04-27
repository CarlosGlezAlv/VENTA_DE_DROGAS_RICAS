package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class InventarioFragment extends Fragment {

    private EditText etCodigo, etNombre, etDescripcion, etExistencia, etPrecio;
    private Button btnGuardar, btnLimpiar;
    private TextInputLayout tilCodigoP, tilNombreP;
    private RecyclerView rvProductos;
    private ProductoAdapter adapter;
    private BD_DrogsDataBase dbHelper;
    private List<ProductoModel> listaProductos = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new BD_DrogsDataBase(requireContext());

        etCodigo = view.findViewById(R.id.etCodigo);
        etNombre = view.findViewById(R.id.etNombre);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        etExistencia = view.findViewById(R.id.etExistencia);
        etPrecio = view.findViewById(R.id.etPrecio);
        tilCodigoP = view.findViewById(R.id.tilCodigoP);
        tilNombreP = view.findViewById(R.id.tilNombreP);
        btnGuardar = view.findViewById(R.id.btnGuardar);
        btnLimpiar = view.findViewById(R.id.btnLimpiar);
        rvProductos = view.findViewById(R.id.rvProductos);

        TraductorManager traductor = TraductorManager.getInstance(requireContext());
        TextView tvTitulo = view.findViewById(R.id.tvTituloProd);
        if (tvTitulo != null) tvTitulo.setText(traductor.getString("prod_title"));
        btnGuardar.setText(traductor.getString("prod_btn_guardar"));
        btnLimpiar.setText(traductor.getString("prod_btn_limpiar"));
        tilCodigoP.setHint(traductor.getString("prod_codigo_hint"));
        tilNombreP.setHint(traductor.getString("prod_nombre_hint"));
        etDescripcion.setHint(traductor.getString("prod_desc_hint"));
        etExistencia.setHint(traductor.getString("prod_existencia_hint"));
        etPrecio.setHint(traductor.getString("prod_precio_hint"));

        rvProductos.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductoAdapter(listaProductos, new ProductoAdapter.OnItemClickListener() {
            @Override public void onItemClick(ProductoModel p) { cargarEnFormulario(p); }
            @Override public void onEditClick(ProductoModel p) { cargarEnFormulario(p); etNombre.requestFocus(); }
            @Override public void onDeleteClick(ProductoModel p) { eliminar(p.getId()); }
        });
        rvProductos.setAdapter(adapter);

        tilCodigoP.setEndIconOnClickListener(v -> buscarPorCodigo());
        tilNombreP.setEndIconOnClickListener(v -> cargarListaProductos(etNombre.getText().toString()));
        btnGuardar.setOnClickListener(v -> guardarOActualizar());
        btnLimpiar.setOnClickListener(v -> limpiarFormulario());

        cargarListaProductos("");
    }

    private void cargarListaProductos(String filtro) {
        listaProductos.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = filtro.isEmpty()
            ? db.rawQuery("SELECT * FROM productos", null)
            : db.rawQuery("SELECT * FROM productos WHERE nombre LIKE ?", new String[]{"%" + filtro + "%"});
        if (cursor.moveToFirst()) {
            do {
                listaProductos.add(new ProductoModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getFloat(3), cursor.getFloat(4)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void buscarPorCodigo() {
        String codigo = etCodigo.getText().toString();
        if (codigo.isEmpty()) { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_ingrese_codigo"), Toast.LENGTH_SHORT).show(); return; }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM productos WHERE id = ?", new String[]{codigo});
        if (cursor.moveToFirst()) {
            cargarEnFormulario(new ProductoModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getFloat(3), cursor.getFloat(4)));
            for (ProductoModel item : listaProductos) item.setSelected(item.getId().equals(codigo));
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_no_encontrado"), Toast.LENGTH_SHORT).show();
            limpiarFormularioExceptoCodigo();
        }
        cursor.close();
    }

    private void cargarEnFormulario(ProductoModel p) {
        etCodigo.setText(p.getId()); etNombre.setText(p.getNombre());
        etDescripcion.setText(p.getDescripcion());
        etExistencia.setText(String.valueOf(p.getCantidad()));
        etPrecio.setText(String.valueOf(p.getPrecio()));
    }

    private void guardarOActualizar() {
        String codigo = etCodigo.getText().toString();
        if (codigo.isEmpty()) { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_debe_ingresar_codigo"), Toast.LENGTH_SHORT).show(); return; }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", codigo); values.put("nombre", etNombre.getText().toString());
        values.put("descripcion", etDescripcion.getText().toString());
        values.put("cantidad", etExistencia.getText().toString());
        values.put("precio", etPrecio.getText().toString());
        long result = db.insertWithOnConflict("productos", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_guardado_exito"), Toast.LENGTH_SHORT).show(); limpiarFormulario(); cargarListaProductos(""); }
        else { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_error_guardar"), Toast.LENGTH_SHORT).show(); }
    }

    private void eliminar(String codigo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete("productos", "id = ?", new String[]{codigo});
        if (count > 0) { Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_eliminado"), Toast.LENGTH_SHORT).show(); limpiarFormulario(); cargarListaProductos(""); }
    }

    private void limpiarFormulario() { etCodigo.setText(""); limpiarFormularioExceptoCodigo(); for (ProductoModel p : listaProductos) p.setSelected(false); adapter.notifyDataSetChanged(); etCodigo.requestFocus(); }
    private void limpiarFormularioExceptoCodigo() { etNombre.setText(""); etDescripcion.setText(""); etExistencia.setText(""); etPrecio.setText(""); }
}
