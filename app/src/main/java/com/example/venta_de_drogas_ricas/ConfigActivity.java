package com.example.venta_de_drogas_ricas;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ConfigActivity extends AppCompatActivity {

    private EditText etNombreTienda, etColorEnfasis;
    private SwitchMaterial swTemaOscuro;
    private RadioGroup rgTamanoLetra;
    private RadioButton rbPequeno, rbMediano, rbGrande;
    private Button btnGuardarConfig;
    private ImageButton btnVolverConfig;
    private ConfigManager configManager;
    private AppConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        configManager = ConfigManager.getInstance(this);
        config = configManager.getConfig();

        // Apariencia
        swTemaOscuro = findViewById(R.id.swTemaOscuro);
        etColorEnfasis = findViewById(R.id.etColorEnfasis);
        rgTamanoLetra = findViewById(R.id.rgTamanoLetra);
        rbPequeno = findViewById(R.id.rbPequeno);
        rbMediano = findViewById(R.id.rbMediano);
        rbGrande = findViewById(R.id.rbGrande);

        // Negocio
        etNombreTienda = findViewById(R.id.etNombreTienda);

        btnGuardarConfig = findViewById(R.id.btnGuardarConfig);
        btnVolverConfig = findViewById(R.id.btnVolverConfig);

        cargarValores();

        btnGuardarConfig.setOnClickListener(v -> guardarCambios());
        btnVolverConfig.setOnClickListener(v -> finish());
    }

    private void cargarValores() {
        // Apariencia
        swTemaOscuro.setChecked(config.apariencia.tema_oscuro);
        etColorEnfasis.setText(config.apariencia.color_enfasis);
        if ("pequeno".equals(config.apariencia.tamano_texto)) rbPequeno.setChecked(true);
        else if ("grande".equals(config.apariencia.tamano_texto)) rbGrande.setChecked(true);
        else rbMediano.setChecked(true);

        // Negocio
        etNombreTienda.setText(config.negocio.nombre_tienda);
    }

    private void guardarCambios() {
        try {
            // Guardar Apariencia
            config.apariencia.tema_oscuro = swTemaOscuro.isChecked();
            config.apariencia.color_enfasis = etColorEnfasis.getText().toString();
            
            int selectedId = rgTamanoLetra.getCheckedRadioButtonId();
            if (selectedId == R.id.rbPequeno) config.apariencia.tamano_texto = "pequeno";
            else if (selectedId == R.id.rbGrande) config.apariencia.tamano_texto = "grande";
            else config.apariencia.tamano_texto = "mediano";

            // Guardar Negocio
            config.negocio.nombre_tienda = etNombreTienda.getText().toString();

            configManager.saveConfig();
            Toast.makeText(this, "Ajustes guardados con éxito", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error en los datos ingresados", Toast.LENGTH_SHORT).show();
        }
    }
}
