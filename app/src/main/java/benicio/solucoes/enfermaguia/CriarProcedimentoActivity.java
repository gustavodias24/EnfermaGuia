package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import benicio.solucoes.enfermaguia.databinding.ActivityCriarProcedimentoBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityMetricasBinding;
import benicio.solucoes.enfermaguia.model.ConteudoModel;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.ItemMoveCallback;

public class CriarProcedimentoActivity extends AppCompatActivity {
    private DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private ActivityCriarProcedimentoBinding mainBinding;
    private RecyclerView rConteudo;
    private List<ConteudoModel> listaConteudo = new ArrayList<>();
    private AdapterConteudo adapterConteudo;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Bundle b;
    private ProcedimentoModel procedimentoModel = new ProcedimentoModel();

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityCriarProcedimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Criar Procedimento");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

//        AlertDialog.Builder b = new AlertDialog.Builder(this);

        configurarRecyclerConteudo();

        b = getIntent().getExtras();
        if (b != null && !b.getString("idProcedimento", "").isEmpty()) {
            procedimentoModel.setId(b.getString("idProcedimento", ""));
            configurarDadosDoProcediementoEdit();
            mainBinding.cadastroUpdate.setText("EDITAR PROCEDIMENTO");
            getSupportActionBar().setTitle("Editar Procedimento");
        }


        mainBinding.pronto.setOnClickListener(view -> {
            String titulo, info;

            titulo = mainBinding.tituloField.getEditText().getText().toString();
            info = mainBinding.conteudoField.getEditText().getText().toString();

            if (titulo.isEmpty() || info.isEmpty()) {
                Toast.makeText(this, "Preencha o Título e o Conteúdo do procedimento.", Toast.LENGTH_SHORT).show();
            } else {
                mainBinding.tituloField.getEditText().setText("");
                mainBinding.conteudoField.getEditText().setText("");

                listaConteudo.add(new ConteudoModel(titulo, info));
                adapterConteudo.notifyDataSetChanged();
            }
        });

        mainBinding.cadastroUpdate.setOnClickListener(view -> {
            String nomeProcedimento = mainBinding.nomeField.getEditText().getText().toString();

            if (nomeProcedimento.isEmpty()) {
                Toast.makeText(this, "Adicione o Nome do Procedimento.", Toast.LENGTH_SHORT).show();
            } else {
                String id = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());

                procedimentoModel.setNomeProcedimento(nomeProcedimento);
                if (procedimentoModel.getId().isEmpty()) {
                    procedimentoModel.setId(id);
                }
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

                procedimentoModel.getListaInformacao().clear();
                procedimentoModel.getListaInformacao().addAll(listaInfoProcedimento);


                refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Operação Concluída!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

            }

        });
    }

    private void configurarDadosDoProcediementoEdit() {
        refProcedimentos.child(procedimentoModel.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    procedimentoModel = snapshot.getValue(ProcedimentoModel.class);
                    mainBinding.nomeField.getEditText().setText(procedimentoModel.getNomeProcedimento());

                    ConteudoModel conteudoModel;
                    for (int i = 0; i < procedimentoModel.getListaInformacao().size(); i += 2) {
                        conteudoModel = new ConteudoModel(); // Criar uma nova instância em cada iteração

                        InfoProcedimento infoTitulo = procedimentoModel.getListaInformacao().get(i);
                        InfoProcedimento infoInfo = procedimentoModel.getListaInformacao().get(i + 1);

                        if (infoTitulo.getTipo() == 0) {
                            conteudoModel.setTitulo(infoTitulo.getInfo());
                            conteudoModel.setInfo(infoInfo.getInfo());
                        } else {
                            conteudoModel.setTitulo(infoInfo.getInfo());
                            conteudoModel.setInfo(infoTitulo.getInfo());
                        }

                        listaConteudo.add(conteudoModel);
                    }
                    adapterConteudo.notifyDataSetChanged();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void configurarRecyclerConteudo() {
        rConteudo = mainBinding.recyclerConteudo;
        rConteudo.setLayoutManager(new LinearLayoutManager(this));
        rConteudo.setHasFixedSize(true);
        rConteudo.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterConteudo = new AdapterConteudo(listaConteudo, this, rConteudo);
        rConteudo.setAdapter(adapterConteudo);

        ItemTouchHelper.Callback callback = new ItemMoveCallback(adapterConteudo);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rConteudo);
    }

    @Override
    public void onBackPressed() {
        perguntarSaida();
    }

    private void perguntarSaida() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Aviso!");
        b.setMessage("Você quer realmente sair da tela atual?");
        b.setNegativeButton("Não", null);
        b.setPositiveButton("Sim", (d, i) -> finish()
        );
        b.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            perguntarSaida();
        }
        return super.onOptionsItemSelected(item);
    }
}