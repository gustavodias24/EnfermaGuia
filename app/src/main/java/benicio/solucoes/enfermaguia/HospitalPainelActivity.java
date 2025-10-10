package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityHospitalPainelBinding;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.PDFGenerator;

public class HospitalPainelActivity extends AppCompatActivity {

    private FloatingActionMenu actionMenu;
    private DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
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

        // Overlay transparente para detectar toque fora do menu
        final View fabOverlay = new View(this);
        fabOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fabOverlay.setClickable(true);
        fabOverlay.setFocusable(true);
        fabOverlay.setVisibility(View.GONE); // só aparece quando o menu abre
        fabOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        // adiciona acima do conteúdo
        ((ViewGroup) findViewById(android.R.id.content)).addView(fabOverlay);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Botão sanduíche
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Itens do menu lateral
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_criar_procedimento) {
                startActivity(new Intent(this, CriarProcedimentoActivity.class));
            } else if (item.getItemId() == R.id.menu_sugestoes) {
                startActivity(new Intent(this, VerSugestoesActivity.class));
            } else if (item.getItemId() == R.id.menu_metricas) {
                startActivity(new Intent(this, MetricasA.class));
            } else if (item.getItemId() == R.id.menu_ajuda) {
                Intent i = new Intent(this, TutorialActivity.class);
                i.putExtra("h", true);
                startActivity(i);
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

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(prefs.getString("nomeUser", ""));
        }
        mainBinding.textView.setText("Painel de Procedimentos do Hospital " + prefs.getString("nomeUser", ""));

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

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

        // ---------------- FAB + MENU CIRCULAR (com insets e abertura para cima) ----------------
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_24));

        final FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(icon)
                .build();

        // posiciona no canto inferior direito com margens em dp
        FrameLayout.LayoutParams fabLp = (FrameLayout.LayoutParams) actionButton.getLayoutParams();
        fabLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        fabLp.rightMargin = dp(16);
        fabLp.bottomMargin = dp(16);
        actionButton.setLayoutParams(fabLp);

        // aplica insets do sistema (gesture bar / nav bar) para o FAB não colar no fundo
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) actionButton.getLayoutParams();
            lp.rightMargin = dp(16) + sys.right;
            lp.bottomMargin = dp(16) + sys.bottom;
            actionButton.setLayoutParams(lp);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        // tamanhos dos subitens
        int buttonSize = getResources().getDimensionPixelSize(R.dimen.fab_size);
        int iconSize = getResources().getDimensionPixelSize(R.dimen.sub_action_icon_size);
        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        itemBuilder.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));

        // ---- Criar Procedimento
        FrameLayout iconContainer1 = new FrameLayout(this);
        iconContainer1.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));
        ImageView itemIcon1 = new ImageView(this);
        itemIcon1.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.criarprocedimento));
        FrameLayout.LayoutParams iconParams1 = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams1.gravity = Gravity.CENTER;
        itemIcon1.setLayoutParams(iconParams1);
        iconContainer1.addView(itemIcon1);
        SubActionButton buttonCriarProcedimento = itemBuilder.setContentView(iconContainer1).build();
        buttonCriarProcedimento.setOnClickListener(v -> startActivity(new Intent(this, CriarProcedimentoActivity.class)));

        // ---- Ver Sugestões
        FrameLayout iconContainer2 = new FrameLayout(this);
        iconContainer2.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));
        ImageView itemIcon2 = new ImageView(this);
        itemIcon2.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.sugestoes));
        FrameLayout.LayoutParams iconParams2 = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams2.gravity = Gravity.CENTER;
        itemIcon2.setLayoutParams(iconParams2);
        iconContainer2.addView(itemIcon2);
        SubActionButton buttonCriarSugestoes = itemBuilder.setContentView(iconContainer2).build();
        buttonCriarSugestoes.setOnClickListener(v -> startActivity(new Intent(this, VerSugestoesActivity.class)));

        // ---- Métricas
        FrameLayout iconContainer3 = new FrameLayout(this);
        iconContainer3.setLayoutParams(new FrameLayout.LayoutParams(buttonSize, buttonSize));
        ImageView itemIcon3 = new ImageView(this);
        itemIcon3.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.metricas));
        FrameLayout.LayoutParams iconParams3 = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams3.gravity = Gravity.CENTER;
        itemIcon3.setLayoutParams(iconParams3);
        iconContainer3.addView(itemIcon3);
        SubActionButton buttonMetricas = itemBuilder.setContentView(iconContainer3).build();
        buttonMetricas.setOnClickListener(v -> startActivity(new Intent(this, MetricasA.class)));

        // raio e ângulos para abrir APENAS PARA CIMA
        int radius = dp(96);
        actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(buttonCriarProcedimento)
                .addSubActionView(buttonCriarSugestoes)
                .addSubActionView(buttonMetricas)
                .setStartAngle(200)   // abre para cima
                .setEndAngle(340)     // evita itens apontarem para baixo
                .setRadius(radius)
                .attachTo(actionButton)
                .build();
        // Mostra/esconde overlay conforme o estado do menu
        actionMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu menu) {
                fabOverlay.setVisibility(View.VISIBLE);
            }

            @Override
            public void onMenuClosed(FloatingActionMenu menu) {
                fabOverlay.setVisibility(View.GONE);
            }
        });

        // Toque fora do menu fecha o FAB
        fabOverlay.setOnClickListener(v -> {
            if (actionMenu != null && actionMenu.isOpen()) actionMenu.close(true);
        });
        // ----------------------------------------------------------------------
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
                if (snapshot.exists()) {
                    listaProcedimento.clear();
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel procedimentoModel = dado.getValue(ProcedimentoModel.class);
                        if (procedimentoModel != null
                                && procedimentoModel.getIdHospital().equals(prefs.getString("id", ""))) {
                            listaProcedimento.add(procedimentoModel);
                        }
                    }
                    adapterProcedimentos.notifyDataSetChanged();
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
