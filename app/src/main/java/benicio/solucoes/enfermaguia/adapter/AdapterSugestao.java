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
import benicio.solucoes.enfermaguia.model.SugestaoModel;

public class AdapterSugestao extends RecyclerView.Adapter<AdapterSugestao.MyViewHolder> {

    Activity a;
    List<SugestaoModel> listaSugestao;

    public AdapterSugestao(Activity a, List<SugestaoModel> listaSugestao) {
        this.a = a;
        this.listaSugestao = listaSugestao;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_sugestao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.sugestaoInfo.setText(listaSugestao.get(position).toString());
    }

    @Override
    public int getItemCount() {
        return listaSugestao.size();
    }

    public static class MyViewHolder extends  RecyclerView.ViewHolder {
        TextView sugestaoInfo;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            sugestaoInfo = itemView.findViewById(R.id.text_info_sugestao);
        }
    }
}
