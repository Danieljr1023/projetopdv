package projetopdv.ui.comandos.caixa;

import projetopdv.ui.comandos.ComandoPainel;

public class ComandoSairCaixa extends ComandoPainel {

    public ComandoSairCaixa() {
        super("/sair", "Encerra o terminal da Frente de Caixa.");
    }

    @Override
    public boolean executar(String[] argumentos) {
        System.out.println("\nFinalizando Frente de Caixa (PDV)... Até logo!");
        return false;
    }
}
