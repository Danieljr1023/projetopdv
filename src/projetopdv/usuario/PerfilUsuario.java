package projetopdv.usuario;

import java.util.EnumSet;
import java.util.Set;

public enum PerfilUsuario {
    ADMIN {
        @Override
        public Set<Permissao> getPermissoesPadrao() {
            // Admin possui todas as permissões do sistema
            return EnumSet.allOf(Permissao.class);
        }
    },
    GERENTE {
        @Override
        public Set<Permissao> getPermissoesPadrao() {
            // Gerente gerencia produtos, clientes, orçamentos completos, vendas e consulta usuários
            return EnumSet.of(
                    Permissao.PRODUTO_TODAS,
                    Permissao.CLIENTE_TODAS,
                    Permissao.ORCAMENTO_TODAS,
                    Permissao.VENDA_TODAS,
                    Permissao.USUARIO_CONSULTAR
            );
        }
    },
    VENDEDOR {
        @Override
        public Set<Permissao> getPermissoesPadrao() {
            // Vendedor cria orçamentos, aplica descontos nos itens, gerencia clientes e descarta orçamentos
            return EnumSet.of(
                    Permissao.PRODUTO_CONSULTAR,
                    Permissao.CLIENTE_CONSULTAR,
                    Permissao.CLIENTE_CADASTRAR,
                    Permissao.CLIENTE_EDITAR,
                    Permissao.ORCAMENTO_CRIAR,
                    Permissao.ORCAMENTO_CONSULTAR,
                    Permissao.ORCAMENTO_EDITAR_ITENS,
                    Permissao.ORCAMENTO_APLICAR_DESCONTO,
                    Permissao.ORCAMENTO_CANCELAR,
                    Permissao.ORCAMENTO_DUPLICAR
            );
        }
    },
    CAIXA {
        @Override
        public Set<Permissao> getPermissoesPadrao() {
            // Caixa opera frente de caixa (fatura, consulta, sangria de rotina para cofre, orçamentos e descarte)
            return EnumSet.of(
                    Permissao.PRODUTO_CONSULTAR,
                    Permissao.CLIENTE_CONSULTAR,
                    Permissao.CLIENTE_CADASTRAR,
                    Permissao.ORCAMENTO_CRIAR,
                    Permissao.ORCAMENTO_EDITAR_ITENS,
                    Permissao.ORCAMENTO_CONSULTAR,
                    Permissao.ORCAMENTO_CANCELAR,
                    Permissao.VENDA_FATURAR,
                    Permissao.VENDA_CONSULTAR,
                    Permissao.CAIXA_SANGRIA
            );
        }
    };

    public abstract Set<Permissao> getPermissoesPadrao();

    public String getNomeExibicao() {
        return switch (this) {
            case ADMIN -> "Administrador";
            case GERENTE -> "Gerente";
            case VENDEDOR -> "Vendedor";
            case CAIXA -> "Operador de Caixa";
        };
    }
}
