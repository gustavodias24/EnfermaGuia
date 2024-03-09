package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import benicio.solucoes.enfermaguia.databinding.ActivityMainBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityVerDetalheProcedimentoBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutCriarSugestaoBinding;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.model.SugestaoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.Utils;

public class VerDetalheProcedimentoActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    String nomeUsuario = "Anônimo";
    int idTitulo;
    int idBody;
    private ActivityVerDetalheProcedimentoBinding mainBinding;
    private DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private DatabaseReference refSugestoes = FirebaseDatabase.getInstance().getReference().child("sugestoes");
    private Bundle b;
    private ProcedimentoModel procedimentoModel;
    private Dialog dialogSugestao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityVerDetalheProcedimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Visualização");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        mainBinding.darSugestao.setOnClickListener(view -> dialogSugestao.show());
        configurarDialogSugestao();
        pegarNomeUsuario();

        b = getIntent().getExtras();


        assert b != null;
        String idProcedimento = b.getString("idProcedimento", "");
        if (!idProcedimento.isEmpty()) {
            refProcedimentos.child(idProcedimento).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    procedimentoModel = task.getResult().getValue(ProcedimentoModel.class);

                    mainBinding.compartilhar.setOnClickListener(view -> {
                        procedimentoModel.setCompartilhamentos(
                                procedimentoModel.getCompartilhamentos() + 1
                        );
                        refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(tas1 -> {
                            if (task.isSuccessful()) {
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("text/plain");
                                shareIntent.putExtra(Intent.EXTRA_TEXT, procedimentoModel.toString());
                                startActivity(Intent.createChooser(shareIntent, "Compartilhar via"));
                            } else {
                                Toast.makeText(this, "Tente Novamente...", Toast.LENGTH_SHORT).show();
                            }
                        });

                    });

                    mainBinding.textTituloProcedimento.setText(procedimentoModel.getNomeProcedimento());

                    for (InfoProcedimento info : procedimentoModel.getListaInformacao()) {
                        if (info.getTipo() == 0) {
                            TextView textTitle = new TextView(this);

                            textTitle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                textTitle.setId(View.generateViewId());
                                idTitulo = textTitle.getId(); // Armazena o ID gerado na variável global
                            } else {
                                textTitle.setId(Utils.generateViewId()); // Você precisará implementar esse método em uma classe Utils
                                idTitulo = textTitle.getId(); // Armazena o ID gerado na variável global
                            }

                            textTitle.setPadding(16, 32, 16, 0); // left, top, right, bottom
                            textTitle.setText(info.getInfo());
                            textTitle.setTextColor(getResources().getColor(R.color.azul_fote));
                            textTitle.setTextSize(48);
                            textTitle.setTypeface(null, Typeface.BOLD);
                            mainBinding.layout.addView(textTitle);
                        } else {
                            TextView textDescri = new TextView(this);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                textDescri.setId(View.generateViewId());
                                idBody = textDescri.getId(); // Armazena o ID gerado na variável global
                            } else {
                                textDescri.setId(Utils.generateViewId()); // Você precisará implementar esse método em uma classe Utils
                                idBody = textDescri.getId(); // Armazena o ID gerado na variável global
                            }

                            textDescri.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            textDescri.setPadding(16, 0, 16, 0); // left, top, right, bottom
                            textDescri.setVisibility(View.GONE);
                            textDescri.setText(info.getInfo());
                            textDescri.setTextSize(28);
                            textDescri.setTypeface(null, Typeface.BOLD);
                            mainBinding.layout.addView(textDescri);

                            View view = new View(this);
                            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)); // Define a altura da linha
                            view.setBackgroundColor(getResources().getColor(R.color.black)); // Define a cor da linha

                            mainBinding.layout.addView(view);

                            TextView textoTitulo = findViewById(idTitulo);
                            TextView textoDes = findViewById(idBody);

                            textoTitulo.setOnClickListener(viewCrypted -> {
                                if (textoDes.getVisibility() == View.VISIBLE) {
                                    textDescri.setVisibility(View.GONE);
                                } else {
                                    textDescri.setVisibility(View.VISIBLE);
                                }
                            });

                        }
                    }


                }
            });
        }
    }

    private void configurarDialogSugestao() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);

        LayoutCriarSugestaoBinding criarSugestaoBinding = LayoutCriarSugestaoBinding.inflate(getLayoutInflater());

        criarSugestaoBinding.cadastro.setOnClickListener(view -> {
            String sugestaoString = criarSugestaoBinding.sugestaoField.getEditText().getText().toString();
            if (sugestaoString.isEmpty()) {
                Toast.makeText(this, "Sugestão não pode ser vazia!", Toast.LENGTH_SHORT).show();
            } else {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                String dataAtual = simpleDateFormat.format(new Date());

                SugestaoModel sugestaoModel = new SugestaoModel();
                sugestaoModel.setIdHospital(procedimentoModel.getIdHospital());
                sugestaoModel.setId(UUID.randomUUID().toString());
                sugestaoModel.setDataSugestao(dataAtual);
                sugestaoModel.setIdProcedimento(procedimentoModel.getId());
                sugestaoModel.setNomeProcedimento(procedimentoModel.getNomeProcedimento());
                sugestaoModel.setInfo(sugestaoString);
                sugestaoModel.setNomeUsuario(nomeUsuario);

                refSugestoes.child(sugestaoModel.getId()).setValue(sugestaoModel).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        procedimentoModel.setSugestoes(procedimentoModel.getSugestoes() + 1);
                        refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(task1 -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Sugestão Enviada com Sucesso!", Toast.LENGTH_SHORT).show();
                                criarSugestaoBinding.sugestaoField.getEditText().setText("");
                                dialogSugestao.dismiss();
                            } else {
                                Toast.makeText(this, "Tente Novamente...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        b.setView(criarSugestaoBinding.getRoot());
        dialogSugestao = b.create();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
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