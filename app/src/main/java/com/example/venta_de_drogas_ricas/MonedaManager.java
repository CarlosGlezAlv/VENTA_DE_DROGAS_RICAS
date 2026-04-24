package com.example.venta_de_drogas_ricas;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MonedaManager {

    private static final String TAG = "MonedaManager";
    private static final String ASSET_FILE = "cambio_moneda.json";
    private static final String DEFAULT_MONEDA = "MXN";

    private static MonedaManager instance;

    private final Context appContext;
    private final Gson gson;

    // Cache de configuración de moneda
    private CambioMonedaConfig config;
    private final Map<String, BigDecimal> mapaTasas = new HashMap<>();

    private MonedaManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.gson = new Gson();
        cargarConfigDesdeAssets();
    }

    public static synchronized MonedaManager getInstance(Context context) {
        if (instance == null) {
            instance = new MonedaManager(context);
        }
        return instance;
    }

    // =========================
    // API pública principal
    // =========================

    public synchronized boolean recargarConfig() {
        return cargarConfigDesdeAssets();
    }

    public synchronized List<String> getMonedasSoportadas() {
        if (config == null || config.monedasSoportadas == null) {
            List<String> fallback = new ArrayList<>();
            fallback.add(DEFAULT_MONEDA);
            return fallback;
        }
        return new ArrayList<>(config.monedasSoportadas);
    }

    public synchronized boolean esMonedaSoportada(String moneda) {
        String m = normalizarMoneda(moneda);
        if (m == null) return false;

        if (config == null || config.monedasSoportadas == null) {
            return DEFAULT_MONEDA.equals(m);
        }
        for (String s : config.monedasSoportadas) {
            if (m.equals(normalizarMoneda(s))) return true;
        }
        return false;
    }

    public synchronized double convertir(double monto, String origen, String destino) {
        BigDecimal convertido = convertir(
            BigDecimal.valueOf(monto),
            origen,
            destino
        );
        return convertido.doubleValue();
    }

    public synchronized BigDecimal convertir(BigDecimal monto, String origen, String destino) {
        if (monto == null) return BigDecimal.ZERO;

        String o = normalizarMoneda(origen);
        String d = normalizarMoneda(destino);

        if (o == null || d == null) {
            Log.w(TAG, "Moneda origen/destino inválida. Se retorna monto sin convertir.");
            return monto;
        }

        if (o.equals(d)) {
            return monto;
        }

        BigDecimal tasa = resolverTasa(o, d);
        if (tasa == null) {
            Log.w(TAG, "No existe tasa para par " + o + " -> " + d + ". Fallback sin conversión.");
            return monto;
        }

        // Cálculo con mayor precisión intermedia, sin forzar escala final aquí
        return monto.multiply(tasa);
    }

    public synchronized BigDecimal redondear(BigDecimal valor, int decimales) {
        if (valor == null) return BigDecimal.ZERO;
        return valor.setScale(Math.max(decimales, 0), RoundingMode.HALF_UP);
    }

    public synchronized String formatear(double monto, String moneda) {
        return formatear(BigDecimal.valueOf(monto), moneda, 2, true);
    }

    public synchronized String formatear(BigDecimal monto, String moneda) {
        return formatear(monto, moneda, 2, true);
    }

    /**
     * Formatea como: "MXN 123.45" o "USD 7.02"
     */
    public synchronized String formatear(
        BigDecimal monto,
        String moneda,
        int decimales,
        boolean incluirCodigo
    ) {
        String m = normalizarMoneda(moneda);
        if (m == null) m = DEFAULT_MONEDA;

        BigDecimal redondeado = redondear(
            monto == null ? BigDecimal.ZERO : monto,
            decimales
        );

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat();
        format.setDecimalFormatSymbols(symbols);
        format.setGroupingUsed(true);
        format.setMaximumFractionDigits(Math.max(decimales, 0));
        format.setMinimumFractionDigits(Math.max(decimales, 0));
        format.setRoundingMode(RoundingMode.HALF_UP);

        String numero = format.format(redondeado);
        if (incluirCodigo) {
            return m + " " + numero;
        }
        return numero;
    }

    /**
     * Convierte de origen a destino y regresa cadena ya formateada en destino.
     */
    public synchronized String convertirYFormatear(
        BigDecimal monto,
        String origen,
        String destino,
        int decimales
    ) {
        BigDecimal conv = convertir(monto, origen, destino);
        return formatear(conv, destino, decimales, true);
    }

    // =========================
    // Resolución de tasas
    // =========================

    private BigDecimal resolverTasa(String origen, String destino) {
        String keyDirecta = key(origen, destino);
        BigDecimal directa = mapaTasas.get(keyDirecta);
        if (directa != null) return directa;

        // Fallback a inversa si existe
        String keyInversa = key(destino, origen);
        BigDecimal inversa = mapaTasas.get(keyInversa);
        if (inversa != null && inversa.compareTo(BigDecimal.ZERO) != 0) {
            try {
                return BigDecimal.ONE.divide(inversa, 12, RoundingMode.HALF_UP);
            } catch (ArithmeticException e) {
                Log.w(TAG, "Error al calcular tasa inversa para " + origen + " -> " + destino, e);
            }
        }
        return null;
    }

    private String key(String origen, String destino) {
        return origen + "->" + destino;
    }

    // =========================
    // Carga de assets
    // =========================

    private synchronized boolean cargarConfigDesdeAssets() {
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = appContext.getAssets().open(ASSET_FILE);
            reader = new BufferedReader(new InputStreamReader(is));
            CambioMonedaConfig cfg = gson.fromJson(reader, CambioMonedaConfig.class);

            if (cfg == null) {
                Log.w(TAG, "Archivo de cambio de moneda vacío o inválido. Usando fallback.");
                aplicarFallback();
                return false;
            }

            if (cfg.monedasSoportadas == null) {
                cfg.monedasSoportadas = new ArrayList<>();
            }

            if (cfg.monedasSoportadas.isEmpty()) {
                cfg.monedasSoportadas.add(DEFAULT_MONEDA);
            }

            this.config = cfg;
            reconstruirMapaTasas();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "No se pudo cargar " + ASSET_FILE + ". Se aplica fallback.", e);
            aplicarFallback();
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) { }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) { }
            }
        }
    }

    private void reconstruirMapaTasas() {
        mapaTasas.clear();

        if (config == null || config.tiposDeCambio == null) {
            return;
        }

        for (TipoCambio t : config.tiposDeCambio) {
            if (t == null) continue;

            String o = normalizarMoneda(t.origen);
            String d = normalizarMoneda(t.destino);

            if (o == null || d == null) continue;
            if (t.tasa <= 0) continue;

            mapaTasas.put(key(o, d), BigDecimal.valueOf(t.tasa));
        }
    }

    private void aplicarFallback() {
        CambioMonedaConfig fallback = new CambioMonedaConfig();
        fallback.accion = "convertir_moneda";
        fallback.monedasSoportadas = new ArrayList<>();
        fallback.monedasSoportadas.add(DEFAULT_MONEDA);
        fallback.tiposDeCambio = new ArrayList<>();
        this.config = fallback;
        mapaTasas.clear();
    }

    private String normalizarMoneda(String moneda) {
        if (moneda == null) return null;
        String m = moneda.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty()) return null;
        return m;
    }

    // =========================
    // Modelos JSON
    // =========================

    public static class CambioMonedaConfig {
        @SerializedName("accion")
        public String accion;

        @SerializedName("monedas_soportadas")
        public List<String> monedasSoportadas;

        @SerializedName("tipos_de_cambio")
        public List<TipoCambio> tiposDeCambio;
    }

    public static class TipoCambio {
        @SerializedName("origen")
        public String origen;

        @SerializedName("destino")
        public String destino;

        @SerializedName("tasa")
        public double tasa;
    }
}
