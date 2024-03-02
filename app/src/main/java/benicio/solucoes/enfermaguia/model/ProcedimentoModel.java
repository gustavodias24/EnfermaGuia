package benicio.solucoes.enfermaguia.model;

import java.util.ArrayList;
import java.util.List;

public class ProcedimentoModel {
    /*
    *
    *  Metricas, compartilhamento, acesso, sugestoes
    *
    * */

    int compartilhamentos = 0;
    int acessos = 0;
    int sugestoes = 0;
    String nomeProcedimento;
    String id, idHospital;

    List<InfoProcedimento> listaInformacao = new ArrayList<>();

    public ProcedimentoModel() {
    }

    public int getCompartilhamentos() {
        return compartilhamentos;
    }

    public void setCompartilhamentos(int compartilhamentos) {
        this.compartilhamentos = compartilhamentos;
    }

    public int getAcessos() {
        return acessos;
    }

    public void setAcessos(int acessos) {
        this.acessos = acessos;
    }

    public int getSugestoes() {
        return sugestoes;
    }

    public void setSugestoes(int sugestoes) {
        this.sugestoes = sugestoes;
    }
    public String returnMetricas(){
        StringBuilder metricasBuilder = new StringBuilder();
        metricasBuilder.append("Procedimento: ").append(this.nomeProcedimento).append("\n\n");
        metricasBuilder.append("Acessos: ").append(this.acessos).append("\n");
        metricasBuilder.append("Compartilhamentos: ").append(this.compartilhamentos).append("\n");
        metricasBuilder.append("Sugestões: ").append(this.sugestoes);
        return metricasBuilder.toString();
    }
    @Override
    public String toString() {
        StringBuilder textToString = new StringBuilder();
        for( InfoProcedimento procedimento : this.listaInformacao){
            if ( procedimento.getTipo() == 0 ){
                textToString.append("*").append(procedimento.getInfo()).append("*").append("\n");
            }else{
                textToString.append("_").append(procedimento.getInfo()).append("_").append("\n\n");
            }
        }

        return textToString.toString();
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
