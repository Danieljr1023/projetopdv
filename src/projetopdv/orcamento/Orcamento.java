package projetopdv.orcamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import projetopdv.cliente.Cliente;
import projetopdv.venda.Venda;

public final class Orcamento {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final int idOrcamento;
    private String nomeOrcamento;
    private final List<ItemOrcamento> itensOrcamento;
    private final LocalDateTime dataOrcamento;
    private BigDecimal valorTotal;
    private Cliente cliente;
    private StatusOrcamento statusOrcamento;
    private Venda venda;

    public enum StatusOrcamento {
        ABERTO, // Um orçamento aberto, capaz de receber alterações.
        CONFIRMADO, // Um orçamento fechado, aguardando faturamento.
        CANCELADO, // Um orçamento descartado, que pode ser recuperado.
        FATURANDO, // Um orçamento faturando no caixa, finalizando uma venda.
        FINALIZADO // Status final quando a venda é realizada. Nunca mais muda!
    }

    public Orcamento(int idOrcamento, String nomeOrcamento, List<ItemOrcamento> itensOrcamento, LocalDateTime dataOrcamento, Cliente cliente, StatusOrcamento statusOrcamento, Venda venda) {
        if (idOrcamento > 0
                && itensOrcamento != null
                && dataOrcamento != null
                && statusOrcamento != null) {
            if (statusOrcamento == StatusOrcamento.FINALIZADO) {

                if (venda == null) {
                    throw new IllegalArgumentException(
                            "Orçamento finalizado deve possuir uma venda."
                    );
                }

                if (venda.getIdOrcamento() != idOrcamento) {
                    throw new IllegalArgumentException(
                            "A venda não pertence a este orçamento."
                    );
                }

            } else if (venda != null) {

                throw new IllegalArgumentException(
                        "Somente orçamento finalizado pode possuir uma venda."
                );
            }

            if (itensOrcamento.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "A lista de itens contém item inválido."
                );
            }

            validarNome(nomeOrcamento);

            this.idOrcamento = idOrcamento;
            this.nomeOrcamento = nomeOrcamento == null ? "" : nomeOrcamento.trim();
            this.itensOrcamento = new ArrayList<>(itensOrcamento);
            this.itensOrcamento.forEach(item -> item.vincularOrcamento(this));
            this.dataOrcamento = dataOrcamento;
            this.valorTotal = this.itensOrcamento.stream()
                    .map(ItemOrcamento::getValorItem)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.cliente = cliente;
            this.statusOrcamento = statusOrcamento;
            this.venda = venda;
        } else {
            throw new IllegalArgumentException("Erro ao definir orçamento: orçamento inválido.");
        }
    }

    public boolean podeAbrir() {
        return this.statusOrcamento == StatusOrcamento.CONFIRMADO
                || this.statusOrcamento == StatusOrcamento.CANCELADO;
    }

    public boolean estaAberto() {
        return this.statusOrcamento == StatusOrcamento.ABERTO;
    }

    public boolean estaFinalizado() {
        return this.statusOrcamento == StatusOrcamento.FINALIZADO;
    }

    public boolean podeAlterar() {
        return this.statusOrcamento == StatusOrcamento.ABERTO
                || this.statusOrcamento == StatusOrcamento.FATURANDO;
    }

    public boolean podeCancelar() {
        return this.statusOrcamento == StatusOrcamento.ABERTO
                || this.statusOrcamento == StatusOrcamento.CONFIRMADO
                || this.statusOrcamento == StatusOrcamento.FATURANDO;
    }

    public boolean estaVazio() {
        return nomeOrcamento.isEmpty()
                && itensOrcamento.isEmpty()
                && !temCliente();
    }

    public boolean podeIniciarFaturamento() {
        return this.statusOrcamento == StatusOrcamento.CONFIRMADO;
    }

    public boolean podeFinalizar() {
        return this.statusOrcamento == StatusOrcamento.FATURANDO;
    }

    public boolean temVenda() {
        return this.venda != null;
    }

    public boolean temCliente() {
        return this.cliente != null;
    }

    // Getters e Setters
    private void verificarSePodeAlterar() {
        if (!podeAlterar()) {
            throw new IllegalStateException(
                    "O orçamento não pode ser alterado no estado atual.");
        }
    }

    public void setCliente(Cliente cliente) {
        verificarSePodeAlterar();
        if (cliente == null) {
            throw new IllegalArgumentException("Erro ao definir cliente: cliente inválido.");
        }
        this.cliente = cliente;
    }

    public void removeCliente() {
        verificarSePodeAlterar();
        this.cliente = null;
    }

    public Cliente getCliente() {
        if (!temCliente()) {
            throw new IllegalStateException("Erro ao obter cliente: cliente não informado.");
        }
        return cliente;
    }

    public int getIdOrcamento() {
        return idOrcamento;
    }

    public String getNomeOrcamento() {
        if (nomeOrcamento.isEmpty()) {
            throw new IllegalStateException("Erro ao obter nome do orçamento: nome do orçamento não informado.");
        }
        return nomeOrcamento;
    }

    public void setNomeOrcamento(String nomeOrcamento) {
        verificarSePodeAlterar();
        if (nomeOrcamento == null || nomeOrcamento.trim().isEmpty()) {
            throw new IllegalArgumentException("Erro ao definir nome do orçamento: nome do orçamento inválido.");
        }
        validarNome(nomeOrcamento);
        this.nomeOrcamento = nomeOrcamento.trim();
    }

    public List<ItemOrcamento> getItensOrcamento() {
        return List.copyOf(itensOrcamento);
    }

    public void recalcularTotal() {
        this.valorTotal = this.itensOrcamento.stream()
                .map(ItemOrcamento::getValorItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getValorBruto() {
        return this.itensOrcamento.stream()
                .map(ItemOrcamento::getValorBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getValorDesconto() {
        return this.itensOrcamento.stream()
                .map(ItemOrcamento::getDescontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean temDescontos() {
        return this.itensOrcamento.stream().anyMatch(ItemOrcamento::temDesconto);
    }

    public void addItensOrcamento(ItemOrcamento itemOrcamento) {
        verificarSePodeAlterar();
        if (itemOrcamento == null) {
            throw new IllegalArgumentException("Erro ao adicionar item ao orçamento: item do orçamento inválido.");
        }
        itemOrcamento.vincularOrcamento(this);
        this.itensOrcamento.add(itemOrcamento);
        recalcularTotal();
    }

    public void removeItensOrcamento(ItemOrcamento itemOrcamento) {
        verificarSePodeAlterar();
        if (itemOrcamento == null) {
            throw new IllegalArgumentException("Erro ao remover item do orçamento: item do orçamento inválido.");
        }
        if (!this.itensOrcamento.remove(itemOrcamento)) {
            throw new IllegalArgumentException("Erro ao remover item do orçamento: item não pertence ao orçamento");
        }
        recalcularTotal();
    }

    public int proximoNumeroItem() {
        return this.itensOrcamento.stream()
                .mapToInt(ItemOrcamento::getNumeroItem)
                .max()
                .orElse(0) + 1;
    }

    public ItemOrcamento buscarItemPorNumero(int numeroItem) {
        return this.itensOrcamento.stream()
                .filter(item -> item.getNumeroItem() == numeroItem)
                .findFirst()
                .orElse(null);
    }

    public boolean removerItemPorNumero(int numeroItem) {
        ItemOrcamento item = buscarItemPorNumero(numeroItem);
        if (item != null) {
            removeItensOrcamento(item);
            return true;
        }
        return false;
    }

    public void aplicarDescontoItem(ItemOrcamento itemOrcamento, BigDecimal novoPrecoUnitario) {
        verificarSePodeAlterar();
        if (itemOrcamento == null || !this.itensOrcamento.contains(itemOrcamento)) {
            throw new IllegalArgumentException("Item não pertence a este orçamento.");
        }
        itemOrcamento.setPrecoUnitario(novoPrecoUnitario);
    }

    public void sincronizarItensComCatalogo() {
        verificarSePodeAlterar();
        if (this.statusOrcamento != StatusOrcamento.ABERTO) {
            throw new IllegalStateException("Só é possível sincronizar itens com o catálogo quando o orçamento estiver ABERTO.");
        }
        for (ItemOrcamento item : this.itensOrcamento) {
            item.atualizarDadosDoProduto();
        }
    }

    public LocalDateTime getDataOrcamento() {
        return dataOrcamento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public int getIdCliente() {
        if (!temCliente()) {
            throw new IllegalStateException("Erro ao obter cliente: cliente não informado.");
        }
        return cliente.getIdCliente();
    }

    public StatusOrcamento getStatusOrcamento() {
        return statusOrcamento;
    }

    public void confirmar() {
        if (!estaAberto()) {
            throw new IllegalStateException("Somente orçamentos abertos podem ser confirmados.");
        }
        if (this.nomeOrcamento == null || this.nomeOrcamento.trim().isEmpty()) {
            throw new IllegalStateException("Para confirmar o orçamento, é obrigatório definir um nome.");
        }
        this.statusOrcamento = StatusOrcamento.CONFIRMADO;
    }

    public void abrir() {
        if (!podeAbrir()) {
            throw new IllegalStateException("Somente orçamentos confirmados ou cancelados podem ser abertos.");
        }
        this.statusOrcamento = StatusOrcamento.ABERTO;
    }

    public void cancelar() {
        if (!podeCancelar()) {
            throw new IllegalStateException("Somente orçamentos abertos, confirmados ou em faturamento podem ser cancelados.");
        }
        this.statusOrcamento = StatusOrcamento.CANCELADO;
    }

    public void iniciarFaturamento() {
        if (!podeIniciarFaturamento()) {
            throw new IllegalStateException("Somente orçamentos confirmados podem ser faturados.");
        }
        this.statusOrcamento = StatusOrcamento.FATURANDO;
    }

    public void interromperFaturamento() {
        if (statusOrcamento != StatusOrcamento.FATURANDO) {
            throw new IllegalStateException(
                    "Somente um orçamento em faturamento pode ter o faturamento interrompido."
            );
        }

        this.statusOrcamento = StatusOrcamento.CONFIRMADO;
    }

    public void finalizar(Venda venda) {
        if (!podeFinalizar()) {
            throw new IllegalStateException(
                    "Somente orçamentos em faturamento podem ser finalizados."
            );
        }

        if (venda == null) {
            throw new IllegalArgumentException(
                    "Não é possível finalizar o orçamento sem uma venda."
            );
        }

        if (venda.getIdOrcamento() != this.idOrcamento) {
            throw new IllegalArgumentException(
                    "A venda não pertence a este orçamento."
            );
        }

        this.venda = venda;
        this.statusOrcamento = StatusOrcamento.FINALIZADO;
    }

    public Venda getVenda() {
        if (!temVenda()) {
            throw new IllegalStateException(
                    "Este orçamento ainda não possui venda."
            );
        }

        return venda;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + idOrcamento;
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
        Orcamento other = (Orcamento) obj;
        return idOrcamento == other.idOrcamento;
    }

    private void validarNome(String nome) {
        if (nome != null && nome.contains("%")) {
            throw new IllegalArgumentException("O nome do orçamento não pode conter o caractere '%'.");
        }
    }

    @Override
    public String toString() {
        String infoCliente = (cliente != null)
                ? String.format("%s (ID: %d)", cliente.getNomeCliente(), cliente.getIdCliente())
                : "Não informado";
        String infoVenda = venda == null
                ? "Não informada"
                : "ID: " + venda.getIdVenda();
        return String.format(
                "ID: %d | Nome: %s | Status: %s | Data: %s | Total: R$ %.2f | Cliente: %s | Itens: %s | Venda: %s",
                idOrcamento,
                nomeOrcamento,
                statusOrcamento.toString(),
                dataOrcamento.format(FORMATO_DATA_HORA),
                valorTotal,
                infoCliente,
                itensOrcamento,
                infoVenda
        );
    }
}
