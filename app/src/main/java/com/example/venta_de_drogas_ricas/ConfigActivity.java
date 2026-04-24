package com.example.venta_de_drogas_ricas;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
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
    private Spinner spIdioma, spMonedaVisual;
    private ConfigManager configManager;
    private AppConfig config;
    private SharedPreferences idiomaPrefs;

    private static final String PREFS_IDIOMA = "idioma_prefs";
    private static final String KEY_IDIOMA = "idioma";

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
        spIdioma = findViewById(R.id.spIdioma);
        spMonedaVisual = findViewById(R.id.spMonedaVisual);

        idiomaPrefs = getSharedPreferences(PREFS_IDIOMA, MODE_PRIVATE);
        setupSelectorIdioma();
        setupSelectorMoneda();

        cargarValores();
        aplicarTraducciones();

        btnGuardarConfig.setOnClickListener(v -> guardarCambios());
        btnVolverConfig.setOnClickListener(v -> finish());
    }

    private void aplicarTraducciones() {
        TraductorManager traductor = TraductorManager.getInstance(this);
        if (swTemaOscuro != null) swTemaOscuro.setText(
            traductor.getString("config_modo_oscuro")
        );
        if (etColorEnfasis != null) etColorEnfasis.setHint(
            traductor.getString("config_color_enfasis_hint")
        );
        if (rbPequeno != null) rbPequeno.setText(
            traductor.getString("config_tamano_pequeno")
        );
        if (rbMediano != null) rbMediano.setText(
            traductor.getString("config_tamano_mediano")
        );
        if (rbGrande != null) rbGrande.setText(
            traductor.getString("config_tamano_grande")
        );
        if (etNombreTienda != null) etNombreTienda.setHint(
            traductor.getString("config_nombre_tienda_hint")
        );
        if (etMinimoAlerta != null) etMinimoAlerta.setHint(
            traductor.getString("config_alerta_minimo_hint")
        );
        if (etCritico != null) etCritico.setHint(
            traductor.getString("config_alerta_critico_hint")
        );
        if (swBloquearSinStock != null) swBloquearSinStock.setText(
            traductor.getString("config_bloquear_sin_stock")
        );
        if (swMostrarPopup != null) swMostrarPopup.setText(
            traductor.getString("config_mostrar_popup")
        );
        if (swUsarColor != null) swUsarColor.setText(
            traductor.getString("config_usar_color")
        );
        if (etMensajeBajo != null) etMensajeBajo.setHint(
            traductor.getString("config_msg_bajo_hint")
        );
        if (etMensajeCritico != null) etMensajeCritico.setHint(
            traductor.getString("config_msg_critico_hint")
        );
        if (etMensajeSinStock != null) etMensajeSinStock.setHint(
            traductor.getString("config_msg_sin_stock_hint")
        );
        if (
            spMonedaVisual != null &&
            spMonedaVisual.getSelectedView() instanceof TextView
        ) {
            ((TextView) spMonedaVisual.getSelectedView()).setTextColor(
                getResources().getColor(android.R.color.black)
            );
        }
        if (btnGuardarConfig != null) btnGuardarConfig.setText(
            traductor.getString("config_btn_guardar")
        );
    }

    private void setupSelectorIdioma() {
        String[] idiomas = { "Español", "English" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            idiomas
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        spIdioma.setAdapter(adapter);

        String idiomaGuardado = idiomaPrefs.getString(KEY_IDIOMA, "es");
        int posicion = "en".equals(idiomaGuardado) ? 1 : 0;
        spIdioma.setSelection(posicion, false);

        spIdioma.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(
                    AdapterView<?> parent,
                    android.view.View view,
                    int position,
                    long id
                ) {
                    String codigo = position == 1 ? "en" : "es";
                    idiomaPrefs.edit().putString(KEY_IDIOMA, codigo).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // No-op
                }
            }
        );
    }

    private void setupSelectorMoneda() {
        if (spMonedaVisual == null) return;

        java.util.List<String> monedas = MonedaManager.getInstance(
            this
        ).getMonedasSoportadas();

        if (monedas == null || monedas.isEmpty()) {
            monedas = new java.util.ArrayList<>();
            monedas.add("MXN");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            monedas
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        spMonedaVisual.setAdapter(adapter);
    }

    private void cargarValores() {
        // Apariencia
        swTemaOscuro.setChecked(config.apariencia.tema_oscuro);
        etColorEnfasis.setText(config.apariencia.color_enfasis);
        if (
            "pequeno".equals(config.apariencia.tamano_texto)
        ) rbPequeno.setChecked(true);
        else if (
            "grande".equals(config.apariencia.tamano_texto)
        ) rbGrande.setChecked(true);
        else rbMediano.setChecked(true);

        // Negocio
        etNombreTienda.setText(config.negocio.nombre_tienda);

        if (spMonedaVisual != null) {
            String monedaVisual = (config != null && config.negocio != null)
                ? config.negocio.moneda_visual
                : "MXN";

            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<
                String
            >) spMonedaVisual.getAdapter();
            if (adapter != null) {
                int position = adapter.getPosition(monedaVisual);
                if (position < 0) {
                    position = adapter.getPosition("MXN");
                }
                if (position >= 0) {
                    spMonedaVisual.setSelection(position, false);
                }
            }
        }

        // Alertas y Stock
        if (etMinimoAlerta != null) {
            etMinimoAlerta.setText(
                String.valueOf(configAlertas.stock.minimo_alerta)
            );
            etCritico.setText(String.valueOf(configAlertas.stock.critico));
            swBloquearSinStock.setChecked(
                configAlertas.stock.bloquear_sin_stock
            );
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
            config.apariencia.color_enfasis = etColorEnfasis
                .getText()
                .toString();

            int selectedId = rgTamanoLetra.getCheckedRadioButtonId();
            if (selectedId == R.id.rbPequeno) config.apariencia.tamano_texto =
                "pequeno";
            else if (
                selectedId == R.id.rbGrande
            ) config.apariencia.tamano_texto = "grande";
            else config.apariencia.tamano_texto = "mediano";

            // Guardar Negocio
            config.negocio.nombre_tienda = etNombreTienda.getText().toString();

            if (
                spMonedaVisual != null &&
                spMonedaVisual.getSelectedItem() != null
            ) {
                config.negocio.moneda_visual = spMonedaVisual
                    .getSelectedItem()
                    .toString();
            }

            // Guardar Alertas y Stock
            if (etMinimoAlerta != null) {
                try {
                    configAlertas.stock.minimo_alerta = Integer.parseInt(
                        etMinimoAlerta.getText().toString()
                    );
                    configAlertas.stock.critico = Integer.parseInt(
                        etCritico.getText().toString()
                    );
                } catch (NumberFormatException e) {
                    // Ignore, keep default/previous values
                }
                configAlertas.stock.bloquear_sin_stock =
                    swBloquearSinStock.isChecked();
                configAlertas.alertas.mostrar_popup =
                    swMostrarPopup.isChecked();
                configAlertas.alertas.usar_color = swUsarColor.isChecked();
                configAlertas.alertas.mensaje_bajo = etMensajeBajo
                    .getText()
                    .toString();
                configAlertas.alertas.mensaje_critico = etMensajeCritico
                    .getText()
                    .toString();
                configAlertas.alertas.mensaje_sin_stock = etMensajeSinStock
                    .getText()
                    .toString();

                configManager.saveConfigAlertas();
            }

            configManager.saveConfig();
            configManager.aplicarConfiguracionBase(this);
            TraductorManager.getInstance(this).cargarIdioma(this);
            aplicarTraducciones();
            Toast.makeText(
                this,
                TraductorManager.getInstance(this).getString(
                    "msg_ajustes_guardados"
                ),
                Toast.LENGTH_LONG
            ).show();

            // Sincronizar inmediatamente y reiniciar el stack en MainActivity
            android.content.Intent intent = new android.content.Intent(
                this,
                MainActivity.class
            );
            intent.addFlags(
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(
                this,
                TraductorManager.getInstance(this).getString("msg_error_datos"),
                Toast.LENGTH_SHORT
            ).show();
        }
    }
}
