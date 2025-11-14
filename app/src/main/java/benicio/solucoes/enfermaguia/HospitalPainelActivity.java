package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityHospitalPainelBinding;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.PDFGenerator;

public class HospitalPainelActivity extends AppCompatActivity {

    // ---------- Logs/Tutorial ----------
    private static final String TAG_TUTORIAL = "mayara";
    private static final long TUTORIAL_PREP_DELAY_MS = 3000L;

    // IDs TapTarget
    private static final int ID_NAV = 9001;
    private static final int ID_SHARE = 9002;
    private static final int ID_FAB = 9003;
    private static final int ID_FAB_CREATE = 9004;
    private static final int ID_FAB_SUG = 9005;
    private static final int ID_FAB_MET = 9006;

    private static final int ID_ITEM_CHECK = 9010;
    private static final int ID_ITEM_VER = 9011;
    private static final int ID_ITEM_FAV = 9012;
    private static final int ID_ITEM_EDIT = 9013;
    private static final int ID_ITEM_DEL = 9014;
    private static final int ID_ITEM_DUP = 9015;

    private final Deque<TapTarget> preparedQueue = new ArrayDeque<>();
    private boolean preparingTutorial = false;

    // ---------- UI/Dados ----------
    private FloatingActionMenu actionMenu;
    private FloatingActionButton actionButtonRef;
    private ImageView fabIconView; // alvo do TapTarget do FAB
    private SubActionButton buttonCriarProcedimentoRef;
    private SubActionButton buttonSugestoesRef;
    private SubActionButton buttonMetricasRef;

    private View fabOverlay;

    private final DatabaseReference refProcedimentos =
            FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private ActivityHospitalPainelBinding mainBinding;

