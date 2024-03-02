package benicio.solucoes.enfermaguia.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public class AdapterMetricas extends RecyclerView.Adapter<AdapterMetricas.MyViewHolder> {

    List<ProcedimentoModel> lista;
    Activity c;

    public AdapterMetricas(List<ProcedimentoModel> lista, Activity c) {
        this.lista = lista;
        this.c = c;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_sugestao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.metricasInfo.setText(lista.get(position).returnMetricas());
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView metricasInfo;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            metricasInfo = itemView.findViewById(R.id.text_info_sugestao);
        }
    }
}
