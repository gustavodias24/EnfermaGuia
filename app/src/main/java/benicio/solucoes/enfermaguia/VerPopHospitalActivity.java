package benicio.solucoes.enfermaguia;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityVerPopHospitalBinding;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.LoadingUtils;
import benicio.solucoes.enfermaguia.utils.PDFGenerator;

public class VerPopHospitalActivity extends AppCompatActivity {

    // ==================== DADOS ====================
    public static List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    public static AdapterProcedimentos adapterProcedimentos;
    public static String nomeHospital = "";

    private RecyclerView rProcedimentos;
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");

    public static TextView nomeHospitalTEXT;
    public static LinearLayout layoutProcedimentos;
    public static Button btnCompartilhar;

    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public static ActivityVerPopHospitalBinding mainBinding;

    // Modo de pesquisa (true = por nome, false = dentro do procedimento)
    private static boolean pesquisarPorNome = true;

    // ==================== TUTORIAL / LOGS ====================
    private static final String TAG_TUTORIAL = "mayara";
    private static final int ID_VER = 201;
    private static final int ID_FAV = 202;
    private static final int ID_CHECK = 203;
    private static final int ID_RADIO_NOME = 211;
    private static final int ID_RADIO_CONTEUDO = 212;
    private static final long TUTORIAL_PREP_DELAY_MS = 3000L; // 3s

    // fila preparada (não exibida até tocar em AJUDA)
    private final Deque<TapTarget> preparedQueue = new ArrayDeque<>();
    private boolean tutorialPrepared = false;
    private boolean preparingTutorial = false;
    private SimpleCallback pendingAfterPrepared = null;

    private RecyclerView.AdapterDataObserver dataObserverPrep;
    private RecyclerView.OnChildAttachStateChangeListener childAttachListenerPrep;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListenerPrep;

    interface SimpleCallback { void run(); }

    // ==================== CICLO DE VIDA ====================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityVerPopHospitalBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Voltar");
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Após 3s: prepara o tutorial (sem mexer no texto/estado do botão)
        mainBinding.tutorial.postDelayed(() -> {
            Log.d(TAG_TUTORIAL, "Delay de 3s concluído. Preparando tutorial…");
            mainBinding.tutorial.setText("Ajuda");
            preResolveTutorialTargetsSilently(() -> Log.d(TAG_TUTORIAL, "Preparação concluída após delay."));
        }, TUTORIAL_PREP_DELAY_MS);

        // Clique de AJUDA: se já preparado, mostra; senão prepara e mostra ao concluir
        mainBinding.tutorial.setOnClickListener(v -> {
            Log.d(TAG_TUTORIAL, "Botão AJUDA clicado. prepared=" + tutorialPrepared + " preparing=" + preparingTutorial);
            if (tutorialPrepared) {
                showPreparedTutorial();
            } else if (!preparingTutorial) {
                preResolveTutorialTargetsSilently(this::showPreparedTutorial);
            } else {
                pendingAfterPrepared = this::showPreparedTutorial;
            }
        });

