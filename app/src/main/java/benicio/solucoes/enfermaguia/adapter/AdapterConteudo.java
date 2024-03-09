package benicio.solucoes.enfermaguia.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.model.ConteudoModel;
import benicio.solucoes.enfermaguia.utils.ItemMoveCallback;

public class AdapterConteudo extends RecyclerView.Adapter<AdapterConteudo.MyViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {

    List<ConteudoModel> lista;
    Activity c;

    public AdapterConteudo(List<ConteudoModel> lista, Activity c) {
        this.lista = lista;
        this.c = c;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_conteudo, parent, false));
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ConteudoModel conteudoModel = lista.get(position);
        holder.titulo.setText(conteudoModel.getTitulo());
        holder.info.setText(conteudoModel.getInfo());

        holder.remover_conteudo.setOnClickListener( view -> {
            lista.remove(position);
            this.notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        ConteudoModel item = lista.remove(fromPosition);
        lista.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, info;
        Button remover_conteudo;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo  = itemView.findViewById(R.id.text_conteudo_titulo);
            info  = itemView.findViewById(R.id.text_conteudo_info);
            remover_conteudo  = itemView.findViewById(R.id.remover_conteudo);
        }
    }
}
