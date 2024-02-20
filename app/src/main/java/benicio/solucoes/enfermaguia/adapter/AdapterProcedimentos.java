package benicio.solucoes.enfermaguia.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.VerDetalheProcedimentoActivity;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public class AdapterProcedimentos extends RecyclerView.Adapter<AdapterProcedimentos.MyViewHolder> {
    List<ProcedimentoModel> lista;
    Activity a;

    public AdapterProcedimentos(List<ProcedimentoModel> lista, Activity a) {
        this.lista = lista;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_procedimento, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProcedimentoModel procedimentoModel = lista.get(position);

        holder.nomeProcedimento.setText(procedimentoModel.getNomeProcedimento());
        holder.itemView.getRootView().setClickable(false);
        holder.btn_ir_ver_procedimento.setOnClickListener(view -> {
            Intent i = new Intent(a, VerDetalheProcedimentoActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("idProcedimento", procedimentoModel.getId());
            a.startActivity(i);
        });

    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView nomeProcedimento;
        ImageButton btn_ir_ver_procedimento;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            nomeProcedimento = itemView.findViewById(R.id.text_nome_procedimento);
            btn_ir_ver_procedimento = itemView.findViewById(R.id.btn_ir_ver_procedimento);
        }
    }
}
