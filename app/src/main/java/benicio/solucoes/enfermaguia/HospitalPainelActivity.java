package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import benicio.solucoes.enfermaguia.databinding.ActivityCadastroUsuarioBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityHospitalPainelBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityMainBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutCriarProcedimentoBinding;
import benicio.solucoes.enfermaguia.model.ConteudoModel;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public class HospitalPainelActivity extends AppCompatActivity {

    private DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private ActivityHospitalPainelBinding mainBinding;
    private Dialog dialogCriarProcedimento;

    private RecyclerView rConteudo;
    private List<ConteudoModel> listaConteudo = new ArrayList<>();
    private AdapterConteudo adapterConteudo;


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

        mainBinding.criarProcecimento.setOnClickListener(view -> dialogCriarProcedimento.show());
        mainBinding.verSugestoes.setOnClickListener(view -> startActivity(new Intent(this, VerSugestoesActivity.class)));

        configurarDialogCriarProcedimento();
        configurarRecyclerProcedimento();
    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this);
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

    @SuppressLint("NotifyDataSetChanged")
    private void configurarDialogCriarProcedimento() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutCriarProcedimentoBinding criarProcedimentoBinding = LayoutCriarProcedimentoBinding.inflate(getLayoutInflater());

        configurarRecyclerConteudo(criarProcedimentoBinding);


        criarProcedimentoBinding.pronto.setOnClickListener(view -> {
            String titulo, info;

            titulo = criarProcedimentoBinding.tituloField.getEditText().getText().toString();
            info = criarProcedimentoBinding.conteudoField.getEditText().getText().toString();

            if (titulo.isEmpty() || info.isEmpty()) {
                Toast.makeText(this, "Preencha o Título e o Conteúdo do procedimento.", Toast.LENGTH_SHORT).show();
            } else {
                criarProcedimentoBinding.tituloField.getEditText().setText("");
                criarProcedimentoBinding.conteudoField.getEditText().setText("");

                listaConteudo.add(new ConteudoModel(titulo, info));
                adapterConteudo.notifyDataSetChanged();
            }
        });

        criarProcedimentoBinding.cadastroUpdate.setOnClickListener(view -> {
            String nomeProcedimento = criarProcedimentoBinding.nomeField.getEditText().getText().toString();

            if (nomeProcedimento.isEmpty()) {
                Toast.makeText(this, "Adicione o Nome do Procedimento.", Toast.LENGTH_SHORT).show();
            } else {
                String id = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
                ProcedimentoModel procedimentoModel = new ProcedimentoModel();

                procedimentoModel.setNomeProcedimento(nomeProcedimento);
                procedimentoModel.setId(id);
                procedimentoModel.setIdHospital(prefs.getString("id", ""));

                List<InfoProcedimento> listaInfoProcedimento = new ArrayList<>();
                for (ConteudoModel conteudoPreview : listaConteudo) {
                    InfoProcedimento infoTitle = new InfoProcedimento();
                    infoTitle.setInfo(conteudoPreview.getTitulo());
                    infoTitle.setTipo(0);

                    InfoProcedimento infoBody = new InfoProcedimento();
                    infoBody.setInfo(conteudoPreview.getInfo());
                    infoBody.setTipo(1);

                    listaInfoProcedimento.add(infoTitle);
                    listaInfoProcedimento.add(infoBody);

                }

                procedimentoModel.getListaInformacao().addAll(listaInfoProcedimento);

                refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listaConteudo.clear();
                        criarProcedimentoBinding.nomeField.getEditText().setText("");
                        Toast.makeText(this, "Procedimento Cadastrado", Toast.LENGTH_SHORT).show();
                        dialogCriarProcedimento.dismiss();
                    }
                });

            }

        });

        b.setView(criarProcedimentoBinding.getRoot());
        dialogCriarProcedimento = b.create();
    }

    private void configurarRecyclerConteudo(LayoutCriarProcedimentoBinding criarProcedimentoBinding) {
        rConteudo = criarProcedimentoBinding.recyclerConteudo;
        rConteudo.setLayoutManager(new LinearLayoutManager(this));
        rConteudo.setHasFixedSize(true);
        rConteudo.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterConteudo = new AdapterConteudo(listaConteudo, this);
        rConteudo.setAdapter(adapterConteudo);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if ( item.getItemId() == R.id.sair_conta){
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