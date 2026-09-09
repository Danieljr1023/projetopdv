package projetopdv.posvenda;

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
import java.util.UUID;
import projetopdv.dados.BancoDeDados;

public class PosVendaDAO {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ValeCompra gerarValeCompra(int vendaOrigemId, Integer clienteId, BigDecimal valor, String observacoes) {
        try (Connection conexao = BancoDeDados.conectar()) {
            return gerarValeCompra(vendaOrigemId, clienteId, valor, observacoes, conexao);
        } catch (SQLException e) {
            System.out.println("Erro ao gerar vale-compra: " + e.getMessage());
            return null;
        }
    }

    public ValeCompra gerarValeCompra(int vendaOrigemId, Integer clienteId, BigDecimal valor, String observacoes,
                                      Connection conexao) throws SQLException {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do vale-compra deve ser positivo.");
        }

        long centavos = valor.movePointRight(2).longValueExact();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime validade = agora.plusDays(90);
        String dataEmissaoFormatada = agora.format(FORMATO_DATA_HORA);
        String dataValidadeFormatada = validade.format(FORMATO_DATA_HORA);

        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String codigoVale = String.format("VALE-%s-%s", agora.format(DateTimeFormatter.ofPattern("yyyyMMdd")), sufixo);

        String sql = """
            INSERT INTO vale_compra
                (codigo_vale, cliente_id, venda_origem_id, valor_original_centavos, saldo_centavos, data_emissao, data_validade, status_vale, observacoes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

        try (PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, codigoVale);

            if (clienteId != null && clienteId > 0) {
                ps.setInt(2, clienteId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            if (vendaOrigemId > 0) {
                ps.setInt(3, vendaOrigemId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setLong(4, centavos);
            ps.setLong(5, centavos);
            ps.setString(6, dataEmissaoFormatada);
            ps.setString(7, dataValidadeFormatada);
            ps.setString(8, StatusValeCompra.ATIVO.name());
            ps.setString(9, observacoes != null ? observacoes.trim() : "");

            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    int idGerado = chaves.getInt(1);
                    return new ValeCompra(
                            idGerado,
                            codigoVale,
                            clienteId,
                            vendaOrigemId > 0 ? vendaOrigemId : null,
                            valor,
                            valor,
                            agora,
                            validade,
                            StatusValeCompra.ATIVO,
                            observacoes
                    );
                }
            }
        }
        throw new SQLException("Não foi possível obter o ID gerado para o vale-compra.");
    }

    public ValeCompra buscarValePorCodigo(String codigoVale) {
        if (codigoVale == null || codigoVale.isBlank()) return null;
        String sql = "SELECT * FROM vale_compra WHERE codigo_vale = ?;";

        try (Connection conn = BancoDeDados.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigoVale.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarVale(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao buscar vale-compra por código: " + e.getMessage());
        }
        return null;
    }

    public ValeCompra buscarValePorId(int idVale) {
        if (idVale <= 0) return null;
        String sql = "SELECT * FROM vale_compra WHERE id_vale_compra = ?;";

        try (Connection conn = BancoDeDados.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idVale);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarVale(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao buscar vale-compra por ID: " + e.getMessage());
        }
        return null;
    }

    public List<ValeCompra> listarValesAtivos() {
        String sql = "SELECT * FROM vale_compra WHERE status_vale = 'ATIVO' AND saldo_centavos > 0 ORDER BY id_vale_compra DESC;";
        List<ValeCompra> lista = new ArrayList<>();

        try (Connection conn = BancoDeDados.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(montarVale(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar vales-compra ativos: " + e.getMessage());
        }
        return lista;
    }

    public List<ValeCompra> listarValesPorCliente(int idCliente) {
        String sql = "SELECT * FROM vale_compra WHERE cliente_id = ? ORDER BY id_vale_compra DESC;";
        List<ValeCompra> lista = new ArrayList<>();

        try (Connection conn = BancoDeDados.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarVale(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar vales-compra por cliente: " + e.getMessage());
        }
        return lista;
    }

    public boolean abaterSaldoVale(int idVale, BigDecimal valorAbatido, Connection conexao) throws SQLException {
        if (idVale <= 0 || valorAbatido == null || valorAbatido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Dados inválidos para abatimento de saldo.");
        }

        String sqlBusca = "SELECT saldo_centavos, status_vale FROM vale_compra WHERE id_vale_compra = ?;";
        long saldoAtual;
        String status;

        try (PreparedStatement ps = conexao.prepareStatement(sqlBusca)) {
            ps.setInt(1, idVale);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                saldoAtual = rs.getLong("saldo_centavos");
                status = rs.getString("status_vale");
            }
        }

        if (!StatusValeCompra.ATIVO.name().equals(status)) {
            throw new IllegalStateException("Vale-compra não está disponível para uso.");
        }

        long abatimentoCentavos = valorAbatido.movePointRight(2).longValueExact();
        if (abatimentoCentavos > saldoAtual) {
            throw new IllegalArgumentException("Valor a abater é maior que o saldo do vale.");
        }

        long novoSaldo = saldoAtual - abatimentoCentavos;
        String novoStatus = (novoSaldo == 0) ? StatusValeCompra.UTILIZADO.name() : StatusValeCompra.ATIVO.name();

        String sqlUpdate = "UPDATE vale_compra SET saldo_centavos = ?, status_vale = ? WHERE id_vale_compra = ?;";
        try (PreparedStatement psUpdate = conexao.prepareStatement(sqlUpdate)) {
            psUpdate.setLong(1, novoSaldo);
            psUpdate.setString(2, novoStatus);
            psUpdate.setInt(3, idVale);
            return psUpdate.executeUpdate() > 0;
        }
    }

    public int getQuantidadeJaDevolvida(int idItemVenda, Connection conexao) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantidade_devolvida), 0) FROM devolucao_item WHERE item_venda_id = ?;";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idItemVenda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public DevolucaoItem processarDevolucaoItem(int idVenda, int idItemVenda, int quantidade, String motivo,
                                                boolean emitirValeCompra, Integer idCliente) {
        if (idVenda <= 0) {
            throw new IllegalArgumentException("ID da venda inválido.");
        }
        if (idItemVenda <= 0) {
            throw new IllegalArgumentException("ID do item inválido.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade a devolver deve ser maior que zero.");
        }

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            // 1. Validar venda
            String sqlVenda = "SELECT status_venda FROM venda WHERE id_venda = ?;";
            try (PreparedStatement psVenda = conexao.prepareStatement(sqlVenda)) {
                psVenda.setInt(1, idVenda);
                try (ResultSet rsVenda = psVenda.executeQuery()) {
                    if (!rsVenda.next()) {
                        throw new IllegalStateException("Venda #" + idVenda + " não encontrada.");
                    }
                    if ("ESTORNADA".equals(rsVenda.getString("status_venda"))) {
                        throw new IllegalStateException("Não é possível realizar devolução de uma venda já estornada.");
                    }
                }
            }

            // 2. Validar item da venda
            String sqlItem = "SELECT quantidade, preco_unitario_centavos, nome_produto FROM item_venda WHERE id_item_venda = ? AND venda_id = ?;";
            int qtdVendida;
            long precoUnitarioCentavos;
            String nomeProduto;

            try (PreparedStatement psItem = conexao.prepareStatement(sqlItem)) {
                psItem.setInt(1, idItemVenda);
                psItem.setInt(2, idVenda);
                try (ResultSet rsItem = psItem.executeQuery()) {
                    if (!rsItem.next()) {
                        throw new IllegalStateException("Item #" + idItemVenda + " não pertence à venda #" + idVenda + ".");
                    }
                    qtdVendida = rsItem.getInt("quantidade");
                    precoUnitarioCentavos = rsItem.getLong("preco_unitario_centavos");
                    nomeProduto = rsItem.getString("nome_produto");
                }
            }

            // 3. Checar quantidade já devolvida
            int qtdJaDevolvida = getQuantidadeJaDevolvida(idItemVenda, conexao);
            int qtdDisponivel = qtdVendida - qtdJaDevolvida;

            if (quantidade > qtdDisponivel) {
                throw new IllegalArgumentException(String.format(
                        "Quantidade a devolver (%d) excede a quantidade disponível para devolução (%d). (Comprados: %d, Devolvidos anteriormente: %d)",
                        quantidade, qtdDisponivel, qtdVendida, qtdJaDevolvida
                ));
            }

            BigDecimal precoUnitario = BigDecimal.valueOf(precoUnitarioCentavos).movePointLeft(2);
            BigDecimal valorTotalReembolso = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
            long valorTotalCentavos = valorTotalReembolso.movePointRight(2).longValueExact();

            Integer idValeGerado = null;
            if (emitirValeCompra) {
                String obs = String.format("Vale gerado pela devolução de %d un do produto '%s' da venda #%d",
                        quantidade, nomeProduto, idVenda);
                ValeCompra vale = gerarValeCompra(idVenda, idCliente, valorTotalReembolso, obs, conexao);
                idValeGerado = vale.getIdValeCompra();
            }

            LocalDateTime agora = LocalDateTime.now();
            String dataDevFormatada = agora.format(FORMATO_DATA_HORA);
            String motivoFinal = (motivo != null && !motivo.isBlank()) ? motivo.trim() : "Devolução de produto";

            String sqlInsertDev = """
                INSERT INTO devolucao_item
                    (venda_id, item_venda_id, quantidade_devolvida, valor_unitario_centavos, valor_total_centavos, motivo, data_devolucao, vale_compra_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                """;

            int idDevGerado;
            try (PreparedStatement psInsert = conexao.prepareStatement(sqlInsertDev, Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setInt(1, idVenda);
                psInsert.setInt(2, idItemVenda);
                psInsert.setInt(3, quantidade);
                psInsert.setLong(4, precoUnitarioCentavos);
                psInsert.setLong(5, valorTotalCentavos);
                psInsert.setString(6, motivoFinal);
                psInsert.setString(7, dataDevFormatada);

                if (idValeGerado != null) {
                    psInsert.setInt(8, idValeGerado);
                } else {
                    psInsert.setNull(8, Types.INTEGER);
                }

                psInsert.executeUpdate();
                try (ResultSet chaves = psInsert.getGeneratedKeys()) {
                    if (chaves.next()) {
                        idDevGerado = chaves.getInt(1);
                    } else {
                        throw new SQLException("Não foi possível obter o ID da devolução gerada.");
                    }
                }
            }

            conexao.commit();

            return new DevolucaoItem(
                    idDevGerado,
                    idVenda,
                    idItemVenda,
                    quantidade,
                    precoUnitario,
                    valorTotalReembolso,
                    motivoFinal,
                    agora,
                    idValeGerado
            );

        } catch (SQLException e) {
            System.out.println("Erro ao processar devolução do item: " + e.getMessage());
            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Erro no rollback da devolução: " + rollbackEx.getMessage());
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

    public List<DevolucaoItem> listarDevolucoesPorVenda(int idVenda) {
        String sql = "SELECT * FROM devolucao_item WHERE venda_id = ? ORDER BY id_devolucao ASC;";
        List<DevolucaoItem> lista = new ArrayList<>();

        try (Connection conn = BancoDeDados.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idVenda);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarDevolucao(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar devoluções da venda: " + e.getMessage());
        }
        return lista;
    }

    private ValeCompra montarVale(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_vale_compra");
        String codigo = rs.getString("codigo_vale");
        int idCliente = rs.getInt("cliente_id");
        boolean clienteNulo = rs.wasNull();
        int idVendaOrigem = rs.getInt("venda_origem_id");
        boolean vendaNula = rs.wasNull();
        long originalCentavos = rs.getLong("valor_original_centavos");
        long saldoCentavos = rs.getLong("saldo_centavos");
        String emissaoStr = rs.getString("data_emissao");
        String validadeStr = rs.getString("data_validade");
        String statusStr = rs.getString("status_vale");
        String obs = rs.getString("observacoes");

        LocalDateTime emissao = LocalDateTime.parse(emissaoStr, FORMATO_DATA_HORA);
        LocalDateTime validade = (validadeStr != null && !validadeStr.isBlank())
                ? LocalDateTime.parse(validadeStr, FORMATO_DATA_HORA)
                : null;

        BigDecimal original = BigDecimal.valueOf(originalCentavos).movePointLeft(2);
        BigDecimal saldo = BigDecimal.valueOf(saldoCentavos).movePointLeft(2);
        StatusValeCompra status = StatusValeCompra.fromString(statusStr);

        return new ValeCompra(
                id,
                codigo,
                !clienteNulo ? idCliente : null,
                !vendaNula ? idVendaOrigem : null,
                original,
                saldo,
                emissao,
                validade,
                status,
                obs
        );
    }

    private DevolucaoItem montarDevolucao(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_devolucao");
        int idVenda = rs.getInt("venda_id");
        int idItemVenda = rs.getInt("item_venda_id");
        int qtd = rs.getInt("quantidade_devolvida");
        long unitCentavos = rs.getLong("valor_unitario_centavos");
        long totalCentavos = rs.getLong("valor_total_centavos");
        String motivo = rs.getString("motivo");
        String dataStr = rs.getString("data_devolucao");
        int idVale = rs.getInt("vale_compra_id");
        boolean valeNulo = rs.wasNull();

        LocalDateTime dataHora = LocalDateTime.parse(dataStr, FORMATO_DATA_HORA);
        BigDecimal unitario = BigDecimal.valueOf(unitCentavos).movePointLeft(2);
        BigDecimal total = BigDecimal.valueOf(totalCentavos).movePointLeft(2);

        return new DevolucaoItem(
                id,
                idVenda,
                idItemVenda,
                qtd,
                unitario,
                total,
                motivo,
                dataHora,
                !valeNulo ? idVale : null
        );
    }
}
