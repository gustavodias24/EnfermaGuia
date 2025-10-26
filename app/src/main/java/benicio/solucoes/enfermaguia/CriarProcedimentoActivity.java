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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Space;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import benicio.solucoes.enfermaguia.adapter.AdapterConteudo;
import benicio.solucoes.enfermaguia.databinding.ActivityCriarProcedimentoBinding;
import benicio.solucoes.enfermaguia.model.ConteudoModel;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.ItemMoveCallback;
import benicio.solucoes.enfermaguia.utils.LoadingUtils;

public class CriarProcedimentoActivity extends AppCompatActivity {

    // ---------------- Tutorial ----------------
    private static final String TAG_TUTORIAL = "mayara";
    private static final long TUTORIAL_PREP_DELAY_MS = 3000L;
    private static final long TUTORIAL_SCROLL_WAIT_MS = 220L;

    // IDs simbólicos (opcionais para debugging)
    private static final int ID_NOME      = 7001;
    private static final int ID_TITULO    = 7002;
    private static final int ID_CONTEUDO  = 7003;
    private static final int ID_PRONTO    = 7004;
    private static final int ID_LISTA     = 7005;
    private static final int ID_SALVAR    = 7006;

    /** Estrutura simples para manter view + textos e só gerar o TapTarget na hora. */
    private static class Step {
        final View view; final String title; final String desc; final int id;
        Step(View v, String t, String d, int i){ view=v; title=t; desc=d; id=i; }
    }
    private final Deque<Step> steps = new ArrayDeque<>();
    private boolean tutorialReady = false;

    // ---------------- Dados/Views ----------------
    private final DatabaseReference refProcedimentos =
            FirebaseDatabase.getInstance().getReference().child("procedimentos");

    private ActivityCriarProcedimentoBinding mainBinding;
    private RecyclerView rConteudo;
    private final List<ConteudoModel> listaConteudo = new ArrayList<>();
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Criar Procedimento");
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        configurarRecyclerConteudo();

        // Edição
        b = getIntent().getExtras();
        if (b != null && !b.getString("idProcedimento", "").isEmpty()) {
            procedimentoModel.setId(b.getString("idProcedimento", ""));
            configurarDadosDoProcediementoEdit();
            mainBinding.cadastroUpdate.setText("EDITAR PROCEDIMENTO");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Editar Procedimento");
        }

        // Adicionar bloco (Pronto)
        mainBinding.pronto.setOnClickListener(view -> {
            String titulo = mainBinding.tituloField.getEditText() != null
                    ? mainBinding.tituloField.getEditText().getText().toString() : "";
            String info = mainBinding.conteudoField.getEditText() != null
                    ? mainBinding.conteudoField.getEditText().getText().toString() : "";

            if (titulo.isEmpty() || info.isEmpty()) {
                LoadingUtils.showLoading2(this, "Atenção", "Preencha o Título e o Conteúdo do procedimento.");
            } else {
                if (mainBinding.tituloField.getEditText() != null) mainBinding.tituloField.getEditText().setText("");
                if (mainBinding.conteudoField.getEditText() != null) mainBinding.conteudoField.getEditText().setText("");

                listaConteudo.add(new ConteudoModel(titulo, info));
                adapterConteudo.notifyDataSetChanged();
                // rola para a lista para o usuário ver o item recém-adicionado
                scrollIntoView(rConteudo, null);
            }
        });

