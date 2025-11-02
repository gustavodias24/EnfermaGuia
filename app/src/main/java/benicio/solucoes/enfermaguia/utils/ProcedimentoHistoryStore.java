package benicio.solucoes.enfermaguia.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import benicio.solucoes.enfermaguia.model.ProcedimentoModel;

public final class ProcedimentoHistoryStore {

    private static final String PREFS_NAME = "procedimentos_history_prefs";
    private static final String KEY_HISTORY = "procedimentos_history_json";
    // opcional: limite de histórico. Ajuste se desejar (<=0 para ilimitado)
    private static final int MAX_HISTORY = 50;

    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<ProcedimentoModel>>() {}.getType();

    private ProcedimentoHistoryStore() {}

    /** Retorna a lista de histórico (pode ser vazia, mas nunca null). */
    public static List<ProcedimentoModel> getHistory(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_HISTORY, "");
        if (TextUtils.isEmpty(json)) return new ArrayList<>();
        try {
            List<ProcedimentoModel> list = gson.fromJson(json, LIST_TYPE);
            return (list != null) ? list : new ArrayList<>();
        } catch (Exception e) {
            // Em caso de JSON corrompido, zera o histórico
            return new ArrayList<>();
        }
    }

    /** Salva toda a lista no SharedPreferences. */
    private static void persist(Context ctx, List<ProcedimentoModel> list) {
        String json = gson.toJson(list, LIST_TYPE);
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY, json)
                .apply();
    }

    /**
     * Adiciona/move um ProcedimentoModel no histórico:
     * - Se já existir (mesmo id e idHospital), move para o início (posição 0)
     * - Senão, adiciona ao final da lista
     */
    public static void saveToHistory(Context ctx, ProcedimentoModel item) {
        if (item == null) return;

        List<ProcedimentoModel> list = getHistory(ctx);

        // Procura por igualdade lógica (por id + idHospital; fallback nome)
        int existingIndex = indexOf(list, item);

        if (existingIndex >= 0) {
            // Já existe → remove e coloca no topo
            ProcedimentoModel existing = list.remove(existingIndex);
            list.add(0, existing);
        } else {
            // Não existe → adiciona no fim
            list.add(item);
        }

        // aplica limite (mantém os mais recentes)
        if (MAX_HISTORY > 0 && list.size() > MAX_HISTORY) {
            list = list.subList(0, MAX_HISTORY);
        }

        persist(ctx, list);
    }

    /** Remove um item específico do histórico. Opcional, útil para UI. */
    public static void removeFromHistory(Context ctx, ProcedimentoModel item) {
        if (item == null) return;
        List<ProcedimentoModel> list = getHistory(ctx);
        int idx = indexOf(list, item);
        if (idx >= 0) {
            list.remove(idx);
            persist(ctx, list);
        }
    }

    /** Limpa todo o histórico. */
    public static void clearHistory(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_HISTORY)
                .apply();
    }

    /** Compara por (id + idHospital). Se id estiver vazio, usa fallback por nome. */
    private static int indexOf(List<ProcedimentoModel> list, ProcedimentoModel target) {
        for (int i = 0; i < list.size(); i++) {
            ProcedimentoModel cur = list.get(i);

            boolean hasIds = !TextUtils.isEmpty(cur.getId()) && !TextUtils.isEmpty(target.getId());
            boolean hasHosp = !TextUtils.isEmpty(cur.getIdHospital()) || !TextUtils.isEmpty(target.getIdHospital());

            boolean sameByIds = hasIds &&
                    Objects.equals(cur.getId(), target.getId()) &&
                    Objects.equals(nullToEmpty(cur.getIdHospital()), nullToEmpty(target.getIdHospital()));

            boolean sameByName = TextUtils.isEmpty(target.getId()) &&
                    TextUtils.isEmpty(cur.getId()) &&
                    !TextUtils.isEmpty(cur.getNomeProcedimento()) &&
                    Objects.equals(cur.getNomeProcedimento(), target.getNomeProcedimento());

            if (sameByIds || (!hasIds && !hasHosp && sameByName)) {
                return i;
            }
        }
        return -1;
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}

