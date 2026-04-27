package com.example.venta_de_drogas_ricas;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private RecyclerView rvHistorial;
    private HistorialAdapter adapter;
    private TextView tvEmptyState, tvTituloHist;
    private ImageButton btnVolverHist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConfigManager manager = ConfigManager.getInstance(this);
        manager.aplicarConfiguracionBase(this);
        setContentView(R.layout.activity_historial);
        manager.aplicarEstilosVisuales(findViewById(android.R.id.content));

        rvHistorial = findViewById(R.id.rvHistorial);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvTituloHist = findViewById(R.id.tvTituloHist);
        btnVolverHist = findViewById(R.id.btnVolverHist);

        TraductorManager traductor = TraductorManager.getInstance(this);
        
        // Traducir títulos si es necesario
        // tvTituloHist.setText(traductor.getString("historial_title")); // Asumiendo que exista en json
        // tvEmptyState.setText(traductor.getString("historial_empty"));

        btnVolverHist.setOnClickListener(v -> finish());

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));

        cargarHistorial();
    }

    private void cargarHistorial() {
        VentasManager ventasManager = new VentasManager(this);
        List<VentasManager.VentaRecord> listaVentas = ventasManager.cargarRegistro();

        if (listaVentas == null || listaVentas.isEmpty()) {
            rvHistorial.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvHistorial.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            // Invertir para mostrar los más recientes primero
            Collections.reverse(listaVentas);

            MonedaManager monedaManager = MonedaManager.getInstance(this);
            adapter = new HistorialAdapter(listaVentas, monedaManager);
            rvHistorial.setAdapter(adapter);
        }
    }
}
