package com.example.venta_de_drogas_ricas;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.List;

public class InicioFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppConfig config = ConfigManager.getInstance(requireContext()).getConfig();
        TextView tvNombre = view.findViewById(R.id.tvInicioNombreTienda);
        if (config != null && config.negocio != null && config.negocio.nombre_tienda != null && !config.negocio.nombre_tienda.isEmpty()) {
            tvNombre.setText(config.negocio.nombre_tienda);
        }

        // Total de productos en BD
        TextView tvTotalProductos = view.findViewById(R.id.tvInicioTotalProductos);
        try {
            BD_DrogsDataBase dbHelper = new BD_DrogsDataBase(requireContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM productos", null);
            if (c.moveToFirst()) tvTotalProductos.setText(String.valueOf(c.getInt(0)));
            c.close();
        } catch (Exception ignored) {}

        // Total de ventas registradas
        TextView tvTotalVentas = view.findViewById(R.id.tvInicioTotalVentas);
        VentasManager vm = new VentasManager(requireContext());
        List<VentasManager.VentaRecord> ventas = vm.cargarRegistro();
        tvTotalVentas.setText(String.valueOf(ventas.size()));

        // Última venta
        TextView tvFolio = view.findViewById(R.id.tvInicioUltimaVentaFolio);
        TextView tvFecha = view.findViewById(R.id.tvInicioUltimaVentaFecha);
        TextView tvTotal = view.findViewById(R.id.tvInicioUltimaVentaTotal);
        if (!ventas.isEmpty()) {
            VentasManager.VentaRecord ultima = ventas.get(ventas.size() - 1);
            tvFolio.setText(ultima.folio != null ? ultima.folio : "—");
            tvFecha.setText(ultima.fecha);
            MonedaManager mm = MonedaManager.getInstance(requireContext());
            String moneda = ultima.moneda_visual != null ? ultima.moneda_visual : "MXN";
            tvTotal.setText(mm.formatear(java.math.BigDecimal.valueOf(ultima.total_visual), moneda));
        }
    }
}