        // Salvar/Atualizar
        mainBinding.cadastroUpdate.setOnClickListener(view -> {
            String nomeProcedimento = mainBinding.nomeField.getEditText() != null
                    ? mainBinding.nomeField.getEditText().getText().toString() : "";

            if (nomeProcedimento.isEmpty()) {
                LoadingUtils.showLoading2(this, "Atenção","Adicione o Nome do Procedimento.");
                return;
            }

            String id = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());

            procedimentoModel.setNomeProcedimento(nomeProcedimento);
            if (procedimentoModel.getId().isEmpty()) procedimentoModel.setId(id);
            procedimentoModel.setIdHospital(prefs.getString("id", ""));

            List<InfoProcedimento> listaInfoProcedimento = new ArrayList<>();
            for (ConteudoModel c : listaConteudo) {
                InfoProcedimento infoTitle = new InfoProcedimento();
                infoTitle.setInfo(c.getTitulo());
                infoTitle.setTipo(0);

                InfoProcedimento infoBody = new InfoProcedimento();
                infoBody.setInfo(c.getInfo());
                infoBody.setTipo(1);

                listaInfoProcedimento.add(infoTitle);
                listaInfoProcedimento.add(infoBody);
            }
            procedimentoModel.getListaInformacao().clear();
            procedimentoModel.getListaInformacao().addAll(listaInfoProcedimento);

            refProcedimentos.child(procedimentoModel.getId()).setValue(procedimentoModel)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Operação Concluída!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
        });

        // Botão AJUDA: “Carregando...” por 3s

        mainBinding.getRoot().postDelayed(() -> {
            prepararTutorial();
            mainBinding.tutorial.setText("AJUDA");
            mainBinding.tutorial.setEnabled(true);
        }, TUTORIAL_PREP_DELAY_MS);

        mainBinding.tutorial.setOnClickListener(v -> {
            Log.d(TAG_TUTORIAL, "AJUDA clicado");
            if (!tutorialReady) prepararTutorial();
            mostrarTutorial();
        });
    }

    // ---------- Carregar dados para edição ----------
    private void configurarDadosDoProcediementoEdit() {
        refProcedimentos.child(procedimentoModel.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        procedimentoModel = snapshot.getValue(ProcedimentoModel.class);
                        if (procedimentoModel == null) return;

                        if (mainBinding.nomeField.getEditText() != null) {
                            mainBinding.nomeField.getEditText().setText(procedimentoModel.getNomeProcedimento());
                        }

                        listaConteudo.clear();
                        for (int i = 0; i < procedimentoModel.getListaInformacao().size(); i += 2) {
                            ConteudoModel c = new ConteudoModel();
                            InfoProcedimento t = procedimentoModel.getListaInformacao().get(i);
                            InfoProcedimento b = procedimentoModel.getListaInformacao().get(i + 1);

                            if (t.getTipo() == 0) {
                                c.setTitulo(t.getInfo());
                                c.setInfo(b.getInfo());
                            } else {
                                c.setTitulo(b.getInfo());
                                c.setInfo(t.getInfo());
                            }
                            listaConteudo.add(c);
                        }
                        adapterConteudo.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
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

    // ====================== Tutorial ======================

    /** Monta a fila de passos; convertemos em TapTarget só na hora de exibir. */
    private void prepararTutorial() {
        steps.clear();

        TextInputLayout nome = mainBinding.nomeField;
        TextInputLayout titulo = mainBinding.tituloField;
        TextInputLayout conteudo = mainBinding.conteudoField;

        if (nome != null)
            steps.add(new Step(
                    nome,
                    "Nome do procedimento",
                    "Defina aqui o nome geral do POP (ex.: ‘Higienização das Mãos’).",
                    ID_NOME));

        if (titulo != null)
            steps.add(new Step(
                    titulo,
                    "Título do bloco",
                    "Título do conteúdo que você vai adicionar ao POP (ex.: ‘Objetivos’).",
                    ID_TITULO));

        if (conteudo != null)
            steps.add(new Step(
                    conteudo,
                    "Conteúdo do bloco",
                    "Texto do bloco selecionado. Escreva instruções, listas, etc.",
                    ID_CONTEUDO));

        if (mainBinding.pronto != null)
            steps.add(new Step(
                    mainBinding.pronto,
                    "Adicionar bloco",
                    "Toque aqui para adicionar o par Título + Conteúdo na lista abaixo.",
                    ID_PRONTO));

        if (rConteudo != null)
            steps.add(new Step(
                    rConteudo,
                    "Lista do procedimento",
                    "• Clique em **Remover** para excluir o item\n•Clique em **Editar** para Editar o item.",
                    ID_LISTA));

        if (mainBinding.cadastroUpdate != null)
            steps.add(new Step(
                    mainBinding.cadastroUpdate,
                    "Salvar procedimento",
                    "Quando terminar, toque aqui para **salvar** o POP no hospital.",
                    ID_SALVAR));

        tutorialReady = true;
        Log.d(TAG_TUTORIAL, "Tutorial preparado. Passos: " + steps.size());
    }

    private TapTarget toTarget(Step s) {
        return TapTarget.forView(s.view, s.title, s.desc)
                .outerCircleColorInt(getColor(R.color.purple_700))
                .targetCircleColorInt(getColor(android.R.color.white))
                .textColorInt(getColor(android.R.color.white))
                .titleTextSize(20)
                .descriptionTextSize(16)
                .transparentTarget(true)
                .drawShadow(true)
                .cancelable(true)
                .id(s.id);
    }

    private void mostrarTutorial() {
        if (!tutorialReady || steps.isEmpty()) {
            prepararTutorial();
            if (steps.isEmpty()) return;
        }
        // copia para não alterar a fila base
        final Deque<Step> q = new ArrayDeque<>(steps);
        showNextStep(q);
    }

    /** Mostra o próximo passo — com auto-scroll se a view estiver fora da tela. */
    private void showNextStep(Deque<Step> q) {
        if (q.isEmpty()) return;
        Step next = q.pollFirst();
        if (next == null || next.view == null) {
            showNextStep(q);
            return;
        }

        // Garante que o alvo esteja visível; só depois abre o TapTarget.
        scrollIntoView(next.view, () -> TapTargetView.showFor(
                this,
                toTarget(next),
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
                        showNextStep(q);
                    }
                }
        ));
    }

    /** Faz scroll suave se a view alvo estiver fora da viewport do NestedScrollView. */
    private void scrollIntoView(View target, Runnable afterScroll) {
        // Nosso root é um NestedScrollView
        final androidx.core.widget.NestedScrollView scroll = (androidx.core.widget.NestedScrollView) mainBinding.getRoot();

        // Pega posição relativa do alvo ao content do scroll
        int targetTop = target.getTop();
        int targetBottom = target.getBottom();

        int scrollY = scroll.getScrollY();
        int viewportH = scroll.getHeight();

        boolean above = targetTop < scrollY;
        boolean below = targetBottom > (scrollY + viewportH);

        if (above || below) {
            int y = Math.max(targetTop - dp(24), 0); // uma margem pra respirar
            scroll.smoothScrollTo(0, y);
            scroll.postDelayed(() -> { if (afterScroll != null) afterScroll.run(); }, TUTORIAL_SCROLL_WAIT_MS);
        } else {
            if (afterScroll != null) afterScroll.run();
        }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    // ---------------- Navegação ----------------
    @Override
    public void onBackPressed() { perguntarSaida(); }

    private void perguntarSaida() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Aviso!");
        b.setMessage("Você quer realmente sair da tela atual?");
        b.setNegativeButton("Não", null);
        b.setPositiveButton("Sim", (d, i) -> finish());
        b.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) perguntarSaida();
        return super.onOptionsItemSelected(item);
    }
}
