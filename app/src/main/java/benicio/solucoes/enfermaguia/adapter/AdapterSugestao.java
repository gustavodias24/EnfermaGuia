package benicio.solucoes.enfermaguia.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.databinding.LayoutCriarSugestaoBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutEnviarFeedbackBinding;
import benicio.solucoes.enfermaguia.model.FeedbackModel;
import benicio.solucoes.enfermaguia.model.SugestaoModel;

public class AdapterSugestao extends RecyclerView.Adapter<AdapterSugestao.MyViewHolder> {

    Activity a;
    List<SugestaoModel> listaSugestao;

    private DatabaseReference refFeedbacks = FirebaseDatabase.getInstance().getReference().child("feedbacks");
    private Dialog dialogFeedback;

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
        holder.sugestaoInfo.setText(Html.fromHtml(listaSugestao.get(position).toString()));

        holder.selecionarDeletar.setOnClickListener(v -> {
            listaSugestao.get(position).setSelecionadoDeletar(
                    holder.selecionarDeletar.isChecked()
            );
        });

        holder.enviarFeedbackUsuario.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(a);
            LayoutEnviarFeedbackBinding criarFeedback = LayoutEnviarFeedbackBinding.inflate(a.getLayoutInflater());

            criarFeedback.cadastro.setOnClickListener(v2 -> {
                String feedback = criarFeedback.sugestaoField.getEditText().getText().toString();

                if (!feedback.isEmpty()) {

                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    String dataAtual = simpleDateFormat.format(new Date());
                    String idFeedback = UUID.randomUUID().toString();

                    FeedbackModel novoFeedBack = new FeedbackModel(
                            dataAtual,
                            feedback,
                            idFeedback,
                            listaSugestao.get(position).getIdUsuario()
                    );

                    refFeedbacks.child(idFeedback).setValue(novoFeedBack).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            dialogFeedback.dismiss();
                            Toast.makeText(a, "Feedback Enviado com Sucesso!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(a, "Escreva Algo Antes de Enviar!", Toast.LENGTH_SHORT).show();
                }
            });

            b.setView(criarFeedback.getRoot());
            dialogFeedback = b.create();
            dialogFeedback.show();
        });
    }

    @Override
    public int getItemCount() {
        return listaSugestao.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
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
