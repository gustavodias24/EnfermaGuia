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
import java.util.Collections;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterSugestao;
import benicio.solucoes.enfermaguia.databinding.ActivityVerDetalheProcedimentoBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityVerSugestoesBinding;
import benicio.solucoes.enfermaguia.model.SugestaoModel;

public class VerSugestoesActivity extends AppCompatActivity {

    private DatabaseReference refSugestoes = FirebaseDatabase.getInstance().getReference().child("sugestoes");
    private SharedPreferences prefs;
    private ActivityVerSugestoesBinding mainBinding;
    private RecyclerView rSugestoes;
    private List<SugestaoModel> listaSugestao = new ArrayList<>();
    private AdapterSugestao adapterSugestao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityVerSugestoesBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Sugestões");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        configurarRecyclerSugestoes();


    }

    private void configurarRecyclerSugestoes() {
        rSugestoes = mainBinding.recyclerSugestoes;
        rSugestoes.setLayoutManager(new LinearLayoutManager(this));
        rSugestoes.setHasFixedSize(true);
        rSugestoes.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterSugestao = new AdapterSugestao(this, listaSugestao);
        rSugestoes.setAdapter(adapterSugestao);


        refSugestoes.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listaSugestao.clear();

                    for (DataSnapshot dado : snapshot.getChildren()) {
                        SugestaoModel sugestao = dado.getValue(SugestaoModel.class);
                        if ( sugestao.getIdHospital() != null && sugestao.getIdHospital().equals(prefs.getString("id", " "))){
                            listaSugestao.add(sugestao);
                        }
                    }
                    Collections.reverse(listaSugestao);
                    adapterSugestao.notifyDataSetChanged();
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