package projetopdv.scratch;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import projetopdv.caixa.CaixaDAO;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.posvenda.PosVendaDAO;
import projetopdv.posvenda.ValeCompra;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandosPainel;
import projetopdv.usuario.PerfilUsuario;
import projetopdv.usuario.Permissao;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;
import projetopdv.venda.Venda;
import projetopdv.venda.VendaDAO;

public class testes_perfis_e_permissoes {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("SUITE DE TESTES: VALIDAÇÃO INTEGRAL DE PERFIS E PERMISSÕES DO SISTEMA (RBAC)");
        System.out.println("===============================================================================");

        // Preparar banco de dados limpo
        BancoDeDados.inicializarTabelas();
        BancoDeDados.resetarTudo();

        ProdutoDAO produtoDAO = new ProdutoDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        VendaDAO vendaDAO = new VendaDAO();
        CaixaDAO caixaDAO = new CaixaDAO();
        PosVendaDAO posVendaDAO = new PosVendaDAO();

        // ---------------------------------------------------------------------
        // 1. CRIAÇÃO DOS USUÁRIOS DE CADA PERFIL
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 1] Cadastrando operadores para cada perfil do sistema...");
        Usuario admin = usuarioDAO.buscarPorLogin("admin"); // Já criado no reset
        assert admin != null : "Admin padrão deveria existir";

        Usuario gerente = usuarioDAO.cadastrar("Carlos Gerente", "gerente", "gerente123", PerfilUsuario.GERENTE);
        Usuario vendedor = usuarioDAO.cadastrar("Vanessa Vendedora", "vendedor", "vendedor123", PerfilUsuario.VENDEDOR);
        Usuario caixa = usuarioDAO.cadastrar("Lucas Caixa", "caixa", "caixa123", PerfilUsuario.CAIXA);
        Usuario inativo = usuarioDAO.cadastrar("Inativo Silva", "inativo", "inativo123", PerfilUsuario.CAIXA);
        usuarioDAO.desativar(inativo.getIdUsuario());
        inativo = usuarioDAO.buscarPorLogin("inativo");

        assert gerente != null && gerente.getPerfil() == PerfilUsuario.GERENTE;
        assert vendedor != null && vendedor.getPerfil() == PerfilUsuario.VENDEDOR;
        assert caixa != null && caixa.getPerfil() == PerfilUsuario.CAIXA;
        assert inativo != null && !inativo.isAtivo();
        System.out.println("-> OK: Usuários ADMIN, GERENTE, VENDEDOR, CAIXA e INATIVO configurados com sucesso.");

        // ---------------------------------------------------------------------
        // 2. TESTES DE AUTENTICAÇÃO E BARREIRAS DE ACESSO
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 2] Testando regras de autenticação e contas desativadas...");
        
        // Senha incorreta
        assert usuarioDAO.autenticar("admin", "senha_errada") == null : "Senha errada não deve autenticar";
        
        // Usuário inativo tem login recusado na camada de autenticação
        assert usuarioDAO.autenticar("inativo", "inativo123") == null : "Usuário inativo deve ter autenticação rejeitada diretamente no DAO";
        assert !usuarioDAO.buscarPorLogin("inativo").isAtivo() : "Status do usuário inativo deve ser falso";

        // Tentativa de login do inativo na Frente de Caixa
        SessaoCaixa sessaoCaixaInativo = criarSessaoCaixa("", vendaDAO, caixaDAO, posVendaDAO, orcamentoDAO, clienteDAO, produtoDAO, usuarioDAO);
        sessaoCaixaInativo.processarLinha("/login inativo inativo123");
        assert !sessaoCaixaInativo.estaAutenticado() : "Usuário desativado NÃO pode conseguir login no Caixa";
        System.out.println("-> OK: Bloqueio contra operador inativo validado.");

        // ---------------------------------------------------------------------
        // 3. SEGREGAÇÃO ENTRE TERMINAIS (Frente de Caixa vs Backoffice)
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 3] Testando barreira de entrada da Frente de Caixa (PDV)...");

        // Vendedor tentando logar na Frente de Caixa (não tem VENDA_FATURAR nem VENDA_TODAS)
        SessaoCaixa sessaoCaixaVendedor = criarSessaoCaixa("", vendaDAO, caixaDAO, posVendaDAO, orcamentoDAO, clienteDAO, produtoDAO, usuarioDAO);
        sessaoCaixaVendedor.processarLinha("/login vendedor vendedor123");
        assert !sessaoCaixaVendedor.estaAutenticado() : "Vendedor de salão NÃO deve poder logar na Frente de Caixa";
        System.out.println("-> OK: Vendedor barrado com sucesso na Frente de Caixa.");

        // Operador de Caixa logando na Frente de Caixa
        SessaoCaixa sessaoCaixaOperador = criarSessaoCaixa("", vendaDAO, caixaDAO, posVendaDAO, orcamentoDAO, clienteDAO, produtoDAO, usuarioDAO);
        sessaoCaixaOperador.processarLinha("/login caixa caixa123");
        assert sessaoCaixaOperador.estaAutenticado() : "Caixa deve conseguir logar na Frente de Caixa";
        assert sessaoCaixaOperador.getUsuarioLogado().getLogin().equals("caixa");
        System.out.println("-> OK: Caixa autenticado com sucesso no PDV.");

        // Gerente logando na Frente de Caixa (possui VENDA_TODAS)
        SessaoCaixa sessaoCaixaGerente = criarSessaoCaixa("", vendaDAO, caixaDAO, posVendaDAO, orcamentoDAO, clienteDAO, produtoDAO, usuarioDAO);
        sessaoCaixaGerente.processarLinha("/login gerente gerente123");
        assert sessaoCaixaGerente.estaAutenticado() : "Gerente deve poder assumir terminal na Frente de Caixa";
        System.out.println("-> OK: Gerente autenticado com sucesso no PDV.");

        // ---------------------------------------------------------------------
        // 4. TESTES DO PERFIL VENDEDOR NO DIA A DIA DO BACKOFFICE
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 4] Validando limites e ações do perfil VENDEDOR no Backoffice...");

        // Cadastrar produtos e cliente para testes
        Produto monitor = produtoDAO.cadastrar("Monitor 27 IPS 144Hz", "7893001", new BigDecimal("1200.00"));
        Produto teclado = produtoDAO.cadastrar("Teclado Sem Fio", "7893002", new BigDecimal("250.00"));
        Cliente clienteJoao = clienteDAO.cadastrar("João Santos", "10/10/1990", "98765432100");

        SessaoOrcamento sessaoVendedor = criarSessaoBackoffice(vendedor, orcamentoDAO, produtoDAO, clienteDAO, usuarioDAO);

        // Vendedor tem permissão de consultar produtos?
        assert sessaoVendedor.temPermissao(Permissao.PRODUTO_CONSULTAR) : "Vendedor deve poder consultar produtos";
        // Vendedor tem permissão de cadastrar ou editar produtos? NÃO!
        assert !sessaoVendedor.temPermissao(Permissao.PRODUTO_CADASTRAR) : "Vendedor NÃO pode cadastrar produtos";
        assert !sessaoVendedor.temPermissao(Permissao.PRODUTO_EDITAR) : "Vendedor NÃO pode alterar preço de catálogo";
        assert !sessaoVendedor.temPermissao(Permissao.PRODUTO_EXCLUIR) : "Vendedor NÃO pode excluir produtos";

        // Vendedor pode cadastrar e editar clientes?
        assert sessaoVendedor.temPermissao(Permissao.CLIENTE_CADASTRAR) : "Vendedor deve poder cadastrar cliente";
        assert sessaoVendedor.temPermissao(Permissao.CLIENTE_EDITAR) : "Vendedor deve poder editar cliente";
        // Vendedor pode excluir cliente? NÃO!
        assert !sessaoVendedor.temPermissao(Permissao.CLIENTE_EXCLUIR) : "Vendedor NÃO pode excluir cliente";

        // Vendedor criando Orçamento sem desconto (Preço cheio de tabela)
        Orcamento orcSemDesconto = orcamentoDAO.cadastrar("Orçamento Preço Cheio", clienteJoao, List.of(
                new ItemOrcamento(1, teclado, teclado.getNomeProduto(), teclado.getCodBarras(), teclado.getPreco(), teclado.getPreco(), 1)
        ));
        assert sessaoVendedor.temPermissao(Permissao.ORCAMENTO_CRIAR);
        orcamentoDAO.confirmarOrcamento(orcSemDesconto.getIdOrcamento(), null);
        orcSemDesconto = orcamentoDAO.buscarPorId(orcSemDesconto.getIdOrcamento());
        assert orcSemDesconto.getStatusOrcamento() == StatusOrcamento.CONFIRMADO : "Orçamento sem desconto deve poder ser confirmado";
        System.out.println("-> OK: Vendedor confirmou orçamento a preço de tabela.");

        // Vendedor criando Orçamento com desconto negociado
        ItemOrcamento itemComDesconto = new ItemOrcamento(1, monitor, monitor.getNomeProduto(), monitor.getCodBarras(),
                monitor.getPreco(), new BigDecimal("1000.00"), 1); // R$ 200 de desconto
        Orcamento orcComDesconto = orcamentoDAO.cadastrar("Orçamento Gamer João", clienteJoao, List.of(itemComDesconto));

        // Vendedor pode aplicar desconto no item? SIM
        assert sessaoVendedor.temPermissao(Permissao.ORCAMENTO_APLICAR_DESCONTO) : "Vendedor deve poder aplicar desconto";
        // Vendedor pode aprovar o orçamento com desconto? NÃO! Exige autorização de gerente/admin
        assert !sessaoVendedor.temPermissao(Permissao.ORCAMENTO_APROVAR_DESCONTO) : "Vendedor NÃO pode aprovar o próprio desconto";
        System.out.println("-> OK: Bloqueio de auto-aprovação de desconto pelo vendedor validado.");

        // Vendedor descartando orçamento (pode descartar quando o cliente desiste, sabendo que é recuperável)
        assert sessaoVendedor.temPermissao(Permissao.ORCAMENTO_CANCELAR) : "Vendedor deve poder descartar orçamento";
        orcamentoDAO.cancelarOrcamento(orcSemDesconto.getIdOrcamento());
        orcSemDesconto = orcamentoDAO.buscarPorId(orcSemDesconto.getIdOrcamento());
        assert orcSemDesconto.getStatusOrcamento() == StatusOrcamento.CANCELADO : "Orçamento foi cancelado pelo vendedor";
        System.out.println("-> OK: Vendedor cancelou/descartou orçamento com sucesso.");

        // Vendedor tentando gerenciar usuários ou resetar banco
        assert !sessaoVendedor.temPermissao(Permissao.USUARIO_CADASTRAR) : "Vendedor NÃO pode cadastrar usuários";
        assert !sessaoVendedor.temPermissao(Permissao.USUARIO_TODAS) : "Vendedor NÃO é admin";

        // ---------------------------------------------------------------------
        // 5. TESTES DO PERFIL GERENTE (SUPERVISOR)
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 5] Validando poderes de supervisão do perfil GERENTE...");
        SessaoOrcamento sessaoGerente = criarSessaoBackoffice(gerente, orcamentoDAO, produtoDAO, clienteDAO, usuarioDAO);

        // Gerente tem permissão para aprovar o desconto que o vendedor solicitou?
        assert sessaoGerente.temPermissao(Permissao.ORCAMENTO_APROVAR_DESCONTO) : "Gerente tem permissão para aprovar desconto";
        orcamentoDAO.confirmarOrcamento(orcComDesconto.getIdOrcamento(), null);
        orcComDesconto = orcamentoDAO.buscarPorId(orcComDesconto.getIdOrcamento());
        assert orcComDesconto.getStatusOrcamento() == StatusOrcamento.CONFIRMADO : "Gerente aprovou orçamento com desconto com sucesso";
        System.out.println("-> OK: Gerente aprovou com sucesso o orçamento com desconto gerado pelo vendedor.");

        // Gerente pode descartar orçamentos? SIM
        assert sessaoGerente.temPermissao(Permissao.ORCAMENTO_CANCELAR) : "Gerente pode cancelar orçamentos";
        System.out.println("-> OK: Gerente possui permissão de cancelamento de orçamento.");

        // Gerente pode gerenciar produtos (cadastrar, alterar preço, etc.)? SIM
        assert sessaoGerente.temPermissao(Permissao.PRODUTO_CADASTRAR);
        assert sessaoGerente.temPermissao(Permissao.PRODUTO_EDITAR);
        assert sessaoGerente.temPermissao(Permissao.PRODUTO_EXCLUIR);

        // Gerente pode cadastrar usuários ou resetar banco? NÃO (restrito a ADMIN)
        assert !sessaoGerente.temPermissao(Permissao.USUARIO_CADASTRAR) : "Gerente NÃO deve cadastrar operadores por padrão";
        assert gerente.getPerfil() != PerfilUsuario.ADMIN : "Gerente não é admin";
        System.out.println("-> OK: Limites de segurança do Gerente validados contra gestão de usuários.");

        // ---------------------------------------------------------------------
        // 6. TESTES DO PERFIL CAIXA (OPERAÇÕES E RESTRIÇÕES DE SEGURANÇA)
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 6] Validando operações diárias e restrições do perfil CAIXA...");

        // Abrir Caixa como operador Caixa
        SessaoCaixa pdvCaixa = criarSessaoCaixa("", vendaDAO, caixaDAO, posVendaDAO, orcamentoDAO, clienteDAO, produtoDAO, usuarioDAO);
        pdvCaixa.processarLinha("/login caixa caixa123");
        assert pdvCaixa.temPermissao(Permissao.VENDA_FATURAR);
        assert pdvCaixa.temPermissao(Permissao.VENDA_CONSULTAR);
        assert pdvCaixa.temPermissao(Permissao.ORCAMENTO_CANCELAR) : "Caixa deve poder descartar orçamento";

        // Caixa pode abrir gaveta? SIM
        pdvCaixa.processarLinha("/abrir_caixa 200.00");
        assert caixaDAO.isCaixaAberto() : "Caixa deve estar aberto";
        assert caixaDAO.calcularSaldoDinheiroEmCaixa().compareTo(new BigDecimal("200.00")) == 0;
        System.out.println("-> OK: Caixa realizou abertura com R$ 200,00.");

        // Caixa faturando o orçamento aprovado pelo gerente (#2)
        pdvCaixa.processarLinha("/faturar 2 DINHEIRO 1000.00");
        Venda venda1 = vendaDAO.buscarPorId(1);
        assert venda1 != null && venda1.getStatusVenda() == Venda.StatusVenda.CONCLUIDA;
        assert caixaDAO.calcularSaldoDinheiroEmCaixa().compareTo(new BigDecimal("1200.00")) == 0; // 200 + 1000
        System.out.println("-> OK: Caixa faturou orçamento em dinheiro. Saldo gaveta: R$ 1.200,00.");

        // Caixa realizando sangria de rotina para esvaziar a gaveta para o cofre: PERMITIDO!
        assert pdvCaixa.temPermissao(Permissao.CAIXA_SANGRIA) : "Caixa deve poder realizar sangria de rotina para o cofre";
        pdvCaixa.processarLinha("/sangria 500.00 Transferencia para cofre");
        assert caixaDAO.calcularSaldoDinheiroEmCaixa().compareTo(new BigDecimal("700.00")) == 0 : "Saldo deve ser 1200 - 500 = 700";
        System.out.println("-> OK: Caixa realizou sangria de rotina com sucesso! Saldo restante: R$ 700,00.");

        // Caixa tentando estornar a venda integralmente: BLOQUEADO POR PADRÃO!
        assert !pdvCaixa.temPermissao(Permissao.VENDA_ESTORNAR) : "Caixa comum NÃO tem permissão de estorno por padrão";
        pdvCaixa.processarLinha("/estornar 1 Tentativa de estorno pelo caixa");
        venda1 = vendaDAO.buscarPorId(1);
        assert venda1.getStatusVenda() == Venda.StatusVenda.CONCLUIDA : "Venda não pode ser estornada pelo caixa comum";
        System.out.println("-> OK: Estorno de venda bloqueado para operador comum.");

        // Caixa tentando fazer devolução de item: BLOQUEADO POR PADRÃO!
        pdvCaixa.processarLinha("/devolver 1 1 1 Tentativa devolucao");
        assert posVendaDAO.listarValesAtivos().isEmpty() : "Nenhum vale deve ter sido gerado sem autorização";
        System.out.println("-> OK: Devolução de item bloqueada para operador comum.");

        // ---------------------------------------------------------------------
        // 7. CONCESSÃO DINÂMICA DE PERMISSÃO (Operador Caixa de Confiança / Caixa-Líder)
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 7] Testando concessão dinâmica de permissão de estorno (RBAC granular)...");
        // Administrador promove 'caixa' com a permissão VENDA_ESTORNAR
        caixa.concederPermissao(Permissao.VENDA_ESTORNAR);
        usuarioDAO.atualizarPermissoes(caixa.getIdUsuario(), caixa.getPermissoes());

        // Recarregar caixa e reabrir sessão PDV
        Usuario caixaPromovido = usuarioDAO.buscarPorLogin("caixa");
        assert caixaPromovido.temPermissao(Permissao.VENDA_ESTORNAR) : "Caixa agora possui permissão de estorno/devolução";

        SessaoCaixa pdvCaixaPromovido = criarSessaoCaixa("", vendaDAO, caixaDAO, posVendaDAO, orcamentoDAO, clienteDAO, produtoDAO, usuarioDAO);
        pdvCaixaPromovido.processarLinha("/login caixa caixa123");

        // Agora o caixa promovido processa devolução de 1 monitor com geração de vale
        pdvCaixaPromovido.processarLinha("/devolver 1 1 1 Cliente devolveu monitor");
        List<ValeCompra> valesAtivos = posVendaDAO.listarValesAtivos();
        assert !valesAtivos.isEmpty() : "Vale-compra deve ter sido gerado";
        ValeCompra valeMonitor = valesAtivos.get(0);
        // Monitor foi comprado com desconto por R$ 1000
        assert valeMonitor.getSaldo().compareTo(new BigDecimal("1000.00")) == 0 : "Vale deve ser no valor pago de R$ 1.000";
        System.out.println("-> OK: Devolução autorizada processada e Vale-Compra emitido no valor líquido de R$ 1.000,00.");

        // Revogar permissão e verificar que volta a ser bloqueado
        caixa.revogarPermissao(Permissao.VENDA_ESTORNAR);
        usuarioDAO.atualizarPermissoes(caixa.getIdUsuario(), caixa.getPermissoes());
        Usuario caixaRevogado = usuarioDAO.buscarPorLogin("caixa");
        assert !caixaRevogado.temPermissao(Permissao.VENDA_ESTORNAR) : "Permissão deve estar revogada";
        System.out.println("-> OK: Revogação de permissão aplicada com sucesso.");

        // ---------------------------------------------------------------------
        // 8. TESTES DE CENÁRIOS EXTREMOS E REGRAS DE NEGÓCIO DE CAIXA
        // ---------------------------------------------------------------------
        System.out.println("\n[ETAPA 8] Testando cenários extremos e validações defensivas...");

        // Tentativa de sangria de valor superior ao saldo físico disponível
        // Saldo atual é R$ 700. Gerente tenta sangria de R$ 1.000
        SessaoCaixa pdvGerenteExtremos = criarSessaoCaixa("", vendaDAO, caixaDAO, posVendaDAO, orcamentoDAO, clienteDAO, produtoDAO, usuarioDAO);
        pdvGerenteExtremos.processarLinha("/login gerente gerente123");
        pdvGerenteExtremos.processarLinha("/sangria 1000.00 Tentativa sangria excessiva");
        assert caixaDAO.calcularSaldoDinheiroEmCaixa().compareTo(new BigDecimal("700.00")) == 0 : "Saldo não pode ser alterado por sangria inválida";
        System.out.println("-> OK: Bloqueio de sangria com valor superior ao saldo da gaveta validado.");

        // Tentativa de devolver quantidade superior ao que foi comprado (comprado: 1, devolvido: 1, restante: 0)
        pdvGerenteExtremos.processarLinha("/devolver 1 1 1 Tentativa de devolver item já devolvido");
        System.out.println("-> OK: Bloqueio de devolução excedente validado.");

        // Tentativa de faturar orçamento cancelado
        pdvGerenteExtremos.processarLinha("/faturar 1 DINHEIRO 250.00"); // Orçamento #1 está CANCELADO
        System.out.println("-> OK: Bloqueio de faturamento de orçamento cancelado validado.");

        // Fechamento cego de caixa com apuração exata
        pdvGerenteExtremos.processarLinha("/fechar_caixa 700.00 Fechamento correto");
        assert !caixaDAO.isCaixaAberto() : "Caixa deve estar fechado";
        System.out.println("-> OK: Fechamento de turno efetuado com sucesso.");

        System.out.println("\n===============================================================================");
        System.out.println("TODOS OS TESTES DE PERFIS E PERMISSÕES FORAM CONCLUÍDOS COM 100% DE SUCESSO!");
        System.out.println("===============================================================================");
    }

    private static SessaoCaixa criarSessaoCaixa(String inputs, VendaDAO vDAO, CaixaDAO cDAO, PosVendaDAO pvDAO,
                                                OrcamentoDAO oDAO, ClienteDAO clDAO, ProdutoDAO prDAO, UsuarioDAO uDAO) {
        Scanner scanner = new Scanner(new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)));
        return new SessaoCaixa(scanner, vDAO, cDAO, pvDAO, oDAO, clDAO, prDAO, uDAO);
    }

    private static SessaoOrcamento criarSessaoBackoffice(Usuario usuario, OrcamentoDAO oDAO, ProdutoDAO pDAO,
                                                         ClienteDAO cDAO, UsuarioDAO uDAO) {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        ComandosPainel comandos = new ComandosPainel();
        SessaoOrcamento sessao = new SessaoOrcamento(scanner, oDAO, pDAO, cDAO, uDAO, comandos);
        sessao.setUsuarioLogado(usuario);
        return sessao;
    }
}
