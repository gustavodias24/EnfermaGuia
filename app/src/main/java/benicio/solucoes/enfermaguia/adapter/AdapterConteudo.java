package benicio.solucoes.enfermaguia.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.ImageDecoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.databinding.LayoutEditarTopicoBinding;
import benicio.solucoes.enfermaguia.model.ConteudoModel;
import benicio.solucoes.enfermaguia.utils.ItemMoveCallback;

public class AdapterConteudo extends RecyclerView.Adapter<AdapterConteudo.MyViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {

    List<ConteudoModel> lista;
    Activity c;
    Dialog dialogEdt = null;
    RecyclerView r;


    public AdapterConteudo(List<ConteudoModel> lista, Activity c, RecyclerView r) {
        this.lista = lista;
        this.c = c;
        this.r = r;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_conteudo, parent, false));
    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ConteudoModel conteudoModel = lista.get(position);
        holder.titulo.setText(conteudoModel.getTitulo());
        holder.info.setText(conteudoModel.getInfo());

        holder.itemView.getRootView().setClickable(false);

        holder.editar_conteudo_t.setOnClickListener(view -> {
            AlertDialog.Builder b = new AlertDialog.Builder(c);
            b.setCancelable(false);
            LayoutEditarTopicoBinding editarTopicoBinding = LayoutEditarTopicoBinding.inflate(c.getLayoutInflater());

            editarTopicoBinding.textEditando.setText("Edite " + conteudoModel.getTitulo());

            editarTopicoBinding.edtTitulo.setText(conteudoModel.getTitulo());
            editarTopicoBinding.edtTextoConteudo.setText(conteudoModel.getInfo());

            editarTopicoBinding.pronto2.setOnClickListener(pronto2View -> {

                funcaoPronto(conteudoModel, editarTopicoBinding, position);
            });
            editarTopicoBinding.pronto.setOnClickListener(prontoView -> {
                funcaoPronto(conteudoModel, editarTopicoBinding, position);
            });
            b.setView(editarTopicoBinding.getRoot());
            dialogEdt = b.create();
            dialogEdt.show();
        });

        holder.remover_conteudo.setOnClickListener(view -> {
            lista.remove(position);
            this.notifyDataSetChanged();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void funcaoPronto(ConteudoModel conteudoModel, LayoutEditarTopicoBinding editarTopicoBinding, int currentPosition) {
        conteudoModel.setTitulo(editarTopicoBinding.edtTitulo.getText().toString());
        conteudoModel.setInfo(editarTopicoBinding.edtTextoConteudo.getText().toString());

        this.notifyDataSetChanged();
        r.getLayoutManager().scrollToPosition(currentPosition);

        dialogEdt.dismiss();
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
        Button remover_conteudo, editar_conteudo_t;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.text_conteudo_titulo);
            info = itemView.findViewById(R.id.text_conteudo_info);
            remover_conteudo = itemView.findViewById(R.id.remover_conteudo);
            editar_conteudo_t = itemView.findViewById(R.id.editar_conteudo_t);
        }
    }
}
