package benicio.solucoes.enfermaguia.model;

public class FeedbackModel {
    String dataFeedback, info, id, IdUsuario;

    boolean SelecionadoDeletar;

    public boolean isSelecionadoDeletar() {
        return SelecionadoDeletar;
    }

    public void setSelecionadoDeletar(boolean selecionadoDeletar) {
        SelecionadoDeletar = selecionadoDeletar;
    }

    public FeedbackModel(String dataFeedback, String info, String id, String idUsuario) {
        this.dataFeedback = dataFeedback;
        this.info = info;
        this.id = id;
        IdUsuario = idUsuario;
    }

    public FeedbackModel() {
    }

    @Override
    public String toString() {
        return "<b>Data: </b>" + dataFeedback + "<br>" +
                "<b>FeedBack: </b><br>" + info;
    }

    public String getDataFeedback() {
        return dataFeedback;
    }

    public void setDataFeedback(String dataFeedback) {
        this.dataFeedback = dataFeedback;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdUsuario() {
        return IdUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        IdUsuario = idUsuario;
    }
}
