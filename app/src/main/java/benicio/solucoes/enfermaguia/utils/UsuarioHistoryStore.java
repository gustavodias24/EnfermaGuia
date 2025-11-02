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

import benicio.solucoes.enfermaguia.model.UsuarioModel;

public final class UsuarioHistoryStore {

    private static final String PREFS_NAME = "usuarios_history_prefs";
    private static final String KEY_HISTORY = "usuarios_history_json";
    // Limite opcional de histórico. Coloque <= 0 para ilimitado.
    private static final int MAX_HISTORY = 50;

    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<UsuarioModel>>() {}.getType();

    private UsuarioHistoryStore() {}

    /** Retorna a lista de histórico (nunca null). */
    public static List<UsuarioModel> getHistory(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_HISTORY, "");
        if (TextUtils.isEmpty(json)) return new ArrayList<>();
        try {
            List<UsuarioModel> list = gson.fromJson(json, LIST_TYPE);
            return (list != null) ? list : new ArrayList<>();
        } catch (Exception e) {
            // JSON corrompido: zera o histórico
            return new ArrayList<>();
        }
    }

    /** Persiste toda a lista. */
    private static void persist(Context ctx, List<UsuarioModel> list) {
        String json = gson.toJson(list, LIST_TYPE);
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY, json)
                .apply();
    }

    /**
     * Adiciona/move um UsuarioModel no histórico:
     * - Se já existir (mesmo id; fallback por login), move para o início (posição 0)
     * - Senão, adiciona ao final da lista
     */
    public static void saveToHistory(Context ctx, UsuarioModel user) {
        if (user == null) return;

        List<UsuarioModel> list = getHistory(ctx);

        int existingIndex = indexOf(list, user);

        if (existingIndex >= 0) {
            UsuarioModel existing = list.remove(existingIndex);
            list.add(0, existing);
        } else {
            list.add(user); // regra: novo vai para o fim
        }

        if (MAX_HISTORY > 0 && list.size() > MAX_HISTORY) {
            list = list.subList(0, MAX_HISTORY);
        }

        persist(ctx, list);
    }

    /** Remove um usuário específico do histórico (útil para UI). */
    public static void removeFromHistory(Context ctx, UsuarioModel user) {
        if (user == null) return;
        List<UsuarioModel> list = getHistory(ctx);
        int idx = indexOf(list, user);
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

    /** Compara por id; se id estiver vazio, fallback por login. */
    private static int indexOf(List<UsuarioModel> list, UsuarioModel target) {
        for (int i = 0; i < list.size(); i++) {
            UsuarioModel cur = list.get(i);

            boolean hasIds = !TextUtils.isEmpty(cur.getId()) && !TextUtils.isEmpty(target.getId());
            boolean sameById = hasIds && Objects.equals(cur.getId(), target.getId());

            boolean idEmptyBoth = TextUtils.isEmpty(cur.getId()) && TextUtils.isEmpty(target.getId());
            boolean hasLoginBoth = !TextUtils.isEmpty(cur.getLogin()) && !TextUtils.isEmpty(target.getLogin());
            boolean sameByLogin = idEmptyBoth && hasLoginBoth && Objects.equals(cur.getLogin(), target.getLogin());

            if (sameById || sameByLogin) {
                return i;
            }
        }
        return -1;
    }
}

