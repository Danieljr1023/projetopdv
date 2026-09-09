package projetopdv.orcamento;

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
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.venda.Venda;
import projetopdv.venda.VendaDAO;

public class OrcamentoDAO {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FORMATO_DATA_HORA_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClienteDAO clienteDAO;
    private final ProdutoDAO produtoDAO;
    private VendaDAO vendaDAO;

    public OrcamentoDAO() {
        this.clienteDAO = new ClienteDAO();
        this.produtoDAO = new ProdutoDAO();
    }

    public OrcamentoDAO(ClienteDAO clienteDAO, ProdutoDAO produtoDAO) {
        this.clienteDAO = clienteDAO != null ? clienteDAO : new ClienteDAO();
        this.produtoDAO = produtoDAO != null ? produtoDAO : new ProdutoDAO();
    }

    private VendaDAO getVendaDAO() {
        if (this.vendaDAO == null) {
            this.vendaDAO = new VendaDAO();
        }
        return this.vendaDAO;
    }

    public Orcamento cadastrar(String nomeOrcamento) {
        return cadastrar(nomeOrcamento, (Integer) null, List.of());
    }

    public Orcamento cadastrar(String nomeOrcamento, Cliente cliente) {
        return cadastrar(nomeOrcamento, cliente != null ? cliente.getIdCliente() : null, List.of());
    }

    public Orcamento cadastrar(String nomeOrcamento, Integer idCliente) {
        return cadastrar(nomeOrcamento, idCliente, List.of());
    }

    public Orcamento cadastrar(String nomeOrcamento, Cliente cliente, List<ItemOrcamento> itens) {
        return cadastrar(nomeOrcamento, cliente != null ? cliente.getIdCliente() : null, itens);
    }

    public Orcamento cadastrar(String nomeOrcamento, Integer idCliente, List<ItemOrcamento> itens) {
        List<ItemOrcamento> itensValidos = itens != null ? new ArrayList<>(itens) : new ArrayList<>();

        BigDecimal total = itensValidos.stream()
                .map(ItemOrcamento::getValorItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCentavos = total.movePointRight(2).longValueExact();
        LocalDateTime agora = LocalDateTime.now();
        String dataFormatada = agora.format(FORMATO_DATA_HORA);
        String nomeTratado = nomeOrcamento != null ? nomeOrcamento.trim() : "";

        String sqlOrcamento = """
            INSERT INTO orcamento
                (nome_orcamento, status_orcamento, data_orcamento, valor_total_centavos, cliente_id)
            VALUES (?, ?, ?, ?, ?);
            """;

        String sqlItem = """
            INSERT INTO item_orcamento
                (orcamento_id, numero_item, produto_id, nome_produto, codigo_barras, quantidade, preco_unitario_tabela_centavos, preco_unitario_centavos)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """;

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            int idOrcamentoGerado;

            try (PreparedStatement comandoOrcamento = conexao.prepareStatement(sqlOrcamento, Statement.RETURN_GENERATED_KEYS)) {
                comandoOrcamento.setString(1, nomeTratado);
                comandoOrcamento.setString(2, StatusOrcamento.ABERTO.name());
                comandoOrcamento.setString(3, dataFormatada);
                comandoOrcamento.setLong(4, totalCentavos);

                if (idCliente != null && idCliente > 0) {
                    comandoOrcamento.setInt(5, idCliente);
                } else {
                    comandoOrcamento.setNull(5, Types.INTEGER);
                }

                comandoOrcamento.executeUpdate();

                try (ResultSet chaves = comandoOrcamento.getGeneratedKeys()) {
                    if (chaves.next()) {
                        idOrcamentoGerado = chaves.getInt(1);
                    } else {
                        throw new SQLException("O banco não retornou o ID do orçamento gerado.");
                    }
                }
            }

            int seqItem = 1;
            for (ItemOrcamento item : itensValidos) {
                try (PreparedStatement comandoItem = conexao.prepareStatement(sqlItem)) {
                    int numeroItem = item.getNumeroItem() > 0 ? item.getNumeroItem() : seqItem;
                    item.setNumeroItem(numeroItem);

                    comandoItem.setInt(1, idOrcamentoGerado);
                    comandoItem.setInt(2, numeroItem);

                    if (item.getIdProduto() > 0) {
                        comandoItem.setInt(3, item.getIdProduto());
                    } else {
                        comandoItem.setNull(3, Types.INTEGER);
                    }

                    comandoItem.setString(4, item.getNomeProduto());
                    comandoItem.setString(5, item.getCodigoBarras());
                    comandoItem.setInt(6, item.getQuantidade());
                    comandoItem.setLong(7, item.getPrecoUnitarioTabela().movePointRight(2).longValueExact());
                    comandoItem.setLong(8, item.getPrecoUnitario().movePointRight(2).longValueExact());

                    comandoItem.executeUpdate();
                    seqItem++;
                }
            }

            conexao.commit();

            Cliente cliente = null;
            if (idCliente != null && idCliente > 0) {
                cliente = clienteDAO.buscarPorId(idCliente);
            }

            return new Orcamento(
                    idOrcamentoGerado,
                    nomeTratado,
                    itensValidos,
                    agora,
                    cliente,
                    StatusOrcamento.ABERTO,
                    null
            );

        } catch (SQLException e) {
            System.out.println("Erro ao cadastrar orçamento:");
            System.out.println(e.getMessage());

            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Erro ao reverter transação: " + rollbackEx.getMessage());
                }
            }
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

