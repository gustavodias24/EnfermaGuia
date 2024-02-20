package benicio.solucoes.enfermaguia.model;

import java.util.ArrayList;
import java.util.List;

public class ProcedimentoModel {

    String nomeProcedimento;
    String id, idHospital;

    List<InfoProcedimento> listaInformacao = new ArrayList<>();

    public ProcedimentoModel() {
    }

    public String getNomeProcedimento() {
        return nomeProcedimento;
    }

    public void setNomeProcedimento(String nomeProcedimento) {
        this.nomeProcedimento = nomeProcedimento;
    }

    public ProcedimentoModel(String id, String idHospital, String nomeProcedimento) {
        this.id = id;
        this.idHospital = idHospital;
        this.nomeProcedimento = nomeProcedimento;
    }

    public List<InfoProcedimento> getListaInformacao() {
        return listaInformacao;
    }

    public void setListaInformacao(List<InfoProcedimento> listaInformacao) {
        this.listaInformacao = listaInformacao;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdHospital() {
        return idHospital;
    }

    public void setIdHospital(String idHospital) {
        this.idHospital = idHospital;
    }

}
