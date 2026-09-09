package projetopdv.venda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Venda {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public enum StatusVenda {
        CONCLUIDA("Concluída"),
        ESTORNADA("Estornada");

        private final String descricao;

        StatusVenda(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }

        public static StatusVenda fromString(String texto) {
            if (texto == null || texto.isBlank()) {
                return CONCLUIDA;
            }
            for (StatusVenda s : values()) {
                if (s.name().equalsIgnoreCase(texto.trim())) {
                    return s;
                }
            }
            return CONCLUIDA;
        }
    }

    private final int idVenda;
    private final int idOrcamento;
    private final LocalDateTime dataVenda;
    private final List<ItemVenda> itensVenda;
    private final BigDecimal valorTotal;
    private String formaPagamento;
    private StatusVenda statusVenda;
    private ModalidadeEstorno modalidadeEstorno;
    private LocalDateTime dataEstorno;
    private String motivoEstorno;

    public Venda(int idVenda, int idOrcamento, LocalDateTime dataVenda, List<ItemVenda> itensVenda,
                 BigDecimal valorTotal, String formaPagamento, StatusVenda statusVenda,
                 ModalidadeEstorno modalidadeEstorno, LocalDateTime dataEstorno, String motivoEstorno) {
        if (idVenda <= 0) {
            throw new IllegalArgumentException("ID da venda inválido.");
        }
        if (idOrcamento <= 0) {
            throw new IllegalArgumentException("ID do orçamento inválido.");
        }
        if (dataVenda == null) {
            throw new IllegalArgumentException("Data da venda inválida.");
        }
        if (itensVenda == null || itensVenda.isEmpty()) {
            throw new IllegalArgumentException("Lista de itens da venda inválida ou vazia.");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total da venda inválido.");
        }

        this.idVenda = idVenda;
        this.idOrcamento = idOrcamento;
        this.dataVenda = dataVenda;
        this.itensVenda = new ArrayList<>(itensVenda);
        this.valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);
        this.formaPagamento = formaPagamento != null ? formaPagamento : "NÃO ESPECIFICADA";
        this.statusVenda = statusVenda != null ? statusVenda : StatusVenda.CONCLUIDA;
        this.modalidadeEstorno = modalidadeEstorno;
        this.dataEstorno = dataEstorno;
        this.motivoEstorno = motivoEstorno != null ? motivoEstorno.trim() : "";
    }

    public Venda(int idVenda, int idOrcamento, LocalDateTime dataVenda, List<ItemVenda> itensVenda,
                 BigDecimal valorTotal, String formaPagamento) {
        this(idVenda, idOrcamento, dataVenda, itensVenda, valorTotal, formaPagamento, StatusVenda.CONCLUIDA, null, null, null);
    }

    public Venda(int idVenda, int idOrcamento, LocalDateTime dataVenda, List<ItemVenda> itensVenda, String formaPagamento) {
        this(
                idVenda,
                idOrcamento,
                dataVenda,
                itensVenda,
                itensVenda != null
                        ? itensVenda.stream().map(ItemVenda::getValorItem).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO,
                formaPagamento
        );
    }

    public int getIdVenda() {
        return idVenda;
    }

    public int getIdOrcamento() {
        return idOrcamento;
    }

    public LocalDateTime getDataVenda() {
        return dataVenda;
    }

    public List<ItemVenda> getItensVenda() {
        return Collections.unmodifiableList(itensVenda);
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public StatusVenda getStatusVenda() {
        return statusVenda;
    }

    public boolean isEstornada() {
        return statusVenda == StatusVenda.ESTORNADA;
    }

    public ModalidadeEstorno getModalidadeEstorno() {
        return modalidadeEstorno;
    }

    public LocalDateTime getDataEstorno() {
        return dataEstorno;
    }

    public String getMotivoEstorno() {
        return motivoEstorno;
    }

    public void estornar(String motivo, ModalidadeEstorno modalidade) {
        if (isEstornada()) {
            throw new IllegalStateException("Esta venda já foi estornada anteriormente.");
        }
        this.statusVenda = StatusVenda.ESTORNADA;
        this.modalidadeEstorno = modalidade != null ? modalidade : ModalidadeEstorno.REEMBOLSO_IMEDIATO;
        this.dataEstorno = LocalDateTime.now();
        this.motivoEstorno = (motivo != null && !motivo.isBlank()) ? motivo.trim() : "Não especificado";
    }

    public BigDecimal getValorSubtotalBruto() {
        return itensVenda.stream()
                .map(ItemVenda::getValorSubtotalBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorTotalDesconto() {
        return itensVenda.stream()
                .map(ItemVenda::getValorDescontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public int getQuantidadeTotalItens() {
        return itensVenda.stream()
                .mapToInt(ItemVenda::getQuantidade)
                .sum();
    }

    public int getQuantidadeItensDistintos() {
        return itensVenda.size();
    }

    public ItemVenda buscarItemPorNumero(int numeroItem) {
        return itensVenda.stream()
                .filter(it -> it.getNumeroItem() == numeroItem)
                .findFirst()
                .orElse(null);
    }

    public ItemVenda buscarItemPorId(int idItemVenda) {
        return itensVenda.stream()
                .filter(it -> it.getIdItemVenda() == idItemVenda)
                .findFirst()
                .orElse(null);
    }

    public String gerarCupomFiscal() {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("               CUPOM NÃO FISCAL - PDV               \n");
        sb.append("====================================================\n");
        sb.append(String.format("VENDA Nº:       %06d\n", idVenda));
        sb.append(String.format("ORÇAMENTO Nº:   %06d\n", idOrcamento));
        sb.append(String.format("DATA DA VENDA:  %s\n", dataVenda.format(FORMATO_DATA_HORA)));
        sb.append(String.format("SITUAÇÃO:       %s\n", statusVenda.getDescricao().toUpperCase()));
        if (isEstornada()) {
            sb.append(String.format("ESTORNADA EM:   %s\n", dataEstorno != null ? dataEstorno.format(FORMATO_DATA_HORA) : ""));
            sb.append(String.format("MODALIDADE:     %s\n", modalidadeEstorno != null ? modalidadeEstorno.getDescricao() : ""));
            sb.append(String.format("MOTIVO ESTORNO: %s\n", motivoEstorno));
        }
        sb.append("----------------------------------------------------\n");
        sb.append("ITEM CÓDIGO    DESCRIÇÃO                QTD x PREÇO   TOTAL\n");
        sb.append("----------------------------------------------------\n");

        for (ItemVenda item : itensVenda) {
            String cod = item.getCodigoBarras().isEmpty() ? String.format("%04d", item.getIdProduto()) : item.getCodigoBarras();
            String nome = item.getNomeProduto();
            if (nome.length() > 22) {
                nome = nome.substring(0, 20) + "..";
            }
            sb.append(String.format("%02d   %-9s %-22s %2dx%-7.2f %7.2f\n",
                    item.getNumeroItem(),
                    cod,
                    nome,
                    item.getQuantidade(),
                    item.getPrecoUnitario(),
                    item.getValorItem()
            ));
            if (item.temDesconto()) {
                sb.append(String.format("     (Preço Tabela: R$ %.2f | Desconto Item: R$ %.2f)\n",
                        item.getPrecoUnitarioTabela(), item.getValorDescontoTotal()));
            }
        }

        sb.append("----------------------------------------------------\n");
        sb.append(String.format("TOTAL DE ITENS VENDIDOS:         %15d un\n", getQuantidadeTotalItens()));
        sb.append(String.format("SUBTOTAL BRUTO:                 R$ %15.2f\n", getValorSubtotalBruto()));
        if (getValorTotalDesconto().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("DESCONTO TOTAL CONCEDIDO:     (-) R$ %15.2f\n", getValorTotalDesconto()));
        }
        sb.append(String.format("VALOR TOTAL A PAGAR:            R$ %15.2f\n", valorTotal));
        sb.append(String.format("FORMA DE PAGAMENTO:             %18s\n", formaPagamento));
        sb.append("====================================================\n");
        return sb.toString();
    }

    public String gerarComprovanteEstorno() {
        if (!isEstornada()) {
            throw new IllegalStateException("A venda não está estornada.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("              COMPROVANTE DE ESTORNO                \n");
        sb.append("====================================================\n");
        sb.append(String.format("VENDA ORIGINAL: %06d\n", idVenda));
        sb.append(String.format("DATA DA VENDA:  %s\n", dataVenda.format(FORMATO_DATA_HORA)));
        sb.append(String.format("DATA DO ESTORNO:%s\n", dataEstorno != null ? dataEstorno.format(FORMATO_DATA_HORA) : ""));
        sb.append(String.format("MODALIDADE:     %s\n", modalidadeEstorno != null ? modalidadeEstorno.getDescricao() : ""));
        sb.append(String.format("VALOR ESTORNADO:R$ %.2f\n", valorTotal));
        sb.append(String.format("FORMA ORIGINAL: %s\n", formaPagamento));
        sb.append(String.format("MOTIVO:         %s\n", motivoEstorno));
        sb.append("====================================================\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venda venda = (Venda) o;
        return idVenda == venda.idVenda;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVenda);
    }

    @Override
    public String toString() {
        return String.format(
                "ID: %d | Orçamento: #%d | Data: %s | Total: R$ %.2f | Pagamento: %s | Status: %s",
                idVenda,
                idOrcamento,
                dataVenda.format(FORMATO_DATA_HORA),
                valorTotal,
                formaPagamento,
                statusVenda.getDescricao()
        );
    }
}
