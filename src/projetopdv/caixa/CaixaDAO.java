package projetopdv.caixa;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import projetopdv.dados.BancoDeDados;
import projetopdv.posvenda.StatusValeCompra;

public class CaixaDAO {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public MovimentacaoCaixa registrarMovimentacao(TipoMovimentacaoCaixa tipo, BigDecimal valor,
                                                   Integer idValeCompra, Integer idVenda,
                                                   String justificativa, Integer idOperador) {
        try (Connection conexao = BancoDeDados.conectar()) {
            return registrarMovimentacao(conexao, tipo, valor, idValeCompra, idVenda, justificativa, idOperador);
        } catch (SQLException e) {
            System.out.println("Erro ao registrar movimentação de caixa: " + e.getMessage());
            return null;
        }
    }

    public MovimentacaoCaixa registrarMovimentacao(Connection conexao, TipoMovimentacaoCaixa tipo, BigDecimal valor,
                                                   Integer idValeCompra, Integer idVenda,
                                                   String justificativa, Integer idOperador) throws SQLException {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de movimentação obrigatório.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da movimentação deve ser positivo.");
        }

        long valorCentavos = valor.movePointRight(2).longValueExact();
        LocalDateTime agora = LocalDateTime.now();
        String dataFormatada = agora.format(FORMATO_DATA_HORA);
        String justTratada = justificativa != null ? justificativa.trim() : "";

