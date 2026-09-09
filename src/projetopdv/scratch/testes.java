package projetopdv.scratch;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoAbrirOrcamento;
import projetopdv.ui.comandos.ComandoCadastrarCliente;
import projetopdv.ui.comandos.ComandoCadastrarProduto;
import projetopdv.ui.comandos.ComandoCadastrarUsuario;
import projetopdv.ui.comandos.ComandoCliente;
import projetopdv.ui.comandos.ComandoConsultarCliente;
import projetopdv.ui.comandos.ComandoConsultarProdutos;
import projetopdv.ui.comandos.ComandoDuplicarOrcamento;
import projetopdv.ui.comandos.ComandoHelp;
import projetopdv.ui.comandos.ComandoListarOrcamentos;
import projetopdv.ui.comandos.ComandoLogin;
import projetopdv.ui.comandos.ComandoLogout;
import projetopdv.ui.comandos.ComandoNovoOrcamento;
import projetopdv.ui.comandos.ComandoProduto;
import projetopdv.ui.comandos.ComandoRecuperarOrcamento;
import projetopdv.ui.comandos.ComandoResetarBanco;
import projetopdv.ui.comandos.ComandoSair;
import projetopdv.ui.comandos.ComandoUsuario;
import projetopdv.ui.comandos.ComandosPainel;
import projetopdv.ui.comandos.orcamento.ComandoDescartarOrcamento;
import projetopdv.usuario.PerfilUsuario;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;

public class testes {

