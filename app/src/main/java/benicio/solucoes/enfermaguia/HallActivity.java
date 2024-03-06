package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import benicio.solucoes.enfermaguia.adapter.AdapterHospitais;
import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityCadastroUsuarioBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutSelecionarHospitalBinding;
import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;

public class HallActivity extends AppCompatActivity {

    private List<UsuarioModel> listaHospitais = new ArrayList<>();
    private ActivityHallBinding mainBinding;
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private RecyclerView rProcedimentos;
    public static List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    public static AdapterProcedimentos adapterProcedimentos;
    public static Dialog dialogSelecionaHospital;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHallBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        configurarRecyclerProcedimento();
        configurarDialogSelecionarHospital();
        configurrarDialogSelecionarHospital();

        mainBinding.selecionarHospital.setOnClickListener(view -> dialogSelecionaHospital.show());

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
                gerarPdfOS(listaParaCompartilharProcedimento);
            }
        });

    }

    private void configurrarDialogSelecionarHospital() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutSelecionarHospitalBinding hospitalBinding = LayoutSelecionarHospitalBinding.inflate(getLayoutInflater());

        RecyclerView recyclerHospital = hospitalBinding.recyclerHospitais;
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
                        if (hospital.isAdmin()) {
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


        b.setView(hospitalBinding.getRoot());
        dialogSelecionaHospital = b.create();
    }

    private void configurarDialogSelecionarHospital() {

    }

    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this);
        rProcedimentos.setAdapter(adapterProcedimentos);

        buscarProcedimentos();

    }

    public static void buscarProcedimentos() {
        refProcedimentos.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listaProcedimento.clear();
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel procedimentoModel = dado.getValue(ProcedimentoModel.class);
                        if (procedimentoModel.getIdHospital().equals(prefs.getString("idHospitalSelecionado", ""))) {
                            listaProcedimento.add(procedimentoModel);
                        }
                        adapterProcedimentos.notifyDataSetChanged();
                    }
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
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void gerarPdfOS(List<ProcedimentoModel> listaProcedimento) {

        Bitmap bmpTemplate = BitmapFactory.decodeResource(getResources(), R.drawable.templaterelatorio);
        Bitmap scaledbmpTemplate = Bitmap.createScaledBitmap(bmpTemplate, 792, 1120, false);
        int pageHeight = 1120;
        int pagewidth = 792;

        int posTituloX = 300;
        int posNormalX = 24;

        PdfDocument pdfDocument = new PdfDocument();

        Paint paint = new Paint();
        Paint title = new Paint();
        Paint restante = new Paint();

        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, 1).create();
        PdfDocument.Page currentPage = pdfDocument.startPage(mypageInfo);

        Canvas canvas = currentPage.getCanvas();

        canvas.drawBitmap(scaledbmpTemplate, 1, 1, paint);

        title.setTextSize(24);
        title.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        title.setColor(ContextCompat.getColor(this, R.color.black));

        restante.setTextSize(16);
        restante.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        restante.setColor(ContextCompat.getColor(this, R.color.black));

        int valorY = 24;

        for (ProcedimentoModel procedimentoModel : listaProcedimento) {
            if (valorY >= pageHeight) {
                pdfDocument.finishPage(currentPage); // Finaliza a página atual
                PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                currentPage = pdfDocument.startPage(newPageInfo); // Começa uma nova página
                canvas = currentPage.getCanvas(); // Obtém o canvas da nova página
                valorY = 24; // Reinicia o valorY para começar a partir do topo da nova página
            }

            canvas.drawText(procedimentoModel.getNomeProcedimento(), posTituloX, valorY, title);
            valorY += 48;
            for (InfoProcedimento info : procedimentoModel.getListaInformacao()) {
                Paint paintAtual;
                if (info.getTipo() == 0) {
                    paintAtual = title;
                } else {
                    paintAtual = restante;
                }

                for (String texto : info.getInfo().split("\n")) {
                    if (valorY >= pageHeight) {
                        pdfDocument.finishPage(currentPage); // Finaliza a página atual
                        PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                        currentPage = pdfDocument.startPage(newPageInfo); // Começa uma nova página
                        canvas = currentPage.getCanvas(); // Obtém o canvas da nova página
                        valorY = 24; // Reinicia o valorY para começar a partir do topo da nova página
                    }
                    canvas.drawText(texto, posNormalX, valorY, paintAtual);
                    valorY += 24;
                }
            }
        }

        pdfDocument.finishPage(currentPage);

        File documentosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        File kaizenProjetosDir = new File(documentosDir, "EnfermaGuia");
        if (!kaizenProjetosDir.exists()) {
            kaizenProjetosDir.mkdirs();
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy");
        String data = simpleDateFormat.format(new Date());
        String nomeArquivo = "guia_" + data + "_" + UUID.randomUUID().toString() + ".pdf";

        File file = new File(kaizenProjetosDir, nomeArquivo);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF salvo em Documents/EnfermaGuia", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("Aviso");
            b.setMessage(e.getMessage());
            b.setPositiveButton("Fechar", null);
            b.create().show();
            e.printStackTrace();
        }
        pdfDocument.close();
        compartilharPDFViaWhatsApp(file);
    }

    private void compartilharPDFViaWhatsApp(File file) {

        Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);

    }

}