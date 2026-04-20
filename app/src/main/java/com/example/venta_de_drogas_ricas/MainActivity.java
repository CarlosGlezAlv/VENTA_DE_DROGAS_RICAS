package com.example.venta_de_drogas_ricas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnEntrar = findViewById(R.id.btnEntrar);
        if (btnEntrar != null) {
            btnEntrar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Producto.class);
                startActivity(intent);
            });
        }
    }
}