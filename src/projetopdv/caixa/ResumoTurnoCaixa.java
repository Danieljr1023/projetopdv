package projetopdv.caixa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ResumoTurnoCaixa {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LocalDateTime dataAbertura;
    private final BigDecimal fundoTrocoInicial;
    private final BigDecimal totalVendasDinheiro;
    private final BigDecimal totalVendasCartaoCredito;
    private final BigDecimal totalVendasCartaoDebito;
    private final BigDecimal totalVendasPix;
    private final BigDecimal totalVendasValeCompra;
    private final BigDecimal totalSuprimentos;
    private final BigDecimal totalSangriasResgateVale;
    private final BigDecimal totalSangriasOperacionais;
    private final BigDecimal totalEstornosDinheiro;
    private final int totalVendasRealizadas;
    private final int totalItensVendidos;

    public ResumoTurnoCaixa(LocalDateTime dataAbertura, BigDecimal fundoTrocoInicial,
                            BigDecimal totalVendasDinheiro, BigDecimal totalVendasCartaoCredito,
                            BigDecimal totalVendasCartaoDebito, BigDecimal totalVendasPix,
                            BigDecimal totalVendasValeCompra, BigDecimal totalSuprimentos,
                            BigDecimal totalSangriasResgateVale, BigDecimal totalSangriasOperacionais,
                            BigDecimal totalEstornosDinheiro, int totalVendasRealizadas, int totalItensVendidos) {
        this.dataAbertura = dataAbertura;
        this.fundoTrocoInicial = (fundoTrocoInicial != null ? fundoTrocoInicial : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalVendasDinheiro = (totalVendasDinheiro != null ? totalVendasDinheiro : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalVendasCartaoCredito = (totalVendasCartaoCredito != null ? totalVendasCartaoCredito : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalVendasCartaoDebito = (totalVendasCartaoDebito != null ? totalVendasCartaoDebito : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalVendasPix = (totalVendasPix != null ? totalVendasPix : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalVendasValeCompra = (totalVendasValeCompra != null ? totalVendasValeCompra : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalSuprimentos = (totalSuprimentos != null ? totalSuprimentos : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalSangriasResgateVale = (totalSangriasResgateVale != null ? totalSangriasResgateVale : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalSangriasOperacionais = (totalSangriasOperacionais != null ? totalSangriasOperacionais : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalEstornosDinheiro = (totalEstornosDinheiro != null ? totalEstornosDinheiro : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.totalVendasRealizadas = totalVendasRealizadas;
        this.totalItensVendidos = totalItensVendidos;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public BigDecimal getFundoTrocoInicial() {
        return fundoTrocoInicial;
    }

    public BigDecimal getTotalVendasDinheiro() {
        return totalVendasDinheiro;
    }

    public BigDecimal getTotalVendasCartaoCredito() {
        return totalVendasCartaoCredito;
    }

    public BigDecimal getTotalVendasCartaoDebito() {
        return totalVendasCartaoDebito;
    }

    public BigDecimal getTotalVendasPix() {
        return totalVendasPix;
    }

    public BigDecimal getTotalVendasValeCompra() {
        return totalVendasValeCompra;
    }

    public BigDecimal getTotalSuprimentos() {
        return totalSuprimentos;
    }

    public BigDecimal getTotalSangriasResgateVale() {
        return totalSangriasResgateVale;
    }

    public BigDecimal getTotalSangriasOperacionais() {
        return totalSangriasOperacionais;
    }

    public BigDecimal getTotalEstornosDinheiro() {
        return totalEstornosDinheiro;
    }

    public int getTotalVendasRealizadas() {
        return totalVendasRealizadas;
    }

    public int getTotalItensVendidos() {
        return totalItensVendidos;
    }

    public BigDecimal getTotalEntradasDinheiro() {
        return fundoTrocoInicial.add(totalVendasDinheiro).add(totalSuprimentos);
    }

    public BigDecimal getTotalSaidasDinheiro() {
        return totalSangriasResgateVale.add(totalSangriasOperacionais).add(totalEstornosDinheiro);
    }

    public BigDecimal getSaldoEsperadoGaveta() {
        return getTotalEntradasDinheiro().subtract(getTotalSaidasDinheiro());
    }

    public BigDecimal getTotalFaturado() {
        return totalVendasDinheiro
                .add(totalVendasCartaoCredito)
                .add(totalVendasCartaoDebito)
                .add(totalVendasPix)
                .add(totalVendasValeCompra);
    }

    public String gerarRelatorioFechamento(BigDecimal valorFisicoInformado) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("            RELATÓRIO DE FECHAMENTO DE CAIXA        \n");
        sb.append("====================================================\n");
        if (dataAbertura != null) {
            sb.append(String.format("DATA/HORA ABERTURA:   %s\n", dataAbertura.format(FORMATO_DATA_HORA)));
        }
        sb.append(String.format("DATA/HORA FECHAMENTO: %s\n", LocalDateTime.now().format(FORMATO_DATA_HORA)));
        sb.append(String.format("VENDAS CONCLUÍDAS:    %d vendas (%d unidades)\n", totalVendasRealizadas, totalItensVendidos));
        sb.append("----------------------------------------------------\n");
        sb.append("FATURAMENTO POR FORMA DE PAGAMENTO:\n");
        sb.append(String.format("  (+) DINHEIRO:              R$ %15.2f\n", totalVendasDinheiro));
        sb.append(String.format("  (+) CARTÃO DE CRÉDITO:     R$ %15.2f\n", totalVendasCartaoCredito));
        sb.append(String.format("  (+) CARTÃO DE DÉBITO:      R$ %15.2f\n", totalVendasCartaoDebito));
        sb.append(String.format("  (+) PIX:                   R$ %15.2f\n", totalVendasPix));
        sb.append(String.format("  (+) VALE-COMPRA:           R$ %15.2f\n", totalVendasValeCompra));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("TOTAL GERAL FATURADO:        R$ %15.2f\n", getTotalFaturado()));
        sb.append("----------------------------------------------------\n");
        sb.append("MOVIMENTAÇÃO FÍSICA DA GAVETA (DINHEIRO EM ESPÉCIE):\n");
        sb.append(String.format("  (+) FUNDO DE TROCO INICIAL:R$ %15.2f\n", fundoTrocoInicial));
        sb.append(String.format("  (+) ENTRADAS POR VENDAS:   R$ %15.2f\n", totalVendasDinheiro));
        if (totalSuprimentos.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("  (+) SUPRIMENTOS DE TROCO:  R$ %15.2f\n", totalSuprimentos));
        }
        if (totalSangriasResgateVale.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("  (-) RESGATE VALE-COMPRA:   R$ %15.2f\n", totalSangriasResgateVale));
        }
        if (totalSangriasOperacionais.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("  (-) SANGRIA OPERACIONAL:   R$ %15.2f\n", totalSangriasOperacionais));
        }
        if (totalEstornosDinheiro.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("  (-) ESTORNOS EM DINHEIRO:  R$ %15.2f\n", totalEstornosDinheiro));
        }
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("SALDO ESPERADO NA GAVETA:    R$ %15.2f\n", getSaldoEsperadoGaveta()));

        if (valorFisicoInformado != null) {
            BigDecimal dif = valorFisicoInformado.subtract(getSaldoEsperadoGaveta());
            sb.append(String.format("VALOR FÍSICO CONTADO:        R$ %15.2f\n", valorFisicoInformado));
            if (dif.compareTo(BigDecimal.ZERO) == 0) {
                sb.append("SITUAÇÃO DA CONFERÊNCIA:     [✓] CAIXA EXATO (SEM DIFERENÇAS)\n");
            } else if (dif.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("SITUAÇÃO DA CONFERÊNCIA:     [!] SOBRA DE CAIXA (+ R$ %.2f)\n", dif));
            } else {
                sb.append(String.format("SITUAÇÃO DA CONFERÊNCIA:     [!] QUEBRA DE CAIXA (- R$ %.2f)\n", dif.abs()));
            }
        }
        sb.append("====================================================\n");
        return sb.toString();
    }
}
