package benicio.solucoes.enfermaguia.utils;


import android.app.Activity;
import android.app.AlertDialog;

import benicio.solucoes.enfermaguia.databinding.LayoutCarregandoBinding;

public class LoadingUtils {
    // Mantém referência estática do AlertDialog
    private static AlertDialog dialog;

    // Exibe o dialog de carregamento
    public static void showLoading(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        if (dialog != null && dialog.isShowing()) return; // evita múltiplos

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutCarregandoBinding binding = LayoutCarregandoBinding.inflate(activity.getLayoutInflater());
        builder.setView(binding.getRoot());
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.show();
    }

    public static void showLoading2(Activity activity, String title, String msg) {
        if (activity == null || activity.isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton("ok", null);

        dialog = builder.create();
        dialog.show();
    }

    // Fecha o dialog se estiver aberto
    public static void dismissLoading() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
