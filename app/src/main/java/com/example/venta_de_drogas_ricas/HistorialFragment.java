package com.example.venta_de_drogas_ricas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class HistorialFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cargarHistorial(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar al volver a la pestaña para mostrar nuevas ventas
        if (getView() != null) cargarHistorial(getView());
    }

    private void cargarHistorial(View view) {
        RecyclerView rvHistorial = view.findViewById(R.id.rvHistorial);
        TextView tvEmptyState = view.findViewById(R.id.tvEmptyState);

        VentasManager ventasManager = new VentasManager(requireContext());
        List<VentasManager.VentaRecord> listaVentas = ventasManager.cargarRegistro();

        if (listaVentas == null || listaVentas.isEmpty()) {
            rvHistorial.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvHistorial.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
            Collections.reverse(listaVentas);
            rvHistorial.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvHistorial.setAdapter(new HistorialAdapter(listaVentas, MonedaManager.getInstance(requireContext())));
        }
    }
}
