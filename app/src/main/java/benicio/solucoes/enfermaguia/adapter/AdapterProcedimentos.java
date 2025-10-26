package benicio.solucoes.enfermaguia.adapter;


import static benicio.solucoes.enfermaguia.VerPopHospitalActivity.buscarProcedimentos;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

// ===== ADIÇÃO: imports para favoritos =====
import java.util.HashSet;
import java.util.Set;

import benicio.solucoes.enfermaguia.utils.LoadingUtils;
import benicio.solucoes.enfermaguia.utils.ProcedimentoFavoritosStore;

import benicio.solucoes.enfermaguia.CriarProcedimentoActivity;
import benicio.solucoes.enfermaguia.R;
import benicio.solucoes.enfermaguia.VerDetalheProcedimentoActivity;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.ProcedimentoHistoryStore;

public class AdapterProcedimentos extends RecyclerView.Adapter<AdapterProcedimentos.MyViewHolder> {
    private static final Logger log = LoggerFactory.getLogger(AdapterProcedimentos.class);
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private List<ProcedimentoModel> itemList;

    private Dialog dialog_duplicar;

    Activity a;

    boolean isAdmin;


    boolean onlyView = false;

    // ===== ADIÇÃO: estado por item (chaves de favoritos) =====
    private final Set<String> favoritosKeys = new HashSet<>();

    public AdapterProcedimentos(List<ProcedimentoModel> lista, Activity a, boolean isAdmin) {
        this.a = a;
        this.isAdmin = isAdmin;
        this.itemList = lista;

        // ===== ADIÇÃO: carregar favoritos persistidos uma vez =====
        for (ProcedimentoModel p : ProcedimentoFavoritosStore.getFavoritos(a)) {
            String k = keyOf(p);
            if (!k.isEmpty()) favoritosKeys.add(k);
        }
    }

    public AdapterProcedimentos(List<ProcedimentoModel> lista, Activity a, boolean isAdmin, boolean onlyView) {
        this.a = a;
        this.isAdmin = isAdmin;
        this.itemList = lista;
        this.onlyView = onlyView;

        // ===== ADIÇÃO: carregar favoritos persistidos uma vez =====
        for (ProcedimentoModel p : ProcedimentoFavoritosStore.getFavoritos(a)) {
            String k = keyOf(p);
            if (!k.isEmpty()) favoritosKeys.add(k);
        }
    }

