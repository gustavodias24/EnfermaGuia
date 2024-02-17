package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import benicio.solucoes.enfermaguia.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mainBinding;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        if (!prefs.getString("id", "").isEmpty()) {
            finish();
            startActivity(new Intent(this, HallActivity.class));
        }


        mainBinding.entrar.setOnClickListener(view -> {
            String login, senha;

            login = mainBinding.loginField.getEditText().getText().toString().trim();
            senha = mainBinding.senhaField.getEditText().getText().toString().trim();

            if (!login.isEmpty() && !senha.isEmpty()) {
                refUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        UsuarioModel usuarioModel = null;
                        boolean encontrado = false;
                        for (DataSnapshot dado : snapshot.getChildren()) {
                            usuarioModel = dado.getValue(UsuarioModel.class);
                            if (usuarioModel.getLogin().equals(login)) {
                                encontrado = true;
                                break;
                            }
                        }

                        if (encontrado) {
                            if (usuarioModel.getSenha().equals(senha)) {
                                finish();
                                editor.putString("id", usuarioModel.getId()).apply();
                                startActivity(new Intent(MainActivity.this, HallActivity.class));
                                Toast.makeText(MainActivity.this, "Bem-vindo de volta!", Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(MainActivity.this, "Senha errada!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Login não encontrado!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Problema de conexão, tente novamente.", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                Toast.makeText(this, "Preencha as informações!", Toast.LENGTH_SHORT).show();
            }
        });


        mainBinding.fazerCadastro.setOnClickListener(view -> startActivity(new Intent(this, CadastroUsuarioActivity.class)));
    }
}