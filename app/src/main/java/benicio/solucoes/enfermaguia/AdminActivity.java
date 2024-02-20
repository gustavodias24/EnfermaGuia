package benicio.solucoes.enfermaguia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterHospitais;
import benicio.solucoes.enfermaguia.databinding.ActivityAdminBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityMainBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutCriarHospitalBinding;
import benicio.solucoes.enfermaguia.model.UsuarioModel;

public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding mainBinding;
    private RecyclerView rHospital;

    private AdapterHospitais adapterHospitais;
    private List<UsuarioModel> listaHospital = new ArrayList<>();
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    private Dialog dialogCriarHospital;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getSupportActionBar().setTitle("Painel Admin");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mainBinding.criarHospital.setOnClickListener(view -> dialogCriarHospital.show());

        configurarDialogCriarHospital();

        configurarRecyclerHospital();
    }

    private void configurarRecyclerHospital() {

        rHospital = mainBinding.recyclerHospitais;
        rHospital.setLayoutManager(new LinearLayoutManager(this));
        rHospital.setHasFixedSize(true);
        rHospital.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterHospitais = new AdapterHospitais(listaHospital, this);
        rHospital.setAdapter(adapterHospitais);


        refUsuarios.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listaHospital.clear();

                    for (DataSnapshot dado : snapshot.getChildren()) {
                        UsuarioModel user = dado.getValue(UsuarioModel.class);
                        if (user.isAdmin()) {
                            listaHospital.add(user);
                        }
                    }

                    adapterHospitais.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void configurarDialogCriarHospital() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutCriarHospitalBinding hospitalBinding = LayoutCriarHospitalBinding.inflate(getLayoutInflater());

        hospitalBinding.cadastro.setOnClickListener(view -> {
            String nome = hospitalBinding.nomeField.getEditText().getText().toString().trim();
            String senha = hospitalBinding.senhaField.getEditText().getText().toString().trim();
            String login = hospitalBinding.loginField.getEditText().getText().toString().trim();

            if (!nome.isEmpty() && !senha.isEmpty() && !login.isEmpty()) {
                String id = Base64.getEncoder().encodeToString(login.getBytes());

                refUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean prosseguir = true;
                        for (DataSnapshot dado : snapshot.getChildren()) {
                            if (dado.getKey().equals(id)) {
                                prosseguir = false;
                                Toast.makeText(AdminActivity.this, "Esse login já está sendo utilizado!", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }

                        if (prosseguir) {
                            refUsuarios.child(id).setValue(new UsuarioModel(id, login, senha, nome, true)).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(AdminActivity.this, "Hospital criado!", Toast.LENGTH_SHORT).show();
                                    dialogCriarHospital.dismiss();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminActivity.this, "Problema de conexão, tente novamente.", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                Toast.makeText(this, "Preencha todas as informações!", Toast.LENGTH_SHORT).show();
            }
        });


        b.setView(hospitalBinding.getRoot());
        dialogCriarHospital = b.create();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


}