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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import benicio.solucoes.enfermaguia.adapter.AdapterHospitais;
import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutCriarSugestaoBinding;
import benicio.solucoes.enfermaguia.model.FeedbackModel;
import benicio.solucoes.enfermaguia.model.SugestaoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.LoadingUtils;

public class HallActivity extends AppCompatActivity {

    // ==================== ESTADO DO TUTORIAL ====================
    /**
     * Fila PREPARADA (alvos resolvidos), não exibida até o usuário clicar em Ajuda
     */
    private final Deque<TapTarget> preparedQueue = new ArrayDeque<>();
    private boolean tutorialPrepared = false;   // já temos todos os alvos prontos?
    private boolean preparingTutorial = false;  // evita corrida de preparação
    private static final int ID_FAVORITAR = 4;  // id lógico do alvo "Favoritar hospital"

    /**
     * Callback pendente para mostrar quando a preparação terminar
     */
    private SimpleCallback pendingAfterPrepared = null;

    /**
     * Observers só para PRÉ-CÁLCULO (resolução da estrela do 1º item)
     */
    private RecyclerView.AdapterDataObserver dataObserverPrep;
    private RecyclerView.OnChildAttachStateChangeListener childAttachListenerPrep;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListenerPrep;

    /**
     * Handler UI
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ==================== OUTROS CAMPOS EXISTENTES ====================
    int newCountFeedbacks = 0;
    public static boolean selecaoAtiva = false;

    MenuItem itemFeedbackMenu;
    public static String nomeUsuario = "";
    public static Dialog dialogSugestao;
    public static List<UsuarioModel> listaHospitais = new ArrayList<>();
    private ActivityHallBinding mainBinding;

    private final DatabaseReference refFeedbacks = FirebaseDatabase.getInstance().getReference().child("feedbacks");
    public static DatabaseReference refSugestoes = FirebaseDatabase.getInstance().getReference().child("sugestoes");
    private final DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public static TextView avisoSelecionarHospital;
    public static RecyclerView recyclerHospital;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private boolean menuInflado = false;

    // ==================== CICLO DE VIDA ====================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHallBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                showExitDialog();
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();
        editor.putString("idHospitalSelecionado", "").apply();

        verificarNovosFeedbacks();
        pegarNomeUsuario();

        drawerLayout = mainBinding.drawerLayout;
        navigationView = mainBinding.navigationView;
        toolbar = mainBinding.toolbar;

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.sugerir_pop) {
                if (prefs.getString("idHospitalSelecionado", "").isEmpty()) {
                    LoadingUtils.showLoading2(this, "Atenção!", "Selecione um hospital primeiro antes de querer enviar uma sugestão de POP");
                    selecaoAtiva = true;
                } else {
                    HallActivity.showSugerirPOP(this);
                }
            } else if (item.getItemId() == R.id.procedimentos_hospital) {
                startActivity(new Intent(this, FavoritoProcedimentosActivity.class));
            } else if (item.getItemId() == R.id.favoritos_hospital) {
                startActivity(new Intent(this, FavoritoHospitaisActivity.class));
            } else if (item.getItemId() == R.id.historico_pop) {
                startActivity(new Intent(this, HistoricoProcedimentosActivity.class));
            } else if (item.getItemId() == R.id.menu_pdf_usuario) {
                try {
                    // 1. Copia o PDF do raw para o cache interno
                    InputStream inputStream = getResources().openRawResource(R.raw.usuario);
                    File outFile = new File(getCacheDir(), "usuario.pdf");

                    FileOutputStream outputStream = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                    // 2. Pega o URI via FileProvider
                    Uri pdfUri = FileProvider.getUriForFile(
                            this,                           // contexto da Activity
                            getPackageName() + ".provider", // <= BATE COM O TEU MANIFEST
                            outFile
                    );

                    // 3. Cria o intent para abrir o PDF
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(pdfUri, "application/pdf");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(intent, "Abrir manual do usuário"));

                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Nenhum aplicativo para abrir PDF encontrado.", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Erro ao abrir o PDF.", Toast.LENGTH_LONG).show();
                }
            } else if (item.getItemId() == R.id.menu_ajuda_usuario) {
                // MOSTRAR o tutorial apenas quando o usuário pedir Ajuda
                if (!tutorialPrepared && !preparingTutorial) {
                    preResolveTutorialTargetsSilently(this::showPreparedTutorial);
                } else if (preparingTutorial) {
                    // registra para mostrar assim que terminar de preparar
                    pendingAfterPrepared = this::showPreparedTutorial;
                } else {
                    showPreparedTutorial();
                }
            } else if (item.getItemId() == R.id.menu_creditos_usuario) {
                startActivity(new Intent(this, CreditosActivity.class));
            } else if (item.getItemId() == R.id.menu_sair_usuario) {
                AlertDialog.Builder builder_saida = new AlertDialog.Builder(HallActivity.this);
                builder_saida.setTitle("Sair do aplicativo?")
                        .setMessage("Deseja realmente sair do aplicativo?")
                        .setCancelable(false);
                builder_saida.setNegativeButton("Não", null);
                builder_saida.setPositiveButton("Sim", (d, i) -> {
                    finish();
                    editor.putString("id", "").apply();
                    startActivity(new Intent(this, MainActivity.class));
                });
                builder_saida.create().show();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        avisoSelecionarHospital = mainBinding.textView9;
        configurarDialogSelecionarHospital();
    }

    @Override
    protected void onStart() {
        super.onStart();
        configurrarRVselecionarHospital();

        // PRÉ-CARREGAR o tutorial assim que o app abre (NÃO mostrar)
        preResolveTutorialTargetsSilently(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachPrepObservers();
        pendingAfterPrepared = null;
    }

    // ==================== SUGERIR POP ====================
    public static void showSugerirPOP(Activity a) {
        AlertDialog.Builder b = new AlertDialog.Builder(a);
        b.setCancelable(false);

        LayoutCriarSugestaoBinding criarSugestaoBinding = LayoutCriarSugestaoBinding.inflate(a.getLayoutInflater());
        criarSugestaoBinding.title.setText("Sugerir um novo POP para " + prefs.getString("nomeHospitalSelecionado", ""));
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
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                String dataAtual = sdf.format(new Date());

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
                        showLoading2(a, "Parabéns!", "Sua sugestão foi enviada com sucesso!");
                        dialogSugestao.dismiss();
                    }
                });
            }
        });

        b.setView(criarSugestaoBinding.getRoot());
        dialogSugestao = b.create();
        dialogSugestao.show();
    }

    // ==================== RECYCLER / CARREGAMENTO HOSPITAIS ====================
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
                        if (hospital != null && hospital.isAdmin()) {
                            listaHospitais.add(hospital);
                        }
                    }
                    adapterHospitais.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void configurarDialogSelecionarHospital() {
        // reservado
    }

    // ==================== MENUS / FEEDBACKS ====================
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sair_conta) {
            AlertDialog.Builder builder_saida = new AlertDialog.Builder(HallActivity.this);
            builder_saida.setTitle("Sair do aplicativo?")
                    .setMessage("Deseja realmente sair do aplicativo?")
                    .setCancelable(false);
            builder_saida.setNegativeButton("Não", null);
            builder_saida.setPositiveButton("Sim", (d, i) -> {
                finish();
                editor.putString("id", "").apply();
                startActivity(new Intent(this, MainActivity.class));
            });
            builder_saida.create().show();
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
        menuInflado = true;
        return super.onCreateOptionsMenu(menu);
    }

    private void pegarNomeUsuario() {
        DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
        refUsuarios.child(prefs.getString("id", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UsuarioModel u = snapshot.getValue(UsuarioModel.class);
                    if (u != null) nomeUsuario = Objects.requireNonNull(u).getNome();
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
                    newCountFeedbacks = 0;
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
                .setPositiveButton("Sim", (dialog, which) -> finishAffinity())
                .setNegativeButton("Não", null)
                .setCancelable(true)
                .show();
    }

    // ==================== PRÉ-CÁLCULO DO TUTORIAL (sem mostrar) ====================

    /**
     * Callback simples compatível com projetos sem lambdas
     */
    interface SimpleCallback {
        void run();
    }

