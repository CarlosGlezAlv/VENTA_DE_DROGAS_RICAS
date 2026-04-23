package com.example.venta_de_drogas_ricas;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TraductorManager {

    private static TraductorManager instance;
    private JSONObject traduccionesActuales;

    private static final String PREFS_IDIOMA = "idioma_prefs";
    private static final String KEY_IDIOMA = "idioma";
    private static final String ARCHIVO_JSON = "traductor.json";

    private TraductorManager(Context context) {
        cargarIdioma(context);
    }

    public static TraductorManager getInstance(Context context) {
        if (instance == null) {
            instance = new TraductorManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Lee el archivo traductor.json desde assets y carga el objeto JSON
     * correspondiente al idioma guardado en SharedPreferences.
     */
    public void cargarIdioma(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_IDIOMA, Context.MODE_PRIVATE);
        String idiomaSeleccionado = prefs.getString(KEY_IDIOMA, "es");

        try {
            InputStream is = context.getAssets().open(ARCHIVO_JSON);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonGlobal = new JSONObject(jsonString);

            // Cargar el bloque del idioma seleccionado, o "es" como respaldo
            if (jsonGlobal.has(idiomaSeleccionado)) {
                traduccionesActuales = jsonGlobal.getJSONObject(idiomaSeleccionado);
            } else if (jsonGlobal.has("es")) {
                traduccionesActuales = jsonGlobal.getJSONObject("es");
            } else {
                traduccionesActuales = new JSONObject();
            }

        } catch (Exception e) {
            e.printStackTrace();
            traduccionesActuales = new JSONObject();
        }
    }

    /**
     * Devuelve el texto traducido para una clave específica.
     * Si no existe, devuelve la misma clave como texto por defecto.
     */
    public String getString(String key) {
        try {
            if (traduccionesActuales != null && traduccionesActuales.has(key)) {
                return traduccionesActuales.getString(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Retorna la key si no se encuentra la traducción, para que el programador note el error
        return key;
    }

    /**
     * Devuelve el texto traducido permitiendo formatear variables,
     * por ejemplo: getString("msg_stock_precio", stock, precio)
     */
    public String getString(String key, Object... args) {
        String texto = getString(key);
        try {
            return String.format(texto, args);
        } catch (Exception e) {
            e.printStackTrace();
            return texto;
        }
    }

    /**
     * Devuelve el idioma activo actual (ej. "es" o "en")
     */
    public String getIdiomaActual(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_IDIOMA, Context.MODE_PRIVATE);
        return prefs.getString(KEY_IDIOMA, "es");
    }
}
