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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.enfermaguia.adapter.AdapterHospitais;
import benicio.solucoes.enfermaguia.adapter.AdapterProcedimentos;
import benicio.solucoes.enfermaguia.databinding.ActivityCadastroUsuarioBinding;
import benicio.solucoes.enfermaguia.databinding.ActivityHallBinding;
import benicio.solucoes.enfermaguia.databinding.LayoutSelecionarHospitalBinding;
import benicio.solucoes.enfermaguia.model.ProcedimentoModel;
import benicio.solucoes.enfermaguia.model.UsuarioModel;

public class HallActivity extends AppCompatActivity {

    private List<UsuarioModel> listaHospitais = new ArrayList<>();
    private ActivityHallBinding mainBinding;
    public static DatabaseReference refProcedimentos = FirebaseDatabase.getInstance().getReference().child("procedimentos");
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference().child("usuarios");
    public static SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private RecyclerView rProcedimentos;
    public static List<ProcedimentoModel> listaProcedimento = new ArrayList<>();
    public static AdapterProcedimentos adapterProcedimentos;
    public static Dialog dialogSelecionaHospital;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityHallBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        configurarRecyclerProcedimento();
        configurarDialogSelecionarHospital();
        configurrarDialogSelecionarHospital();

        mainBinding.selecionarHospital.setOnClickListener( view -> dialogSelecionaHospital.show());


    }

    private void configurrarDialogSelecionarHospital() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutSelecionarHospitalBinding hospitalBinding = LayoutSelecionarHospitalBinding.inflate(getLayoutInflater());

        RecyclerView recyclerHospital = hospitalBinding.recyclerHospitais;
        recyclerHospital.setLayoutManager(new LinearLayoutManager(this));
        recyclerHospital.setHasFixedSize(true);
        recyclerHospital.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        AdapterHospitais adapterHospitais = new AdapterHospitais(listaHospitais, this, true, editor);
        recyclerHospital.setAdapter(adapterHospitais);

        refUsuarios.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if ( snapshot.exists() ){
                    listaHospitais.clear();
                    for ( DataSnapshot dado : snapshot.getChildren()){
                        UsuarioModel hospital = dado.getValue(UsuarioModel.class);
                        if ( hospital.isAdmin() ){
                            listaHospitais.add(hospital);
                        }
                    }

                    adapterHospitais.notifyDataSetChanged();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        b.setView(hospitalBinding.getRoot());
        dialogSelecionaHospital = b.create();
    }

    private void configurarDialogSelecionarHospital(){

    }
    private void configurarRecyclerProcedimento() {
        rProcedimentos = mainBinding.recyclerProcedimentos;
        rProcedimentos.setLayoutManager(new LinearLayoutManager(this));
        rProcedimentos.setHasFixedSize(true);
        rProcedimentos.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterProcedimentos = new AdapterProcedimentos(listaProcedimento, this);
        rProcedimentos.setAdapter(adapterProcedimentos);

        buscarProcedimentos();

    }

    public static void buscarProcedimentos(){
        refProcedimentos.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listaProcedimento.clear();
                    for (DataSnapshot dado : snapshot.getChildren()) {
                        ProcedimentoModel procedimentoModel = dado.getValue(ProcedimentoModel.class);
                        if (procedimentoModel.getIdHospital().equals(prefs.getString("idHospitalSelecionado", ""))) {
                            listaProcedimento.add(procedimentoModel);
                        }
                        adapterProcedimentos.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if ( item.getItemId() == R.id.sair_conta){
            finish();
            editor.putString("id", "").apply();
            startActivity(new Intent(this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

}