        nomeHospitalTEXT = mainBinding.textView7;
        layoutProcedimentos = mainBinding.layoutProcedimentos;
        btnCompartilhar = mainBinding.compartilhar;
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        // Hints (opcional) + define flag pesquisarPorNome
        if (mainBinding.rbPorNome != null && mainBinding.edtPesquisar != null) {
            mainBinding.rbPorNome.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) mainBinding.edtPesquisar.setHint("Digite aqui para pesquisar...");
                pesquisarPorNome = true;
            });
        }
        if (mainBinding.rbDentroProcedimento != null && mainBinding.edtPesquisar != null) {
            mainBinding.rbDentroProcedimento.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) mainBinding.edtPesquisar.setHint("Pesquisar dentro do procedimento…");
                pesquisarPorNome = false;
            });
        }

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
                PDFGenerator.generateAndSharePDF(this, listaParaCompartilharProcedimento, "Procedimentos do Hospital " + nomeHospital);
            }
        });

        configurarRecyclerProcedimento();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachPrepObservers();
        pendingAfterPrepared = null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    // ==================== BUSCA ====================
    public void pesquisarProcedimento(View view){
        String query = mainBinding.edtPesquisar.getText().toString();
        LoadingUtils.showLoading(this);

        if (!query.isEmpty()){
            adapterProcedimentos.filter(query);
        } else {
            buscarProcedimentos(false, "");
        }
    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this, false);
        rProcedimentos.setAdapter(adapterProcedimentos);

        LoadingUtils.showLoading(this);
        buscarProcedimentos(false, "");
    }

    public static void buscarProcedimentos(boolean filter, String query) {
        refProcedimentos.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                LoadingUtils.dismissLoading();
                if (snapshot.exists()) {
                    listaProcedimento.clear();
                    mainBinding.textView12.setVisibility(View.VISIBLE);
                    String q = (query != null) ? query.toLowerCase().trim() : "";

                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel procedimentoModel = dado.getValue(ProcedimentoModel.class);
                        if (procedimentoModel == null) continue;

                        if (procedimentoModel.getIdHospital() != null &&
                                procedimentoModel.getIdHospital().equals(prefs.getString("idHospitalSelecionado", ""))) {

                            boolean matches = true;

                            if (filter && !q.isEmpty()) {
                                if (pesquisarPorNome) {
                                    matches = contemQueryPorNome(procedimentoModel, q);
                                } else {
                                    matches = contemQueryEmQualquerCampo(procedimentoModel, q);
                                }
                            }

                            if (matches) {
                                listaProcedimento.add(procedimentoModel);
                                mainBinding.textView12.setVisibility(View.GONE);
                            }
                        }
                    }
                    adapterProcedimentos.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                LoadingUtils.dismissLoading();
            }
        });
    }

    private static boolean contemQueryPorNome(ProcedimentoModel p, String query) {
        return verificaTexto(p.getNomeProcedimento(), query);
    }

    private static boolean contemQueryEmQualquerCampo(ProcedimentoModel p, String query) {
        try {
            if (verificaTexto(p.getNomeProcedimento(), query)) return true;
            if (verificaTexto(p.getId(), query)) return true;
            if (verificaTexto(p.getIdHospital(), query)) return true;
            if (verificaTexto(String.valueOf(p.getAcessos()), query)) return true;
            if (verificaTexto(String.valueOf(p.getCompartilhamentos()), query)) return true;
            if (verificaTexto(String.valueOf(p.getSugestoes()), query)) return true;

            if (p.getListaInformacao() != null) {
                for (InfoProcedimento info : p.getListaInformacao()) {
                    if (verificaTexto(info.getInfo(), query)) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+", "");
        n = n.toLowerCase();
        n = n.replaceAll("[^a-z0-9]+", " ");
        return n.trim();
    }

    private static boolean verificaTexto(String valor, String query) {
        if (valor == null || query == null) return false;
        String nv = normalize(valor);
        String nq = normalize(query);
        if (nq.isEmpty() || nv.isEmpty()) return false;
        if (nv.contains(nq)) return true;
        String[] tokens = nv.split("\\s+");
        for (String t : tokens) {
            if (t.startsWith(nq)) return true;
        }
        return false;
    }

    // ==================== TUTORIAL: PRÉ-RESOLUÇÃO (sem mostrar) ====================
    private void preResolveTutorialTargetsSilently(@Nullable SimpleCallback afterPrepared) {
        if (preparingTutorial) {
            if (afterPrepared != null) pendingAfterPrepared = afterPrepared;
            Log.d(TAG_TUTORIAL, "preResolve: já preparando; callback pendente registrado");
            return;
        }

        preparingTutorial = true;
        tutorialPrepared = false;
        preparedQueue.clear();
        Log.d(TAG_TUTORIAL, "preResolve: iniciando");

        // 1) Alvos fixos (botões e campo)
        preparedQueue.add(
                TapTarget.forView(
                                mainBinding.compartilhar,
                                "Compartilhar selecionados",
                                "Marque procedimentos e toque aqui para gerar um PDF e compartilhar.")
                        .outerCircleColorInt(getColorCompat(R.color.purple_700))
                        .targetCircleColorInt(getColorCompat(android.R.color.white))
                        .textColorInt(getColorCompat(android.R.color.white))
                        .transparentTarget(true)
                        .id(1)
        );
        preparedQueue.add(
                TapTarget.forView(
                                mainBinding.edtPesquisar,
                                "Pesquisar",
                                "Busque por nome ou dentro do conteúdo do procedimento.")
                        .outerCircleColorInt(getColorCompat(R.color.purple_700))
                        .targetCircleColorInt(getColorCompat(android.R.color.white))
                        .textColorInt(getColorCompat(android.R.color.white))
                        .transparentTarget(true)
                        .id(2)
        );

        // 1.1) Radios explicativos
        preparedQueue.add(
                TapTarget.forView(
                                mainBinding.rbPorNome,
                                "Modo de busca: Por nome",
                                "Filtra usando apenas o NOME do procedimento. Rápido e direto.")
                        .outerCircleColorInt(getColorCompat(R.color.purple_700))
                        .targetCircleColorInt(getColorCompat(android.R.color.white))
                        .textColorInt(getColorCompat(android.R.color.white))
                        .transparentTarget(true)
                        .id(ID_RADIO_NOME)
        );
        preparedQueue.add(
                TapTarget.forView(
                                mainBinding.rbDentroProcedimento,
                                "Modo de busca: Dentro do procedimento",
                                "Procura o termo no conteúdo do POP (texto interno). Útil quando lembra uma frase específica.")
                        .outerCircleColorInt(getColorCompat(R.color.purple_700))
                        .targetCircleColorInt(getColorCompat(android.R.color.white))
                        .textColorInt(getColorCompat(android.R.color.white))
                        .transparentTarget(true)
                        .id(ID_RADIO_CONTEUDO)
        );

        // 2) Preparar alvos do item 0 do Recycler (garante visibilidade)
        if (rProcedimentos == null || rProcedimentos.getAdapter() == null) {
            Log.d(TAG_TUTORIAL, "preResolve: recycler/adapter nulos; finalizando parcial");
            finishPrepare(afterPrepared);
            return;
        }

        RecyclerView.LayoutManager lm = rProcedimentos.getLayoutManager();
        if (lm instanceof LinearLayoutManager) {
            ((LinearLayoutManager) lm).scrollToPositionWithOffset(0, 0);
        } else {
            rProcedimentos.scrollToPosition(0);
        }

        attachPrepObservers();

        rProcedimentos.post(() -> {
            View ver = findChildInViewHolder(0, R.id.btn_ir_ver_procedimento);
            View fav = findChildInViewHolder(0, R.id.favoritarProcecimento);
            View chk = findChildInViewHolder(0, R.id.checkBoxMarcarCompartilhar);
            Log.d(TAG_TUTORIAL, "preResolve/post: ver=" + (ver != null) + " fav=" + (fav != null) + " chk=" + (chk != null));

            if (ver != null) preparedQueue.add(buildTapForView(ver, "Ver procedimento", "Abra o POP para leitura.", ID_VER));
            if (fav != null) preparedQueue.add(buildTapForView(fav, "Favoritar procedimento", "Salve este POP nos seus favoritos.", ID_FAV));
            if (chk != null) preparedQueue.add(buildTapForView(chk, "Marcar para compartilhar", "Selecione para incluir no PDF.", ID_CHECK));

            if (ver != null || fav != null || chk != null) {
                finishPrepare(afterPrepared);
            } else {
                // tenta novamente ao concluir o layout/anexos
                if (globalLayoutListenerPrep == null) {
                    globalLayoutListenerPrep = new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override public void onGlobalLayout() {
                            resolveRecyclerTargetsIfPossible();
                            rProcedimentos.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            globalLayoutListenerPrep = null;
                        }
                    };
                    rProcedimentos.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListenerPrep);
                }
            }
        });
    }

    private void finishPrepare(@Nullable SimpleCallback afterPrepared) {
        preparingTutorial = false;
        tutorialPrepared = true;
        detachPrepObservers();
        Log.d(TAG_TUTORIAL, "preResolve: finalizado. queue=" + preparedQueue.size());

        SimpleCallback cb = (afterPrepared != null) ? afterPrepared : pendingAfterPrepared;
        pendingAfterPrepared = null;
        if (cb != null) cb.run();
    }

    private void attachPrepObservers() {
        if (rProcedimentos == null) return;

        if (dataObserverPrep == null && rProcedimentos.getAdapter() != null) {
            dataObserverPrep = new RecyclerView.AdapterDataObserver() {
                @Override public void onChanged() { resolveRecyclerTargetsIfPossible(); }
                @Override public void onItemRangeInserted(int positionStart, int itemCount) { resolveRecyclerTargetsIfPossible(); }
            };
            rProcedimentos.getAdapter().registerAdapterDataObserver(dataObserverPrep);
        }

        if (childAttachListenerPrep == null) {
            childAttachListenerPrep = new RecyclerView.OnChildAttachStateChangeListener() {
                @Override public void onChildViewAttachedToWindow(@NonNull View view) { resolveRecyclerTargetsIfPossible(); }
                @Override public void onChildViewDetachedFromWindow(@NonNull View view) { /* noop */ }
            };
            rProcedimentos.addOnChildAttachStateChangeListener(childAttachListenerPrep);
        }
    }

    private void detachPrepObservers() {
        if (rProcedimentos != null) {
            if (dataObserverPrep != null && rProcedimentos.getAdapter() != null) {
                rProcedimentos.getAdapter().unregisterAdapterDataObserver(dataObserverPrep);
            }
            if (childAttachListenerPrep != null) {
                rProcedimentos.removeOnChildAttachStateChangeListener(childAttachListenerPrep);
            }
            if (globalLayoutListenerPrep != null) {
                rProcedimentos.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListenerPrep);
            }
        }
        dataObserverPrep = null;
        childAttachListenerPrep = null;
        globalLayoutListenerPrep = null;
    }

    private void resolveRecyclerTargetsIfPossible() {
        if (!preparingTutorial || tutorialPrepared) return;

        boolean added = false;

        View ver = findChildInViewHolder(0, R.id.btn_ir_ver_procedimento);
        View fav = findChildInViewHolder(0, R.id.favoritarProcecimento);
        View chk = findChildInViewHolder(0, R.id.checkBoxMarcarCompartilhar);
        Log.d(TAG_TUTORIAL, "resolveIfPossible: ver=" + (ver != null) + " fav=" + (fav != null) + " chk=" + (chk != null));

        if (ver != null) { preparedQueue.add(buildTapForView(ver, "Ver procedimento", "Abra o POP para leitura.", ID_VER)); added = true; }
        if (fav != null) { preparedQueue.add(buildTapForView(fav, "Favoritar procedimento", "Salve este POP nos seus favoritos.", ID_FAV)); added = true; }
        if (chk != null) { preparedQueue.add(buildTapForView(chk, "Marcar para compartilhar", "Selecione para incluir no PDF.", ID_CHECK)); added = true; }

        if (added) finishPrepare(null);
    }

    /** Obtém a view filha de um ViewHolder em 'position' e só retorna se estiver anexada à janela */
    private @Nullable View findChildInViewHolder(int position, int childId) {
        if (rProcedimentos == null) return null;

        RecyclerView.ViewHolder vh = rProcedimentos.findViewHolderForAdapterPosition(position);
        if (vh == null) {
            rProcedimentos.scrollToPosition(position);
            vh = rProcedimentos.findViewHolderForAdapterPosition(position);
            if (vh == null) {
                Log.d(TAG_TUTORIAL, "findChild: ViewHolder pos" + position + " ainda null");
                return null;
            }
        }

        View child = vh.itemView.findViewById(childId);
        Log.d(TAG_TUTORIAL, "findChild: id=" + childId + " found=" + (child != null));
        if (child != null && !ViewCompat.isAttachedToWindow(child)) {
            Log.d(TAG_TUTORIAL, "findChild: child não está attachedToWindow");
            return null;
        }
        return child;
    }

    private TapTarget buildTapForView(@NonNull View v, String title, String desc, int id) {
        return TapTarget.forView(v, title, desc)
                .outerCircleColorInt(getColorCompat(R.color.purple_700))
                .targetCircleColorInt(getColorCompat(android.R.color.white))
                .textColorInt(getColorCompat(android.R.color.white))
                .titleTextSize(20).descriptionTextSize(16)
                .transparentTarget(true)
                .drawShadow(true)
                .cancelable(true)
                .id(id);
    }

    private int getColorCompat(int id) {
        return getResources().getColor(id, getTheme());
    }

    // ==================== EXIBIÇÃO (somente quando tocar em AJUDA) ====================
    private void showPreparedTutorial() {
        Log.d(TAG_TUTORIAL, "showPreparedTutorial: prepared=" + tutorialPrepared + " queue=" + preparedQueue.size());
        if (!tutorialPrepared || preparedQueue.isEmpty()) {
            Log.d(TAG_TUTORIAL, "showPreparedTutorial: não preparado/vazio. Preparando agora…");
            preResolveTutorialTargetsSilently(this::showPreparedTutorial);
            return;
        }

        final Deque<TapTarget> queue = new ArrayDeque<>(preparedQueue);
        showNext(queue);
    }

    private void showNext(Deque<TapTarget> queue) {
        if (queue.isEmpty()) {
            Log.d(TAG_TUTORIAL, "showNext: fim do tutorial");
            return;
        }

        TapTarget next = queue.pollFirst();
        Log.d(TAG_TUTORIAL, "showNext: exibindo id=" + (next != null ? next.id() : -1));

        // Revalida alvos do Recycler (caso a lista tenha mudado entre preparar e exibir)
        if (next != null && (next.id() == ID_VER || next.id() == ID_FAV || next.id() == ID_CHECK)) {
            int targetId =
                    next.id() == ID_VER ? R.id.btn_ir_ver_procedimento :
                            next.id() == ID_FAV ? R.id.favoritarProcecimento :
                                    R.id.checkBoxMarcarCompartilhar;

            View v = findChildInViewHolder(0, targetId);
            if (v == null) {
                Log.d(TAG_TUTORIAL, "showNext: alvo do Recycler sumiu (id=" + targetId + "). Pulando.");
                showNext(queue);
                return;
            }
            String title = next.id() == ID_VER ? "Ver procedimento" :
                    next.id() == ID_FAV ? "Favoritar procedimento" :
                            "Marcar para compartilhar";
            String desc  = next.id() == ID_VER ? "Abra o POP para leitura." :
                    next.id() == ID_FAV ? "Salve este POP nos seus favoritos." :
                            "Selecione para incluir no PDF.";
            next = buildTapForView(v, title, desc, next.id());
        }

        final TapTarget target = next;
        TapTargetView.showFor(
                this,
                target,
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
                        Log.d(TAG_TUTORIAL, "onTargetDismissed: próximo passo");
                        showNext(queue);
                    }
                }
        );
    }
}
