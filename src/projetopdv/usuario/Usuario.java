package projetopdv.usuario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class Usuario {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final int idUsuario;
    private String nomeUsuario;
    private String login;
    private String senhaHash;
    private String salt;
    private PerfilUsuario perfil;
    private boolean ativo;
    private final LocalDateTime dataCadastro;
    private final Set<Permissao> permissoes;

    public Usuario(
            int idUsuario,
            String nomeUsuario,
            String login,
            String senhaHash,
            String salt,
            PerfilUsuario perfil,
            boolean ativo,
            LocalDateTime dataCadastro,
            Set<Permissao> permissoes
    ) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException("ID do usuário inválido.");
        }
        validarNome(nomeUsuario);
        validarLogin(login);
        validarSenhaHash(senhaHash);
        validarSalt(salt);
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil do usuário não pode ser nulo.");
        }
        if (dataCadastro == null) {
            throw new IllegalArgumentException("Data de cadastro não pode ser nula.");
        }

        this.idUsuario = idUsuario;
        this.nomeUsuario = nomeUsuario.trim();
        this.login = login.trim();
        this.senhaHash = senhaHash.trim();
        this.salt = salt.trim();
        this.perfil = perfil;
        this.ativo = ativo;
        this.dataCadastro = dataCadastro;

        this.permissoes = EnumSet.noneOf(Permissao.class);
        if (permissoes == null || permissoes.isEmpty()) {
            this.permissoes.addAll(perfil.getPermissoesPadrao());
        } else {
            this.permissoes.addAll(permissoes);
        }
    }


    // ==========================================
    // MÉTODOS DE AVALIAÇÃO E GESTÃO DE PERMISSÕES
    // ==========================================

    public boolean temPermissao(Permissao permissaoDesejada) {
        if (!this.ativo || permissaoDesejada == null) {
            return false;
        }

        // 1. Possui a permissão diretamente concedida
        if (this.permissoes.contains(permissaoDesejada)) {
            return true;
        }

        // 2. Navega recursivamente subindo pelas permissões mães (Árvore)
        Permissao atual = permissaoDesejada.getMae();
        while (atual != null) {
            if (this.permissoes.contains(atual)) {
                return true;
            }
            atual = atual.getMae();
        }

        return false;
    }

    public void concederPermissao(Permissao permissao) {
        if (permissao == null) {
            throw new IllegalArgumentException("Permissão a conceder não pode ser nula.");
        }
        this.permissoes.add(permissao);

        // Se concedeu uma permissão mãe, limpa filhas redundantes para otimizar o conjunto
        if (permissao.isMae()) {
            for (Permissao filha : permissao.getFilhas()) {
                this.permissoes.remove(filha);
            }
        }
    }

    public void revogarPermissao(Permissao permissao) {
        if (permissao == null) {
            throw new IllegalArgumentException("Permissão a revogar não pode ser nula.");
        }

        if (this.permissoes.contains(permissao)) {
            this.permissoes.remove(permissao);
        }

        // Se o usuário possui a permissão mãe da permissão revogada,
        // expandimos a mãe em todas as filhas ativas, exceto a que foi revogada.
        Permissao mae = permissao.getMae();
        if (mae != null && this.permissoes.contains(mae)) {
            this.permissoes.remove(mae);
            for (Permissao filha : mae.getFilhas()) {
                if (filha != permissao) {
                    this.permissoes.add(filha);
                }
            }
        }

        // Se a permissão revogada é ela própria uma mãe, removemos também todas as suas filhas
        if (permissao.isMae()) {
            for (Permissao filha : permissao.getFilhas()) {
                this.permissoes.remove(filha);
            }
        }
    }

    public void redefinirParaPadraoDoPerfil() {
        this.permissoes.clear();
        this.permissoes.addAll(this.perfil.getPermissoesPadrao());
    }

    public Set<Permissao> getPermissoes() {
        return Collections.unmodifiableSet(permissoes);
    }

    public Set<Permissao> getPermissoesAdicionais() {
        Set<Permissao> adicionais = EnumSet.noneOf(Permissao.class);
        Set<Permissao> padrao = perfil.getPermissoesPadrao();
        for (Permissao p : permissoes) {
            if (!padrao.contains(p)) {
                adicionais.add(p);
            }
        }
        return Collections.unmodifiableSet(adicionais);
    }

    public Set<Permissao> getPermissoesRevogadas() {
        Set<Permissao> revogadas = EnumSet.noneOf(Permissao.class);
        Set<Permissao> padrao = perfil.getPermissoesPadrao();
        for (Permissao p : padrao) {
            if (!temPermissao(p)) {
                revogadas.add(p);
            }
        }
        return Collections.unmodifiableSet(revogadas);
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        validarNome(nomeUsuario);
        this.nomeUsuario = nomeUsuario.trim();
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        validarLogin(login);
        this.login = login.trim();
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        validarSenhaHash(senhaHash);
        this.senhaHash = senhaHash.trim();
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        validarSalt(salt);
        this.salt = salt.trim();
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilUsuario perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil do usuário não pode ser nulo.");
        }
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    // ==========================================
    // VALIDAÇÕES DEFENSIVAS
    // ==========================================

    private void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do usuário não pode ser vazio.");
        }
        if (nome.contains("%")) {
            throw new IllegalArgumentException("O nome do usuário não pode conter o caractere '%'.");
        }
    }

    private void validarLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("O login do usuário não pode ser vazio.");
        }
        if (login.contains("%")) {
            throw new IllegalArgumentException("O login do usuário não pode conter o caractere '%'.");
        }
        if (login.contains(" ")) {
            throw new IllegalArgumentException("O login do usuário não pode conter espaços.");
        }
    }

    private void validarSenhaHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            throw new IllegalArgumentException("O hash de senha não pode ser vazio.");
        }
    }

    private void validarSalt(String salt) {
        if (salt == null || salt.trim().isEmpty()) {
            throw new IllegalArgumentException("O salt de segurança não pode ser vazio.");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + idUsuario;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Usuario other = (Usuario) obj;
        return idUsuario == other.idUsuario;
    }

    @Override
    public String toString() {
        return String.format(
                "ID: %d | Nome: %s | Login: %s | Perfil: %s | Status: %s | Cadastro: %s",
                idUsuario,
                nomeUsuario,
                login,
                perfil.name(),
                ativo ? "ATIVO" : "INATIVO",
                dataCadastro.format(FORMATO_DATA_HORA)
        );
    }
}
