package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterMetricas;
import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.adapter.AdapterSugestao;
import benicio.solucoes.enfermaguia.databinding.ActivityMetricasBinding;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public class MetricasA extends AppCompatActivity {

    private DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private SharedPreferences prefs;
    private ActivityMetricasBinding mainBinding;
    private RecyclerView recyclerMetricas;
    private List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    private AdapterMetricas adapterMetricas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMetricasBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Métricas");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        configurarRecyclerMetricas();
    }

    private void configurarRecyclerMetricas() {
        recyclerMetricas = mainBinding.recyclerMetricas;
        recyclerMetricas.setLayoutManager(new LinearLayoutManager(this));
        recyclerMetricas.setHasFixedSize(true);
        recyclerMetricas.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterMetricas = new AdapterMetricas(listaProcedimento, this);
        recyclerMetricas.setAdapter(adapterMetricas);

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
                        adapterMetricas.notifyDataSetChanged();
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
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}