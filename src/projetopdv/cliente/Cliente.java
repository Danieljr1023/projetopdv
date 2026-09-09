package projetopdv.cliente;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class Cliente {

    public static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int idCliente;
    private String nomeCliente;
    private LocalDate dataNascimento;
    private String cpf;

    public Cliente(int idCliente, String nomeCliente, LocalDate dataNascimento, String cpf) {
        if (idCliente <= 0) {
            throw new IllegalArgumentException("Erro ao definir id do cliente: id do cliente inválido.");
        }
        validarNome(nomeCliente);
        validarCpf(cpf);

        this.idCliente = idCliente;
        this.nomeCliente = nomeCliente != null ? nomeCliente.trim() : "";
        this.dataNascimento = dataNascimento;
        this.cpf = cpf != null ? cpf.trim() : "";
    }

    // Getters e Setters
    public int getIdCliente() {
        return idCliente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        validarNome(nomeCliente);
        this.nomeCliente = nomeCliente != null ? nomeCliente.trim() : "";
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        validarCpf(cpf);
        this.cpf = cpf != null ? cpf.trim() : "";
    }

    private void validarNome(String nome) {
        if (nome != null && nome.contains("%")) {
            throw new IllegalArgumentException("O nome do cliente não pode conter o caractere '%'.");
        }
    }

    private void validarCpf(String cpf) {
        if (cpf != null && cpf.contains("%")) {
            throw new IllegalArgumentException("O CPF do cliente não pode conter o caractere '%'.");
        }
    }

    @Override
    public String toString() {
        String nasc = dataNascimento != null ? dataNascimento.format(FORMATO_DATA) : "Não informada";
        return String.format("ID: %d | Nome: %s | Data de Nascimento: %s | CPF: %s",
                idCliente, nomeCliente, nasc, cpf.isEmpty() ? "Não informado" : cpf);
    }
}