    /**
     * Pré-resolve todos os alvos do tutorial SEM mostrar nada. Chame no onStart.
     */
    private void preResolveTutorialTargetsSilently(SimpleCallback afterPrepared) {
        // Se já estamos preparando, apenas registra callback e sai
        if (preparingTutorial) {
            if (afterPrepared != null) pendingAfterPrepared = afterPrepared;
            return;
        }

        // Garante que o menu (toolbar items) já existe. NÃO travar preparingTutorial aqui.
        if (!menuInflado) {
            if (afterPrepared != null) pendingAfterPrepared = afterPrepared;
            mainHandler.postDelayed(() -> preResolveTutorialTargetsSilently(pendingAfterPrepared), 60);
            return;
        }

        preparingTutorial = true;
        tutorialPrepared = false;
        preparedQueue.clear();

        // 1) Targets de toolbar — já podem ser preparados agora
        TapTarget navTarget = TapTarget.forToolbarNavigationIcon(
                        toolbar, "Menu lateral", "Abra o menu com favoritos, histórico, ajuda e outras opções.")
                .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                .outerCircleAlpha(0.96f)
                .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                .titleTextSize(20).descriptionTextSize(16)
                .transparentTarget(true)
                .drawShadow(true)
                .cancelable(true)
                .id(1);

        TapTarget emailTarget = TapTarget.forToolbarMenuItem(
                        toolbar, R.id.go_to_feedbacks,
                        "Feedbacks", "Veja suas mensagens e avisos. Se aparecer novo ícone, tem novidade.")
                .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                .outerCircleAlpha(0.96f)
                .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                .titleTextSize(20).descriptionTextSize(16)
                .transparentTarget(true)
                .drawShadow(true)
                .cancelable(true)
                .id(2);

        TapTarget exitTarget = TapTarget.forToolbarMenuItem(
                        toolbar, R.id.sair_conta,
                        "Sair do aplicativo", "Encerra a sessão e volta para a tela inicial.")
                .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                .outerCircleAlpha(0.96f)
                .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                .titleTextSize(20).descriptionTextSize(16)
                .transparentTarget(true)
                .drawShadow(true)
                .cancelable(true)
                .id(3);

        preparedQueue.add(navTarget);
        preparedQueue.add(emailTarget);
        preparedQueue.add(exitTarget);

        // 2) Resolver o alvo "Favoritar hospital" usando a view REAL
        if (recyclerHospital == null) {
            // sem recycler ainda; finaliza preparação parcial
            finishPrepare(afterPrepared);
            return;
        }

        // rola pro item 0 para garantir que a célula exista
        RecyclerView.LayoutManager lm = recyclerHospital.getLayoutManager();
        if (lm instanceof LinearLayoutManager) {
            ((LinearLayoutManager) lm).scrollToPositionWithOffset(0, 0);
        } else {
            recyclerHospital.scrollToPosition(0);
        }

        attachPrepObservers();

        // tenta resolver imediatamente
        recyclerHospital.post(() -> {
            View star = findFirstStarFromRecyclerPrepared();
            if (star != null) {
                preparedQueue.add(buildFavoritarTarget(star));
                finishPrepare(afterPrepared);
                return;
            }
            // se ainda não existe, aguardamos pelos observers
            if (globalLayoutListenerPrep == null) {
                globalLayoutListenerPrep = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        resolveFavoritarIfPossible();
                        recyclerHospital.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        globalLayoutListenerPrep = null;
                    }
                };
                recyclerHospital.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListenerPrep);
            }
        });
    }

    private void finishPrepare(SimpleCallback afterPrepared) {
        preparingTutorial = false;
        tutorialPrepared = true;
        detachPrepObservers();

        // prioriza callback explícito; senão usa o pendente
        SimpleCallback cb = (afterPrepared != null) ? afterPrepared : pendingAfterPrepared;
        pendingAfterPrepared = null;
        if (cb != null) cb.run();
    }

    private void attachPrepObservers() {
        if (recyclerHospital == null) return;

        if (dataObserverPrep == null && recyclerHospital.getAdapter() != null) {
            dataObserverPrep = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    resolveFavoritarIfPossible();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    resolveFavoritarIfPossible();
                }
            };
            recyclerHospital.getAdapter().registerAdapterDataObserver(dataObserverPrep);
        }

        if (childAttachListenerPrep == null) {
            childAttachListenerPrep = new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(@NonNull View view) {
                    resolveFavoritarIfPossible();
                }

                @Override
                public void onChildViewDetachedFromWindow(@NonNull View view) { /* noop */ }
            };
            recyclerHospital.addOnChildAttachStateChangeListener(childAttachListenerPrep);
        }
    }

    private void detachPrepObservers() {
        if (recyclerHospital != null) {
            if (dataObserverPrep != null && recyclerHospital.getAdapter() != null) {
                recyclerHospital.getAdapter().unregisterAdapterDataObserver(dataObserverPrep);
            }
            if (childAttachListenerPrep != null) {
                recyclerHospital.removeOnChildAttachStateChangeListener(childAttachListenerPrep);
            }
            if (globalLayoutListenerPrep != null) {
                recyclerHospital.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListenerPrep);
            }
        }
        dataObserverPrep = null;
        childAttachListenerPrep = null;
        globalLayoutListenerPrep = null;
    }

    /**
     * Tenta resolver a estrela e finalizar a preparação apenas uma vez
     */
    private void resolveFavoritarIfPossible() {
        if (!preparingTutorial) return;
        if (tutorialPrepared) return;

        View star = findFirstStarFromRecyclerPrepared();
        if (star != null) {
            preparedQueue.add(buildFavoritarTarget(star));
            finishPrepare(null);
        }
    }

    /**
     * Ajuste o ID da estrela do seu item aqui se for diferente
     */
    private View findFirstStarFromRecyclerPrepared() {
        if (recyclerHospital == null) return null;
        RecyclerView.ViewHolder vh = recyclerHospital.findViewHolderForAdapterPosition(0);
        if (vh == null) return null;

        // Seu XML mostra exatamente @+id/favoritarHospital — então está correto:
        View star = vh.itemView.findViewById(R.id.favoritarHospital);
        if (star == null) {
            int alt = getResources().getIdentifier("btn_favorito", "id", getPackageName());
            if (alt != 0) star = vh.itemView.findViewById(alt);
        }
        return star;
    }

    private TapTarget buildFavoritarTarget(@NonNull View starView) {
        return TapTarget.forView(
                        starView,
                        "Favoritar hospital",
                        "Toque na estrela para salvar o hospital e acessar mais rápido.")
                .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                .outerCircleAlpha(0.96f)
                .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                .titleTextSize(20).descriptionTextSize(16)
                .transparentTarget(true)
                .drawShadow(true)
                .cancelable(true)
                .id(ID_FAVORITAR);
    }

    // ==================== EXIBIÇÃO (somente quando usuário clica em Ajuda) ====================
    private void showPreparedTutorial() {
        if (!tutorialPrepared) {
            // prepara e chama de novo quando terminar
            preResolveTutorialTargetsSilently(this::showPreparedTutorial);
            return;
        }
        if (preparedQueue.isEmpty()) {
            // tentativa final de preparar “on demand”
            preResolveTutorialTargetsSilently(this::showPreparedTutorial);
            return;
        }

        // Copia para execução (preserva a fila preparada para próximas vezes, se quiser)
        final Deque<TapTarget> queueToRun = new ArrayDeque<>(preparedQueue);
        showNextTargetFromQueue(queueToRun);
    }

    private void showNextTargetFromQueue(Deque<TapTarget> queue) {
        if (queue.isEmpty()) return;

        TapTarget next = queue.pollFirst();

        // Segurança extra: revalida a estrela no momento da exibição
        if (next != null && next.id() == ID_FAVORITAR) {
            View star = findFirstStarFromRecyclerPrepared();
            if (star == null) {
                // se não existir agora, pula essa etapa (ou requeue, se preferir)
                showNextTargetFromQueue(queue);
                return;
            } else {
                next = buildFavoritarTarget(star);
            }
        }

        final TapTarget targetToShow = next;
        if (targetToShow == null) {
            showNextTargetFromQueue(queue);
            return;
        }

        TapTargetView.showFor(
                this,
                targetToShow,
                new TapTargetView.Listener() {
                    @Override
                    public void onTargetClick(TapTargetView view) {
                        super.onTargetClick(view);
                        view.dismiss(true);
                    }

                    @Override
                    public void onOuterCircleClick(TapTargetView view) {
                        super.onOuterCircleClick(view);
                        view.dismiss(true);
                    }

                    @Override
                    public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                        // chama o próximo no loop da UI
                        mainHandler.post(() -> showNextTargetFromQueue(queue));
                    }
                }
        );
    }
}
