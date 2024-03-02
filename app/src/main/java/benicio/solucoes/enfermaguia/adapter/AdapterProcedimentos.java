package benicio.solucoes.enfermaguia.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.VerDetalheProcedimentoActivity;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public class AdapterProcedimentos extends RecyclerView.Adapter<AdapterProcedimentos.MyViewHolder> {
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
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
            procedimentoModel.setAcessos(procedimentoModel.getAcessos() + 1);

            refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent i = new Intent(a, VerDetalheProcedimentoActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra("idProcedimento", procedimentoModel.getId());
                    a.startActivity(i);
                } else {
                    Toast.makeText(a, "Tente novamente!", Toast.LENGTH_SHORT).show();
                }
            });
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
