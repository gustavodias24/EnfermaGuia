package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import benicio.solucoes.enfermaguia.databinding.ActivityVerDetalheProcedimentoBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutCriarSugestaoBinding;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.model.SugestaoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;

public class VerDetalheProcedimentoActivity extends AppCompatActivity {

    // ===== Logs / Tutorial =====
    private static final String TAG = "mayara";
    private static final long PREP_DELAY_MS = 3000L;
    private static final int ID_SUGESTAO = 501;
    private static final int ID_PLUS = 502;

    // ===== State =====
    private SharedPreferences prefs;
    private String nomeUsuario = "Anônimo";
    private ActivityVerDetalheProcedimentoBinding mainBinding;
    private final DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private final DatabaseReference refSugestoes = FirebaseDatabase.getInstance().getReference().child("sugestoes");
    private final DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    private ProcedimentoModel procedimentoModel;
    private Dialog dialogSugestao;

    // guardar a referência do PRIMEIRO ícone “+” criado dinamicamente
    private @Nullable View firstPlusView = null;

    // fila do tutorial
    private final java.util.ArrayDeque<TapTarget> preparedQueue = new java.util.ArrayDeque<>();
    private boolean preparing = false;
    private boolean prepared = false;
    private @Nullable Runnable pendingShow;

    // listener para aguardar layout caso o “+” ainda não exista
    private @Nullable ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityVerDetalheProcedimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Voltar");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Botão sugestão abre o dialog (tela já tinha isso)
        mainBinding.darSugestao.setOnClickListener(v -> dialogSugestao.show());
        configurarDialogSugestao();
        pegarNomeUsuario();

