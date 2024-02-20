package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import benicio.solucoes.enfermaguia.databinding.ActivityMainBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityVerDetalheProcedimentoBinding;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.utils.Utils;

public class VerDetalheProcedimentoActivity extends AppCompatActivity {
    int idTitulo;
    int idBody;
    private ActivityVerDetalheProcedimentoBinding mainBinding;
    private DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private Bundle b;
    private ProcedimentoModel procedimentoModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityVerDetalheProcedimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Carregando...");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        b = getIntent().getExtras();

        assert b != null;
        String idProcedimento = b.getString("idProcedimento", "");
        if (!idProcedimento.isEmpty()) {
            refProcedimentos.child(idProcedimento).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    procedimentoModel = task.getResult().getValue(ProcedimentoModel.class);
                    getSupportActionBar().setTitle(procedimentoModel.getNomeProcedimento());


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
                            textTitle.setTextSize(24);
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
                            textDescri.setText(info.getInfo());
                            textDescri.setTextSize(14);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}