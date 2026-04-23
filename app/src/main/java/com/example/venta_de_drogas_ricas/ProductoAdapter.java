package com.example.venta_de_drogas_ricas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ProductoAdapter
    extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>
{

    private List<ProductoModel> productoList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ProductoModel producto);
        void onEditClick(ProductoModel producto);
        void onDeleteClick(ProductoModel producto);
    }

    public ProductoAdapter(
        List<ProductoModel> productoList,
        OnItemClickListener listener
    ) {
        this.productoList = productoList;
        this.listener = listener;
    }

    public void setProductos(List<ProductoModel> productos) {
        this.productoList = productos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(
        @NonNull ViewGroup parent,
        int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
            R.layout.item_producto,
            parent,
            false
        );
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
        @NonNull ProductoViewHolder holder,
        int position
    ) {
        ProductoModel producto = productoList.get(position);

        holder.tvItemCodigo.setText(
            TraductorManager.getInstance(
                holder.itemView.getContext()
            ).getString("formato_codigo", producto.getId())
        );
        holder.tvItemNombre.setText(producto.getNombre());
        holder.tvItemPrecio.setText(
            TraductorManager.getInstance(
                holder.itemView.getContext()
            ).getString("formato_precio", String.valueOf(producto.getPrecio()))
        );
        holder.tvItemStock.setText(
            TraductorManager.getInstance(
                holder.itemView.getContext()
            ).getString("formato_stock", String.valueOf(producto.getCantidad()))
        );

        if (producto.isSelected()) {
            holder.cardProducto.setStrokeWidth(4);
            holder.cardProducto.setStrokeColor(
                holder.itemView
                    .getContext()
                    .getResources()
                    .getColor(android.R.color.holo_blue_light)
            );
            holder.layoutAcciones.setVisibility(View.VISIBLE);
        } else {
            holder.cardProducto.setStrokeWidth(0);
            holder.layoutAcciones.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            for (ProductoModel p : productoList) {
                p.setSelected(false);
            }
            producto.setSelected(true);
            notifyDataSetChanged();
            listener.onItemClick(producto);
        });

        holder.btnItemEditar.setOnClickListener(v ->
            listener.onEditClick(producto)
        );
        holder.btnItemEliminar.setOnClickListener(v ->
            listener.onDeleteClick(producto)
        );
    }

    @Override
    public int getItemCount() {
        return productoList.size();
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardProducto;
        TextView tvItemCodigo, tvItemNombre, tvItemPrecio, tvItemStock;
        LinearLayout layoutAcciones;
        ImageButton btnItemEditar, btnItemEliminar;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProducto = itemView.findViewById(R.id.cardProducto);
            tvItemCodigo = itemView.findViewById(R.id.tvItemCodigo);
            tvItemNombre = itemView.findViewById(R.id.tvItemNombre);
            tvItemPrecio = itemView.findViewById(R.id.tvItemPrecio);
            tvItemStock = itemView.findViewById(R.id.tvItemStock);
            layoutAcciones = itemView.findViewById(R.id.layoutAcciones);
            btnItemEditar = itemView.findViewById(R.id.btnItemEditar);
            btnItemEliminar = itemView.findViewById(R.id.btnItemEliminar);
        }
    }
}
