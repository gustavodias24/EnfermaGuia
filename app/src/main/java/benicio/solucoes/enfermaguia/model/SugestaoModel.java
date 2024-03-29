package benicio.solucoes.enfermaguia.model;

public class SugestaoModel {
    String dataSugestao, info, nomeUsuario, id, nomeProcedimento, idProcedimento, idHospital;

    @Override
    public String toString() {
        return  "Usuário: " + nomeUsuario + " Data: " + dataSugestao + "\n" +
                "Procedimento: " + nomeProcedimento + "\n" +
                "Sugestão: " + info ;
    }

    public SugestaoModel() {
    }

    public SugestaoModel(String dataSugestao, String info, String nomeUsuario, String id, String nomeProcedimento, String idProcedimento) {
        this.dataSugestao = dataSugestao;
        this.info = info;
        this.nomeUsuario = nomeUsuario;
        this.id = id;
        this.nomeProcedimento = nomeProcedimento;
        this.idProcedimento = idProcedimento;
    }

    public String getIdHospital() {
        return idHospital;
    }

    public void setIdHospital(String idHospital) {
        this.idHospital = idHospital;
    }

    public String getIdProcedimento() {
        return idProcedimento;
    }

    public void setIdProcedimento(String idProcedimento) {
        this.idProcedimento = idProcedimento;
    }

    public String getDataSugestao() {
        return dataSugestao;
    }

    public void setDataSugestao(String dataSugestao) {
        this.dataSugestao = dataSugestao;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomeProcedimento() {
        return nomeProcedimento;
    }

    public void setNomeProcedimento(String nomeProcedimento) {
        this.nomeProcedimento = nomeProcedimento;
    }
}