    private RecyclerView rProcedimentos;
    private final List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    private AdapterProcedimentos adapterProcedimentos;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    // -------- helper dp -> px --------
    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHospitalPainelBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Drawer/Toolbar
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_criar_procedimento) {
                startActivity(new Intent(this, CriarProcedimentoActivity.class));
            } else if (item.getItemId() == R.id.menu_sugestoes) {
                startActivity(new Intent(this, VerSugestoesActivity.class));
            } else if (item.getItemId() == R.id.menu_metricas) {
                startActivity(new Intent(this, MetricasA.class));
            }
            else if (item.getItemId() == R.id.menu_pdf_hospital){
                try {
                    // 1. Copia o PDF do raw para o cache interno
                    InputStream inputStream = getResources().openRawResource(R.raw.hospital);
                    File outFile = new File(getCacheDir(), "hospital.pdf");

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
            }
            else if (item.getItemId() == R.id.menu_ajuda) {
                // Reconstrói a fila SEM travar, incluindo itens do adapter
                Log.d(TAG_TUTORIAL, "Ajuda: reconstruindo fila do tutorial");
                preResolveTutorialTargetsSilently(this::showPreparedTutorial);
            } else if (item.getItemId() == R.id.menu_creditos) {
                startActivity(new Intent(this, CreditosActivity.class));
            } else if (item.getItemId() == R.id.menu_sair) {
                finish();
                if (editor != null) editor.putString("id", "").apply();
                startActivity(new Intent(this, MainActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Prefs/títulos
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(prefs.getString("nomeUser", ""));
        }
        mainBinding.textView.setText("Painel de Procedimentos do Hospital " + prefs.getString("nomeUser", ""));

        // Recycler
        configurarRecyclerProcedimento();

        mainBinding.compartilhar.setOnClickListener(view -> {
            List<ProcedimentoModel> listaParaCompartilharProcedimento = new ArrayList<>();
            for (ProcedimentoModel procedimento : listaProcedimento) {
                if (procedimento.isChecado()) listaParaCompartilharProcedimento.add(procedimento);
            }
            if (listaParaCompartilharProcedimento.isEmpty()) {
                Toast.makeText(this, "Selecione pelo menos 1 procedimento!", Toast.LENGTH_SHORT).show();
            } else {
                PDFGenerator.generateAndSharePDF(this, listaParaCompartilharProcedimento,
                        "Procedimentos do Hospital " + prefs.getString("nomeUser", ""));
            }
        });

        // FAB/menu circular
        fabOverlay = new View(this);
        fabOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fabOverlay.setClickable(true);
        fabOverlay.setFocusable(true);
        fabOverlay.setVisibility(View.GONE);
        fabOverlay.setBackgroundColor(Color.TRANSPARENT);
        ((ViewGroup) findViewById(android.R.id.content)).addView(fabOverlay);

        fabIconView = new ImageView(this);
        fabIconView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_24));

        actionButtonRef = new FloatingActionButton.Builder(this)
                .setContentView(fabIconView)
                .build();

        FrameLayout.LayoutParams fabLp = (FrameLayout.LayoutParams) actionButtonRef.getLayoutParams();
        fabLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        fabLp.rightMargin = dp(16);
        fabLp.bottomMargin = dp(16);
        actionButtonRef.setLayoutParams(fabLp);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) actionButtonRef.getLayoutParams();
            lp.rightMargin = dp(16) + sys.right;
            lp.bottomMargin = dp(16) + sys.bottom;
            actionButtonRef.setLayoutParams(lp);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        int buttonSize = getResources().getDimensionPixelSize(R.dimen.fab_size);
        int iconSize = getResources().getDimensionPixelSize(R.dimen.sub_action_icon_size);
        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        itemBuilder.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));

        // Sub-ação: Criar Procedimento
        FrameLayout iconContainer1 = new FrameLayout(this);
        iconContainer1.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));
        ImageView itemIcon1 = new ImageView(this);
        itemIcon1.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.criarprocedimento));
        FrameLayout.LayoutParams iconParams1 = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams1.gravity = Gravity.CENTER;
        itemIcon1.setLayoutParams(iconParams1);
        iconContainer1.addView(itemIcon1);
        buttonCriarProcedimentoRef = itemBuilder.setContentView(iconContainer1).build();
        buttonCriarProcedimentoRef.setOnClickListener(v -> startActivity(new Intent(this, CriarProcedimentoActivity.class)));

        // Sub-ação: Ver Sugestões
        FrameLayout iconContainer2 = new FrameLayout(this);
        iconContainer2.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));
        ImageView itemIcon2 = new ImageView(this);
        itemIcon2.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.sugestoes));
        FrameLayout.LayoutParams iconParams2 = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams2.gravity = Gravity.CENTER;
        itemIcon2.setLayoutParams(iconParams2);
        iconContainer2.addView(itemIcon2);
        buttonSugestoesRef = itemBuilder.setContentView(iconContainer2).build();
        buttonSugestoesRef.setOnClickListener(v -> startActivity(new Intent(this, VerSugestoesActivity.class)));

        // Sub-ação: Métricas
        FrameLayout iconContainer3 = new FrameLayout(this);
        iconContainer3.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));
        ImageView itemIcon3 = new ImageView(this);
        itemIcon3.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.metricas));
        FrameLayout.LayoutParams iconParams3 = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams3.gravity = Gravity.CENTER;
        itemIcon3.setLayoutParams(iconParams3);
        iconContainer3.addView(itemIcon3);
        buttonMetricasRef = itemBuilder.setContentView(iconContainer3).build();
        buttonMetricasRef.setOnClickListener(v -> startActivity(new Intent(this, MetricasA.class)));

        int radius = dp(96);
        actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(buttonCriarProcedimentoRef)
                .addSubActionView(buttonSugestoesRef)
                .addSubActionView(buttonMetricasRef)
                .setStartAngle(200)
                .setEndAngle(340)
                .setRadius(radius)
                .attachTo(actionButtonRef)
                .build();

        actionMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override public void onMenuOpened(FloatingActionMenu menu) { fabOverlay.setVisibility(View.VISIBLE); }
            @Override public void onMenuClosed(FloatingActionMenu menu) { fabOverlay.setVisibility(View.GONE); }
        });
        fabOverlay.setOnClickListener(v -> { if (actionMenu != null && actionMenu.isOpen()) actionMenu.close(true); });

        // Preload opcional (vamos recalcular na Ajuda de qualquer jeito)
        mainBinding.getRoot().postDelayed(() -> {
            Log.d(TAG_TUTORIAL, "Delay 3s concluído: aquecendo tutorial…");
            preResolveTutorialTargetsSilently(null);
        }, TUTORIAL_PREP_DELAY_MS);
    }

    @Override
    public void onBackPressed() {
        if (actionMenu != null && actionMenu.isOpen()) {
            actionMenu.close(true);
            return;
        }
        super.onBackPressed();
    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this, true);
        rProcedimentos.setAdapter(adapterProcedimentos);

        refProcedimentos.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaProcedimento.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel p = dado.getValue(ProcedimentoModel.class);
                        if (p != null && p.getIdHospital().equals(prefs.getString("id", ""))) {
                            listaProcedimento.add(p);
                        }
                    }
                }
                adapterProcedimentos.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sair_conta) {
            finish();
            editor.putString("id", "").apply();
            startActivity(new Intent(this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // ============================== Tutorial ==============================

    /** Recalcula a fila de TapTargets (inclui itens do adapter se houver) sem bloquear UI. */
    private void preResolveTutorialTargetsSilently(Runnable afterPrepared) {
        if (preparingTutorial) {
            Log.d(TAG_TUTORIAL, "preResolve: já em andamento");
            return;
        }
        preparingTutorial = true;
        preparedQueue.clear();

        // 1) Hambúrguer
        preparedQueue.add(
                TapTarget.forToolbarNavigationIcon(
                                toolbar,
                                "Menu lateral",
                                "Acesse criação de POPs, sugestões, métricas e ajuda.")
                        .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                        .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                        .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                        .transparentTarget(true)
                        .id(ID_NAV)
        );

        // 2) Compartilhar selecionados
        preparedQueue.add(
                TapTarget.forView(
                                mainBinding.compartilhar,
                                "Compartilhar selecionados",
                                "Marque os POPs e gere um PDF para compartilhar.")
                        .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                        .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                        .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                        .transparentTarget(true)
                        .id(ID_SHARE)
        );

        // 3) FAB
        if (fabIconView != null) {
            preparedQueue.add(
                    TapTarget.forView(
                                    fabIconView,
                                    "Menu rápido",
                                    "Toque para abrir atalhos: Criar POP, Sugestões e Métricas.")
                            .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                            .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                            .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                            .transparentTarget(true)
                            .id(ID_FAB)
            );
        }

        // 4) Itens do primeiro card do Recycler
        int count = (adapterProcedimentos != null) ? adapterProcedimentos.getItemCount() : 0;
        Log.d(TAG_TUTORIAL, "preResolve: itens adapter=" + count);

        if (count > 0) {
            ensureFirstItemVisible();

            // tenta agora…
            addItemTargetsIfVisible();

            // …e também tenta mais uma vez logo depois (caso o ViewHolder ainda esteja montando)
            mainBinding.getRoot().postDelayed(this::addItemTargetsIfVisible, 120);
        } else {
            Log.d(TAG_TUTORIAL, "preResolve: sem itens — passos do item não serão adicionados.");
        }

        preparingTutorial = false;
        if (afterPrepared != null) afterPrepared.run();
    }

    /** Tenta localizar as views do primeiro item e adiciona os TapTargets sem bloquear. */
    private void addItemTargetsIfVisible() {
        View vCheck = safeFindChildInFirstItem(R.id.checkBoxMarcarCompartilhar);
        View vDup   = safeFindChildInFirstItem(R.id.btn_ir_duplicar_procedimento);
        View vVer   = safeFindChildInFirstItem(R.id.btn_ir_ver_procedimento);
        View vFav   = safeFindChildInFirstItem(R.id.favoritarProcecimento);
        View vEdit  = safeFindChildInFirstItem(R.id.editarProcediemento);
        View vDel   = safeFindChildInFirstItem(R.id.excluirProcediemento);

        if (vCheck != null) preparedQueue.add(buildTarget(vCheck, "Marcar para compartilhar", "Inclui este POP no PDF.", ID_ITEM_CHECK));
        if (vDup   != null) preparedQueue.add(buildTarget(vDup,   "Duplicar POP", "Cria uma cópia deste procedimento.", ID_ITEM_DUP));
        if (vVer   != null) preparedQueue.add(buildTarget(vVer,   "Ver POP", "Abre o procedimento para leitura.", ID_ITEM_VER));
        if (vFav   != null) preparedQueue.add(buildTarget(vFav,   "Favoritar POP", "Guarde este POP nos favoritos.", ID_ITEM_FAV));
        if (vEdit  != null) preparedQueue.add(buildTarget(vEdit,  "Editar POP", "Altere o conteúdo deste procedimento.", ID_ITEM_EDIT));
        if (vDel   != null) preparedQueue.add(buildTarget(vDel,   "Excluir POP", "Remove o procedimento do painel.", ID_ITEM_DEL));

        Log.d(TAG_TUTORIAL, "addItemTargetsIfVisible: adicionados="
                + ((vCheck!=null?1:0)+(vDup!=null?1:0)+(vVer!=null?1:0)+(vFav!=null?1:0)+(vEdit!=null?1:0)+(vDel!=null?1:0)));
    }

    /** Busca segura: tenta achar o filho no primeiro item sem forçar layout; se não houver, agenda 2 re-tentativas rápidas e retorna null. */
    private View safeFindChildInFirstItem(int childId) {
        if (rProcedimentos == null || rProcedimentos.getLayoutManager() == null) return null;

        // 1) tenta via LayoutManager (não força medição/layout)
        View itemView = rProcedimentos.getLayoutManager().findViewByPosition(0);
        if (itemView != null) {
            View child = itemView.findViewById(childId);
            if (child != null && child.getVisibility() == View.VISIBLE) {
                Log.d(TAG_TUTORIAL, "safeFindChildInFirstItem: found id=" + childId);
                return child;
            }
        }

        // 2) agenda até 2 tentativas; se achar depois, injeta target na fila
        final int[] tries = {0};
        Runnable retry = new Runnable() {
            @Override public void run() {
                View v = rProcedimentos.getLayoutManager().findViewByPosition(0);
                if (v != null) {
                    View c = v.findViewById(childId);
                    if (c != null && c.getVisibility() == View.VISIBLE) {
                        preparedQueue.add(buildTarget(c, titleFor(childId), descFor(childId), idFor(childId)));
                        Log.d(TAG_TUTORIAL, "safeFindChildInFirstItem: late-found id=" + childId);
                        return;
                    }
                }
                if (++tries[0] < 2) {
                    rProcedimentos.postDelayed(this, 80);
                } else {
                    Log.d(TAG_TUTORIAL, "safeFindChildInFirstItem: id=" + childId + " não encontrado. Pulando.");
                }
            }
        };
        rProcedimentos.postDelayed(retry, 60);

        return null; // não bloqueia o fluxo atual
    }

    private int idFor(int childId) {
        if (childId == R.id.checkBoxMarcarCompartilhar) return ID_ITEM_CHECK;
        if (childId == R.id.btn_ir_duplicar_procedimento) return ID_ITEM_DUP;
        if (childId == R.id.btn_ir_ver_procedimento)     return ID_ITEM_VER;
        if (childId == R.id.favoritarProcecimento)       return ID_ITEM_FAV;
        if (childId == R.id.editarProcediemento)         return ID_ITEM_EDIT;
        if (childId == R.id.excluirProcediemento)        return ID_ITEM_DEL;
        return 0;
    }

    private String titleFor(int childId) {
        if (childId == R.id.checkBoxMarcarCompartilhar) return "Marcar para compartilhar";
        if (childId == R.id.btn_ir_duplicar_procedimento) return "Duplicar POP";
        if (childId == R.id.btn_ir_ver_procedimento)     return "Ver POP";
        if (childId == R.id.favoritarProcecimento)       return "Favoritar POP";
        if (childId == R.id.editarProcediemento)         return "Editar POP";
        if (childId == R.id.excluirProcediemento)        return "Excluir POP";
        return "";
    }

    private String descFor(int childId) {
        if (childId == R.id.checkBoxMarcarCompartilhar) return "Inclui este POP no PDF.";
        if (childId == R.id.btn_ir_duplicar_procedimento) return "Cria uma cópia deste procedimento.";
        if (childId == R.id.btn_ir_ver_procedimento)     return "Abre o procedimento para leitura.";
        if (childId == R.id.favoritarProcecimento)       return "Guarde este POP nos favoritos.";
        if (childId == R.id.editarProcediemento)         return "Altere o conteúdo deste procedimento.";
        if (childId == R.id.excluirProcediemento)        return "Remove o procedimento do painel.";
        return "";
    }

    /** Mostra a fila preparada. */
    private void showPreparedTutorial() {
        final Deque<TapTarget> queue = new ArrayDeque<>(preparedQueue);
        showNext(queue);
    }

    private void showNext(Deque<TapTarget> queue) {
        if (queue.isEmpty()) {
            Log.d(TAG_TUTORIAL, "showNext: fim do tutorial");
            return;
        }

        TapTarget next = queue.pollFirst();
        boolean isFabStep = (next != null && next.id() == ID_FAB);

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
                        if (isFabStep) {
                            try {
                                if (actionMenu != null && !actionMenu.isOpen()) actionMenu.open(true);
                                if (buttonCriarProcedimentoRef != null) {
                                    queue.addFirst(buildTarget(buttonMetricasRef, "Métricas", "Acompanhe acessos e engajamento dos POPs.", ID_FAB_MET));
                                    queue.addFirst(buildTarget(buttonSugestoesRef, "Sugestões", "Veja e gerencie as sugestões recebidas.", ID_FAB_SUG));
                                    queue.addFirst(buildTarget(buttonCriarProcedimentoRef, "Criar POP", "Crie um novo procedimento para o hospital.", ID_FAB_CREATE));
                                }
                            } catch (Exception ignore) {}
                        }
                        showNext(queue);
                    }
                }
        );
    }

    // ---------- helpers visuais ----------
    private TapTarget buildTarget(@NonNull View v, String title, String desc, int id) {
        return TapTarget.forView(v, title, desc)
                .outerCircleColorInt(ContextCompat.getColor(this, R.color.purple_700))
                .targetCircleColorInt(ContextCompat.getColor(this, android.R.color.white))
                .textColorInt(ContextCompat.getColor(this, android.R.color.white))
                .titleTextSize(20).descriptionTextSize(16)
                .transparentTarget(true)
                .drawShadow(true)
                .cancelable(true)
                .id(id);
    }

    private void ensureFirstItemVisible() {
        if (rProcedimentos == null) return;
        RecyclerView.LayoutManager lm = rProcedimentos.getLayoutManager();
        if (lm instanceof LinearLayoutManager) {
            ((LinearLayoutManager) lm).scrollToPositionWithOffset(0, 0);
        } else {
            rProcedimentos.scrollToPosition(0);
        }
    }
}