    // Lista com 100 produtos alimentícios reais do mercado brasileiro (Nome, Código de Barras EAN-13, Preço de Venda)
    private static final String[][] PRODUTOS_ALIMENTICIOS = {
        {"Arroz Branco Tipo 1 5kg Camil", "7896006711116", "29.90"},
        {"Arroz Parboilizado 1kg Tio João", "7893000392015", "7.49"},
        {"Arroz Integral 1kg Tio João", "7893000392053", "8.99"},
        {"Feijão Carioca Tipo 1 1kg Camil", "7896006721016", "8.49"},
        {"Feijão Preto Tipo 1 1kg Kicaldo", "7896434900146", "7.99"},
        {"Feijão Vermelho 1kg Camil", "7896006721115", "10.50"},
        {"Feijão Fradinho 1kg Kicaldo", "7896434900252", "9.20"},
        {"Grão-de-Bico 500g Yoki", "7891095000507", "9.89"},
        {"Lentilha Seca 500g Yoki", "7891095000606", "8.75"},
        {"Ervilha Partida Seca 500g Yoki", "7891095000705", "6.30"},
        {"Milho de Pipoca Premium 500g Yoki", "7891095001108", "5.99"},
        {"Aveia em Flocos Finos 170g Quaker", "7891000244101", "5.49"},
        {"Aveia em Flocos Regulares 170g Quaker", "7891000244200", "5.49"},
        {"Farelo de Aveia 170g Quaker", "7891000244309", "6.29"},
        {"Quinoa em Grãos Real 250g Mãe Terra", "7896280500125", "16.90"},
        {"Farinha de Trigo Tradicional Tipo 1 1kg Dona Benta", "7896005200109", "5.29"},
        {"Farinha de Trigo com Fermento 1kg Dona Benta", "7896005200208", "6.19"},
        {"Farinha de Trigo Integral 1kg Renata", "7896016101112", "6.89"},
        {"Farinha de Mandioca Torrada 1kg Yoki", "7891095002105", "7.99"},
        {"Farinha de Mandioca Fina 1kg Yoki", "7891095002204", "7.59"},
        {"Farofa Pronta Temperada Suave 500g Yoki", "7891095003102", "6.49"},
        {"Farofa Pronta Picante 500g Yoki", "7891095003201", "6.49"},
        {"Polvilho Azedo 500g Yoki", "7891095004109", "8.99"},
        {"Polvilho Doce 500g Yoki", "7891095004208", "8.49"},
        {"Fubá Mimoso Enriquecido 1kg Yoki", "7891095005106", "4.39"},
        {"Flocão de Milho para Cuscuz 500g Maratá", "7896023700100", "3.29"},
        {"Amido de Milho Original 500g Maizena", "7891025103100", "7.99"},
        {"Açúcar Refinado Especial 1kg União", "7891910000107", "4.89"},
        {"Açúcar Cristal Branco 1kg União", "7891910000206", "4.39"},
        {"Açúcar Demerara Natural 1kg União", "7891910000305", "6.79"},
        {"Açúcar Mascavo Orgânico 500g Mãe Terra", "7896280500200", "9.50"},
        {"Açúcar de Confeiteiro Glaçúcar 500g União", "7891910000404", "5.60"},
        {"Sal Refinado Iodado 1kg Cisne", "7896014300012", "2.99"},
        {"Sal Grosso para Churrasco 1kg Cisne", "7896014300029", "3.49"},
        {"Óleo de Soja Refinado 900ml Liza", "7891080010016", "6.79"},
        {"Óleo de Girassol Puro 900ml Liza", "7891080010023", "10.99"},
        {"Óleo de Milho Refinado 900ml Mazola", "7891080010030", "12.49"},
        {"Óleo de Canola Refinado 900ml Liza", "7891080010047", "11.89"},
        {"Azeite de Oliva Extra Virgem 500ml Gallo", "5601111000018", "44.90"},
        {"Azeite de Oliva Tipo Único 500ml Borges", "8410118000012", "36.90"},
        {"Vinagre de Álcool Tradicional 750ml Castelo", "7896017200012", "2.49"},
        {"Vinagre de Maçã Puro Natural 750ml Castelo", "7896017200029", "5.89"},
        {"Vinagre Balsâmico Tradicional 500ml Castelo", "7896017200036", "14.50"},
        {"Macarrão Espaguete nº 8 Grano Duro 500g Barilla", "8076809513753", "8.99"},
        {"Macarrão Penne Rigate Grano Duro 500g Barilla", "8076809513760", "8.99"},
        {"Macarrão Parafuso com Ovos 500g Renata", "7896016102102", "4.99"},
        {"Macarrão Espaguete Tradicional 500g Dona Benta", "7896005201106", "3.89"},
        {"Macarrão Instantâneo Sabor Galinha Caipira 85g Nissin Lámen", "7891079012341", "2.79"},
        {"Macarrão Instantâneo Sabor Carne 85g Nissin Lámen", "7891079012358", "2.79"},
        {"Massa para Lasanha com Ovos 500g Barilla", "8076809513777", "12.90"},
        {"Molho de Tomate Tradicional Sachê 300g Pomarola", "7891080020015", "3.29"},
        {"Molho de Tomate com Manjericão 300g Heinz", "7891080020022", "4.79"},
        {"Extrato de Tomate Concentrado 340g Elefante", "7891080020039", "6.19"},
        {"Polpa de Tomate Passata Rústica 500g Cirio", "8001440123456", "11.90"},
        {"Café Torrado e Moído Tradicional 500g Pilão", "7891090001011", "19.89"},
        {"Café Torrado e Moído Extraforte 500g Pilão", "7891090001028", "19.89"},
        {"Café Especial Puro Arábica 250g Melitta", "7891021001011", "16.90"},
        {"Café Solúvel Granulado 100g Nescafé Tradição", "7891000100100", "14.50"},
        {"Filtro de Papel para Café nº 103 Melitta", "7891021002018", "5.99"},
        {"Chá Preto Tradicional 25 Sachês Leão", "7891025001017", "6.89"},
        {"Chá de Camomila 15 Sachês Leão", "7891025001024", "5.49"},
        {"Achocolatado em Pó Original 400g Nescau", "7891000053109", "9.79"},
        {"Achocolatado em Pó Chocolate com Malte 400g Toddy", "7894321711269", "8.99"},
        {"Cacau em Pó Puro Solúvel 200g Dr. Oetker", "7891070001010", "13.90"},
        {"Leite UHT Integral 1L Italac", "7898080640010", "4.99"},
        {"Leite UHT Desnatado 1L Piracanjuba", "7898215151015", "5.19"},
        {"Leite UHT Sem Lactose 1L Piracanjuba", "7898215151022", "6.49"},
        {"Leite em Pó Integral Instantâneo Lata 400g Ninho", "7891000300104", "18.90"},
        {"Leite Condensado Semidesnatado TP 395g Moça Nestlé", "7891000255107", "6.99"},
        {"Creme de Leite Leve TP 200g Nestlé", "7891000256104", "3.99"},
        {"Manteiga de Primeira Qualidade com Sal 200g Aviação", "7896051110018", "13.90"},
        {"Margarina Cremosa com Sal 500g Qualy", "7893000500014", "8.49"},
        {"Requeijão Cremoso Tradicional Copo 200g Poços de Caldas", "7891025301018", "8.79"},
        {"Queijo Parmesão Ralado Pacote 50g Vigor", "7891025302015", "4.89"},
        {"Maionese Tradicional Pote 500g Hellmanns", "7891030001019", "9.89"},
        {"Ketchup Tradicional Pet 397g Heinz", "7891030002016", "10.49"},
        {"Mostarda Amarela Tradicional Pet 255g Heinz", "7891030003013", "9.49"},
        {"Molho Shoyu Tradicional 500ml Sakura", "7896055500013", "6.99"},
        {"Molho Inglês 150ml Kenko", "7896055500020", "4.90"},
        {"Molho de Pimenta Vermelha Tradicional 60ml Tabasco", "011210000015", "18.50"},
        {"Milho Verde em Conserva Lata 170g Quero", "7896102500010", "3.89"},
        {"Ervilha Fresca em Conserva Lata 170g Quero", "7896102500027", "3.89"},
        {"Dueto Milho e Ervilha Lata 170g Quero", "7896102500034", "4.19"},
        {"Atum Sólido em Óleo Lata 170g Gomes da Costa", "7891164001018", "9.90"},
        {"Atum Ralado ao Natural Lata 170g Coqueiro", "7891164002015", "6.99"},
        {"Sardinha com Molho de Tomate Lata 125g Coqueiro", "7891164003012", "4.99"},
        {"Azeitona Verde sem Caroço Sachê 150g Raiola", "7896082200015", "6.89"},
        {"Cogumelo Champignon Fatiado em Conserva 100g Hemmer", "7891060001014", "11.90"},
        {"Palmito Pupunha Inteiro Vidro 300g Rigomel", "7898912345012", "22.90"},
        {"Pão de Forma Tradicional 500g Wickbold", "7896064200011", "8.99"},
        {"Torrada Tradicional Salgada 140g Bauducco", "7891962000018", "4.99"},
        {"Biscoito Cream Cracker Folhata 200g Marilan", "7896003700014", "3.99"},
        {"Biscoito Recheado Chocolate 136g Passatempo Nestlé", "7891000201012", "2.99"},
        {"Biscoito Recheado Sabor Baunilha 90g Oreo", "7622210123456", "3.89"},
        {"Biscoito Maisena Tradicional 360g Tostines", "7891000202019", "6.49"},
        {"Chocolate ao Leite em Barra 90g Garoto", "7891008000013", "5.99"},
        {"Chocolate Meio Amargo em Barra 90g Lacta", "7622300123457", "6.49"},
        {"Doce de Leite Cremoso Tradicional Pote 400g Itambé", "7896051120017", "11.90"},
        {"Goiabada Cascão Tradicional Barra 300g Predilecta", "7896292200013", "5.89"},
        {"Gelatina em Pó Sabor Morango 20g Royal", "7622300001014", "1.99"}
    };

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("CADASTRO EM LOTE DE 100 PRODUTOS ALIMENTÍCIOS VIA PAINEL (OPERADOR: MAQUINA)");
        System.out.println("===============================================================================\n");

