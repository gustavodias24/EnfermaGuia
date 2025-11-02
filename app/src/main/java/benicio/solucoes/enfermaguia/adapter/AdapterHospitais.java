package benicio.solucoes.enfermaguia.adapter;

import static android.text.TextUtils.isEmpty;
import static benicio.solucoes.enfermaguia.HallActivity.selecaoAtiva;
import static benicio.solucoes.enfermaguia.HallActivity.showSugerirPOP;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import benicio.solucoes.enfermaguia.HallActivity;
import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.VerPopHospitalActivity;
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.LoadingUtils;
import benicio.solucoes.enfermaguia.utils.UsuarioHistoryStore;

public class AdapterHospitais extends RecyclerView.Adapter<AdapterHospitais.MyViewHolder> {

    private final DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    private final List<UsuarioModel> lista;
    private final Activity c;
    private final boolean isSelecao;
    private final SharedPreferences.Editor editor;

    // Mantém favoritos por item (id ou login)
    private final java.util.Set<String> favoritos = new java.util.HashSet<>();

    public AdapterHospitais(List<UsuarioModel> lista, Activity c) {
        this(lista, c, false, null);
    }

    public AdapterHospitais(List<UsuarioModel> lista, Activity c, boolean isSelecao, SharedPreferences.Editor editor) {
        this.lista = lista;
        this.c = c;
        this.isSelecao = isSelecao;
        this.editor = editor;

        // Carrega favoritos uma vez
        for (UsuarioModel u : UsuarioHistoryStore.getHistory(c)) {
            String k = keyOf(u);
            if (!k.isEmpty()) favoritos.add(k);
        }
    }

    private String keyOf(UsuarioModel u) {
        // Prioriza id; se não tiver, usa login
        if (u == null) return "";
        if (!isEmpty(u.getId())) return "ID:" + u.getId();
        if (!isEmpty(u.getLogin())) return "LOGIN:" + u.getLogin();
        return "";
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_hospital_exibir, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        UsuarioModel hospital = lista.get(position);

        if (isSelecao) {
            holder.rmButton.setVisibility(View.GONE);
            holder.infos.setText(hospital.getNome());

            // Define ícone conforme favorito por item
            boolean isFav = favoritos.contains(keyOf(hospital));
            holder.favoritarHospital.setImageResource(isFav ? R.drawable.estrela2 : R.drawable.estrela1);

            holder.favoritarHospital.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                UsuarioModel h = lista.get(pos);
                String k = keyOf(h);
                boolean fav = favoritos.contains(k);

                if (fav) {
                    favoritos.remove(k);
                    holder.favoritarHospital.setImageResource(R.drawable.estrela1);
                    UsuarioHistoryStore.removeFromHistory(c, h);
                    LoadingUtils.showLoading2(c, "Atenção", h.getNome() + " removido dos favoritos");
                } else {
                    favoritos.add(k);
                    holder.favoritarHospital.setImageResource(R.drawable.estrela2);
                    UsuarioHistoryStore.saveToHistory(c, h);
                    LoadingUtils.showLoading2(c, "Atenção", h.getNome() + " adicionado aos favoritos");
                }
            });

            holder.itemView.setOnClickListener(view -> {
                editor.putString("idHospitalSelecionado", hospital.getId()).apply();
                editor.putString("nomeHospitalSelecionado", hospital.getNome()).apply();
                if (selecaoAtiva) {
                    HallActivity.showSugerirPOP(c);
                } else {
                    Toast.makeText(c, "Hospital " + hospital.getNome() + " Selecionado", Toast.LENGTH_SHORT).show();
                    c.startActivity(new Intent(c, VerPopHospitalActivity.class));
                }
            });

        } else {
            holder.infos.setText(hospital.toString());
            holder.rmButton.setVisibility(View.VISIBLE);
            holder.rmButton.setOnClickListener(view -> {
                refUsuarios.child(hospital.getId()).setValue(null);
                Toast.makeText(c, "Removido!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView infos;
        Button rmButton;
        ImageButton favoritarHospital;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            infos = itemView.findViewById(R.id.dados_user_hospital);
            favoritarHospital = itemView.findViewById(R.id.favoritarHospital);
            rmButton = itemView.findViewById(R.id.remover_user_hospital);
        }
    }
}
