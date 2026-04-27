package com.example.venta_de_drogas_ricas;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ConfigFragment extends Fragment {

    private EditText etNombreTienda, etColorEnfasis;
    private SwitchMaterial swTemaOscuro;
    private RadioGroup rgTamanoLetra;
    private RadioButton rbPequeno, rbMediano, rbGrande;
    private Button btnGuardarConfig;
    private Spinner spIdioma, spMonedaVisual;
    private ConfigManager configManager;
    private AppConfig config;
    private SharedPreferences idiomaPrefs;
    private static final String PREFS_IDIOMA = "idioma_prefs";
    private static final String KEY_IDIOMA = "idioma";
    private EditText etMinimoAlerta, etCritico, etMensajeBajo, etMensajeCritico, etMensajeSinStock;
    private SwitchMaterial swBloquearSinStock, swMostrarPopup, swUsarColor;
    private ConfiguracionAlertas configAlertas;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        configManager = ConfigManager.getInstance(requireContext());
        config = configManager.getConfig();
        configAlertas = configManager.getConfigAlertas();
        idiomaPrefs = requireContext().getSharedPreferences(PREFS_IDIOMA, android.content.Context.MODE_PRIVATE);

        swTemaOscuro = view.findViewById(R.id.swTemaOscuro);
        etColorEnfasis = view.findViewById(R.id.etColorEnfasis);
        rgTamanoLetra = view.findViewById(R.id.rgTamanoLetra);
        rbPequeno = view.findViewById(R.id.rbPequeno);
        rbMediano = view.findViewById(R.id.rbMediano);
        rbGrande = view.findViewById(R.id.rbGrande);
        etNombreTienda = view.findViewById(R.id.etNombreTienda);
        etMinimoAlerta = view.findViewById(R.id.etMinimoAlerta);
        etCritico = view.findViewById(R.id.etCritico);
        swBloquearSinStock = view.findViewById(R.id.swBloquearSinStock);
        swMostrarPopup = view.findViewById(R.id.swMostrarPopup);
        swUsarColor = view.findViewById(R.id.swUsarColor);
        etMensajeBajo = view.findViewById(R.id.etMensajeBajo);
        etMensajeCritico = view.findViewById(R.id.etMensajeCritico);
        etMensajeSinStock = view.findViewById(R.id.etMensajeSinStock);
        btnGuardarConfig = view.findViewById(R.id.btnGuardarConfig);
        spIdioma = view.findViewById(R.id.spIdioma);
        spMonedaVisual = view.findViewById(R.id.spMonedaVisual);

        setupSelectorIdioma();
        setupSelectorMoneda();
        cargarValores();
        aplicarTraducciones();

        btnGuardarConfig.setOnClickListener(v -> guardarCambios());
    }

    private void aplicarTraducciones() {
        TraductorManager t = TraductorManager.getInstance(requireContext());
        if (swTemaOscuro != null) swTemaOscuro.setText(t.getString("config_modo_oscuro"));
        if (etColorEnfasis != null) etColorEnfasis.setHint(t.getString("config_color_enfasis_hint"));
        if (rbPequeno != null) rbPequeno.setText(t.getString("config_tamano_pequeno"));
        if (rbMediano != null) rbMediano.setText(t.getString("config_tamano_mediano"));
        if (rbGrande != null) rbGrande.setText(t.getString("config_tamano_grande"));
        if (etNombreTienda != null) etNombreTienda.setHint(t.getString("config_nombre_tienda_hint"));
        if (etMinimoAlerta != null) etMinimoAlerta.setHint(t.getString("config_alerta_minimo_hint"));
        if (etCritico != null) etCritico.setHint(t.getString("config_alerta_critico_hint"));
        if (swBloquearSinStock != null) swBloquearSinStock.setText(t.getString("config_bloquear_sin_stock"));
        if (swMostrarPopup != null) swMostrarPopup.setText(t.getString("config_mostrar_popup"));
        if (swUsarColor != null) swUsarColor.setText(t.getString("config_usar_color"));
        if (etMensajeBajo != null) etMensajeBajo.setHint(t.getString("config_msg_bajo_hint"));
        if (etMensajeCritico != null) etMensajeCritico.setHint(t.getString("config_msg_critico_hint"));
        if (etMensajeSinStock != null) etMensajeSinStock.setHint(t.getString("config_msg_sin_stock_hint"));
        if (btnGuardarConfig != null) btnGuardarConfig.setText(t.getString("config_btn_guardar"));
    }

    private void setupSelectorIdioma() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new String[]{"Español", "English"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIdioma.setAdapter(adapter);
        spIdioma.setSelection("en".equals(idiomaPrefs.getString(KEY_IDIOMA, "es")) ? 1 : 0, false);
        spIdioma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { idiomaPrefs.edit().putString(KEY_IDIOMA, pos == 1 ? "en" : "es").apply(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupSelectorMoneda() {
        if (spMonedaVisual == null) return;
        java.util.List<String> monedas = MonedaManager.getInstance(requireContext()).getMonedasSoportadas();
        if (monedas == null || monedas.isEmpty()) { monedas = new java.util.ArrayList<>(); monedas.add("MXN"); }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, monedas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonedaVisual.setAdapter(adapter);
    }

    private void cargarValores() {
        swTemaOscuro.setChecked(config.apariencia.tema_oscuro);
        etColorEnfasis.setText(config.apariencia.color_enfasis);
        if ("pequeno".equals(config.apariencia.tamano_texto)) rbPequeno.setChecked(true);
        else if ("grande".equals(config.apariencia.tamano_texto)) rbGrande.setChecked(true);
        else rbMediano.setChecked(true);
        etNombreTienda.setText(config.negocio.nombre_tienda);
        if (spMonedaVisual != null) {
            String monedaVisual = (config.negocio != null) ? config.negocio.moneda_visual : "MXN";
            @SuppressWarnings("unchecked") ArrayAdapter<String> adapter = (ArrayAdapter<String>) spMonedaVisual.getAdapter();
            if (adapter != null) { int pos = adapter.getPosition(monedaVisual); if (pos < 0) pos = adapter.getPosition("MXN"); if (pos >= 0) spMonedaVisual.setSelection(pos, false); }
        }
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
            config.apariencia.tema_oscuro = swTemaOscuro.isChecked();
            config.apariencia.color_enfasis = etColorEnfasis.getText().toString();
            int selectedId = rgTamanoLetra.getCheckedRadioButtonId();
            if (selectedId == R.id.rbPequeno) config.apariencia.tamano_texto = "pequeno";
            else if (selectedId == R.id.rbGrande) config.apariencia.tamano_texto = "grande";
            else config.apariencia.tamano_texto = "mediano";
            config.negocio.nombre_tienda = etNombreTienda.getText().toString();
            if (spMonedaVisual != null && spMonedaVisual.getSelectedItem() != null) config.negocio.moneda_visual = spMonedaVisual.getSelectedItem().toString();
            if (etMinimoAlerta != null) {
                try { configAlertas.stock.minimo_alerta = Integer.parseInt(etMinimoAlerta.getText().toString()); configAlertas.stock.critico = Integer.parseInt(etCritico.getText().toString()); } catch (NumberFormatException ignored) {}
                configAlertas.stock.bloquear_sin_stock = swBloquearSinStock.isChecked();
                configAlertas.alertas.mostrar_popup = swMostrarPopup.isChecked();
                configAlertas.alertas.usar_color = swUsarColor.isChecked();
                configAlertas.alertas.mensaje_bajo = etMensajeBajo.getText().toString();
                configAlertas.alertas.mensaje_critico = etMensajeCritico.getText().toString();
                configAlertas.alertas.mensaje_sin_stock = etMensajeSinStock.getText().toString();
                configManager.saveConfigAlertas();
            }
            configManager.saveConfig();
            configManager.aplicarConfiguracionBase((androidx.appcompat.app.AppCompatActivity) requireActivity());
            TraductorManager.getInstance(requireContext()).cargarIdioma(requireContext());
            aplicarTraducciones();
            Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_ajustes_guardados"), Toast.LENGTH_LONG).show();
            // Ya no necesitamos reiniciar el stack completo — solo recrear la Activity host para aplicar tema
            requireActivity().recreate();
        } catch (Exception e) {
            Toast.makeText(requireContext(), TraductorManager.getInstance(requireContext()).getString("msg_error_datos"), Toast.LENGTH_SHORT).show();
        }
    }
}
