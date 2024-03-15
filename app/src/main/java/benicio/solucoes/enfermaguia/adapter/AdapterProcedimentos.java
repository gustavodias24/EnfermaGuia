package benicio.solucoes.enfermaguia.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import benicio.solucoes.enfermaguia.CriarProcedimentoActivity;
import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.VerDetalheProcedimentoActivity;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public class AdapterProcedimentos extends RecyclerView.Adapter<AdapterProcedimentos.MyViewHolder> {
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    List<ProcedimentoModel> lista;
    Activity a;

    boolean isAdmin;

    public AdapterProcedimentos(List<ProcedimentoModel> lista, Activity a, boolean isAdmin) {
        this.lista = lista;
        this.a = a;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_procedimento, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProcedimentoModel procedimentoModel = lista.get(position);


        if (!isAdmin) {
            holder.editarProcediemento.setVisibility(View.GONE);
            holder.excluirProcediemento.setVisibility(View.GONE);
        }

        holder.editarProcediemento.setOnClickListener(view -> {
            Intent i = new Intent(a, CriarProcedimentoActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("idProcedimento", procedimentoModel.getId());
            a.startActivity(i);
        });

        holder.excluirProcediemento.setOnClickListener(view -> {
            AlertDialog.Builder b = new AlertDialog.Builder(a);
            b.setTitle("Aviso!");
            b.setMessage("Deseja realmente realizar a operação de remoção do procedimento?");
            b.setNegativeButton("Não", null);
            b.setPositiveButton("Sim", (d, i ) ->{
                refProcedimentos.child(procedimentoModel.getId()).setValue(null).addOnCompleteListener(task ->
                        Toast.makeText(a, "Excluído com Sucesso!", Toast.LENGTH_SHORT).show());
            });
            b.create().show();
        });

        procedimentoModel.setChecado(false);
        holder.checkBoxMarcarCompartilhar.setChecked(false);

        holder.checkBoxMarcarCompartilhar.setOnClickListener(view ->
                procedimentoModel.setChecado(holder.checkBoxMarcarCompartilhar.isChecked())
        );

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
        CheckBox checkBoxMarcarCompartilhar;
        Button editarProcediemento;
        Button excluirProcediemento;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            nomeProcedimento = itemView.findViewById(R.id.text_nome_procedimento);
            btn_ir_ver_procedimento = itemView.findViewById(R.id.btn_ir_ver_procedimento);
            checkBoxMarcarCompartilhar = itemView.findViewById(R.id.checkBoxMarcarCompartilhar);
            editarProcediemento = itemView.findViewById(R.id.editarProcediemento);
            excluirProcediemento = itemView.findViewById(R.id.excluirProcediemento);
        }
    }
}
