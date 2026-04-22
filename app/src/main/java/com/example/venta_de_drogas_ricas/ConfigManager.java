package com.example.venta_de_drogas_ricas;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final String FILE_NAME = "appconfig.json";
    private static final String FILE_NAME_ALERTAS = "configuracion_alertas.json";
    private static ConfigManager instance;
    private AppConfig config;
    private ConfiguracionAlertas configAlertas;
    private Context context;

    private ConfigManager(Context context) {
        this.context = context.getApplicationContext();
        loadConfig();
        loadConfigAlertas();
    }

    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigManager(context);
        }
        return instance;
    }

    public AppConfig getConfig() {
        if (config == null) loadConfig();
        return config;
    }

    public ConfiguracionAlertas getConfigAlertas() {
        if (configAlertas == null) loadConfigAlertas();
        return configAlertas;
    }


    public void aplicarConfiguracionBase(AppCompatActivity activity) {
        if (config == null) loadConfig();
        
        if (config.negocio != null && config.negocio.nombre_tienda != null) {
            activity.setTitle(config.negocio.nombre_tienda);
        }
        
        if (config.apariencia != null) {
            if (config.apariencia.tema_oscuro) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    public void aplicarEstilosVisuales(View root) {
        if (config == null) loadConfig();
        aplicarRecursivo(root);
    }

    private void aplicarRecursivo(View v) {
        boolean isDark = config.apariencia.tema_oscuro;

        // 1. Fondos y Overlays
        if (v.getId() == R.id.overlayView) {
            v.setBackgroundColor(isDark ? Color.parseColor("#CC000000") : Color.parseColor("#99FFFFFF"));
        }

        // 2. Tarjetas (Compatibilidad con ambos tipos)
        if (v instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) v;
            card.setCardBackgroundColor(isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#FFFFFF"));
            card.setStrokeColor(ColorStateList.valueOf(isDark ? Color.parseColor("#55FFFFFF") : Color.parseColor("#DDDDDD")));
        } else if (v instanceof CardView) {
            ((CardView) v).setCardBackgroundColor(isDark ? Color.parseColor("#222222") : Color.parseColor("#FFFFFF"));
        }

        // 3. Textos, Botones y Inputs
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            
            // Tamaño de letra
            float size = 16f; // Mediano
            if ("pequeno".equals(config.apariencia.tamano_texto)) size = 13f;
            else if ("grande".equals(config.apariencia.tamano_texto)) size = 26f;
            tv.setTextSize(size);

            // Color de texto (evitar sobreescribir colores especiales de estado)
            if (v.getId() != R.id.tvInfoProd && v.getId() != R.id.tvSubtotalConsultado && v.getId() != R.id.tvTotalGeneral) {
                tv.setTextColor(isDark ? Color.WHITE : Color.BLACK);
            }
        }

        // 4. Tintado de Botones e Iconos
        if (v instanceof Button) {
            try {
                String hex = config.apariencia.color_enfasis;
                if (!hex.startsWith("#")) hex = "#" + hex;
                int color = Color.parseColor(hex);
                v.setBackgroundTintList(ColorStateList.valueOf(color));
                ((Button) v).setTextColor(isColorDark(color) ? Color.WHITE : Color.BLACK);
            } catch (Exception e) {}
        } else if (v instanceof ImageButton) {
            ((ImageButton) v).setImageTintList(ColorStateList.valueOf(isDark ? Color.WHITE : Color.BLACK));
        }
        
        // 5. TextInputLayout
        if (v instanceof TextInputLayout) {
            TextInputLayout til = (TextInputLayout) v;
            til.setHintTextColor(ColorStateList.valueOf(isDark ? Color.WHITE : Color.BLACK));
            try {
                String hex = config.apariencia.color_enfasis;
                if (!hex.startsWith("#")) hex = "#" + hex;
                til.setBoxStrokeColor(Color.parseColor(hex));
            } catch (Exception e) {}
        }

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                aplicarRecursivo(vg.getChildAt(i));
            }
        }
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    public void loadConfig() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        Gson gson = new Gson();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = gson.fromJson(reader, AppConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
        if (config == null) config = new AppConfig();
    }

    public void saveConfig() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfigAlertas() {
        File file = new File(context.getFilesDir(), FILE_NAME_ALERTAS);
        Gson gson = new Gson();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                configAlertas = gson.fromJson(reader, ConfiguracionAlertas.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
        if (configAlertas == null) configAlertas = new ConfiguracionAlertas();
    }

    public void saveConfigAlertas() {
        File file = new File(context.getFilesDir(), FILE_NAME_ALERTAS);
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(configAlertas, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