    // ===== ADIÇÃO: chave lógica única do procedimento (prioriza id + idHospital; fallback nome) =====
    private String keyOf(ProcedimentoModel p) {
        if (p == null) return "";
        String id = (p.getId() == null) ? "" : p.getId();
        String hosp = (p.getIdHospital() == null) ? "" : p.getIdHospital();
        if (!id.isEmpty()) {
            // inclui hospital para evitar colisões do mesmo id em hospitais diferentes
            return "ID:" + id + "|H:" + hosp;
        }
        String nome = (p.getNomeProcedimento() == null) ? "" : p.getNomeProcedimento();
        if (!nome.isEmpty()) return "NOME:" + nome;
        return "";
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_procedimento, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProcedimentoModel procedimentoModel = itemList.get(position);

        if ( isAdmin ){
            holder.favoritarProcecimento.setVisibility(View.GONE);
        }

        if ( onlyView ){
            holder.checkBoxMarcarCompartilhar.setVisibility(View.INVISIBLE);
        }

        holder.btn_ir_duplicar_procedimento.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(a);
            b.setTitle("Atenção!");
            b.setMessage("Deseja realmente duplicar esse procedimento?");
            b.setCancelable(false);
            b.setNegativeButton("Não", (d, i) -> dialog_duplicar.dismiss());
            b.setPositiveButton("Sim", (d,i) ->{
                Toast.makeText(a, "Duplicando...", Toast.LENGTH_SHORT).show();
                String novo_id = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
                ProcedimentoModel novoProcedimentoCopia = procedimentoModel;
                novoProcedimentoCopia.setId(novo_id);
                refProcedimentos.child(novo_id).setValue(
                        novoProcedimentoCopia
                );
            });
            dialog_duplicar = b.create();
            dialog_duplicar.show();
        });


        if (!isAdmin) {
            holder.editarProcediemento.setVisibility(View.GONE);
            holder.btn_ir_duplicar_procedimento.setVisibility(View.GONE);
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
            b.setPositiveButton("Sim", (d, i) -> {
                refProcedimentos.child(procedimentoModel.getId()).setValue(null).addOnCompleteListener(task -> {
                    Toast.makeText(a, "Excluído com Sucesso!", Toast.LENGTH_SHORT).show();
                    // ===== ADIÇÃO opcional: se estava favoritado, remove das chaves e do store =====
                    String k = keyOf(procedimentoModel);
                    if (favoritosKeys.contains(k)) {
                        favoritosKeys.remove(k);
                        ProcedimentoFavoritosStore.removeFavorito(a, procedimentoModel);
                        notifyItemChanged(getBindingAdapterPositionSafe(holder));
                    }
                });
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

            ProcedimentoHistoryStore.saveToHistory(a, procedimentoModel);

            procedimentoModel.setAcessos(procedimentoModel.getAcessos() + 1);

            refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent i = new Intent(a, VerDetalheProcedimentoActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra("idProcedimento", procedimentoModel.getId());
                    i.putExtra("idHospital", procedimentoModel.getIdHospital());
                    a.startActivity(i);
                } else {
                    Toast.makeText(a, "Tente novamente!", Toast.LENGTH_SHORT).show();
                }
            });



        });

        // ===== ADIÇÃO: refletir estado da estrela de favorito no bind =====
        boolean isFav = favoritosKeys.contains(keyOf(procedimentoModel));
        holder.favoritarProcecimento.setImageResource(isFav ? R.drawable.estrela2 : R.drawable.estrela1);

        // ===== ADIÇÃO: toggle de favorito independente por item + persistência =====
        holder.favoritarProcecimento.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            ProcedimentoModel p = itemList.get(pos);
            String k = keyOf(p);

            boolean novoEstado = ProcedimentoFavoritosStore.toggleFavorito(a, p);
            if (novoEstado) {
                favoritosKeys.add(k);
                holder.favoritarProcecimento.setImageResource(R.drawable.estrela2);
                LoadingUtils.showLoading2(a, "Atenção","Procedimento adicionado aos favoritos");
            } else {
                favoritosKeys.remove(k);
                holder.favoritarProcecimento.setImageResource(R.drawable.estrela1);
                LoadingUtils.showLoading2(a, "Atenção","Procedimento removido dos favoritos");
            }
        });
    }

    // depois (compatível com versões antigas do RecyclerView)
    private int getBindingAdapterPositionSafe(@NonNull RecyclerView.ViewHolder h) {
        int p = h.getAdapterPosition();
        if (p == RecyclerView.NO_POSITION) {
            p = h.getAdapterPosition();
        }
        return p == RecyclerView.NO_POSITION ? -1 : p;
    }


    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView nomeProcedimento;
        ImageButton favoritarProcecimento;
        ImageButton btn_ir_ver_procedimento;
        ImageButton btn_ir_duplicar_procedimento;
        CheckBox checkBoxMarcarCompartilhar;
        Button editarProcediemento;
        Button excluirProcediemento;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            nomeProcedimento = itemView.findViewById(R.id.text_nome_procedimento);
            btn_ir_ver_procedimento = itemView.findViewById(R.id.btn_ir_ver_procedimento);
            btn_ir_duplicar_procedimento = itemView.findViewById(R.id.btn_ir_duplicar_procedimento);
            checkBoxMarcarCompartilhar = itemView.findViewById(R.id.checkBoxMarcarCompartilhar);
            editarProcediemento = itemView.findViewById(R.id.editarProcediemento);
            excluirProcediemento = itemView.findViewById(R.id.excluirProcediemento);
            favoritarProcecimento = itemView.findViewById(R.id.favoritarProcecimento);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String text) {
        buscarProcedimentos(true, text);
    }
}
