package benicio.solucoes.enfermaguia.model;

public class UsuarioModel {
    String id, login, senha, nome;

    boolean isAdmin = false;

    public UsuarioModel() {
    }

    public UsuarioModel(String id, String login, String senha, String nome) {
        this.id = id;
        this.login = login;
        this.senha = senha;
        this.nome = nome;
    }

    public UsuarioModel(String id, String login, String senha, String nome, boolean isAdmin) {
        this.id = id;
        this.login = login;
        this.senha = senha;
        this.nome = nome;
        this.isAdmin = isAdmin;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String toString() {
        return
                "Login do Hospital: " + login + '\n' +
                "Senha: " + senha + '\n' +
                "Nome do Hospital: " + nome ;
    }
}
