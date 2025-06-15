package benicio.solucoes.enfermaguia;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityTutorialBinding;

public class TutorialActivity extends AppCompatActivity {
    List<Integer> tutorialUser = new ArrayList<>();
    private ActivityTutorialBinding mainBinding;
    int position = 0;

    int limit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityTutorialBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        Bundle b = getIntent().getExtras();

        if ( b != null && b.getBoolean("h")) {
            tutorialUser.add(R.raw.h1);
            mainBinding.imageTutorial.setImageResource(
                    tutorialUser.get(position)
            );
            tutorialUser.add(R.raw.h2);
            tutorialUser.add(R.raw.h3);
            tutorialUser.add(R.raw.h4);
            limit = 3;
        } else {
            tutorialUser.add(R.raw.u1);
            tutorialUser.add(R.raw.u2);
            tutorialUser.add(R.raw.u3);
            tutorialUser.add(R.raw.u4);
            tutorialUser.add(R.raw.u5);
            tutorialUser.add(R.raw.u6);
            tutorialUser.add(R.raw.u7);
            limit = 6;
        }


        mainBinding.imageTutorial.setOnClickListener(v -> {
            if (position >= limit) {
                Toast.makeText(this, "Fim do Tutorial", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                position += 1;
                mainBinding.imageTutorial.setImageResource(
                        tutorialUser.get(position)
                );
            }
        });

    }
}