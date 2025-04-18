package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import benicio.solucoes.enfermaguia.adapter.AdapterHospitais;
import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityCadastroUsuarioBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutCriarSugestaoBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutSelecionarHospitalBinding;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.model.SugestaoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.PDFGenerator;

public class HallActivity extends AppCompatActivity {

    private String nomeUsuario = "";
    public Dialog dialogSugestao;
    public static List<UsuarioModel> listaHospitais = new ArrayList<>();
    private ActivityHallBinding mainBinding;

    private DatabaseReference refSugestoes = FirebaseDatabase.getInstance().getReference().child("sugestoes");
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private RecyclerView rProcedimentos;
    public static List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    public static AdapterProcedimentos adapterProcedimentos;
    public static Dialog dialogSelecionaHospital;
    public static String nomeHospital = "";
    public static String idHospital = "";

    public static Button btnCompartilhar;
    public static TextView avisoSelecionarHospital;
    public static RecyclerView recyclerHospital;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    public static TextView nomeHospitalTEXT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHallBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        pegarNomeUsuario();

        nomeHospitalTEXT = mainBinding.textView7;
        drawerLayout = mainBinding.drawerLayout;
        navigationView = mainBinding.navigationView;
        toolbar = mainBinding.toolbar;

        setSupportActionBar(toolbar);

        // Habilita botão sanduíche
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Clique nos itens do menu
        navigationView.setNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.buscar_procedimentos) {

            } else if (item.getItemId() == R.id.selecionar_hospital) {
                mainBinding.rvSelecionarHospital.setVisibility(View.VISIBLE);
                mainBinding.textView9.setVisibility(View.VISIBLE);
            } else if (item.getItemId() == R.id.sugerir_pop) {
                showSugerirPOP();
            } else if (item.getItemId() == R.id.menu_ajuda_usuario) {

            } else if (item.getItemId() == R.id.menu_creditos_usuario) {

            } else if (item.getItemId() == R.id.menu_sair_usuario) {
                finish();
                editor.putString("id", "").apply();
                startActivity(new Intent(this, MainActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });

        btnCompartilhar = mainBinding.compartilhar;
        avisoSelecionarHospital = mainBinding.textView9;

        configurarRecyclerProcedimento();
        configurarDialogSelecionarHospital();
        configurrarRVselecionarHospital();

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


    }


    private void showSugerirPOP() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);

        LayoutCriarSugestaoBinding criarSugestaoBinding = LayoutCriarSugestaoBinding.inflate(getLayoutInflater());
        criarSugestaoBinding.title.setText("Sugerir um novo POP");
        criarSugestaoBinding.subtitle.setText("Sugerir a criação de um novo POP");

        criarSugestaoBinding.cadastro.setOnClickListener(view -> {
            String sugestaoString = criarSugestaoBinding.sugestaoField.getText().toString();
            if (sugestaoString.isEmpty()) {
                Toast.makeText(this, "Sugestão não pode ser vazia!", Toast.LENGTH_SHORT).show();
            } else {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                String dataAtual = simpleDateFormat.format(new Date());

                SugestaoModel sugestaoModel = new SugestaoModel();
                sugestaoModel.setIdHospital(prefs.getString("idHospitalSelecionado", ""));
                sugestaoModel.setId(UUID.randomUUID().toString());
                sugestaoModel.setDataSugestao(dataAtual);
                sugestaoModel.setIdProcedimento("");
                sugestaoModel.setNomeProcedimento("");
                sugestaoModel.setInfo("SUGESTÃO DE POP: " + sugestaoString);
                sugestaoModel.setNomeUsuario(nomeUsuario);
                sugestaoModel.setIdUsuario(prefs.getString("id", ""));

                refSugestoes.child(sugestaoModel.getId()).setValue(sugestaoModel).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Sugestão Enviada com Sucesso!", Toast.LENGTH_SHORT).show();
                        dialogSugestao.dismiss();
                    }
                });
            }
        });

        b.setView(criarSugestaoBinding.getRoot());
        dialogSugestao = b.create();
        dialogSugestao.show();
    }


    public static void setNomeHospitalAtual() {
        for (UsuarioModel hospital : listaHospitais) {
            if (prefs.getString("idHospitalSelecionado", "").equals(hospital.getId())) {
                nomeHospital = hospital.getNome();
                break;
            }
        }
    }

    private void configurrarRVselecionarHospital() {

        recyclerHospital = mainBinding.rvSelecionarHospital;
        recyclerHospital.setLayoutManager(new LinearLayoutManager(this));
        recyclerHospital.setHasFixedSize(true);
        recyclerHospital.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        AdapterHospitais adapterHospitais = new AdapterHospitais(listaHospitais, this, true, editor);
        recyclerHospital.setAdapter(adapterHospitais);

        refUsuarios.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listaHospitais.clear();
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        UsuarioModel hospital = dado.getValue(UsuarioModel.class);
                        if (hospital.isAdmin()) {
                            listaHospitais.add(hospital);
                        }
                    }

                    adapterHospitais.notifyDataSetChanged();
                    setNomeHospitalAtual();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void configurarDialogSelecionarHospital() {

    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this, false);
        rProcedimentos.setAdapter(adapterProcedimentos);

        buscarProcedimentos();

    }

    public static void buscarProcedimentos() {

        refProcedimentos.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listaProcedimento.clear();
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel procedimentoModel = dado.getValue(ProcedimentoModel.class);
                        if (procedimentoModel.getIdHospital().equals(prefs.getString("idHospitalSelecionado", ""))) {
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
        } else if (item.getItemId() == R.id.go_to_feedbacks) {
            startActivity(new Intent(this, FeedBacksActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void pegarNomeUsuario() {
        DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
        refUsuarios.child(prefs.getString("id", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nomeUsuario = Objects.requireNonNull(snapshot.getValue(UsuarioModel.class)).getNome();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

}