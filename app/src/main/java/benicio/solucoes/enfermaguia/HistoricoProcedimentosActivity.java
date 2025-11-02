package benicio.solucoes.enfermaguia;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityHistoricoProcedimentosBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityTutorialBinding;
import benicio.solucoes.enfermaguia.utils.LoadingUtils;
import benicio.solucoes.enfermaguia.utils.ProcedimentoHistoryStore;

public class HistoricoProcedimentosActivity extends AppCompatActivity {

    RecyclerView rProcedimentos;
    AdapterProcedimentos adapterProcedimentos;
    private ActivityHistoricoProcedimentosBinding mainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHistoricoProcedimentosBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Voltar");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onStart() {
        super.onStart();
        configurarRecyclerProcedimento();
    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(ProcedimentoHistoryStore.getHistory(this), this, false, true);
        rProcedimentos.setAdapter(adapterProcedimentos);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}