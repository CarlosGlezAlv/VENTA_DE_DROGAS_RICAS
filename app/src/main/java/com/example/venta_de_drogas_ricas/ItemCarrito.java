package com.example.venta_de_drogas_ricas;

public class ItemCarrito {
    public String id;
    public String nombre;
    public float cantidad;
    public float precio;
    public float subtotal;

    public ItemCarrito(String id, String nombre, float cantidad, float precio) {
        this.id = id;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precio = precio;
        this.subtotal = cantidad * precio;
    }
}
