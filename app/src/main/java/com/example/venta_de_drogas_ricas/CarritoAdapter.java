package com.example.venta_de_drogas_ricas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CarritoAdapter
    extends RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>
{

    private List<ItemCarrito> itemsCarrito;
    private CarritoListener listener;

    public interface CarritoListener {
        void onSumarCantidad(ItemCarrito item, int position);
        void onRestarCantidad(ItemCarrito item, int position);
        void onRemoverItem(ItemCarrito item, int position);
    }

    public CarritoAdapter(
        List<ItemCarrito> itemsCarrito,
        CarritoListener listener
    ) {
        this.itemsCarrito = itemsCarrito;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarritoViewHolder onCreateViewHolder(
        @NonNull ViewGroup parent,
        int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
            R.layout.item_carrito,
            parent,
            false
        );
        return new CarritoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
        @NonNull CarritoViewHolder holder,
        int position
    ) {
        ItemCarrito item = itemsCarrito.get(position);

        holder.tvCarritoNombre.setText(item.nombre);
        holder.tvCarritoPrecioUnitario.setText(
            TraductorManager.getInstance(
                holder.itemView.getContext()
            ).getString("formato_precio_cu", String.valueOf(item.precio))
        );
        holder.tvCarritoCantidad.setText(String.valueOf(item.cantidad));
        holder.tvCarritoSubtotal.setText(
            TraductorManager.getInstance(
                holder.itemView.getContext()
            ).getString("formato_precio", String.valueOf(item.subtotal))
        );

        holder.btnSumar.setOnClickListener(v ->
            listener.onSumarCantidad(item, position)
        );
        holder.btnRestar.setOnClickListener(v ->
            listener.onRestarCantidad(item, position)
        );
        holder.btnRemoverItem.setOnClickListener(v ->
            listener.onRemoverItem(item, position)
        );
    }

    @Override
    public int getItemCount() {
        return itemsCarrito.size();
    }

    public static class CarritoViewHolder extends RecyclerView.ViewHolder {

        TextView tvCarritoNombre, tvCarritoPrecioUnitario, tvCarritoCantidad, tvCarritoSubtotal;
        ImageButton btnSumar, btnRestar, btnRemoverItem;

        public CarritoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarritoNombre = itemView.findViewById(R.id.tvCarritoNombre);
            tvCarritoPrecioUnitario = itemView.findViewById(
                R.id.tvCarritoPrecioUnitario
            );
            tvCarritoCantidad = itemView.findViewById(R.id.tvCarritoCantidad);
            tvCarritoSubtotal = itemView.findViewById(R.id.tvCarritoSubtotal);
            btnSumar = itemView.findViewById(R.id.btnSumar);
            btnRestar = itemView.findViewById(R.id.btnRestar);
            btnRemoverItem = itemView.findViewById(R.id.btnRemoverItem);
        }
    }
}
