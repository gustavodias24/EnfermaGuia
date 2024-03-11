package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import benicio.solucoes.enfermaguia.adapter.AdapterConteudo;
import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityHospitalPainelBinding;
import benicio.solucoes.enfermaguia.model.ConteudoModel;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.PDFGenerator;

public class HospitalPainelActivity extends AppCompatActivity {

    private DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private ActivityHospitalPainelBinding mainBinding;

    private RecyclerView rProcedimentos;
    private List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    private AdapterProcedimentos adapterProcedimentos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHospitalPainelBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Painel Hospital");

        mainBinding.criarProcecimento.setOnClickListener(view -> startActivity(new Intent(this, CriarProcedimentoActivity.class)));
        mainBinding.verSugestoes.setOnClickListener(view -> startActivity(new Intent(this, VerSugestoesActivity.class)));

        configurarRecyclerProcedimento();

        mainBinding.metricas.setOnClickListener(view -> startActivity(new Intent(this, MetricasA.class)));

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
//                HallActivity.gerarPdfOS(listaParaCompartilharProcedimento, this);
                PDFGenerator.generateAndSharePDF(this, listaParaCompartilharProcedimento);
            }
        });
    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this, true);
        rProcedimentos.setAdapter(adapterProcedimentos);

        refProcedimentos.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listaProcedimento.clear();
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel procedimentoModel = dado.getValue(ProcedimentoModel.class);
                        if (procedimentoModel.getIdHospital().equals(prefs.getString("id", ""))) {
                            listaProcedimento.add(procedimentoModel);
                        }
                        adapterProcedimentos.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sair_conta) {
            finish();
            editor.putString("id", "").apply();
            startActivity(new Intent(this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}