package benicio.solucoes.enfermaguia;

import static benicio.solucoes.enfermaguia.utils.LoadingUtils.showLoading2;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
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
import benicio.solucoes.enfermaguia.model.FeedbackModel;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.model.SugestaoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.LoadingUtils;
import benicio.solucoes.enfermaguia.utils.PDFGenerator;

public class HallActivity extends AppCompatActivity {
    int newCountFeedbacks = 0;

    // caso ativa quando clicar em um hospital vai ser para abrir a segestão
    public static  boolean selecaoAtiva  = false;

    MenuItem itemFeedbackMenu;
    public static String nomeUsuario = "";
    public static Dialog dialogSugestao;
    public static List<UsuarioModel> listaHospitais = new ArrayList<>();
    private ActivityHallBinding mainBinding;

    private DatabaseReference refFeedbacks = FirebaseDatabase.getInstance().getReference().child("feedbacks");
    public static DatabaseReference refSugestoes = FirebaseDatabase.getInstance().getReference().child("sugestoes");
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public static TextView avisoSelecionarHospital;
    public static RecyclerView recyclerHospital;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHallBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Se o menu lateral estiver aberto, fecha primeiro
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }

                // Pergunta se deseja sair do app
                showExitDialog();
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        editor.putString("idHospitalSelecionado", "").apply();

        verificarNovosFeedbacks();

        pegarNomeUsuario();


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
//            if (item.getItemId() == R.id.selecionar_hospital) {
//                mainBinding.rvSelecionarHospital.setVisibility(View.VISIBLE);
//                mainBinding.textView9.setVisibility(View.VISIBLE);
//                editor.putString("idHospitalSelecionado", "").apply();
//            } else
                if (item.getItemId() == R.id.sugerir_pop) {
                if (prefs.getString("idHospitalSelecionado", "").isEmpty()) {
                    LoadingUtils.showLoading2(this, "Atenção!", "Selecione um hospital primeiro antes de querer enviar uma sugestão de POP");
                    selecaoAtiva = true;
                } else {
                    HallActivity.showSugerirPOP(this);
                }
            } else if (item.getItemId() == R.id.menu_ajuda_usuario) {
                startActivity(new Intent(this, TutorialActivity.class));
            } else if (item.getItemId() == R.id.menu_creditos_usuario) {
                startActivity(new Intent(this, CreditosActivity.class));
            } else if (item.getItemId() == R.id.menu_sair_usuario) {
                finish();
                editor.putString("id", "").apply();
                startActivity(new Intent(this, MainActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });

        avisoSelecionarHospital = mainBinding.textView9;

        configurarDialogSelecionarHospital();
        configurrarRVselecionarHospital();

    }




    public static void showSugerirPOP(Activity a) {
        AlertDialog.Builder b = new AlertDialog.Builder(a);

        b.setCancelable(false);

        LayoutCriarSugestaoBinding criarSugestaoBinding = LayoutCriarSugestaoBinding.inflate(a.getLayoutInflater());
        criarSugestaoBinding.title.setText("Sugerir um novo POP para " + prefs.getString("nomeHospitalSelecionado", "") );
        criarSugestaoBinding.subtitle.setText("Sugerir a criação de um novo POP");

        criarSugestaoBinding.cancelar.setOnClickListener(v -> {
            dialogSugestao.dismiss();
            selecaoAtiva = false;
        });

        criarSugestaoBinding.cadastro.setOnClickListener(view -> {
            String sugestaoString = criarSugestaoBinding.sugestaoField.getText().toString();
            if (sugestaoString.isEmpty()) {
                Toast.makeText(a, "Sugestão não pode ser vazia!", Toast.LENGTH_SHORT).show();
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
                        showLoading2(a, "Parabéns!","Sua sugestão foi enviada com sucesso!");
                        dialogSugestao.dismiss();
                    }
                });
            }
        });

        b.setView(criarSugestaoBinding.getRoot());
        dialogSugestao = b.create();
        dialogSugestao.show();
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
                    //setNomeHospitalAtual();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void configurarDialogSelecionarHospital() {

    }





    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sair_conta) {
            finish();
            editor.putString("id", "").apply();
            startActivity(new Intent(this, MainActivity.class));
        } else if (item.getItemId() == R.id.go_to_feedbacks) {
            mudarIcone(false);
            startActivity(new Intent(this, FeedBacksActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        itemFeedbackMenu = menu.findItem(R.id.go_to_feedbacks);
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


    private void mudarIcone(Boolean newFeedbacks) {
        if (itemFeedbackMenu != null) {
            if (newFeedbacks) {
                itemFeedbackMenu.setIcon(R.drawable.email_new_feedback);
            } else {
                itemFeedbackMenu.setIcon(R.drawable.email_icon_white);
            }
        }
    }

    private void verificarNovosFeedbacks() {

        int oldCountFeedbacks = prefs.getInt("oldCountFeedbacks", 0);

        refFeedbacks.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        FeedbackModel feedbackModel = dado.getValue(FeedbackModel.class);
                        try {
                            if (feedbackModel != null && feedbackModel.getIdUsuario().equals(prefs.getString("id", ""))) {
                                newCountFeedbacks += 1;
                            }
                        } catch (Exception ignored) {

                        }
                    }
                    if (oldCountFeedbacks != newCountFeedbacks) {
                        mudarIcone(true);
                    }
                    editor.putInt("oldCountFeedbacks", newCountFeedbacks).apply();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sair do aplicativo?")
                .setMessage("Deseja realmente sair do aplicativo?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    // Encerra todas as activities do app
                    finishAffinity();
                })
                .setNegativeButton("Não", null)
                .setCancelable(true)
                .show();
    }

}