package benicio.solucoes.enfermaguia.utils;

import static com.itextpdf.kernel.pdf.PdfName.Font;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.itextpdf.io.font.FontConstants;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.AreaBreakType;
import com.itextpdf.layout.property.TextAlignment;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import benicio.solucoes.enfermaguia.model.InfoProcedimento;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public class PDFGenerator {
    public static void generateAndSharePDF(Activity activity, List<ProcedimentoModel> procedimentos, String dadosCabecalho) {

        Toast.makeText(activity, "Gerando o PDF", Toast.LENGTH_SHORT).show();
        File documentosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File enfermaGuiaDir = new File(documentosDir, "EnfermaGuia");

        if (!enfermaGuiaDir.exists()) {
            enfermaGuiaDir.mkdirs();
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy");
        String data = simpleDateFormat.format(new Date());
        String nomeArquivo = "guia_" + data + "_" + UUID.randomUUID().toString() + ".pdf";
        File file = new File(enfermaGuiaDir, nomeArquivo);

        try {
            PdfDocument pdfDocument = createPDF(procedimentos, file, dadosCabecalho);
            Toast.makeText(activity, "PDF salvo em Documents/EnfermaGuia", Toast.LENGTH_SHORT).show();
            compartilharPDFViaWhatsApp(activity, file);
        } catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Aviso");
            builder.setMessage(e.getMessage());
            builder.setPositiveButton("Fechar", null);
            builder.create().show();
            e.printStackTrace();
        }
    }

    private static PdfDocument createPDF(List<ProcedimentoModel> procedimentos, File file, String dadosCabecalho) throws IOException {
        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(new FileOutputStream(file)));
        Document document = new Document(pdfDocument, PageSize.A4);
        document.setMargins(50, 50, 50, 50);
        PdfFont font = PdfFontFactory.createFont(FontConstants.HELVETICA);

        boolean cabecalho = true;
        for (ProcedimentoModel procedimento : procedimentos) {

            Paragraph nomeProcedimento = new Paragraph(procedimento.getNomeProcedimento())
                    .setFont(font)
                    .setBold()
                    .setUnderline()
                    .setTextAlignment(TextAlignment.CENTER);

            if (cabecalho) {
                cabecalho = false;
                Paragraph nomeCabecalho = new Paragraph(String.valueOf(dadosCabecalho))
                        .setFont(font)
                        .setBold()
                        .setUnderline()
                        .setTextAlignment(TextAlignment.CENTER);
                document.add(nomeCabecalho);
            }

            document.add(nomeProcedimento);


            for (InfoProcedimento info : procedimento.getListaInformacao()) {
                Paragraph texto = new Paragraph(info.getInfo().replace("\n", " "))
                        .setFont(font)
                        .setTextAlignment(TextAlignment.LEFT);

                if (info.getTipo() == 0) {
                    texto.setBold();
                }
                document.add(texto);
            }

            document.add(new Paragraph("\n"));
        }

        Paragraph rodape = new Paragraph("Todos os direitos reservados, é permitida a reprodução total ou parcial, desde que citada a fonte.")
                .setFont(font)
                .setMarginTop(50)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);

        document.add(rodape);


        document.close();
        return pdfDocument;
    }

    private static void compartilharPDFViaWhatsApp(Activity activity, File file) {
        Uri contentUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }


}