        return null;
    }

    public ItemOrcamento adicionarItem(int idOrcamento, Produto produto, int quantidade) throws SQLException {
        if (produto == null) {
            throw new IllegalArgumentException("Produto inválido.");
        }
        return adicionarItem(idOrcamento, produto, quantidade, produto.getPrecoVenda(), produto.getPrecoVenda());
    }

    public ItemOrcamento adicionarItem(int idOrcamento, Produto produto, int quantidade, BigDecimal precoUnitarioLiquido) throws SQLException {
        if (produto == null) {
            throw new IllegalArgumentException("Produto inválido.");
        }
        BigDecimal precoTabela = (produto.getPrecoVenda() != null) ? produto.getPrecoVenda() : precoUnitarioLiquido;
        return adicionarItem(idOrcamento, produto, quantidade, precoTabela, precoUnitarioLiquido);
    }

    public ItemOrcamento adicionarItem(int idOrcamento, Produto produto, int quantidade, BigDecimal precoUnitarioTabela, BigDecimal precoUnitarioLiquido) throws SQLException {
        if (produto == null) {
            throw new IllegalArgumentException("Produto inválido.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (precoUnitarioTabela == null || precoUnitarioTabela.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço unitário de tabela não pode ser nulo ou negativo.");
        }
        if (precoUnitarioLiquido == null || precoUnitarioLiquido.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço unitário líquido não pode ser nulo ou negativo.");
        }

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
            try (PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)) {
                stmtVerifica.setInt(1, idOrcamento);
                try (ResultSet rs = stmtVerifica.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
                    }
                    String statusStr = rs.getString("status_orcamento");
                    if (!StatusOrcamento.ABERTO.name().equals(statusStr)) {
                        throw new IllegalStateException("Não é possível adicionar itens em um orçamento com status " + statusStr + ".");
                    }
                }
            }

            int proximoNumero = 1;
            String sqlMaxNumero = "SELECT COALESCE(MAX(numero_item), 0) + 1 FROM item_orcamento WHERE orcamento_id = ?;";
            try (PreparedStatement stmtMax = conexao.prepareStatement(sqlMaxNumero)) {
                stmtMax.setInt(1, idOrcamento);
                try (ResultSet rs = stmtMax.executeQuery()) {
                    if (rs.next()) {
                        proximoNumero = rs.getInt(1);
                    }
                }
            }

            String sqlInsertItem = """
                INSERT INTO item_orcamento
                    (orcamento_id, numero_item, produto_id, nome_produto, codigo_barras, quantidade, preco_unitario_tabela_centavos, preco_unitario_centavos)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                """;

            long precoTabelaCentavos = precoUnitarioTabela.movePointRight(2).longValueExact();
            long precoLiquidoCentavos = precoUnitarioLiquido.movePointRight(2).longValueExact();
            try (PreparedStatement stmtItem = conexao.prepareStatement(sqlInsertItem)) {
                stmtItem.setInt(1, idOrcamento);
                stmtItem.setInt(2, proximoNumero);

                if (produto.getIdProduto() > 0) {
                    stmtItem.setInt(3, produto.getIdProduto());
                } else {
                    stmtItem.setNull(3, Types.INTEGER);
                }

                stmtItem.setString(4, produto.getNomeProduto());
                stmtItem.setString(5, produto.getCodBarras());
                stmtItem.setInt(6, quantidade);
                stmtItem.setLong(7, precoTabelaCentavos);
                stmtItem.setLong(8, precoLiquidoCentavos);
                stmtItem.executeUpdate();
            }

            recalcularEAtualizarTotal(idOrcamento, conexao);

            conexao.commit();

            return new ItemOrcamento(
                    proximoNumero,
                    produto,
                    produto.getNomeProduto(),
                    produto.getCodBarras(),
                    precoUnitarioTabela,
                    precoUnitarioLiquido,
                    quantidade
            );

        } catch (SQLException e) {
            if (conexao != null) {
                conexao.rollback();
            }
            throw e;
        } finally {
            if (conexao != null) {
                conexao.setAutoCommit(true);
                conexao.close();
            }
        }
    }

    public boolean removerItem(int idOrcamento, int numeroItem) throws SQLException {
        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
            try (PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)) {
                stmtVerifica.setInt(1, idOrcamento);
                try (ResultSet rs = stmtVerifica.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
                    }
                    String statusStr = rs.getString("status_orcamento");
                    if (!StatusOrcamento.ABERTO.name().equals(statusStr)) {
                        throw new IllegalStateException("Não é possível remover itens de um orçamento com status " + statusStr + ".");
                    }
                }
            }

            String sqlDelete = "DELETE FROM item_orcamento WHERE orcamento_id = ? AND numero_item = ?;";
            int linhas;
            try (PreparedStatement stmtDelete = conexao.prepareStatement(sqlDelete)) {
                stmtDelete.setInt(1, idOrcamento);
                stmtDelete.setInt(2, numeroItem);
                linhas = stmtDelete.executeUpdate();
            }

            if (linhas > 0) {
                recalcularEAtualizarTotal(idOrcamento, conexao);
                conexao.commit();
                return true;
            } else {
                conexao.rollback();
                return false;
            }

        } catch (SQLException e) {
            if (conexao != null) {
                conexao.rollback();
            }
            throw e;
        } finally {
            if (conexao != null) {
                conexao.setAutoCommit(true);
                conexao.close();
            }
        }
    }

    public boolean aplicarDescontoItem(int idOrcamento, int numeroItem, BigDecimal novoPrecoUnitario) throws SQLException {
        if (novoPrecoUnitario == null || novoPrecoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço unitário inválido.");
        }

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
            try (PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)) {
                stmtVerifica.setInt(1, idOrcamento);
                try (ResultSet rs = stmtVerifica.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
                    }
                    String statusStr = rs.getString("status_orcamento");
                    if (!StatusOrcamento.ABERTO.name().equals(statusStr) && !StatusOrcamento.FATURANDO.name().equals(statusStr)) {
                        throw new IllegalStateException("Não é possível alterar valores em orçamento no status " + statusStr + ".");
                    }
                }
            }

            String sqlUpdate = "UPDATE item_orcamento SET preco_unitario_centavos = ? WHERE orcamento_id = ? AND numero_item = ?;";
            int linhas;
            try (PreparedStatement stmt = conexao.prepareStatement(sqlUpdate)) {
                stmt.setLong(1, novoPrecoUnitario.movePointRight(2).longValueExact());
                stmt.setInt(2, idOrcamento);
                stmt.setInt(3, numeroItem);
                linhas = stmt.executeUpdate();
            }

            if (linhas > 0) {
                recalcularEAtualizarTotal(idOrcamento, conexao);
                conexao.commit();
                return true;
            } else {
                conexao.rollback();
                return false;
            }

        } catch (SQLException e) {
            if (conexao != null) {
                conexao.rollback();
            }
            throw e;
        } finally {
            if (conexao != null) {
                conexao.setAutoCommit(true);
                conexao.close();
            }
        }
    }

    public boolean alterarQuantidadeItem(int idOrcamento, int numeroItem, int novaQuantidade) throws SQLException {
        if (novaQuantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
            try (PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)) {
                stmtVerifica.setInt(1, idOrcamento);
                try (ResultSet rs = stmtVerifica.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
                    }
                    String statusStr = rs.getString("status_orcamento");
                    if (!StatusOrcamento.ABERTO.name().equals(statusStr)) {
                        throw new IllegalStateException("Não é possível alterar a quantidade de itens em um orçamento com status " + statusStr + ".");
                    }
                }
            }

            String sqlUpdate = "UPDATE item_orcamento SET quantidade = ? WHERE orcamento_id = ? AND numero_item = ?;";
            int linhas;
            try (PreparedStatement stmt = conexao.prepareStatement(sqlUpdate)) {
                stmt.setInt(1, novaQuantidade);
                stmt.setInt(2, idOrcamento);
                stmt.setInt(3, numeroItem);
                linhas = stmt.executeUpdate();
            }

            if (linhas > 0) {
                recalcularEAtualizarTotal(idOrcamento, conexao);
                conexao.commit();
                return true;
            } else {
                conexao.rollback();
                return false;
            }

        } catch (SQLException e) {
            if (conexao != null) {
                conexao.rollback();
            }
            throw e;
        } finally {
            if (conexao != null) {
                conexao.setAutoCommit(true);
                conexao.close();
            }
        }
    }

    public boolean sincronizarTodosItensComCatalogo(int idOrcamento) throws SQLException {
        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
            try (PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)) {
                stmtVerifica.setInt(1, idOrcamento);
                try (ResultSet rs = stmtVerifica.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
                    }
                    String statusStr = rs.getString("status_orcamento");
                    if (!StatusOrcamento.ABERTO.name().equals(statusStr)) {
                        throw new IllegalStateException("Só é possível sincronizar itens com o catálogo quando o orçamento estiver ABERTO.");
                    }
                }
            }

            String sqlSincronizar = """
                UPDATE item_orcamento
                SET
                    nome_produto = (SELECT nome_produto FROM produto WHERE produto.id_produto = item_orcamento.produto_id),
                    codigo_barras = (SELECT codigo_barras FROM produto WHERE produto.id_produto = item_orcamento.produto_id)
                WHERE orcamento_id = ? AND produto_id IS NOT NULL;
                """;

            try (PreparedStatement stmtSinc = conexao.prepareStatement(sqlSincronizar)) {
                stmtSinc.setInt(1, idOrcamento);
                stmtSinc.executeUpdate();
            }

            conexao.commit();
            return true;

        } catch (SQLException e) {
            if (conexao != null) {
                conexao.rollback();
            }
            throw e;
        } finally {
            if (conexao != null) {
                conexao.setAutoCommit(true);
                conexao.close();
            }
        }
    }

    public Orcamento buscarPorId(int idOrcamento) {
        String sqlOrcamento = """
            SELECT
                id_orcamento,
                nome_orcamento,
                status_orcamento,
                data_orcamento,
                valor_total_centavos,
                cliente_id
            FROM orcamento
            WHERE id_orcamento = ?;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comandoOrcamento = conexao.prepareStatement(sqlOrcamento)
        ) {
            comandoOrcamento.setInt(1, idOrcamento);

            try (ResultSet resultado = comandoOrcamento.executeQuery()) {
                if (resultado.next()) {
                    return montarOrcamento(resultado, conexao);
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar orçamento por ID:");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public List<Orcamento> listarTodos() throws SQLException {
        List<Orcamento> orcamentos = new ArrayList<>();

        String sql = """
            SELECT
                id_orcamento,
                nome_orcamento,
                status_orcamento,
                data_orcamento,
                valor_total_centavos,
                cliente_id
            FROM orcamento
            ORDER BY id_orcamento;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql);
                ResultSet resultado = comando.executeQuery()
        ) {
            while (resultado.next()) {
                orcamentos.add(montarOrcamento(resultado, conexao));
            }
        }

        return orcamentos;
    }

    public List<Orcamento> listarPorStatus(StatusOrcamento status) throws SQLException {
        if (status == null) {
            return listarTodos();
        }

        List<Orcamento> orcamentos = new ArrayList<>();

        String sql = """
            SELECT
                id_orcamento,
                nome_orcamento,
                status_orcamento,
                data_orcamento,
                valor_total_centavos,
                cliente_id
            FROM orcamento
            WHERE status_orcamento = ?
            ORDER BY id_orcamento;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {
            comando.setString(1, status.name());

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    orcamentos.add(montarOrcamento(resultado, conexao));
                }
            }
        }

        return orcamentos;
    }

    public List<Orcamento> listarPorCliente(int idCliente) throws SQLException {
        List<Orcamento> orcamentos = new ArrayList<>();

        String sql = """
            SELECT
                id_orcamento,
                nome_orcamento,
                status_orcamento,
                data_orcamento,
                valor_total_centavos,
                cliente_id
            FROM orcamento
            WHERE cliente_id = ?
            ORDER BY id_orcamento;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {
            comando.setInt(1, idCliente);

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    orcamentos.add(montarOrcamento(resultado, conexao));
                }
            }
        }

        return orcamentos;
    }

    public boolean atualizar(Orcamento orcamento) throws SQLException {
        if (orcamento == null) {
            return false;
        }

        String sqlOrcamento = """
            UPDATE orcamento
            SET
                nome_orcamento = ?,
                status_orcamento = ?,
                valor_total_centavos = ?,
                cliente_id = ?
            WHERE id_orcamento = ?;
            """;

        String sqlLimparItens = "DELETE FROM item_orcamento WHERE orcamento_id = ?;";

        String sqlInserirItem = """
            INSERT INTO item_orcamento
                (orcamento_id, numero_item, produto_id, nome_produto, codigo_barras, quantidade, preco_unitario_tabela_centavos, preco_unitario_centavos)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """;

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
            try (PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)) {
                stmtVerifica.setInt(1, orcamento.getIdOrcamento());
                try (ResultSet rs = stmtVerifica.executeQuery()) {
                    if (!rs.next()) {
                        conexao.rollback();
                        return false;
                    }
                    String statusAtual = rs.getString("status_orcamento");
                    if (StatusOrcamento.FINALIZADO.name().equals(statusAtual)) {
                        throw new IllegalStateException("Orçamento no status FINALIZADO não pode ser modificado.");
                    }
                }
            }

            try (PreparedStatement comandoOrcamento = conexao.prepareStatement(sqlOrcamento)) {
                String nome;
                try {
                    nome = orcamento.getNomeOrcamento();
                } catch (IllegalStateException e) {
                    nome = "";
                }

                comandoOrcamento.setString(1, nome);
                comandoOrcamento.setString(2, orcamento.getStatusOrcamento().name());
                comandoOrcamento.setLong(3, orcamento.getValorTotal().movePointRight(2).longValueExact());

                if (orcamento.temCliente()) {
                    comandoOrcamento.setInt(4, orcamento.getIdCliente());
                } else {
                    comandoOrcamento.setNull(4, Types.INTEGER);
                }

                comandoOrcamento.setInt(5, orcamento.getIdOrcamento());

                int linhasAlteradas = comandoOrcamento.executeUpdate();
                if (linhasAlteradas == 0) {
                    conexao.rollback();
                    return false;
                }
            }

            try (PreparedStatement comandoLimpar = conexao.prepareStatement(sqlLimparItens)) {
                comandoLimpar.setInt(1, orcamento.getIdOrcamento());
                comandoLimpar.executeUpdate();
            }

            int seqItem = 1;
            for (ItemOrcamento item : orcamento.getItensOrcamento()) {
                try (PreparedStatement comandoItem = conexao.prepareStatement(sqlInserirItem)) {
                    int numeroItem = item.getNumeroItem() > 0 ? item.getNumeroItem() : seqItem;
                    item.setNumeroItem(numeroItem);

                    comandoItem.setInt(1, orcamento.getIdOrcamento());
                    comandoItem.setInt(2, numeroItem);

                    if (item.getIdProduto() > 0) {
                        comandoItem.setInt(3, item.getIdProduto());
                    } else {
                        comandoItem.setNull(3, Types.INTEGER);
                    }

                    comandoItem.setString(4, item.getNomeProduto());
                    comandoItem.setString(5, item.getCodigoBarras());
                    comandoItem.setInt(6, item.getQuantidade());
                    comandoItem.setLong(7, item.getPrecoUnitarioTabela().movePointRight(2).longValueExact());
                    comandoItem.setLong(8, item.getPrecoUnitarioLiquido().movePointRight(2).longValueExact());

                    comandoItem.executeUpdate();
                    seqItem++;
                }
            }

            conexao.commit();
            return true;

        } catch (SQLException e) {
            if (conexao != null) {
                conexao.rollback();
            }
            throw e;
        } finally {
            if (conexao != null) {
                conexao.setAutoCommit(true);
                conexao.close();
            }
        }
    }

    public boolean atualizarStatus(int idOrcamento, StatusOrcamento novoStatus) throws SQLException {
        if (novoStatus == null) {
            throw new IllegalArgumentException("Status não pode ser nulo.");
        }

        String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
        StatusOrcamento statusAtual;
        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)
        ) {
            stmtVerifica.setInt(1, idOrcamento);
            try (ResultSet rs = stmtVerifica.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                statusAtual = StatusOrcamento.valueOf(rs.getString("status_orcamento"));
            }
        }

        if (statusAtual == novoStatus) {
            return true;
        }

        // Validação estrita das regras de transição da máquina de estados
        switch (novoStatus) {
            case ABERTO -> {
                if (statusAtual != StatusOrcamento.CONFIRMADO && statusAtual != StatusOrcamento.CANCELADO) {
                    throw new IllegalStateException("Somente orçamentos confirmados ou cancelados podem ser abertos.");
                }
            }
            case CONFIRMADO -> {
                if (statusAtual != StatusOrcamento.ABERTO && statusAtual != StatusOrcamento.FATURANDO) {
                    throw new IllegalStateException("Somente orçamentos abertos ou em faturamento podem ser confirmados.");
                }
            }
            case CANCELADO -> {
                if (statusAtual != StatusOrcamento.ABERTO && statusAtual != StatusOrcamento.CONFIRMADO && statusAtual != StatusOrcamento.FATURANDO) {
                    throw new IllegalStateException("Somente orçamentos abertos, confirmados ou em faturamento podem ser cancelados.");
                }
            }
            case FATURANDO -> {
                if (statusAtual != StatusOrcamento.CONFIRMADO) {
                    throw new IllegalStateException("Somente orçamentos confirmados podem ser faturados.");
                }
            }
            case FINALIZADO -> throw new IllegalStateException(
                "Orçamento não pode ser finalizado diretamente por atualizarStatus. Utilize a finalização de venda."
            );
        }

        String sql = """
            UPDATE orcamento
            SET status_orcamento = ?
            WHERE id_orcamento = ?;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {
            comando.setString(1, novoStatus.name());
            comando.setInt(2, idOrcamento);

            return comando.executeUpdate() > 0;
        }
    }

    public boolean cancelarOrcamento(int idOrcamento) throws SQLException {
        return atualizarStatus(idOrcamento, StatusOrcamento.CANCELADO);
    }

    public boolean nomeConfirmadoJaExiste(String nome, int idOrcamentoIgnorar) throws SQLException {
        if (nome == null || nome.trim().isEmpty()) {
            return false;
        }
        String sql = """
            SELECT COUNT(*) FROM orcamento
            WHERE LOWER(nome_orcamento) = LOWER(?)
              AND status_orcamento = 'CONFIRMADO'
              AND id_orcamento != ?;
            """;
        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {
            comando.setString(1, nome.trim());
            comando.setInt(2, idOrcamentoIgnorar);
            try (ResultSet rs = comando.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<Orcamento> buscarPorNome(String termo) throws SQLException {
        List<Orcamento> lista = new ArrayList<>();
        if (termo == null || termo.trim().isEmpty()) {
            return lista;
        }

        String termoTratado = termo.trim();
        String padrao;
        if (termoTratado.contains("%")) {
            padrao = termoTratado.endsWith("%") ? termoTratado : termoTratado + "%";
        } else {
            padrao = "%" + termoTratado + "%";
        }
        padrao = padrao.toLowerCase();

        String sql = """
            SELECT
                id_orcamento,
                nome_orcamento,
                status_orcamento,
                data_orcamento,
                valor_total_centavos,
                cliente_id
            FROM orcamento
            WHERE LOWER(nome_orcamento) LIKE ?
            ORDER BY id_orcamento;
            """;
        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {
            comando.setString(1, padrao);
            try (ResultSet rs = comando.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarOrcamento(rs, conexao));
                }
            }
        }
        return lista;
    }

    public boolean renomear(int idOrcamento, String novoNome) throws SQLException {
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do orçamento não pode ser vazio.");
        }
        if (novoNome.contains("%")) {
            throw new IllegalArgumentException("O nome do orçamento não pode conter o caractere '%'.");
        }

        String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)
        ) {
            stmtVerifica.setInt(1, idOrcamento);
            try (ResultSet rs = stmtVerifica.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
                }
                String status = rs.getString("status_orcamento");
                if (!StatusOrcamento.ABERTO.name().equals(status)) {
                    throw new IllegalStateException("Apenas orçamentos ABERTOS podem ser renomeados.");
                }
            }

            String sqlUpdate = "UPDATE orcamento SET nome_orcamento = ? WHERE id_orcamento = ?;";
            try (PreparedStatement stmtUpdate = conexao.prepareStatement(sqlUpdate)) {
                stmtUpdate.setString(1, novoNome.trim());
                stmtUpdate.setInt(2, idOrcamento);
                return stmtUpdate.executeUpdate() > 0;
            }
        }
    }

    public Orcamento duplicar(int idOrcamentoOrigem) throws SQLException {
        Orcamento origem = buscarPorId(idOrcamentoOrigem);
        if (origem == null) {
            throw new IllegalArgumentException("Orçamento de origem #" + idOrcamentoOrigem + " não encontrado.");
        }

        String nomeBase;
        try {
            nomeBase = origem.getNomeOrcamento();
        } catch (Exception e) {
            nomeBase = "Orçamento #" + idOrcamentoOrigem;
        }

        String novoNome = "Cópia de " + nomeBase;
        if (novoNome.length() > 50) {
            novoNome = novoNome.substring(0, 50);
        }

        Orcamento novo = cadastrar(novoNome, (Integer) null);
        if (novo == null) {
            throw new SQLException("Erro ao criar novo orçamento duplicado.");
        }

        for (ItemOrcamento item : origem.getItensOrcamento()) {
            adicionarItem(novo.getIdOrcamento(), item.getProduto(), item.getQuantidade(), item.getPrecoUnitarioTabela(), item.getPrecoUnitarioLiquido());
        }

        return buscarPorId(novo.getIdOrcamento());
    }

    public boolean vincularCliente(int idOrcamento, Integer idCliente) throws SQLException {
        String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)
        ) {
            stmtVerifica.setInt(1, idOrcamento);
            try (ResultSet rs = stmtVerifica.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
                }
                String status = rs.getString("status_orcamento");
                if (!StatusOrcamento.ABERTO.name().equals(status) && !StatusOrcamento.FATURANDO.name().equals(status)) {
                    throw new IllegalStateException("Não é possível alterar cliente em orçamento no status " + status + ".");
                }
            }

            String sqlUpdate = "UPDATE orcamento SET cliente_id = ? WHERE id_orcamento = ?;";
            try (PreparedStatement stmtUpdate = conexao.prepareStatement(sqlUpdate)) {
                if (idCliente != null && idCliente > 0) {
                    stmtUpdate.setInt(1, idCliente);
                } else {
                    stmtUpdate.setNull(1, Types.INTEGER);
                }
                stmtUpdate.setInt(2, idOrcamento);
                return stmtUpdate.executeUpdate() > 0;
            }
        }
    }

    public boolean confirmarOrcamento(int idOrcamento, String nomeSeVazio) throws SQLException {
        Orcamento orc = buscarPorId(idOrcamento);
        if (orc == null) {
            throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
        }
        if (orc.getStatusOrcamento() != StatusOrcamento.ABERTO) {
            throw new IllegalStateException("Somente orçamentos abertos podem ser confirmados.");
        }

        String nomeFinal;
        try {
            nomeFinal = orc.getNomeOrcamento();
        } catch (Exception e) {
            nomeFinal = "";
        }

        if (nomeFinal.isEmpty() && nomeSeVazio != null && !nomeSeVazio.trim().isEmpty()) {
            nomeFinal = nomeSeVazio.trim();
        }
        if (nomeFinal.isEmpty()) {
            throw new IllegalStateException("Para confirmar o orçamento, é obrigatório definir um nome.");
        }
        if (nomeFinal.contains("%")) {
            throw new IllegalArgumentException("O nome do orçamento não pode conter o caractere '%'.");
        }

        if (nomeConfirmadoJaExiste(nomeFinal, idOrcamento)) {
            throw new IllegalStateException("Já existe outro orçamento CONFIRMADO com o nome '" + nomeFinal + "'. Escolha outro nome.");
        }

        String sql = "UPDATE orcamento SET nome_orcamento = ?, status_orcamento = ? WHERE id_orcamento = ?;";
        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement stmt = conexao.prepareStatement(sql)
        ) {
            stmt.setString(1, nomeFinal);
            stmt.setString(2, StatusOrcamento.CONFIRMADO.name());
            stmt.setInt(3, idOrcamento);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deletar(int idOrcamento) throws SQLException {
        String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement stmtVerifica = conexao.prepareStatement(sqlVerifica)
        ) {
            stmtVerifica.setInt(1, idOrcamento);
            try (ResultSet rs = stmtVerifica.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String status = rs.getString("status_orcamento");
                if (StatusOrcamento.FINALIZADO.name().equals(status)) {
                    throw new IllegalStateException("Orçamento FINALIZADO não pode ser deletado.");
                }
            }

            String sql = """
                DELETE FROM orcamento
                WHERE id_orcamento = ?;
                """;

            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setInt(1, idOrcamento);
                return comando.executeUpdate() > 0;
            }
        }
    }

    private void recalcularEAtualizarTotal(int idOrcamento, Connection conexao) throws SQLException {
        String sqlSoma = "SELECT COALESCE(SUM(quantidade * preco_unitario_centavos), 0) FROM item_orcamento WHERE orcamento_id = ?;";
        long novoTotalCentavos = 0;
        try (PreparedStatement stmtSoma = conexao.prepareStatement(sqlSoma)) {
            stmtSoma.setInt(1, idOrcamento);
            try (ResultSet rs = stmtSoma.executeQuery()) {
                if (rs.next()) {
                    novoTotalCentavos = rs.getLong(1);
                }
            }
        }

        String sqlUpdateTotal = "UPDATE orcamento SET valor_total_centavos = ? WHERE id_orcamento = ?;";
        try (PreparedStatement stmtUpdate = conexao.prepareStatement(sqlUpdateTotal)) {
            stmtUpdate.setLong(1, novoTotalCentavos);
            stmtUpdate.setInt(2, idOrcamento);
            stmtUpdate.executeUpdate();
        }
    }

    private Orcamento montarOrcamento(ResultSet resultado, Connection conexao) throws SQLException {
        int idOrcamento = resultado.getInt("id_orcamento");
        String nomeOrcamento = resultado.getString("nome_orcamento");
        String statusStr = resultado.getString("status_orcamento");
        String dataStr = resultado.getString("data_orcamento");
        int idCliente = resultado.getInt("cliente_id");
        boolean clienteNulo = resultado.wasNull();

        StatusOrcamento status = StatusOrcamento.valueOf(statusStr);
        LocalDateTime data = parseDataHora(dataStr);

        Cliente cliente = null;
        if (!clienteNulo && idCliente > 0) {
            cliente = clienteDAO.buscarPorId(idCliente);
        }

        List<ItemOrcamento> itens = carregarItensOrcamento(idOrcamento, conexao);

        Venda venda = null;
        if (status == StatusOrcamento.FINALIZADO) {
            venda = getVendaDAO().buscarPorOrcamento(idOrcamento, conexao);
        }

        return new Orcamento(
                idOrcamento,
                nomeOrcamento,
                itens,
                data,
                cliente,
                status,
                venda
        );
    }

    private List<ItemOrcamento> carregarItensOrcamento(int idOrcamento, Connection conexao) throws SQLException {
        List<ItemOrcamento> itens = new ArrayList<>();

        String sql = """
            SELECT
                numero_item,
                produto_id,
                nome_produto,
                codigo_barras,
                quantidade,
                preco_unitario_tabela_centavos,
                preco_unitario_centavos
            FROM item_orcamento
            WHERE orcamento_id = ?
            ORDER BY numero_item;
            """;

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idOrcamento);

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    int numeroItem = resultado.getInt("numero_item");
                    int idProduto = resultado.getInt("produto_id");
                    String nomeProduto = resultado.getString("nome_produto");
                    String codBarras = resultado.getString("codigo_barras");
                    int quantidade = resultado.getInt("quantidade");
                    long precoTabelaCentavos = resultado.getLong("preco_unitario_tabela_centavos");
                    long precoLiquidoCentavos = resultado.getLong("preco_unitario_centavos");
                    if (precoTabelaCentavos == 0) {
                        precoTabelaCentavos = precoLiquidoCentavos;
                    }

                    BigDecimal precoTabela = BigDecimal.valueOf(precoTabelaCentavos, 2);
                    BigDecimal precoLiquido = BigDecimal.valueOf(precoLiquidoCentavos, 2);

                    Produto produto = null;
                    if (idProduto > 0) {
                        produto = produtoDAO.buscarPorId(idProduto);
                    }
                    if (produto == null && idProduto > 0) {
                        produto = new Produto(idProduto, nomeProduto, codBarras, precoTabela);
                    }

                    ItemOrcamento item = new ItemOrcamento(
                            numeroItem,
                            produto,
                            nomeProduto,
                            codBarras,
                            precoTabela,
                            precoLiquido,
                            quantidade
                    );

                    itens.add(item);
                }
            }
        }

        return itens;
    }

    private static LocalDateTime parseDataHora(String dataStr) {
        if (dataStr == null || dataStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String tratada = dataStr.trim();
        try {
            return LocalDateTime.parse(tratada, FORMATO_DATA_HORA);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(tratada, FORMATO_DATA_HORA_ISO);
            } catch (Exception ex) {
                try {
                    return LocalDateTime.parse(tratada);
                } catch (Exception ex2) {
                    return LocalDateTime.now();
                }
            }
        }
    }
}
