package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.Scanner;
import projetopdv.dados.BancoDeDados;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.PerfilUsuario;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;

public class ComandoResetarBanco extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoResetarBanco(SessaoOrcamento sessao) {
        super(
                "/resetar_banco",
                """
                Realiza a limpeza completa e zera os contadores de ID de uma tabela específica ou de todo o banco de dados.
                Comando restrito exclusivamente a administradores (perfil ADMIN) com confirmação e reautenticação obrigatória.

                Uso:
                /resetar_banco                                                         (Menu interativo de seleção)
                /resetar_banco <All | produto | cliente | orcamento | venda | usuario> (Execução direta)
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        // 1. Validação de autenticação e perfil ADMIN
        if (!sessao.isAutenticado()) {
            System.out.println("Acesso negado: Você precisa fazer login antes de utilizar o sistema.");
            System.out.println("Digite /login para entrar ou /sair para encerrar.");
            return true;
        }

        Usuario usuarioAtual = sessao.getUsuarioLogado();
        if (usuarioAtual.getPerfil() != PerfilUsuario.ADMIN) {
            System.out.println("\n❌ Acesso Negado: Este comando é restrito exclusivamente a administradores do sistema (perfil ADMIN).");
            return true;
        }

        // 2. Obtenção do alvo (modo interativo ou por argumento)
        String alvoParam;
        if (argumentos.length < 1) {
            System.out.println("\n==================================================");
            System.out.println("           MENU DE RESET DO BANCO DE DADOS        ");
            System.out.println("==================================================");
            System.out.println("[1] Tudo (All) - Limpa todas as tabelas e restaura o administrador padrão");
            System.out.println("[2] Produtos - Limpa todos os produtos e zera o contador de ID");
            System.out.println("[3] Clientes - Limpa todos os clientes e zera o contador de ID");
            System.out.println("[4] Orçamentos - Limpa todos os orçamentos e itens e zera os contadores");
            System.out.println("[5] Vendas - Limpa vendas, itens, devoluções, vales e caixa");
            System.out.println("[6] Usuários - Limpa operadores (restaura admin padrão) e zera contador");
            System.out.println("[0] Cancelar");
            System.out.println("--------------------------------------------------");
            System.out.print("Escolha uma opção (0-6 ou 'cancelar'): ");

            Scanner entrada = sessao.getEntrada();
            String escolha = entrada.nextLine().trim();
            if (escolha.isEmpty() || escolha.equals("0") || escolha.equalsIgnoreCase("cancelar")) {
                System.out.println("Operação de reset cancelada.");
                return true;
            }

            switch (escolha) {
                case "1" -> alvoParam = "all";
                case "2" -> alvoParam = "produto";
                case "3" -> alvoParam = "cliente";
                case "4" -> alvoParam = "orcamento";
                case "5" -> alvoParam = "venda";
                case "6" -> alvoParam = "usuario";
                default -> {
                    System.out.println("Opção inválida.");
                    return true;
                }
            }
        } else {
            alvoParam = argumentos[0].trim().toLowerCase();
        }

        String alvoDescricao;

        switch (alvoParam) {
            case "all", "tudo", "todos" -> alvoDescricao = "TODO O BANCO DE DADOS (Produtos, Clientes, Orçamentos, Vendas e Usuários)";
            case "produto", "produtos" -> alvoDescricao = "PRODUTOS (Tabela de Produtos)";
            case "cliente", "clientes" -> alvoDescricao = "CLIENTES (Tabela de Clientes)";
            case "orcamento", "orcamentos", "item_orcamento", "itens" -> alvoDescricao = "ORÇAMENTOS E ITENS (Tabelas de Orçamentos e Itens)";
            case "venda", "vendas", "caixa" -> alvoDescricao = "VENDAS, CAIXA E PÓS-VENDA (Tabelas de Vendas, Itens, Vales, Devoluções e Caixa)";
            case "usuario", "usuarios", "operador", "operadores" -> alvoDescricao = "USUÁRIOS (Tabela de Operadores/Usuários)";
            default -> {
                System.out.println("\n❌ Alvo de reset inválido: '" + argumentos[0] + "'");
                System.out.println("Opções aceitas: All, produto, cliente, orcamento, venda, usuario");
                return true;
            }
        }

        Scanner entrada = sessao.getEntrada();
        UsuarioDAO usuarioDAO = sessao.getUsuarioDAO();

        // 3. Confirmação de Segurança
        System.out.println("\n================================================================================");
        System.out.println("             ⚠️  AVISO DE SEGURANÇA - OPERAÇÃO CRÍTICA E IRREVERSÍVEL  ⚠️        ");
        System.out.println("================================================================================");
        System.out.println("Alvo selecionado para reset: " + alvoDescricao);
        System.out.println("Todos os registros correspondentes serão EXCLUÍDOS PERMANENTEMENTE e os");
        System.out.println("contadores de ID (AUTOINCREMENT) serão zerados para reiniciarem em 1.");
        System.out.println("================================================================================");
        System.out.print("Deseja realmente continuar? Digite 'CONFIRMAR' para prosseguir: ");
        String confirmacao = entrada.nextLine().trim();

        if (!confirmacao.equalsIgnoreCase("CONFIRMAR")) {
            System.out.println("\nOperação de reset cancelada com segurança. Nenhuma alteração foi efetuada.");
            return true;
        }

        // 4. Reautenticação obrigatória de credenciais
        System.out.println("\n--- CONFIRMAÇÃO DE IDENTIDADE NECESSÁRIA ---");
        System.out.print("Digite o usuário administrador: ");
        String loginConfirmacao = entrada.nextLine().trim();

        String senhaConfirmacao = sessao.lerSenha("Digite a senha: ");

        try {
            Usuario adminConfirmado = usuarioDAO.autenticar(loginConfirmacao, senhaConfirmacao);
            if (adminConfirmado == null || adminConfirmado.getPerfil() != PerfilUsuario.ADMIN) {
                System.out.println("\n❌ Falha na autenticação: Usuário ou senha incorretos (ou o usuário não possui perfil ADMIN).");
                System.out.println("A operação de reset foi abortada por motivos de segurança.");
                return true;
            }

            // 5. Execução do reset de acordo com o alvo
            switch (alvoParam) {
                case "all", "tudo", "todos" -> {
                    if (sessao.temOrcamentoAtivo()) {
                        sessao.sairOrcamento();
                    }
                    BancoDeDados.resetarTudo();
                    Usuario adminRestaurado = usuarioDAO.autenticar("admin", "admin123");
                    if (adminRestaurado != null) {
                        sessao.setUsuarioLogado(adminRestaurado);
                    }
                    System.out.println("\n✅ SUCESSO: Todas as tabelas do banco de dados foram completamente limpas!");
                    System.out.println("Contadores de ID zerados. Usuário padrão 'admin' (senha: 'admin123') restaurado.");
                }

                case "produto", "produtos" -> {
                    BancoDeDados.resetarTabelaProduto();
                    System.out.println("\n✅ SUCESSO: Tabela de PRODUTOS limpa com sucesso!");
                    System.out.println("Contador de ID de produtos zerado para 1.");
                }

                case "cliente", "clientes" -> {
                    BancoDeDados.resetarTabelaCliente();
                    System.out.println("\n✅ SUCESSO: Tabela de CLIENTES limpa com sucesso!");
                    System.out.println("Contador de ID de clientes zerado para 1.");
                }

                case "orcamento", "orcamentos", "item_orcamento", "itens" -> {
                    if (sessao.temOrcamentoAtivo()) {
                        sessao.sairOrcamento();
                    }
                    BancoDeDados.resetarTabelaOrcamento();
                    System.out.println("\n✅ SUCESSO: Tabelas de ORÇAMENTOS e ITENS limpas com sucesso!");
                    System.out.println("Contador de ID de orçamentos zerado para 1.");
                }

                case "venda", "vendas", "caixa" -> {
                    BancoDeDados.resetarTabelaVenda();
                    System.out.println("\n✅ SUCESSO: Tabelas de VENDAS, ITENS DE VENDA, VALES-COMPRA, DEVOLUÇÕES e CAIXA limpas com sucesso!");
                    System.out.println("Contador de ID de vendas zerado para 1.");
                }

                case "usuario", "usuarios", "operador", "operadores" -> {
                    BancoDeDados.resetarTabelaUsuario();
                    Usuario adminRestaurado = usuarioDAO.autenticar("admin", "admin123");
                    if (adminRestaurado != null) {
                        sessao.setUsuarioLogado(adminRestaurado);
                    }
                    System.out.println("\n✅ SUCESSO: Tabela de USUÁRIOS limpa com sucesso!");
                    System.out.println("Contador de ID de usuários zerado e usuário padrão 'admin' (senha: 'admin123') restaurado.");
                }
            }

        } catch (SQLException e) {
            System.out.println("\n❌ Erro no banco de dados ao executar o reset: " + e.getMessage());
        }

        return true;
    }
}
