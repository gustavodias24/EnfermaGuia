package benicio.solucoes.enfermaguia.adapter;

import android.app.Activity;
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

import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.model.UsuarioModel;

public class AdapterHospitais extends RecyclerView.Adapter<AdapterHospitais.MyViewHolder> {

    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    List<UsuarioModel> lista;

    Activity c;

    public AdapterHospitais(List<UsuarioModel> lista, Activity c) {
        this.lista = lista;
        this.c = c;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_hospital_exibir, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        UsuarioModel hospital = lista.get(position);
        
        holder.infos.setText(hospital.toString());
        holder.rmButton.setOnClickListener( view -> {
            refUsuarios.child(hospital.getId()).setValue(null);
            Toast.makeText(c, "Removido!", Toast.LENGTH_SHORT).show();
        });
        
        
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
