package com.example.venta_de_drogas_ricas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConfigManager manager = ConfigManager.getInstance(this);
        manager.aplicarConfiguracionBase(this);
        AppConfig config = manager.getConfig();

        setContentView(R.layout.activity_main);

        TextView tvTitulo = findViewById(R.id.textViewWelcome);
        if (tvTitulo != null && config.negocio != null) {
            tvTitulo.setText(config.negocio.nombre_tienda);
        }

        Button btnEntrar = findViewById(R.id.btnEntrar);
        if (btnEntrar != null) {
            btnEntrar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Producto.class);
                startActivity(intent);
            });
        }

        Button btnConfiguracion = findViewById(R.id.btnConfiguracion);
        if (btnConfiguracion != null) {
            btnConfiguracion.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
                startActivity(intent);
            });
        }
    }
}