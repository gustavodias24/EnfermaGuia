package benicio.solucoes.enfermaguia.adapter;

import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.model.FeedbackModel;

public class AdapterFeedbacks extends RecyclerView.Adapter<AdapterFeedbacks.MyViewHolder> {

    Activity a;
    List<FeedbackModel> list;

    public AdapterFeedbacks(Activity a, List<FeedbackModel> list) {
        this.a = a;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_sugestao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.enviarFeedbackUsuario.setVisibility(View.GONE);
        holder.selecionarDeletar.setVisibility(View.GONE);

        holder.sugestaoInfo.setText(
                Html.fromHtml(list.get(position).toString())
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView sugestaoInfo;
        CheckBox selecionarDeletar;

        ImageButton enviarFeedbackUsuario;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            sugestaoInfo = itemView.findViewById(R.id.text_info_sugestao);
            selecionarDeletar = itemView.findViewById(R.id.selecionarDeletar);
            enviarFeedbackUsuario = itemView.findViewById(R.id.enviarFeedbackUsuario);
        }
    }
}
