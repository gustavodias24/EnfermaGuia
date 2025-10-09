package benicio.solucoes.enfermaguia;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityVerPopHospitalBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutCarregandoBinding;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.LoadingUtils;
import benicio.solucoes.enfermaguia.utils.PDFGenerator;

public class VerPopHospitalActivity extends AppCompatActivity {

    public static List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    public static AdapterProcedimentos adapterProcedimentos;
    public static String nomeHospital = "";

    private RecyclerView rProcedimentos;
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");

    public static TextView nomeHospitalTEXT;
    public static LinearLayout layoutProcedimentos;
    public static Button btnCompartilhar;


    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;




    public static ActivityVerPopHospitalBinding mainBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityVerPopHospitalBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        getSupportActionBar().setTitle("Voltar");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        nomeHospitalTEXT = mainBinding.textView7;
        layoutProcedimentos = mainBinding.layoutProcedimentos;
        btnCompartilhar = mainBinding.compartilhar;
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();



        mainBinding.compartilhar.setOnClickListener(view -> {
            List<ProcedimentoModel> listaParaCompartilharProcedimento = new ArrayList<>();
            for (ProcedimentoModel procedimento : listaProcedimento) {
                if (procedimento.isChecado()) {
                    listaParaCompartilharProcedimento.add(procedimento);
                }
            }

            if (listaParaCompartilharProcedimento.isEmpty()) {
                Toast.makeText(this, "Selecione pelo menos 1 procedimento!", Toast.LENGTH_SHORT).show();
            } else {
//                gerarPdfOS(listaParaCompartilharProcedimento, this);
                PDFGenerator.generateAndSharePDF(this, listaParaCompartilharProcedimento, "Procedimentos do Hospital " + nomeHospital);
            }
        });

        configurarRecyclerProcedimento();

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void pesquisarProcedimento(View view){
        String query = mainBinding.edtPesquisar.getText().toString();
        if (!query.isEmpty()){
            adapterProcedimentos.filter(query);
        }else{
            LoadingUtils.showLoading(this);
            buscarProcedimentos(true, query);
        }
    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this, false);
        rProcedimentos.setAdapter(adapterProcedimentos);

        LoadingUtils.showLoading(this);
        buscarProcedimentos(false, "");

    }

    public static void buscarProcedimentos(boolean filter, String query) {

        refProcedimentos.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                LoadingUtils.dismissLoading();

                if (snapshot.exists()) {
                    listaProcedimento.clear();
                    mainBinding.textView12.setVisibility(View.VISIBLE);

                    String q = (query != null) ? query.toLowerCase().trim() : "";

                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel procedimentoModel = dado.getValue(ProcedimentoModel.class);

                        if (procedimentoModel == null) continue;

                        if (procedimentoModel.getIdHospital().equals(
                                prefs.getString("idHospitalSelecionado", ""))) {

                            boolean matches = true;

                            if (filter && !q.isEmpty()) {
                                matches = contemQueryEmQualquerCampo(procedimentoModel, q);
                            }

                            if (matches) {
                                listaProcedimento.add(procedimentoModel);
                                mainBinding.textView12.setVisibility(View.GONE);
                            }
                        }
                    }

                    adapterProcedimentos.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                LoadingUtils.dismissLoading();
            }
        });
    }

    /**
     * Verifica se a query está presente em qualquer campo do objeto ProcedimentoModel.
     */
    private static boolean contemQueryEmQualquerCampo(ProcedimentoModel p, String query) {
        try {
            // Busca nos campos principais
            if (verificaTexto(p.getNomeProcedimento(), query)) return true;
            if (verificaTexto(p.getId(), query)) return true;
            if (verificaTexto(p.getIdHospital(), query)) return true;
            if (verificaTexto(String.valueOf(p.getAcessos()), query)) return true;
            if (verificaTexto(String.valueOf(p.getCompartilhamentos()), query)) return true;
            if (verificaTexto(String.valueOf(p.getSugestoes()), query)) return true;

            // Busca dentro da lista de informações
            if (p.getListaInformacao() != null) {
                for (InfoProcedimento info : p.getListaInformacao()) {
                    if (verificaTexto(info.getInfo(), query)) return true;
                }
            }

        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Método auxiliar para comparar strings ignorando maiúsculas/minúsculas e nulos.
     */
    private static boolean verificaTexto(String valor, String query) {
        return valor != null && valor.toLowerCase().contains(query);
    }



//    public static void setNomeHospitalAtual() {
//        for (UsuarioModel hospital : listaHospitais) {
//            if (prefs.getString("idHospitalSelecionado", "").equals(hospital.getId())) {
//                nomeHospital = hospital.getNome();
//                break;
//            }
//        }
//    }
}