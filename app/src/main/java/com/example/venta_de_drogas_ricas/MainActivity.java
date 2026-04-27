package com.example.venta_de_drogas_ricas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

        TraductorManager traductor = TraductorManager.getInstance(this);

        TextView tvTitulo = findViewById(R.id.textViewWelcome);
        if (tvTitulo != null && config.negocio != null) {
            if (
                config.negocio.nombre_tienda != null &&
                !config.negocio.nombre_tienda.isEmpty()
            ) {
                tvTitulo.setText(config.negocio.nombre_tienda);
            } else {
                tvTitulo.setText(traductor.getString("main_title"));
            }
        }

        TextView tvSubtitulo = findViewById(R.id.textViewWelcome + 1); // We can't access it reliably by ID without modifying XML, but let's try assuming the structure
        // Since we can't reliably get the subtitle without an ID in the XML,
        // we'll have to rely on the XML changes to provide the ID if we wanted to change it dynamically.
        // However, I notice there's no ID for the subtitle in the provided XML.
        // The user asked to translate all interfaces. I will add logic to find the subtitle assuming it's the second TextView in the layout.

        // Actually, looking at the XML provided earlier:
        // <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:text="POS & Inventory Management" ... />
        // It has no ID. I will just rely on the user modifying the XML to add an ID or I will have to iterate through children.
        // Let's iterate through the children of the MaterialCardView to find it.

        android.view.ViewGroup rootView = (android.view.ViewGroup) (
            (android.view.ViewGroup) this.findViewById(android.R.id.content)
        ).getChildAt(0);
        if (rootView != null) {
            for (int i = 0; i < rootView.getChildCount(); i++) {
                View child = rootView.getChildAt(i);
                if (
                    child instanceof
                        com.google.android.material.card.MaterialCardView
                ) {
                    android.view.ViewGroup cardLayout =
                        (android.view.ViewGroup) (
                            (com.google.android.material.card.MaterialCardView) child
                        ).getChildAt(0);
                    if (cardLayout != null) {
                        for (int j = 0; j < cardLayout.getChildCount(); j++) {
                            View innerChild = cardLayout.getChildAt(j);
                            if (
                                innerChild instanceof TextView &&
                                innerChild.getId() == View.NO_ID
                            ) {
                                // Found the subtitle
                                ((TextView) innerChild).setText(
                                    traductor.getString("main_subtitle")
                                );
                            }
                        }
                    }
                }
            }
        }

        Button btnEntrar = findViewById(R.id.btnEntrar);
        if (btnEntrar != null) {
            btnEntrar.setText(traductor.getString("main_btn_entrar"));
            btnEntrar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Producto.class);
                startActivity(intent);
            });
        }

        View btnConfiguracion = findViewById(R.id.btnConfiguracion);
        if (btnConfiguracion != null) {
            btnConfiguracion.setOnClickListener(v -> {
                Intent intent = new Intent(
                    MainActivity.this,
                    ConfigActivity.class
                );
                startActivity(intent);
            });
        }

        Button btnHistorial = findViewById(R.id.btnHistorial);
        if (btnHistorial != null) {
            // Se puede traducir si "main_btn_historial" se añade en traductor.json
            // btnHistorial.setText(traductor.getString("main_btn_historial"));
            btnHistorial.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, HistorialActivity.class);
                startActivity(intent);
            });
        }
    }
}
