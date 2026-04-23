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
    
    // Alertas y Stock
    private EditText etMinimoAlerta, etCritico, etMensajeBajo, etMensajeCritico, etMensajeSinStock;
    private SwitchMaterial swBloquearSinStock, swMostrarPopup, swUsarColor;
    private ConfiguracionAlertas configAlertas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        configManager = ConfigManager.getInstance(this);
        config = configManager.getConfig();
        configAlertas = configManager.getConfigAlertas();

        // Apariencia
        swTemaOscuro = findViewById(R.id.swTemaOscuro);
        etColorEnfasis = findViewById(R.id.etColorEnfasis);
        rgTamanoLetra = findViewById(R.id.rgTamanoLetra);
        rbPequeno = findViewById(R.id.rbPequeno);
        rbMediano = findViewById(R.id.rbMediano);
        rbGrande = findViewById(R.id.rbGrande);

        // Negocio
        etNombreTienda = findViewById(R.id.etNombreTienda);

        // Alertas y Stock
        etMinimoAlerta = findViewById(R.id.etMinimoAlerta);
        etCritico = findViewById(R.id.etCritico);
        swBloquearSinStock = findViewById(R.id.swBloquearSinStock);
        swMostrarPopup = findViewById(R.id.swMostrarPopup);
        swUsarColor = findViewById(R.id.swUsarColor);
        etMensajeBajo = findViewById(R.id.etMensajeBajo);
        etMensajeCritico = findViewById(R.id.etMensajeCritico);
        etMensajeSinStock = findViewById(R.id.etMensajeSinStock);

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

        // Alertas y Stock
        if (etMinimoAlerta != null) {
            etMinimoAlerta.setText(String.valueOf(configAlertas.stock.minimo_alerta));
            etCritico.setText(String.valueOf(configAlertas.stock.critico));
            swBloquearSinStock.setChecked(configAlertas.stock.bloquear_sin_stock);
            swMostrarPopup.setChecked(configAlertas.alertas.mostrar_popup);
            swUsarColor.setChecked(configAlertas.alertas.usar_color);
            etMensajeBajo.setText(configAlertas.alertas.mensaje_bajo);
            etMensajeCritico.setText(configAlertas.alertas.mensaje_critico);
            etMensajeSinStock.setText(configAlertas.alertas.mensaje_sin_stock);
        }
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

            // Guardar Alertas y Stock
            if (etMinimoAlerta != null) {
                try {
                    configAlertas.stock.minimo_alerta = Integer.parseInt(etMinimoAlerta.getText().toString());
                    configAlertas.stock.critico = Integer.parseInt(etCritico.getText().toString());
                } catch (NumberFormatException e) {
                    // Ignore, keep default/previous values
                }
                configAlertas.stock.bloquear_sin_stock = swBloquearSinStock.isChecked();
                configAlertas.alertas.mostrar_popup = swMostrarPopup.isChecked();
                configAlertas.alertas.usar_color = swUsarColor.isChecked();
                configAlertas.alertas.mensaje_bajo = etMensajeBajo.getText().toString();
                configAlertas.alertas.mensaje_critico = etMensajeCritico.getText().toString();
                configAlertas.alertas.mensaje_sin_stock = etMensajeSinStock.getText().toString();
                
                configManager.saveConfigAlertas();
            }

            configManager.saveConfig();
            configManager.aplicarConfiguracionBase(this);
            Toast.makeText(this, "Ajustes guardados con éxito", Toast.LENGTH_LONG).show();

            // Sincronizar inmediatamente y reiniciar el stack en MainActivity
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error en los datos ingresados", Toast.LENGTH_SHORT).show();
        }
    }
}
