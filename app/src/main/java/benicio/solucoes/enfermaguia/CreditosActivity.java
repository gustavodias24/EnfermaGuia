package benicio.solucoes.enfermaguia;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import benicio.solucoes.enfermaguia.databinding.ActivityCreditosBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;

public class CreditosActivity extends AppCompatActivity {

    private ActivityCreditosBinding mainBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityCreditosBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        getSupportActionBar().setTitle("Voltar");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        StringBuilder textCredito = new StringBuilder();
        textCredito.append("<h1>").append("CRÉDITOS").append("</h1>").append("<br><br>");
        textCredito.append("<p>").append("Este aplicativo é um produto da Tese de Doutorado em Enfermagem da Universidade de Brasília (UnB) intitulada \"Tecnologia Mobile sobre Procedimentos Operacionais Padrão da Assistência de Enfermagem: usabilidade heurística e Desing participativo\", concebida pelo discente Remo Rodrigues Carneiro, sob orientação da Profª Drª Maria Cristina Soares Rodrigues. ").append("</p>").append("<br>");
        textCredito.append("<p>").append("Esta pesquisa recebeu o apoio financeiro concedido pela Universidade de Brasília (UnB), por meio de editais de incentivo a participação de eventos científicos, pesquisa, inovação e produção tecnológica, promovidos pelo Decanato de Pós-Graduação (DPG) e pelo Programa de Pós-Graduação em Enfermagem (PPGENF).").append("</p>").append("<br>");
        textCredito.append("<p>").append("Dedicamos especial agradecimento ao Grupo de Pesquisa LEPSP (Laboratório de Estudos e Pesquisas Multidisciplinares em Segurança do Paciente), da UnB. ").append("</p>").append("<br><br>");
        textCredito.append("<b>").append("Programação: ").append("</b>").append("<br>").append("BENICIO SOLUCÕES 54.169.866/0001-50").append("<br>");
        textCredito.append("<b>Contato: </b>")
                .append("<a href='https://wa.me/5591980101707'>")
                .append("55 91 9 8010-1707")
                .append("</a><br>");

        mainBinding.textView10.setText(Html.fromHtml(textCredito.toString(), Html.FROM_HTML_MODE_LEGACY));
        mainBinding.textView10.setMovementMethod(LinkMovementMethod.getInstance());

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}