package com.example.venta_de_drogas_ricas;

public class PdfConfig {
    public Apariencia apariencia;
    public Negocio negocio;

    public static class Apariencia {
        public String color_enfasis;
        public String tamano_texto;
    }

    public static class Negocio {
        public String nombre_tienda;
    }
}
