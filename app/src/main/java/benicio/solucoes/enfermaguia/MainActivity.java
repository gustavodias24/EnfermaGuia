package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import benicio.solucoes.enfermaguia.model.UsuarioModel;
import benicio.solucoes.enfermaguia.utils.NetworkUtils;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mainBinding;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    private DatabaseReference refAcessos = FirebaseDatabase.getInstance().getReference().child("acessos");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        if (!prefs.getString("id", "").isEmpty()) {
            finish();
            if (prefs.getBoolean("isAdmin", false)) {
                startActivity(new Intent(this, HospitalPainelActivity.class));
            } else {
                startActivity(new Intent(this, HallActivity.class));
            }
        }


        mainBinding.entrar.setOnClickListener(view -> {
            if (NetworkUtils.isNetworkAvailable(this)){
                String login, senha;

                login = mainBinding.loginField.getEditText().getText().toString().trim();
                senha = mainBinding.senhaField.getEditText().getText().toString().trim();

                if (!login.isEmpty() && !senha.isEmpty()) {

                    if (login.equals("adm-master") && senha.equals("M4st3r@Adm")) {
                        startActivity(new Intent(this, AdminActivity.class));
                    } else {
                        refUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists()){
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
                                            if (usuarioModel.isAdmin()) {
                                                editor.putBoolean("isAdmin", true).apply();
                                                startActivity(new Intent(MainActivity.this, HospitalPainelActivity.class));
                                            } else {
                                                editor.putBoolean("isAdmin", false).apply();
                                                startActivity(new Intent(MainActivity.this, HallActivity.class));
                                            }

                                            editor.putString("nomeUser", usuarioModel.getNome()).apply();

                                            Toast.makeText(MainActivity.this, "Bem-vindo de volta!", Toast.LENGTH_LONG).show();
                                            refAcessos.get().addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    if (task.getResult().exists()) {
                                                        int countAtual = task.getResult().getValue(Integer.class);
                                                        countAtual++;

                                                        refAcessos.setValue(countAtual);

                                                    }
                                                }
                                            });
                                        } else {
                                            Toast.makeText(MainActivity.this, "Senha errada!", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this, "Login não encontrado!", Toast.LENGTH_SHORT).show();
                                    }
                                }else{
                                    Toast.makeText(MainActivity.this, "Problema de conexão, tente novamente.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MainActivity.this, "Problema de conexão, tente novamente.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                } else {
                    Toast.makeText(this, "Preencha as informações!", Toast.LENGTH_SHORT).show();
                }
            }else{
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle("AVISO!");
                b.setMessage("Você está sem uma conexão de internet aceitável, tente novamente com a conexão!");
                b.setPositiveButton("ok", null);
                b.create().show();
            }

        });


        mainBinding.fazerCadastro.setOnClickListener(view -> startActivity(new Intent(this, CadastroUsuarioActivity.class)));
    }
}