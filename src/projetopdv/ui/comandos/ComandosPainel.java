package projetopdv.ui.comandos;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComandosPainel {

    private final Map<String, ComandoPainel> comandos
            = new LinkedHashMap<>();

    public void registrar(ComandoPainel comando) {
        comandos.put(
                comando.getNome().toLowerCase(),
                comando
        );
    }

    public ComandoPainel buscar(String nome) {
        return comandos.get(nome.toLowerCase());
    }

    public Collection<ComandoPainel> listar() {
        return comandos.values();
    }
}
