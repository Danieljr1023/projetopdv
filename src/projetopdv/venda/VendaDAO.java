package projetopdv.venda;

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
import projetopdv.caixa.CaixaDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.posvenda.PosVendaDAO;
import projetopdv.posvenda.ValeCompra;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.venda.Venda.StatusVenda;

public class VendaDAO {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ProdutoDAO produtoDAO;
    private final CaixaDAO caixaDAO;
    private final PosVendaDAO posVendaDAO;

    public VendaDAO() {
        this.produtoDAO = new ProdutoDAO();
        this.caixaDAO = new CaixaDAO();
        this.posVendaDAO = new PosVendaDAO();
    }

    public VendaDAO(ProdutoDAO produtoDAO, CaixaDAO caixaDAO, PosVendaDAO posVendaDAO) {
        this.produtoDAO = produtoDAO != null ? produtoDAO : new ProdutoDAO();
        this.caixaDAO = caixaDAO != null ? caixaDAO : new CaixaDAO();
        this.posVendaDAO = posVendaDAO != null ? posVendaDAO : new PosVendaDAO();
    }

    public Venda faturarOrcamento(Orcamento orcamento, String formaPagamento) {
        return faturarOrcamento(orcamento, formaPagamento, null);
    }

    public Venda faturarOrcamento(Orcamento orcamento, String formaPagamento, String codigoVale) {
        if (orcamento == null) {
            throw new IllegalArgumentException("Orçamento não pode ser nulo.");
        }
        if (orcamento.getIdOrcamento() <= 0) {
            throw new IllegalArgumentException("Orçamento com ID inválido.");
        }
        if (orcamento.getItensOrcamento() == null || orcamento.getItensOrcamento().isEmpty()) {
            throw new IllegalStateException("Não é possível faturar um orçamento sem itens.");
        }
        if (!orcamento.podeIniciarFaturamento() && !orcamento.podeFinalizar()) {
            throw new IllegalStateException(String.format(
                    "Orçamento #%d não pode ser faturado pois está com status '%s'.",
                    orcamento.getIdOrcamento(), orcamento.getStatusOrcamento()
            ));
        }

        FormaPagamento forma = FormaPagamento.fromString(formaPagamento);
        String formaFinal = forma != FormaPagamento.OUTRO ? forma.getDescricao() : (formaPagamento != null ? formaPagamento.trim() : "NÃO ESPECIFICADA");

        BigDecimal total = orcamento.getValorTotal();
        long totalCentavos = total.movePointRight(2).longValueExact();
        LocalDateTime agora = LocalDateTime.now();
        String dataFormatada = agora.format(FORMATO_DATA_HORA);

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            // 1. Validar no banco se o orçamento está elegível
            String sqlVerifica = "SELECT status_orcamento FROM orcamento WHERE id_orcamento = ?;";
            try (PreparedStatement psVerifica = conexao.prepareStatement(sqlVerifica)) {
                psVerifica.setInt(1, orcamento.getIdOrcamento());
                try (ResultSet rs = psVerifica.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Orçamento não encontrado no banco.");
                    }
                    String statusBanco = rs.getString("status_orcamento");
                    if (StatusOrcamento.FINALIZADO.name().equals(statusBanco)) {
                        throw new IllegalStateException("Este orçamento já foi finalizado anteriormente.");
                    }
                    if (StatusOrcamento.CANCELADO.name().equals(statusBanco)) {
                        throw new IllegalStateException("Não é possível faturar um orçamento cancelado.");
                    }
                }
            }