        // 1. Inicializar tabelas e resetar catálogo de produtos para um lote limpo
        BancoDeDados.inicializarTabelas();
        BancoDeDados.resetarTabelaProduto();

        ProdutoDAO produtoDAO = new ProdutoDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        // 2. Garantir a existência do usuário 'maquina' com senha '0000' e perfil GERENTE
        Usuario usuarioMaquina = usuarioDAO.buscarPorLogin("maquina");
        if (usuarioMaquina == null) {
            System.out.println("-> Cadastrando operador 'maquina' com perfil GERENTE...");
            usuarioDAO.cadastrar("Máquina", "maquina", "0000", PerfilUsuario.GERENTE);
        } else {
            System.out.println("-> Operador 'maquina' encontrado. Atualizando senha para '0000'...");
            usuarioDAO.alterarSenha(usuarioMaquina.getIdUsuario(), "0000");
            if (!usuarioMaquina.isAtivo()) {
                usuarioDAO.ativar(usuarioMaquina.getIdUsuario());
            }
        }

        // 3. Montar sequência de comandos e entradas para o Painel
        // Fluxo: /login maquina 0000 -> 100x /cadastrar_produto (Nome \n Codigo \n Preco) -> /sair
        StringBuilder bufferComandos = new StringBuilder();
        bufferComandos.append("/login maquina 0000\n");

