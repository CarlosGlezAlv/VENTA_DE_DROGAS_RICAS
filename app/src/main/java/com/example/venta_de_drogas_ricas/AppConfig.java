package com.example.venta_de_drogas_ricas;

public class AppConfig {

    public Apariencia apariencia;
    public Seguridad seguridad;
    public Negocio negocio;

    public AppConfig() {
        apariencia = new Apariencia();
        seguridad = new Seguridad();
        negocio = new Negocio();
    }

    public static class Apariencia {

        public boolean tema_oscuro = true;
        public String color_enfasis = "#4CAF50";
        public String tamano_texto = "mediano";
    }

    public static class Seguridad {

        public boolean pin_activo = false;
        public String pin_codigo = "0000";
        public boolean modo_administrador = true;
    }

    public static class Negocio {

        public String nombre_tienda = "Venta de Drogas Ricas";
        public String moneda_base = "MXN";
        public String moneda_visual = "MXN";
        public boolean mostrar_codigo_moneda = true;
        public int decimales_moneda = 2;
    }
}
