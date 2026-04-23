package com.example.venta_de_drogas_ricas;

public class ProductoModel {
    private String id;
    private String nombre;
    private String descripcion;
    private float cantidad;
    private float precio;
    private boolean isSelected;

    public ProductoModel(String id, String nombre, String descripcion, float cantidad, float precio) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precio = precio;
        this.isSelected = false;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public float getCantidad() { return cantidad; }
    public float getPrecio() { return precio; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