        for (String[] prod : PRODUTOS_ALIMENTICIOS) {
            bufferComandos.append("/cadastrar_produto\n");
            bufferComandos.append(prod[0]).append("\n"); // Nome do produto
            bufferComandos.append(prod[1]).append("\n"); // Código de barras
            bufferComandos.append(prod[2]).append("\n"); // Preço de venda
        }

        bufferComandos.append("/sair\n");

        // 4. Instanciar o Painel com os mesmos comandos registrados no sistema
        Scanner entradaSimulada = new Scanner(new ByteArrayInputStream(bufferComandos.toString().getBytes(StandardCharsets.UTF_8)));
        ComandosPainel comandos = new ComandosPainel();
        SessaoOrcamento sessao = new SessaoOrcamento(entradaSimulada, orcamentoDAO, produtoDAO, clienteDAO, usuarioDAO, comandos);

        // Registro idêntico ao Painel.java
        comandos.registrar(new ComandoSair());
        comandos.registrar(new ComandoHelp(comandos));
        comandos.registrar(new ComandoLogin(sessao));
        comandos.registrar(new ComandoLogout(sessao));
        comandos.registrar(new ComandoCadastrarUsuario(sessao));
        comandos.registrar(new ComandoUsuario(sessao));
        comandos.registrar(new ComandoResetarBanco(sessao));
        comandos.registrar(new ComandoCadastrarProduto(sessao));
        comandos.registrar(new ComandoConsultarProdutos(sessao));
        comandos.registrar(new ComandoProduto(sessao));
        comandos.registrar(new ComandoCadastrarCliente(sessao));
        comandos.registrar(new ComandoConsultarCliente(sessao));
        comandos.registrar(new ComandoCliente(sessao));
        comandos.registrar(new ComandoNovoOrcamento(sessao));
        comandos.registrar(new ComandoAbrirOrcamento(sessao));
        comandos.registrar(new ComandoRecuperarOrcamento(sessao));
        comandos.registrar(new ComandoListarOrcamentos(sessao));
        comandos.registrar(new ComandoDuplicarOrcamento(sessao));
        comandos.registrar(new ComandoDescartarOrcamento(sessao));

        // 5. Executar fluxo interativo através da Sessão do Painel
        boolean executando = true;
        while (executando && entradaSimulada.hasNextLine()) {
            String linha = entradaSimulada.nextLine();
            try {
                executando = sessao.processarLinha(linha);
            } catch (Exception e) {
                System.out.println("Erro ao processar linha: " + e.getMessage());
            }
        }

        // 6. Relatório final de confirmação
        try {
            List<Produto> produtosCadastrados = produtoDAO.listarTodos();
            System.out.println("\n===============================================================================");
            System.out.println("        ✅ LOTE DE 100 PRODUTOS ALIMENTÍCIOS CADASTRADOS COM SUCESSO!          ");
            System.out.println("===============================================================================");
            System.out.printf("Total de produtos presentes no banco de dados: %d produto(s)%n", produtosCadastrados.size());
            System.out.println("Operador responsável pelo cadastro: maquina (Perfil: GERENTE)");
            System.out.println("===============================================================================\n");
        } catch (SQLException e) {
            System.out.println("Erro ao consultar produtos: " + e.getMessage());
        }
    }
}
