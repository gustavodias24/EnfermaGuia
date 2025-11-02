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

/**
 * Store de favoritos de ProcedimentoModel em SharedPreferences.
 * Regra:
 *  - Se já existir, move para o topo (posição 0)
 *  - Se não existir, adiciona ao fim
 */
public final class ProcedimentoFavoritosStore {

    private static final String PREFS_NAME = "procedimentos_favoritos_prefs";
    private static final String KEY_FAVORITOS = "procedimentos_favoritos_json";
    // Opcional: limite de itens (<=0 para ilimitado)
    private static final int MAX_FAVORITOS = 200;

    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<ProcedimentoModel>>() {}.getType();

    private ProcedimentoFavoritosStore() {}

    /** Retorna a lista de favoritos (nunca null). */
    public static List<ProcedimentoModel> getFavoritos(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_FAVORITOS, "");
        if (TextUtils.isEmpty(json)) return new ArrayList<>();
        try {
            List<ProcedimentoModel> list = gson.fromJson(json, LIST_TYPE);
            return (list != null) ? list : new ArrayList<>();
        } catch (Exception e) {
            // JSON corrompido: retorna lista vazia
            return new ArrayList<>();
        }
    }

    /** Persiste a lista completa. */
    private static void persist(Context ctx, List<ProcedimentoModel> list) {
        String json = gson.toJson(list, LIST_TYPE);
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FAVORITOS, json)
                .apply();
    }

    /** Retorna true se o procedimento está nos favoritos. */
    public static boolean isFavorito(Context ctx, ProcedimentoModel item) {
        return indexOf(getFavoritos(ctx), item) >= 0;
    }

    /**
     * Adiciona/move um procedimento aos favoritos:
     * - Se já existir (mesma chave lógica), move para o topo (0)
     * - Se não existir, adiciona ao fim
     */
    public static void addFavorito(Context ctx, ProcedimentoModel item) {
        if (item == null) return;

        List<ProcedimentoModel> list = getFavoritos(ctx);
        int idx = indexOf(list, item);

        if (idx >= 0) {
            ProcedimentoModel existing = list.remove(idx);
            list.add(0, existing);
        } else {
            list.add(item);
            // se quiser novo no topo, troque por: list.add(0, item);
        }

        if (MAX_FAVORITOS > 0 && list.size() > MAX_FAVORITOS) {
            list = list.subList(0, MAX_FAVORITOS);
        }

        persist(ctx, list);
    }

    /** Remove um procedimento dos favoritos (se existir). */
    public static void removeFavorito(Context ctx, ProcedimentoModel item) {
        if (item == null) return;

        List<ProcedimentoModel> list = getFavoritos(ctx);
        int idx = indexOf(list, item);
        if (idx >= 0) {
            list.remove(idx);
            persist(ctx, list);
        }
    }

    /**
     * Alterna favorito. Retorna o novo estado:
     * true = ficou favorito; false = deixou de ser favorito.
     */
    public static boolean toggleFavorito(Context ctx, ProcedimentoModel item) {
        if (item == null) return false;

        List<ProcedimentoModel> list = getFavoritos(ctx);
        int idx = indexOf(list, item);
        boolean nowFav;

        if (idx >= 0) {
            // era favorito → remove
            list.remove(idx);
            nowFav = false;
        } else {
            // não era → adiciona ao fim (ou topo, se preferir)
            list.add(item);
            nowFav = true;
        }

        if (nowFav && MAX_FAVORITOS > 0 && list.size() > MAX_FAVORITOS) {
            list = list.subList(0, MAX_FAVORITOS);
        }

        persist(ctx, list);
        return nowFav;
    }

    /** Limpa todos os favoritos. */
    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_FAVORITOS)
                .apply();
    }

    /**
     * Busca índice do item na lista comparando por chave lógica:
     *  - Preferência: id + idHospital
     *  - Fallback: nomeProcedimento quando ambos ids estiverem vazios
     */
    private static int indexOf(List<ProcedimentoModel> list, ProcedimentoModel target) {
        if (list == null || target == null) return -1;

        String tId = safe(target.getId());
        String tHosp = safe(target.getIdHospital());
        String tNome = safe(target.getNomeProcedimento());

        for (int i = 0; i < list.size(); i++) {
            ProcedimentoModel cur = list.get(i);
            String cId = safe(cur.getId());
            String cHosp = safe(cur.getIdHospital());

            boolean sameByIds = !tId.isEmpty() && !cId.isEmpty() &&
                    Objects.equals(cId, tId) &&
                    Objects.equals(cHosp, tHosp);

            boolean bothIdsEmpty = tId.isEmpty() && cId.isEmpty();
            boolean sameByName = bothIdsEmpty &&
                    !tNome.isEmpty() &&
                    Objects.equals(safe(cur.getNomeProcedimento()), tNome);

            if (sameByIds || sameByName) {
                return i;
            }
        }
        return -1;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}

