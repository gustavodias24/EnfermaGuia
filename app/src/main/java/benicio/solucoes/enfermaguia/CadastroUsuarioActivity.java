package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Base64;

import benicio.solucoes.enfermaguia.databinding.ActivityCadastroUsuarioBinding;
import benicio.solucoes.enfermaguia.model.UsuarioModel;

public class CadastroUsuarioActivity extends AppCompatActivity {

    private ActivityCadastroUsuarioBinding mainBinding;
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityCadastroUsuarioBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        prefs = getSharedPreferences("user_prefs",MODE_PRIVATE);
        editor = prefs.edit();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Cadastro");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mainBinding.cadastro.setOnClickListener( view -> {
            String nome, senha, login, id;

            nome = mainBinding.nomeField.getEditText().getText().toString().trim();
            senha = mainBinding.senhaField.getEditText().getText().toString().trim();
            login = mainBinding.loginField.getEditText().getText().toString().trim();

            if ( !nome.isEmpty() && !senha.isEmpty() && !login.isEmpty() ){
                id = Base64.getEncoder().encodeToString(login.getBytes());


                refUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean prosseguir = true;
                        for ( DataSnapshot dado : snapshot.getChildren()){
                            if (dado.getKey().equals(id)){
                                prosseguir = false;
                                Toast.makeText(CadastroUsuarioActivity.this, "Esse login já está sendo utilizado!", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }

                        if ( prosseguir ){
                            refUsuarios.child(id).setValue(new UsuarioModel(id, login, senha, nome)).addOnCompleteListener(task -> {
                               if ( task.isSuccessful() ){
                                   Toast.makeText(CadastroUsuarioActivity.this, "Usuário criado!", Toast.LENGTH_SHORT).show();
                                   editor.putString("id", id).apply();
                                   startActivity(new Intent(CadastroUsuarioActivity.this, HallActivity.class));
                               }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CadastroUsuarioActivity.this, "Problema de conexão, tente novamente.", Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                Toast.makeText(this, "Preencha todas as informações!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if ( item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}