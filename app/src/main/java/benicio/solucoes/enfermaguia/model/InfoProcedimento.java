package benicio.solucoes.enfermaguia.model;

public class InfoProcedimento {
    String info;
    int tipo;

    public InfoProcedimento(String info, int tipo) {
        this.info = info;
        this.tipo = tipo;
    }

    public InfoProcedimento() {
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getTipo() {
        return tipo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }
}
