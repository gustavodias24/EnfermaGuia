package benicio.solucoes.enfermaguia;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterFeedbacks;
import benicio.solucoes.enfermaguia.databinding.ActivityFeedBacksBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;
import benicio.solucoes.enfermaguia.model.FeedbackModel;
import benicio.solucoes.enfermaguia.model.SugestaoModel;

public class FeedBacksActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private DatabaseReference refFeedbacks = FirebaseDatabase.getInstance().getReference().child("feedbacks");
    private ActivityFeedBacksBinding mainBinding;

    private List<FeedbackModel> ListFeedbackUser = new ArrayList<>();

    private AdapterFeedbacks adapterFeedbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityFeedBacksBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        mainBinding.recyclerFeedbacks.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.recyclerFeedbacks.setHasFixedSize(true);
        mainBinding.recyclerFeedbacks.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterFeedbacks = new AdapterFeedbacks(this, ListFeedbackUser);
        mainBinding.recyclerFeedbacks.setAdapter(adapterFeedbacks);

        refFeedbacks.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mainBinding.textCarregando.setVisibility(View.VISIBLE);
                ListFeedbackUser.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        FeedbackModel feedbackModel = dado.getValue(FeedbackModel.class);
                        try {
                            if (feedbackModel != null && feedbackModel.getIdUsuario().equals(prefs.getString("id", ""))) {
                                ListFeedbackUser.add(feedbackModel);
                            }
                        } catch (Exception ignored) {

                        }
                    }
                    mainBinding.textCarregando.setVisibility(View.GONE);
                    adapterFeedbacks.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        mainBinding.removerSelecionados.setOnClickListener(v -> {
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setTitle("Atenção");
            d.setMessage("Você tem certeza que deseja remover os Feedbacks selecionados?");
            d.setNegativeButton("Não", null);
            d.setPositiveButton("Sim", (dialogInterface, i) -> {
                for (FeedbackModel feedbackModel : ListFeedbackUser) {
                    if (feedbackModel.isSelecionadoDeletar()) {
                        refFeedbacks.child(feedbackModel.getId()).removeValue();
                    }
                }

                Toast.makeText(this, "Todos os Selecionados Foram Removidos!", Toast.LENGTH_SHORT).show();
            });
            d.create().show();
        });
    }
}