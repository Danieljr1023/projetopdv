package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.List;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoConsultarProdutos extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoConsultarProdutos(SessaoOrcamento sessao) {
        super(
                "/consultar_produtos",
                "Lista todos os produtos cadastrados."
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.PRODUTO_CONSULTAR)) {
            return true;
        }

        if (argumentos.length > 0) {
            System.out.println("Uso correto: /consultar_produtos");
            return true;
        }

        ProdutoDAO produtoDAO = sessao.getProdutoDAO();

        try {
            List<Produto> produtos = produtoDAO.listarTodos();

            if (produtos.isEmpty()) {
                System.out.println("Nenhum produto cadastrado.");
                return true;
            }

            System.out.println("\nProdutos cadastrados:");
            for (Produto produto : produtos) {
                System.out.println(produto);
            }

            return true;
        } catch (SQLException e) {
            System.out.println("Erro ao consultar produtos: " + e.getMessage());
            return true;
        }
    }
}
