package projetopdv.usuario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Permissao {

    // ==========================================
    // MÓDULO DE PRODUTOS
    // ==========================================
    PRODUTO_TODAS("Gerenciar Produtos Completo", null),
    PRODUTO_CADASTRAR("Cadastrar novos produtos", PRODUTO_TODAS),
    PRODUTO_CONSULTAR("Consultar e listar produtos", PRODUTO_TODAS),
    PRODUTO_EDITAR("Editar dados e preços de produtos", PRODUTO_TODAS),
    PRODUTO_EXCLUIR("Excluir produtos", PRODUTO_TODAS),

    // ==========================================
    // MÓDULO DE CLIENTES
    // ==========================================
    CLIENTE_TODAS("Gerenciar Clientes Completo", null),
    CLIENTE_CADASTRAR("Cadastrar novos clientes", CLIENTE_TODAS),
    CLIENTE_CONSULTAR("Consultar e pesquisar clientes", CLIENTE_TODAS),
    CLIENTE_EDITAR("Editar dados de clientes", CLIENTE_TODAS),
    CLIENTE_EXCLUIR("Excluir clientes", CLIENTE_TODAS),

    // ==========================================
    // MÓDULO DE ORÇAMENTOS
    // ==========================================
    ORCAMENTO_TODAS("Gerenciar Orçamentos Completo", null),
    ORCAMENTO_CRIAR("Criar novos orçamentos", ORCAMENTO_TODAS),
    ORCAMENTO_CONSULTAR("Listar e visualizar orçamentos", ORCAMENTO_TODAS),
    ORCAMENTO_EDITAR_ITENS("Adicionar, alterar e remover itens de orçamento", ORCAMENTO_TODAS),
    ORCAMENTO_APLICAR_DESCONTO("Aplicar descontos em itens do orçamento", ORCAMENTO_TODAS),
    ORCAMENTO_APROVAR_DESCONTO("Aprovar e confirmar orçamentos com desconto", ORCAMENTO_TODAS),
    ORCAMENTO_CANCELAR("Cancelar ou descartar orçamentos", ORCAMENTO_TODAS),
    ORCAMENTO_DUPLICAR("Duplicar orçamentos existentes", ORCAMENTO_TODAS),

    // ==========================================
    // MÓDULO DE VENDAS E CAIXA
    // ==========================================
    VENDA_TODAS("Operações de Venda e Caixa Completo", null),
    VENDA_FATURAR("Finalizar faturamento e receber pagamentos no caixa", VENDA_TODAS),
    VENDA_CONSULTAR("Consultar histórico de vendas e cupons", VENDA_TODAS),
    VENDA_RELATORIOS("Visualizar relatórios de fechamento de caixa e faturamento", VENDA_TODAS),
    VENDA_ESTORNAR("Estornar vendas e autorizar devoluções de itens", VENDA_TODAS),
    CAIXA_SANGRIA("Realizar sangrias de caixa e resgates de vale-compra", VENDA_TODAS),

    // ==========================================
    // MÓDULO DE USUÁRIOS E SEGURANÇA
    // ==========================================
    USUARIO_TODAS("Gerenciar Usuários Completo", null),
    USUARIO_CADASTRAR("Cadastrar novos operadores do sistema", USUARIO_TODAS),
    USUARIO_CONSULTAR("Listar e visualizar dados de usuários", USUARIO_TODAS),
    USUARIO_EDITAR("Editar nome, login e perfil de usuários", USUARIO_TODAS),
    USUARIO_ALTERAR_PERMISSOES("Conceder ou revogar permissões especiais de usuários", USUARIO_TODAS),
    USUARIO_ALTERAR_SENHA("Alterar ou redefinir senhas de usuários", USUARIO_TODAS),
    USUARIO_DESATIVAR("Ativar ou desativar operadores", USUARIO_TODAS);

    private final String descricao;
    private final Permissao mae;

    Permissao(String descricao, Permissao mae) {
        this.descricao = descricao;
        this.mae = mae;
    }

    public String getDescricao() {
        return descricao;
    }

    public Permissao getMae() {
        return mae;
    }

    public boolean isMae() {
        return mae == null;
    }

    public List<Permissao> getFilhas() {
        List<Permissao> filhas = new ArrayList<>();
        for (Permissao p : values()) {
            if (p.mae == this) {
                filhas.add(p);
            }
        }
        return Collections.unmodifiableList(filhas);
    }
}
