package com.example.venta_de_drogas_ricas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {

    private List<VentasManager.VentaRecord> listaVentas;
    private MonedaManager monedaManager;

    public HistorialAdapter(List<VentasManager.VentaRecord> listaVentas, MonedaManager monedaManager) {
        this.listaVentas = listaVentas;
        this.monedaManager = monedaManager;
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false);
        return new HistorialViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        VentasManager.VentaRecord venta = listaVentas.get(position);
        
        holder.tvFolio.setText(venta.folio != null ? venta.folio : "V-?");
        holder.tvFecha.setText(venta.fecha);
        
        StringBuilder productosBuilder = new StringBuilder();
        if (venta.productos != null) {
            for (VentasManager.ProductoVendido p : venta.productos) {
                productosBuilder.append(String.format("%sx %s ($%.2f)\n", 
                    String.valueOf((int)p.cantidad), p.nombre, p.subtotal));
            }
        }
        holder.tvProductosLista.setText(productosBuilder.toString().trim());

        String moneda = venta.moneda_visual != null ? venta.moneda_visual : "MXN";
        String totalFormateado = monedaManager.formatear(
            java.math.BigDecimal.valueOf(venta.total_visual), 
            moneda
        );
        holder.tvTotalVisual.setText(totalFormateado);
    }

    @Override
    public int getItemCount() {
        return listaVentas != null ? listaVentas.size() : 0;
    }

    public static class HistorialViewHolder extends RecyclerView.ViewHolder {
        TextView tvFolio, tvFecha, tvProductosLista, tvTotalVisual;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFolio = itemView.findViewById(R.id.tvFolio);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvProductosLista = itemView.findViewById(R.id.tvProductosLista);
            tvTotalVisual = itemView.findViewById(R.id.tvTotalVisual);
        }
    }
}
