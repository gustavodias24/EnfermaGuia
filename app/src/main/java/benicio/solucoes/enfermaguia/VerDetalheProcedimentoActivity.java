package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    private Bundle b;
    private ProcedimentoModel procedimentoModel;
    private Dialog dialogSugestao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityVerDetalheProcedimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Voltar");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        mainBinding.darSugestao.setOnClickListener(view -> dialogSugestao.show());
        configurarDialogSugestao();
        pegarNomeUsuario();

        b = getIntent().getExtras();

        assert b != null;
        String idHospital = b.getString("idHospital", "");
        refUsuarios.child(idHospital).get().addOnCompleteListener( task -> {
            if ( task.isSuccessful() ){
                UsuarioModel hospital = task.getResult().getValue(UsuarioModel.class);
                assert hospital != null;
                mainBinding.textTituloHospital.setText(hospital.getNome());
            }
        });

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

                    String titulo = "";
                    for (InfoProcedimento info : procedimentoModel.getListaInformacao()) {
                        if (info.getTipo() == 0) {
                              titulo = info.getInfo();
                        } else {
                            String descricao = info.getInfo();
                            adicionarItemDinamico(titulo, descricao,  mainBinding.layout , this);
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
            String sugestaoString = criarSugestaoBinding.sugestaoField.getText().toString();
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
                sugestaoModel.setIdUsuario(prefs.getString("id", ""));

                refSugestoes.child(sugestaoModel.getId()).setValue(sugestaoModel).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        procedimentoModel.setSugestoes(procedimentoModel.getSugestoes() + 1);
                        refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(task1 -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Sugestão Enviada com Sucesso!", Toast.LENGTH_SHORT).show();
                                criarSugestaoBinding.sugestaoField.setText("");
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


    private void adicionarItemDinamico(String titulo, String descricao, LinearLayout container, Context context) {
        // Layout principal (vertical)
        LinearLayout layoutPrincipal = new LinearLayout(context);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);
//
        // Criar LayoutParams e adicionar margem
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        layoutPrincipal.setLayoutParams(params);
        layoutPrincipal.setPadding(8, 16, 16, 8);

        layoutPrincipal.setBackground(ContextCompat.getDrawable(context, R.drawable.back_redondo)); // seu drawable

        // Layout horizontal com imagem e título
        LinearLayout layoutHorizontal = new LinearLayout(context);
        layoutHorizontal.setOrientation(LinearLayout.HORIZONTAL);
        layoutHorizontal.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // ImageView
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(android.R.drawable.ic_input_add);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Título (TextView)
        TextView tituloTextView = new TextView(context);
        tituloTextView.setText(titulo);
        tituloTextView.setGravity(Gravity.CENTER);
        tituloTextView.setTextColor(ContextCompat.getColor(context, R.color.azul_medio));
        tituloTextView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        // Descrição (TextView)
        TextView descricaoTextView = new TextView(context);
        descricaoTextView.setText(descricao);
        descricaoTextView.setGravity(Gravity.CENTER);
        descricaoTextView.setTextColor(ContextCompat.getColor(context, R.color.black));
        descricaoTextView.setVisibility(View.GONE);
        descricaoTextView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Clique no título
        imageView.setOnClickListener(v -> {
            if (descricaoTextView.getVisibility() == View.GONE) {
                descricaoTextView.setAlpha(0f);
                descricaoTextView.setVisibility(View.VISIBLE);
                descricaoTextView.animate().alpha(1f).setDuration(300).start();
            } else {
                descricaoTextView.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                    descricaoTextView.setVisibility(View.GONE);
                }).start();
            }
        });

        // Montar a hierarquia
        layoutHorizontal.addView(imageView);
        layoutHorizontal.addView(tituloTextView);

        layoutPrincipal.addView(layoutHorizontal);
        layoutPrincipal.addView(descricaoTextView);

        // Adicionar ao container principal da sua Activity/Fragment
        container.addView(layoutPrincipal);
    }

}