        // Carrega hospital e procedimento
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String idHospital = b.getString("idHospital", "");
            refUsuarios.child(idHospital).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    UsuarioModel hospital = task.getResult().getValue(UsuarioModel.class);
                    if (hospital != null) {
                        mainBinding.textTituloHospital.setText(hospital.getNome());
                    }
                }
            });

            String idProcedimento = b.getString("idProcedimento", "");
            if (!idProcedimento.isEmpty()) {
                refProcedimentos.child(idProcedimento).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        procedimentoModel = task.getResult().getValue(ProcedimentoModel.class);
                        if  (procedimentoModel.getListaInformacao().isEmpty()){
                            Toast.makeText(this, "Nenhum procedimento encontrado!", Toast.LENGTH_SHORT).show();
                            finish();
                            
                        }
                        if (procedimentoModel == null) return;

                        mainBinding.textTituloProcedimento.setText(procedimentoModel.getNomeProcedimento());

                        // Monta blocos dinâmicos (cada bloco cria um ImageView “+” clicável)
                        String titulo = "";
                        for (InfoProcedimento info : procedimentoModel.getListaInformacao()) {
                            if (info.getTipo() == 0) {
                                titulo = info.getInfo();
                            } else {
                                String descricao = info.getInfo();
                                adicionarItemDinamico(titulo, descricao, mainBinding.layout, this);
                            }
                        }

                        // Compartilhar (mantido)
                        mainBinding.compartilhar.setOnClickListener(view -> {
                            procedimentoModel.setCompartilhamentos(procedimentoModel.getCompartilhamentos() + 1);
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
                    }
                });
            }
        }


        // prepara 3s depois; troca texto para AJUDA e habilita
        mainBinding.tutorial.postDelayed(() -> {
            Log.d(TAG, "Delay 3s concluído: preparando tutorial...");
            prepararTutorial(() -> {
                Log.d(TAG, "Tutorial preparado. Habilitando botão.");
                mainBinding.tutorial.setText("AJUDA");
                mainBinding.tutorial.setEnabled(true);
            });
        }, PREP_DELAY_MS);

        // clique para exibir
        mainBinding.tutorial.setOnClickListener(v -> {
            Log.d(TAG, "AJUDA clicado. prepared=" + prepared + " preparing=" + preparing);
            if (prepared) {
                mostrarTutorial();
            } else if (!preparing) {
                prepararTutorial(this::mostrarTutorial);
            } else {
                pendingShow = this::mostrarTutorial;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (globalLayoutListener != null) {
            mainBinding.layout.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
            globalLayoutListener = null;
        }
        pendingShow = null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    private void configurarDialogSugestao() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutCriarSugestaoBinding criar = LayoutCriarSugestaoBinding.inflate(getLayoutInflater());

        criar.cancelar.setOnClickListener(v -> dialogSugestao.dismiss());

        criar.cadastro.setOnClickListener(view -> {
            String sugestaoString = criar.sugestaoField.getText().toString();
            if (sugestaoString.isEmpty()) {
                Toast.makeText(this, "Sugestão não pode ser vazia!", Toast.LENGTH_SHORT).show();
            } else {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                String dataAtual = sdf.format(new Date());

                SugestaoModel s = new SugestaoModel();
                s.setIdHospital(procedimentoModel.getIdHospital());
                s.setId(UUID.randomUUID().toString());
                s.setDataSugestao(dataAtual);
                s.setIdProcedimento(procedimentoModel.getId());
                s.setNomeProcedimento(procedimentoModel.getNomeProcedimento());
                s.setInfo(sugestaoString);
                s.setNomeUsuario(nomeUsuario);
                s.setIdUsuario(prefs.getString("id", ""));

                refSugestoes.child(s.getId()).setValue(s).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        procedimentoModel.setSugestoes(procedimentoModel.getSugestoes() + 1);
                        refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Toast.makeText(this, "Sugestão Enviada com Sucesso!", Toast.LENGTH_SHORT).show();
                                criar.sugestaoField.setText("");
                                dialogSugestao.dismiss();
                            } else {
                                Toast.makeText(this, "Tente Novamente...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        b.setView(criar.getRoot());
        dialogSugestao = b.create();
    }

    private void pegarNomeUsuario() {
        DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
        refUsuarios.child(prefs.getString("id", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UsuarioModel u = snapshot.getValue(UsuarioModel.class);
                    if (u != null) nomeUsuario = u.getNome();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ====== Tutorial ======
    private void prepararTutorial(@Nullable Runnable after) {
        if (preparing) {
            if (after != null) pendingShow = after;
            Log.d(TAG, "prepararTutorial: já preparando; callback pendente registrado");
            return;
        }
        preparing = true;
        prepared = false;
        preparedQueue.clear();

        // 1) “Enviar sugestão”
        preparedQueue.add(
                TapTarget.forView(
                                mainBinding.darSugestao,
                                "Enviar sugestão",
                                "Achou algo para melhorar neste POP? Toque aqui para enviar sua sugestão.")
                        .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                        .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                        .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                        .transparentTarget(true)
                        .id(ID_SUGESTAO)
        );

        // 2) Primeiro “+” (se já existir). Se não existir, esperamos o layout.
        View plus = firstPlusView != null ? firstPlusView : procurarPrimeiroPlusNaArvore(mainBinding.layout);
        Log.d(TAG, "prepararTutorial: firstPlusView já conhecido? " + (firstPlusView != null) + " encontradoAgora? " + (plus != null));
        if (plus != null) {
            preparedQueue.add(buildTapPlus(plus));
            finalizarPreparacao(after);
        } else {
            if (globalLayoutListener != null) {
                mainBinding.layout.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
            }
            globalLayoutListener = () -> {
                View p = firstPlusView != null ? firstPlusView : procurarPrimeiroPlusNaArvore(mainBinding.layout);
                Log.d(TAG, "onGlobalLayout: procurando plus -> " + (p != null));
                if (p != null) {
                    preparedQueue.add(buildTapPlus(p));
                    finalizarPreparacao(after);
                    // remove listener
                    mainBinding.layout.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
                    globalLayoutListener = null;
                }
            };
            mainBinding.layout.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
            // caso já esteja visível ao postar
            mainBinding.layout.post(() -> {
                if (prepared) return;
                View p2 = firstPlusView != null ? firstPlusView : procurarPrimeiroPlusNaArvore(mainBinding.layout);
                if (p2 != null) {
                    preparedQueue.add(buildTapPlus(p2));
                    finalizarPreparacao(after);
                    if (globalLayoutListener != null) {
                        mainBinding.layout.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
                        globalLayoutListener = null;
                    }
                }
            });
        }
    }

    private void finalizarPreparacao(@Nullable Runnable after) {
        preparing = false;
        prepared = true;
        Log.d(TAG, "finalizarPreparacao: queue=" + preparedQueue.size());
        Runnable cb = (after != null) ? after : pendingShow;
        pendingShow = null;
        if (cb != null) cb.run();
    }

    private TapTarget buildTapPlus(@NonNull View v) {
        return TapTarget.forView(
                        v,
                        "Abrir seção",
                        "Toque no ícone **+** para expandir e ver o conteúdo desta seção.")
                .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                .transparentTarget(true)
                .id(ID_PLUS);
    }

    private void mostrarTutorial() {
        if (!prepared || preparedQueue.isEmpty()) {
            Log.d(TAG, "mostrarTutorial: não preparado/vazio. Repreparando…");
            prepararTutorial(this::mostrarTutorial);
            return;
        }
        final java.util.ArrayDeque<TapTarget> queue = new java.util.ArrayDeque<>(preparedQueue);
        exibirProximo(queue);
    }

    private void exibirProximo(java.util.ArrayDeque<TapTarget> queue) {
        if (queue.isEmpty()) {
            Log.d(TAG, "exibirProximo: fim do tutorial");
            return;
        }
        TapTarget next = queue.pollFirst();

        // Revalida o alvo do “+” na hora de exibir
        if (next != null && next.id() == ID_PLUS) {
            View plus = (firstPlusView != null) ? firstPlusView : procurarPrimeiroPlusNaArvore(mainBinding.layout);
            if (plus == null) {
                Log.d(TAG, "exibirProximo: plus sumiu, pulando.");
                exibirProximo(queue);
                return;
            }
            next = buildTapPlus(plus);
        }

        TapTargetView.showFor(
                this,
                next,
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        super.onTargetClick(view);
                        view.dismiss(true);
                    }
                    @Override public void onOuterCircleClick(TapTargetView view) {
                        super.onOuterCircleClick(view);
                        view.dismiss(true);
                    }
                    @Override public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                        exibirProximo(queue);
                    }
                }
        );
    }

    // ====== UI Dinâmica ======
    private void adicionarItemDinamico(String titulo, String descricao, LinearLayout container, Context context) {
        LinearLayout layoutPrincipal = new LinearLayout(context);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        layoutPrincipal.setLayoutParams(params);
        layoutPrincipal.setPadding(8, 16, 16, 8);
        layoutPrincipal.setBackground(ContextCompat.getDrawable(context, R.drawable.back_redondo));

        LinearLayout layoutHorizontal = new LinearLayout(context);
        layoutHorizontal.setOrientation(LinearLayout.HORIZONTAL);
        layoutHorizontal.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(android.R.drawable.ic_input_add);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        imageView.setContentDescription("expandir_secao");
        imageView.setTag("plus_item"); // tag para poder localizar depois
        if (firstPlusView == null) {
            firstPlusView = imageView; // guarda o primeiro “+” criado
            Log.d(TAG, "adicionarItemDinamico: firstPlusView registrado");
        }

        TextView tituloTextView = new TextView(context);
        tituloTextView.setText(titulo);
        tituloTextView.setGravity(Gravity.CENTER);
        tituloTextView.setTextColor(ContextCompat.getColor(context, R.color.azul_medio));
        tituloTextView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView descricaoTextView = new TextView(context);
        descricaoTextView.setText(descricao);
        descricaoTextView.setGravity(Gravity.CENTER);
        descricaoTextView.setTextColor(ContextCompat.getColor(context, R.color.black));
        descricaoTextView.setVisibility(View.GONE);
        descricaoTextView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

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

        layoutHorizontal.addView(imageView);
        layoutHorizontal.addView(tituloTextView);
        layoutPrincipal.addView(layoutHorizontal);
        layoutPrincipal.addView(descricaoTextView);
        container.addView(layoutPrincipal);
    }

    // busca recursiva pelo primeiro “+” caso a referência não esteja setada
    private @Nullable View procurarPrimeiroPlusNaArvore(View root) {
        if (root == null) return null;
        if (root instanceof ImageView) {
            Object tag = root.getTag();
            if (tag != null && "plus_item".equals(tag)) return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = procurarPrimeiroPlusNaArvore(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}
