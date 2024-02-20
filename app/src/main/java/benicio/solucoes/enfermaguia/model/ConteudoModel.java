package benicio.solucoes.enfermaguia.model;

public class ConteudoModel {
    String titulo, info;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public ConteudoModel(String titulo, String info) {
        this.titulo = titulo;
        this.info = info;
    }
}
