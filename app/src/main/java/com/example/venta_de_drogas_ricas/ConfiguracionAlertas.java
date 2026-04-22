package com.example.venta_de_drogas_ricas;

public class ConfiguracionAlertas {
    public Stock stock;
    public Alertas alertas;

    public ConfiguracionAlertas() {
        stock = new Stock();
        alertas = new Alertas();
    }

    public static class Stock {
        public int minimo_alerta = 3;
        public int critico = 1;
        public boolean bloquear_sin_stock = true;
    }

    public static class Alertas {
        public boolean mostrar_popup = true;
        public boolean usar_color = true;
        public String mensaje_bajo = "Stock bajo quedan pocas unidades del producto";
        public String mensaje_critico = "Stock crítico queda una sola unidad del producto";
        public String mensaje_sin_stock = "Producto agotado sin stock";
    }
}
