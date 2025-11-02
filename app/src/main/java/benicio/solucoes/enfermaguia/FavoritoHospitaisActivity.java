package benicio.solucoes.enfermaguia;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import benicio.solucoes.enfermaguia.adapter.AdapterHospitais;
import benicio.solucoes.enfermaguia.databinding.ActivityFavoritoHospitaisBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityHistoricoProcedimentosBinding;
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.UsuarioHistoryStore;

public class FavoritoHospitaisActivity extends AppCompatActivity {

    ActivityFavoritoHospitaisBinding mainBinding;
    public static RecyclerView recyclerHospital;

    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityFavoritoHospitaisBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Voltar");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();


    }

    @Override
    protected void onStart() {
        super.onStart();
        configurrarRVselecionarHospital();

    }

    private void configurrarRVselecionarHospital() {

        recyclerHospital = mainBinding.rvSelecionarHospital;
        recyclerHospital.setLayoutManager(new LinearLayoutManager(this));
        recyclerHospital.setHasFixedSize(true);
        recyclerHospital.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        AdapterHospitais adapterHospitais = new AdapterHospitais(UsuarioHistoryStore.getHistory(this), this, true, editor);
        recyclerHospital.setAdapter(adapterHospitais);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}