        String sql = """
            INSERT INTO movimentacao_caixa
                (tipo_movimentacao, valor_centavos, data_hora, vale_compra_id, venda_id, justificativa, operador_id)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        try (PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tipo.name());
            ps.setLong(2, valorCentavos);
            ps.setString(3, dataFormatada);

            if (idValeCompra != null && idValeCompra > 0) {
                ps.setInt(4, idValeCompra);
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            if (idVenda != null && idVenda > 0) {
                ps.setInt(5, idVenda);
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, justTratada);

            if (idOperador != null && idOperador > 0) {
                ps.setInt(7, idOperador);
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    int idGerado = chaves.getInt(1);
                    return new MovimentacaoCaixa(idGerado, tipo, valor, agora, idValeCompra, idVenda, justTratada, idOperador);
                }
            }
        }
        throw new SQLException("Não foi possível obter o ID da movimentação gerada.");
    }

    public MovimentacaoCaixa registrarSangriaResgateVale(int idValeCompra, BigDecimal valorResgate,
                                                         String justificativa, Integer idOperador) {
        if (idValeCompra <= 0) {
            throw new IllegalArgumentException("ID do vale-compra inválido.");
        }
        if (valorResgate == null || valorResgate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do resgate deve ser maior que zero.");
        }

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            String sqlBuscaVale = "SELECT saldo_centavos, status_vale FROM vale_compra WHERE id_vale_compra = ?;";
            long saldoCentavosAtual;
            String statusVale;

            try (PreparedStatement ps = conexao.prepareStatement(sqlBuscaVale)) {
                ps.setInt(1, idValeCompra);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Vale-compra #" + idValeCompra + " não encontrado.");
                    }
                    saldoCentavosAtual = rs.getLong("saldo_centavos");
                    statusVale = rs.getString("status_vale");
                }
            }

            if (!StatusValeCompra.ATIVO.name().equals(statusVale)) {
                throw new IllegalStateException("Vale-compra não está ativo (Status: " + statusVale + ").");
            }

            long resgateCentavos = valorResgate.movePointRight(2).longValueExact();
            if (resgateCentavos > saldoCentavosAtual) {
                BigDecimal saldoDisponivel = BigDecimal.valueOf(saldoCentavosAtual).movePointLeft(2);
                throw new IllegalArgumentException(String.format(
                        "Valor solicitado para resgate (R$ %.2f) é superior ao saldo do vale (R$ %.2f).",
                        valorResgate, saldoDisponivel
                ));
            }

            long novoSaldoCentavos = saldoCentavosAtual - resgateCentavos;
            String novoStatus = (novoSaldoCentavos == 0)
                    ? StatusValeCompra.RESGATADO_SANGRIA.name()
                    : StatusValeCompra.ATIVO.name();

            String sqlAtualizaVale = """
                UPDATE vale_compra
                SET saldo_centavos = ?, status_vale = ?
                WHERE id_vale_compra = ?;
                """;

            try (PreparedStatement psAtualiza = conexao.prepareStatement(sqlAtualizaVale)) {
                psAtualiza.setLong(1, novoSaldoCentavos);
                psAtualiza.setString(2, novoStatus);
                psAtualiza.setInt(3, idValeCompra);
                psAtualiza.executeUpdate();
            }

            String justFinal = (justificativa != null && !justificativa.isBlank())
                    ? justificativa.trim()
                    : "Resgate em dinheiro de vale-compra #" + idValeCompra;

            MovimentacaoCaixa mov = registrarMovimentacao(
                    conexao,
                    TipoMovimentacaoCaixa.SANGRIA_RESGATE_VALE,
                    valorResgate,
                    idValeCompra,
                    null,
                    justFinal,
                    idOperador
            );

            conexao.commit();
            return mov;

        } catch (SQLException e) {
            System.out.println("Erro ao registrar sangria por resgate de vale: " + e.getMessage());
            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Erro no rollback da sangria: " + rollbackEx.getMessage());
                }
            }
            return null;
        } finally {
            if (conexao != null) {
                try {
                    conexao.setAutoCommit(true);
                    conexao.close();
                } catch (SQLException closeEx) {
                    System.out.println("Erro ao fechar conexão: " + closeEx.getMessage());
                }
            }
        }
    }

    public MovimentacaoCaixa registrarEstornoDinheiro(int idVenda, BigDecimal valorEstornado,
                                                      String justificativa, Integer idOperador,
                                                      Connection conexao) throws SQLException {
        String just = (justificativa != null && !justificativa.isBlank())
                ? justificativa.trim()
                : "Reembolso imediato em dinheiro de estorno da venda #" + idVenda;

        return registrarMovimentacao(
                conexao,
                TipoMovimentacaoCaixa.ESTORNO_VENDA_DINHEIRO,
                valorEstornado,
                null,
                idVenda,
                just,
                idOperador
        );
    }

    public List<MovimentacaoCaixa> listarTodas() {
        String sql = """
            SELECT id_movimentacao, tipo_movimentacao, valor_centavos, data_hora, vale_compra_id, venda_id, justificativa, operador_id
            FROM movimentacao_caixa
            ORDER BY id_movimentacao DESC;
            """;

        List<MovimentacaoCaixa> lista = new ArrayList<>();
        try (Connection conn = BancoDeDados.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(montarMovimentacao(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar movimentações de caixa: " + e.getMessage());
        }
        return lista;
    }

    public List<MovimentacaoCaixa> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        String sql = """
            SELECT id_movimentacao, tipo_movimentacao, valor_centavos, data_hora, vale_compra_id, venda_id, justificativa, operador_id
            FROM movimentacao_caixa
            ORDER BY id_movimentacao ASC;
            """;

        List<MovimentacaoCaixa> filtradas = new ArrayList<>();
        try (Connection conn = BancoDeDados.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                MovimentacaoCaixa mov = montarMovimentacao(rs);
                boolean depoisInicio = (inicio == null) || !mov.getDataHora().isBefore(inicio);
                boolean antesFim = (fim == null) || !mov.getDataHora().isAfter(fim);

                if (depoisInicio && antesFim) {
                    filtradas.add(mov);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar movimentações por período: " + e.getMessage());
        }
        return filtradas;
    }

    public BigDecimal calcularSaldoDinheiroEmCaixa() {
        BigDecimal totalEntradas = BigDecimal.ZERO;
        BigDecimal totalSaidas = BigDecimal.ZERO;

        String sqlVendasDinheiro = """
            SELECT SUM(valor_total_centavos)
            FROM venda
            WHERE UPPER(forma_pagamento) = 'DINHEIRO' AND status_venda != 'ESTORNADA';
            """;

        String sqlMovimentacoes = """
            SELECT tipo_movimentacao, SUM(valor_centavos)
            FROM movimentacao_caixa
            GROUP BY tipo_movimentacao;
            """;

        try (Connection conn = BancoDeDados.conectar()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rsVendas = stmt.executeQuery(sqlVendasDinheiro)) {
                if (rsVendas.next()) {
                    long centavosVendas = rsVendas.getLong(1);
                    totalEntradas = totalEntradas.add(BigDecimal.valueOf(centavosVendas).movePointLeft(2));
                }
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rsMov = stmt.executeQuery(sqlMovimentacoes)) {
                while (rsMov.next()) {
                    String tipoStr = rsMov.getString(1);
                    long centavos = rsMov.getLong(2);
                    BigDecimal valor = BigDecimal.valueOf(centavos).movePointLeft(2);
                    TipoMovimentacaoCaixa tipo = TipoMovimentacaoCaixa.fromString(tipoStr);

                    if (tipo == TipoMovimentacaoCaixa.SUPRIMENTO_TROCO || tipo == TipoMovimentacaoCaixa.ABERTURA_CAIXA) {
                        totalEntradas = totalEntradas.add(valor);
                    } else if (tipo == TipoMovimentacaoCaixa.SANGRIA_OPERACIONAL
                            || tipo == TipoMovimentacaoCaixa.SANGRIA_RESGATE_VALE
                            || tipo == TipoMovimentacaoCaixa.ESTORNO_VENDA_DINHEIRO) {
                        totalSaidas = totalSaidas.add(valor);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao calcular saldo em caixa: " + e.getMessage());
        }

        return totalEntradas.subtract(totalSaidas);
    }

    public boolean isCaixaAberto() {
        String sql = """
            SELECT tipo_movimentacao FROM movimentacao_caixa
            WHERE tipo_movimentacao IN ('ABERTURA_CAIXA', 'FECHAMENTO_CAIXA')
            ORDER BY id_movimentacao DESC
            LIMIT 1;
            """;
        try (Connection conn = BancoDeDados.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return TipoMovimentacaoCaixa.ABERTURA_CAIXA.name().equals(rs.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("Erro ao verificar status do caixa: " + e.getMessage());
        }
        return false;
    }

    public MovimentacaoCaixa obterUltimaAbertura() {
        String sql = """
            SELECT * FROM movimentacao_caixa
            WHERE tipo_movimentacao = 'ABERTURA_CAIXA'
            ORDER BY id_movimentacao DESC
            LIMIT 1;
            """;
        try (Connection conn = BancoDeDados.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return montarMovimentacao(rs);
            }
        } catch (SQLException e) {
            System.out.println("Erro ao obter última abertura de caixa: " + e.getMessage());
        }
        return null;
    }

    public MovimentacaoCaixa registrarAberturaCaixa(BigDecimal fundoTroco, String justificativa, Integer idOperador) {
        if (isCaixaAberto()) {
            throw new IllegalStateException("O caixa já se encontra aberto.");
        }
        if (fundoTroco == null || fundoTroco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O fundo de troco não pode ser negativo.");
        }
        String just = (justificativa != null && !justificativa.isBlank())
                ? justificativa.trim()
                : "Abertura de caixa com fundo de troco inicial";
        return registrarMovimentacao(TipoMovimentacaoCaixa.ABERTURA_CAIXA, fundoTroco, null, null, just, idOperador);
    }

    public MovimentacaoCaixa registrarFechamentoCaixa(BigDecimal valorContadoFisico, String justificativa, Integer idOperador) {
        if (!isCaixaAberto()) {
            throw new IllegalStateException("Não é possível fechar o caixa pois ele já se encontra fechado.");
        }
        BigDecimal valorFinal = (valorContadoFisico != null && valorContadoFisico.compareTo(BigDecimal.ZERO) >= 0)
                ? valorContadoFisico
                : BigDecimal.ZERO;
        String just = (justificativa != null && !justificativa.isBlank())
                ? justificativa.trim()
                : "Fechamento de turno de caixa";
        return registrarMovimentacao(TipoMovimentacaoCaixa.FECHAMENTO_CAIXA, valorFinal, null, null, just, idOperador);
    }

    public MovimentacaoCaixa registrarSangriaOperacional(BigDecimal valor, String justificativa, Integer idOperador) {
        if (!isCaixaAberto()) {
            throw new IllegalStateException("O caixa físico precisa estar aberto para realizar sangria.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da sangria deve ser maior que zero.");
        }
        BigDecimal saldoAtual = calcularSaldoDinheiroEmCaixa();
        if (valor.compareTo(saldoAtual) > 0) {
            throw new IllegalArgumentException(String.format("Saldo insuficiente em caixa (R$ %.2f) para sangria de R$ %.2f.", saldoAtual, valor));
        }
        String just = (justificativa != null && !justificativa.isBlank())
                ? justificativa.trim()
                : "Sangria operacional de dinheiro";
        return registrarMovimentacao(TipoMovimentacaoCaixa.SANGRIA_OPERACIONAL, valor, null, null, just, idOperador);
    }

    public MovimentacaoCaixa registrarSuprimento(BigDecimal valor, String justificativa, Integer idOperador) {
        if (!isCaixaAberto()) {
            throw new IllegalStateException("O caixa físico precisa estar aberto para realizar suprimento.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do suprimento deve ser maior que zero.");
        }
        String just = (justificativa != null && !justificativa.isBlank())
                ? justificativa.trim()
                : "Suprimento de troco";
        return registrarMovimentacao(TipoMovimentacaoCaixa.SUPRIMENTO_TROCO, valor, null, null, just, idOperador);
    }

    public ResumoTurnoCaixa obterResumoTurnoAtual() {
        MovimentacaoCaixa abertura = obterUltimaAbertura();
        LocalDateTime dataAbertura = abertura != null ? abertura.getDataHora() : null;
        BigDecimal fundoTroco = abertura != null ? abertura.getValor() : BigDecimal.ZERO;

        BigDecimal totalDinheiro = BigDecimal.ZERO;
        BigDecimal totalCartaoCredito = BigDecimal.ZERO;
        BigDecimal totalCartaoDebito = BigDecimal.ZERO;
        BigDecimal totalPix = BigDecimal.ZERO;
        BigDecimal totalValeCompra = BigDecimal.ZERO;
        int totalVendas = 0;
        int totalItens = 0;

        BigDecimal totalSuprimentos = BigDecimal.ZERO;
        BigDecimal totalSangriasResgateVale = BigDecimal.ZERO;
        BigDecimal totalSangriasOperacionais = BigDecimal.ZERO;
        BigDecimal totalEstornosDinheiro = BigDecimal.ZERO;

        String sqlVendas = "SELECT forma_pagamento, valor_total_centavos FROM venda WHERE status_venda != 'ESTORNADA'";
        if (dataAbertura != null) {
            sqlVendas += " AND data_venda >= '" + dataAbertura.format(FORMATO_DATA_HORA) + "'";
        }

        try (Connection conn = BancoDeDados.conectar()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlVendas)) {
                while (rs.next()) {
                    String forma = rs.getString("forma_pagamento");
                    long centavos = rs.getLong("valor_total_centavos");
                    BigDecimal valor = BigDecimal.valueOf(centavos).movePointLeft(2);
                    totalVendas++;

                    if ("Dinheiro".equalsIgnoreCase(forma) || "DINHEIRO".equalsIgnoreCase(forma)) {
                        totalDinheiro = totalDinheiro.add(valor);
                    } else if (forma != null && forma.toUpperCase().contains("CRÉDITO")) {
                        totalCartaoCredito = totalCartaoCredito.add(valor);
                    } else if (forma != null && forma.toUpperCase().contains("DÉBITO")) {
                        totalCartaoDebito = totalCartaoDebito.add(valor);
                    } else if ("PIX".equalsIgnoreCase(forma)) {
                        totalPix = totalPix.add(valor);
                    } else if (forma != null && forma.toUpperCase().contains("VALE")) {
                        totalValeCompra = totalValeCompra.add(valor);
                    } else {
                        totalDinheiro = totalDinheiro.add(valor);
                    }
                }
            }

            String sqlItens = """
                SELECT COALESCE(SUM(iv.quantidade), 0)
                FROM item_venda iv
                INNER JOIN venda v ON iv.venda_id = v.id_venda
                WHERE v.status_venda != 'ESTORNADA'
                """;
            if (dataAbertura != null) {
                sqlItens += " AND v.data_venda >= '" + dataAbertura.format(FORMATO_DATA_HORA) + "'";
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlItens)) {
                if (rs.next()) {
                    totalItens = rs.getInt(1);
                }
            }

            String sqlMov = "SELECT tipo_movimentacao, valor_centavos FROM movimentacao_caixa WHERE 1=1";
            if (dataAbertura != null) {
                sqlMov += " AND data_hora >= '" + dataAbertura.format(FORMATO_DATA_HORA) + "'";
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlMov)) {
                while (rs.next()) {
                    String tipoStr = rs.getString("tipo_movimentacao");
                    long centavos = rs.getLong("valor_centavos");
                    BigDecimal val = BigDecimal.valueOf(centavos).movePointLeft(2);
                    TipoMovimentacaoCaixa tipo = TipoMovimentacaoCaixa.fromString(tipoStr);

                    switch (tipo) {
                        case SUPRIMENTO_TROCO -> totalSuprimentos = totalSuprimentos.add(val);
                        case SANGRIA_RESGATE_VALE -> totalSangriasResgateVale = totalSangriasResgateVale.add(val);
                        case SANGRIA_OPERACIONAL -> totalSangriasOperacionais = totalSangriasOperacionais.add(val);
                        case ESTORNO_VENDA_DINHEIRO -> totalEstornosDinheiro = totalEstornosDinheiro.add(val);
                        default -> {}
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao apurar resumo do turno do caixa: " + e.getMessage());
        }

        return new ResumoTurnoCaixa(
                dataAbertura,
                fundoTroco,
                totalDinheiro,
                totalCartaoCredito,
                totalCartaoDebito,
                totalPix,
                totalValeCompra,
                totalSuprimentos,
                totalSangriasResgateVale,
                totalSangriasOperacionais,
                totalEstornosDinheiro,
                totalVendas,
                totalItens
        );
    }

    private MovimentacaoCaixa montarMovimentacao(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_movimentacao");
        String tipoStr = rs.getString("tipo_movimentacao");
        long centavos = rs.getLong("valor_centavos");
        String dataStr = rs.getString("data_hora");
        int idVale = rs.getInt("vale_compra_id");
        boolean valeNulo = rs.wasNull();
        int idVenda = rs.getInt("venda_id");
        boolean vendaNula = rs.wasNull();
        String just = rs.getString("justificativa");
        int idOperador = rs.getInt("operador_id");
        boolean opNulo = rs.wasNull();

        LocalDateTime data = LocalDateTime.parse(dataStr, FORMATO_DATA_HORA);
        BigDecimal valor = BigDecimal.valueOf(centavos).movePointLeft(2);
        TipoMovimentacaoCaixa tipo = TipoMovimentacaoCaixa.fromString(tipoStr);

        return new MovimentacaoCaixa(
                id,
                tipo,
                valor,
                data,
                !valeNulo ? idVale : null,
                !vendaNula ? idVenda : null,
                just,
                !opNulo ? idOperador : null
        );
    }
}
