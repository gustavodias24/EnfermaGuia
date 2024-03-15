package benicio.solucoes.enfermaguia.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import benicio.solucoes.enfermaguia.HallActivity;
import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.model.UsuarioModel;

public class AdapterHospitais extends RecyclerView.Adapter<AdapterHospitais.MyViewHolder> {

    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    List<UsuarioModel> lista;

    Activity c;

    boolean isSelecao = false;
    SharedPreferences.Editor editor;

    public AdapterHospitais(List<UsuarioModel> lista, Activity c) {
        this.lista = lista;
        this.c = c;
    }

    public AdapterHospitais(List<UsuarioModel> lista, Activity c, boolean isSelecao, SharedPreferences.Editor editor) {
        this.lista = lista;
        this.c = c;
        this.isSelecao = isSelecao;
        this.editor = editor;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_hospital_exibir, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        UsuarioModel hospital = lista.get(position);
        if ( isSelecao ){
            holder.rmButton.setVisibility(View.GONE);
            holder.infos.setText(hospital.getNome());
            holder.itemView.getRootView().setOnClickListener(view -> {
                editor.putString("idHospitalSelecionado", hospital.getId()).apply();
                HallActivity.dialogSelecionaHospital.dismiss();
                Toast.makeText(c, "Hospital " + hospital.getNome() + " Selecionado", Toast.LENGTH_SHORT).show();
                HallActivity.setNomeHospitalAtual();
                HallActivity.buscarProcedimentos();
            });
        }else{
            holder.infos.setText(hospital.toString());
            holder.rmButton.setOnClickListener( view -> {
                refUsuarios.child(hospital.getId()).setValue(null);
                Toast.makeText(c, "Removido!", Toast.LENGTH_SHORT).show();
            });
        }

        
        
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView infos;
        Button rmButton;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            infos = itemView.findViewById(R.id.dados_user_hospital);
            rmButton = itemView.findViewById(R.id.remover_user_hospital);
            
        }
    }
}
