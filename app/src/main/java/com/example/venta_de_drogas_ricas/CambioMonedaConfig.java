package com.example.venta_de_drogas_ricas;

import java.util.ArrayList;
import java.util.List;

public class CambioMonedaConfig {
    public String accion = "convertir_moneda";
    public List<String> monedas_soportadas = new ArrayList<>();
    public List<TipoCambio> tipos_de_cambio = new ArrayList<>();

    public static class TipoCambio {
        public String origen;
        public String destino;
        public double tasa;

        public TipoCambio() {
        }

        public TipoCambio(String origen, String destino, double tasa) {
            this.origen = origen;
            this.destino = destino;
            this.tasa = tasa;
        }
    }
}