            // 2. Verificar se já existe venda para este orçamento
            String sqlCheckVenda = "SELECT COUNT(*) FROM venda WHERE orcamento_id = ?;";
            try (PreparedStatement psCheck = conexao.prepareStatement(sqlCheckVenda)) {
                psCheck.setInt(1, orcamento.getIdOrcamento());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalStateException("Já existe uma venda vinculada a este orçamento.");
                    }
                }
            }

            // 3. Se a forma de pagamento for Vale-Compra, validar e abater saldo
            if (forma == FormaPagamento.VALE_COMPRA) {
                if (codigoVale == null || codigoVale.isBlank()) {
                    throw new IllegalArgumentException("Para pagamento com Vale-Compra, é obrigatório informar o código do vale.");
                }
                ValeCompra vale = posVendaDAO.buscarValePorCodigo(codigoVale);
                if (vale == null) {
                    throw new IllegalArgumentException("Vale-compra '" + codigoVale + "' não encontrado.");
                }
                if (!vale.podeSerUsado()) {
                    throw new IllegalStateException("Vale-compra '" + codigoVale + "' indisponível para uso (Status: " + vale.getStatusVale() + ").");
                }
                if (vale.getSaldo().compareTo(total) < 0) {
                    throw new IllegalArgumentException(String.format(
                            "Saldo do vale-compra (R$ %.2f) é insuficiente para cobrir o total da venda (R$ %.2f).",
                            vale.getSaldo(), total
                    ));
                }
                posVendaDAO.abaterSaldoVale(vale.getIdValeCompra(), total, conexao);
            }

            // 4. Inserir cabeçalho da venda
            String sqlVenda = """
                INSERT INTO venda
                    (orcamento_id, data_venda, valor_total_centavos, forma_pagamento, status_venda)
                VALUES (?, ?, ?, ?, ?);
                """;

            int idVendaGerada;
            try (PreparedStatement psVenda = conexao.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                psVenda.setInt(1, orcamento.getIdOrcamento());
                psVenda.setString(2, dataFormatada);
                psVenda.setLong(3, totalCentavos);
                psVenda.setString(4, formaFinal);
                psVenda.setString(5, StatusVenda.CONCLUIDA.name());
                psVenda.executeUpdate();

                try (ResultSet chaves = psVenda.getGeneratedKeys()) {
                    if (chaves.next()) {
                        idVendaGerada = chaves.getInt(1);
                    } else {
                        throw new SQLException("O banco não retornou o ID da venda gerada.");
                    }
                }
            }

            // 5. Inserir itens da venda (snapshot)
            String sqlItem = """
                INSERT INTO item_venda
                    (venda_id, numero_item, numero_item_orcamento, produto_id, nome_produto, codigo_barras, quantidade, preco_unitario_tabela_centavos, preco_unitario_centavos)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

            List<ItemVenda> itensVendaCriados = new ArrayList<>();
            int seq = 1;

            for (ItemOrcamento itemOrc : orcamento.getItensOrcamento()) {
                try (PreparedStatement psItem = conexao.prepareStatement(sqlItem, Statement.RETURN_GENERATED_KEYS)) {
                    int numItemVenda = seq;
                    psItem.setInt(1, idVendaGerada);
                    psItem.setInt(2, numItemVenda);
                    psItem.setInt(3, itemOrc.getNumeroItem());

                    if (itemOrc.getIdProduto() > 0) {
                        psItem.setInt(4, itemOrc.getIdProduto());
                    } else {
                        psItem.setNull(4, Types.INTEGER);
                    }

                    psItem.setString(5, itemOrc.getNomeProduto());
                    psItem.setString(6, itemOrc.getCodigoBarras());
                    psItem.setInt(7, itemOrc.getQuantidade());
                    psItem.setLong(8, itemOrc.getPrecoUnitarioTabela().movePointRight(2).longValueExact());
                    psItem.setLong(9, itemOrc.getPrecoUnitarioLiquido().movePointRight(2).longValueExact());

                    psItem.executeUpdate();

                    int idItemVendaGerado = 0;
                    try (ResultSet chavesItem = psItem.getGeneratedKeys()) {
                        if (chavesItem.next()) {
                            idItemVendaGerado = chavesItem.getInt(1);
                        }
                    }

                    ItemVenda itemVenda = new ItemVenda(
                            idItemVendaGerado,
                            numItemVenda,
                            itemOrc.getNumeroItem(),
                            itemOrc.getProduto(),
                            itemOrc.getIdProduto(),
                            itemOrc.getNomeProduto(),
                            itemOrc.getCodigoBarras(),
                            itemOrc.getQuantidade(),
                            itemOrc.getPrecoUnitarioTabela(),
                            itemOrc.getPrecoUnitarioLiquido(),
                            0
                    );
                    itensVendaCriados.add(itemVenda);
                    seq++;
                }
            }

            // 6. Atualizar orçamento para FINALIZADO
            String sqlAtualizaOrcamento = "UPDATE orcamento SET status_orcamento = ? WHERE id_orcamento = ?;";
            try (PreparedStatement psOrc = conexao.prepareStatement(sqlAtualizaOrcamento)) {
                psOrc.setString(1, StatusOrcamento.FINALIZADO.name());
                psOrc.setInt(2, orcamento.getIdOrcamento());
                psOrc.executeUpdate();
            }

            // 7. Commit atômico
            conexao.commit();

            // 8. Atualizar objeto em memória
            Venda venda = new Venda(
                    idVendaGerada,
                    orcamento.getIdOrcamento(),
                    agora,
                    itensVendaCriados,
                    total,
                    formaFinal,
                    StatusVenda.CONCLUIDA,
                    null,
                    null,
                    null
            );

            if (orcamento.getStatusOrcamento() == StatusOrcamento.CONFIRMADO) {
                orcamento.iniciarFaturamento();
            }
            orcamento.finalizar(venda);

            return venda;

        } catch (SQLException e) {
            System.out.println("Erro ao faturar orçamento: " + e.getMessage());
            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Erro ao reverter transação de faturamento: " + rollbackEx.getMessage());
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

    public Venda faturarOrcamento(int idOrcamento, String formaPagamento) {
        OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
        Orcamento orcamento = orcamentoDAO.buscarPorId(idOrcamento);
        if (orcamento == null) {
            throw new IllegalArgumentException("Orçamento #" + idOrcamento + " não encontrado.");
        }
        return faturarOrcamento(orcamento, formaPagamento);
    }

    public boolean estornarVenda(int idVenda, String motivo, ModalidadeEstorno modalidade, Integer idOperador) {
        if (idVenda <= 0) {
            throw new IllegalArgumentException("ID da venda inválido.");
        }
        ModalidadeEstorno modFinal = modalidade != null ? modalidade : ModalidadeEstorno.REEMBOLSO_IMEDIATO;
        String motivoFinal = (motivo != null && !motivo.isBlank()) ? motivo.trim() : "Estorno de venda";

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            // 1. Buscar dados da venda
            String sqlBusca = "SELECT status_venda, valor_total_centavos, forma_pagamento, orcamento_id FROM venda WHERE id_venda = ?;";
            String statusVenda;
            long valorTotalCentavos;
            String formaPagamento;
            int orcamentoId;

            try (PreparedStatement ps = conexao.prepareStatement(sqlBusca)) {
                ps.setInt(1, idVenda);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Venda #" + idVenda + " não encontrada.");
                    }
                    statusVenda = rs.getString("status_venda");
                    valorTotalCentavos = rs.getLong("valor_total_centavos");
                    formaPagamento = rs.getString("forma_pagamento");
                    orcamentoId = rs.getInt("orcamento_id");
                }
            }

            if (StatusVenda.ESTORNADA.name().equals(statusVenda)) {
                throw new IllegalStateException("Venda #" + idVenda + " já foi estornada anteriormente.");
            }

            // 2. Verificar devoluções parciais existentes
            String sqlDevolucoes = "SELECT COALESCE(SUM(valor_total_centavos), 0) FROM devolucao_item WHERE venda_id = ?;";
            long totalJaDevolvidoCentavos = 0;
            try (PreparedStatement psDev = conexao.prepareStatement(sqlDevolucoes)) {
                psDev.setInt(1, idVenda);
                try (ResultSet rsDev = psDev.executeQuery()) {
                    if (rsDev.next()) {
                        totalJaDevolvidoCentavos = rsDev.getLong(1);
                    }
                }
            }

            long valorEfetivoEstornoCentavos = valorTotalCentavos - totalJaDevolvidoCentavos;
            if (valorEfetivoEstornoCentavos <= 0) {
                throw new IllegalStateException("Todos os itens desta venda já foram devolvidos anteriormente.");
            }
            BigDecimal valorEstorno = BigDecimal.valueOf(valorEfetivoEstornoCentavos).movePointLeft(2);

            LocalDateTime agora = LocalDateTime.now();
            String dataEstornoFormatada = agora.format(FORMATO_DATA_HORA);

            // 3. Atualizar status da venda
            String sqlUpdateVenda = """
                UPDATE venda
                SET status_venda = ?, modalidade_estorno = ?, data_estorno = ?, motivo_estorno = ?
                WHERE id_venda = ?;
                """;

            try (PreparedStatement psUp = conexao.prepareStatement(sqlUpdateVenda)) {
                psUp.setString(1, StatusVenda.ESTORNADA.name());
                psUp.setString(2, modFinal.name());
                psUp.setString(3, dataEstornoFormatada);
                psUp.setString(4, motivoFinal);
                psUp.setInt(5, idVenda);
                psUp.executeUpdate();
            }

            // 4. Executar regra da modalidade
            if (modFinal == ModalidadeEstorno.REEMBOLSO_IMEDIATO) {
                // Se foi dinheiro, registra saída na gaveta física imediatamente
                if (FormaPagamento.DINHEIRO.getDescricao().equalsIgnoreCase(formaPagamento)
                        || "DINHEIRO".equalsIgnoreCase(formaPagamento)) {
                    caixaDAO.registrarEstornoDinheiro(idVenda, valorEstorno, motivoFinal, idOperador, conexao);
                }
                // Se foi cartão ou PIX, o estorno ocorre na própria operadora/banco sem movimentar dinheiro físico
            } else if (modFinal == ModalidadeEstorno.VALE_COMPRA) {
                // Obter id_cliente do orçamento se houver
                Integer idCliente = null;
                String sqlCliente = "SELECT cliente_id FROM orcamento WHERE id_orcamento = ?;";
                try (PreparedStatement psCli = conexao.prepareStatement(sqlCliente)) {
                    psCli.setInt(1, orcamentoId);
                    try (ResultSet rsCli = psCli.executeQuery()) {
                        if (rsCli.next()) {
                            int cliId = rsCli.getInt("cliente_id");
                            if (!rsCli.wasNull() && cliId > 0) {
                                idCliente = cliId;
                            }
                        }
                    }
                }
                String obs = String.format("Vale gerado pelo estorno total da venda #%d (%s)", idVenda, motivoFinal);
                posVendaDAO.gerarValeCompra(idVenda, idCliente, valorEstorno, obs, conexao);
            }

            conexao.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("Erro ao estornar venda: " + e.getMessage());
            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Erro ao reverter transação de estorno: " + rollbackEx.getMessage());
                }
            }
            return false;
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

    public Venda buscarPorId(int idVenda) {
        if (idVenda <= 0) return null;
        try (Connection conn = BancoDeDados.conectar()) {
            return buscarPorId(idVenda, conn);
        } catch (SQLException e) {
            System.out.println("Erro ao buscar venda por ID: " + e.getMessage());
            return null;
        }
    }

    public Venda buscarPorId(int idVenda, Connection conexao) throws SQLException {
        if (idVenda <= 0) return null;
        String sql = "SELECT * FROM venda WHERE id_venda = ?;";

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idVenda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarVenda(rs, conexao);
                }
            }
        }
        return null;
    }

    public Venda buscarPorOrcamento(int idOrcamento) {
        if (idOrcamento <= 0) return null;
        try (Connection conn = BancoDeDados.conectar()) {
            return buscarPorOrcamento(idOrcamento, conn);
        } catch (SQLException e) {
            System.out.println("Erro ao buscar venda por orçamento: " + e.getMessage());
            return null;
        }
    }

    public Venda buscarPorOrcamento(int idOrcamento, Connection conexao) throws SQLException {
        if (idOrcamento <= 0) return null;
        String sql = "SELECT * FROM venda WHERE orcamento_id = ?;";

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idOrcamento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarVenda(rs, conexao);
                }
            }
        }
        return null;
    }

    public List<Venda> listarTodas() {
        String sql = "SELECT * FROM venda ORDER BY id_venda DESC;";
        List<Venda> lista = new ArrayList<>();

        try (Connection conn = BancoDeDados.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(montarVenda(rs, conn));
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar todas as vendas: " + e.getMessage());
        }
        return lista;
    }

    public List<Venda> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        String sql = "SELECT * FROM venda ORDER BY id_venda ASC;";
        List<Venda> lista = new ArrayList<>();

        try (Connection conn = BancoDeDados.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Venda v = montarVenda(rs, conn);
                boolean depoisInicio = (inicio == null) || !v.getDataVenda().isBefore(inicio);
                boolean antesFim = (fim == null) || !v.getDataVenda().isAfter(fim);

                if (depoisInicio && antesFim) {
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar vendas por período: " + e.getMessage());
        }
        return lista;
    }

    public List<Venda> listarPorCliente(int idCliente) {
        String sql = """
            SELECT v.*
            FROM venda v
            INNER JOIN orcamento o ON v.orcamento_id = o.id_orcamento
            WHERE o.cliente_id = ?
            ORDER BY v.id_venda DESC;
            """;
        List<Venda> lista = new ArrayList<>();

        try (Connection conn = BancoDeDados.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarVenda(rs, conn));
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar vendas por cliente: " + e.getMessage());
        }
        return lista;
    }

    public boolean existeVendaParaOrcamento(int idOrcamento) {
        if (idOrcamento <= 0) return false;
        String sql = "SELECT COUNT(*) FROM venda WHERE orcamento_id = ?;";

        try (Connection conn = BancoDeDados.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idOrcamento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erro ao verificar existência de venda: " + e.getMessage());
            return false;
        }
    }

    public BigDecimal calcularTotalVendasPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<Venda> vendas = listarPorPeriodo(inicio, fim);
        return vendas.stream()
                .filter(v -> !v.isEstornada())
                .map(Venda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularTotalDescontosPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<Venda> vendas = listarPorPeriodo(inicio, fim);
        return vendas.stream()
                .filter(v -> !v.isEstornada())
                .map(Venda::getValorTotalDesconto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Venda montarVenda(ResultSet rs, Connection conexao) throws SQLException {
        int idVenda = rs.getInt("id_venda");
        int idOrcamento = rs.getInt("orcamento_id");
        String dataStr = rs.getString("data_venda");
        long valorTotalCentavos = rs.getLong("valor_total_centavos");
        String formaPagamento = rs.getString("forma_pagamento");
        String statusVendaStr = rs.getString("status_venda");
        String modalidadeEstornoStr = rs.getString("modalidade_estorno");
        String dataEstornoStr = rs.getString("data_estorno");
        String motivoEstorno = rs.getString("motivo_estorno");

        LocalDateTime dataVenda = LocalDateTime.parse(dataStr, FORMATO_DATA_HORA);
        BigDecimal valorTotal = BigDecimal.valueOf(valorTotalCentavos).movePointLeft(2);
        StatusVenda statusVenda = StatusVenda.fromString(statusVendaStr);
        ModalidadeEstorno modalidadeEstorno = (modalidadeEstornoStr != null && !modalidadeEstornoStr.isBlank())
                ? ModalidadeEstorno.fromString(modalidadeEstornoStr)
                : null;
        LocalDateTime dataEstorno = (dataEstornoStr != null && !dataEstornoStr.isBlank())
                ? LocalDateTime.parse(dataEstornoStr, FORMATO_DATA_HORA)
                : null;

        List<ItemVenda> itens = carregarItensVenda(idVenda, conexao);

        return new Venda(
                idVenda,
                idOrcamento,
                dataVenda,
                itens,
                valorTotal,
                formaPagamento,
                statusVenda,
                modalidadeEstorno,
                dataEstorno,
                motivoEstorno
        );
    }

    private List<ItemVenda> carregarItensVenda(int idVenda, Connection conexao) throws SQLException {
        List<ItemVenda> itens = new ArrayList<>();
        String sql = """
            SELECT
                id_item_venda,
                numero_item,
                numero_item_orcamento,
                produto_id,
                nome_produto,
                codigo_barras,
                quantidade,
                preco_unitario_tabela_centavos,
                preco_unitario_centavos
            FROM item_venda
            WHERE venda_id = ?
            ORDER BY numero_item;
            """;

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idVenda);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idItemVenda = rs.getInt("id_item_venda");
                    int numeroItem = rs.getInt("numero_item");
                    int numeroItemOrc = rs.getInt("numero_item_orcamento");
                    boolean orcNulo = rs.wasNull();
                    int produtoId = rs.getInt("produto_id");
                    boolean prodNulo = rs.wasNull();
                    String nomeProduto = rs.getString("nome_produto");
                    String codigoBarras = rs.getString("codigo_barras");
                    int quantidade = rs.getInt("quantidade");
                    long precoTabelaCentavos = rs.getLong("preco_unitario_tabela_centavos");
                    long precoCentavos = rs.getLong("preco_unitario_centavos");

                    BigDecimal precoTabela = BigDecimal.valueOf(precoTabelaCentavos).movePointLeft(2);
                    BigDecimal preco = BigDecimal.valueOf(precoCentavos).movePointLeft(2);

                    Produto produto = null;
                    if (!prodNulo && produtoId > 0) {
                        produto = produtoDAO.buscarPorId(produtoId);
                    }

                    int qtdDevolvida = posVendaDAO.getQuantidadeJaDevolvida(idItemVenda, conexao);

                    ItemVenda item = new ItemVenda(
                            idItemVenda,
                            numeroItem,
                            !orcNulo ? numeroItemOrc : null,
                            produto,
                            produtoId,
                            nomeProduto,
                            codigoBarras,
                            quantidade,
                            precoTabela,
                            preco,
                            qtdDevolvida
                    );
                    itens.add(item);
                }
            }
        }
        return itens;
    }
}
