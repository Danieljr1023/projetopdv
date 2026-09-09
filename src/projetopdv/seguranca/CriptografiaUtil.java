package projetopdv.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class CriptografiaUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TAMANHO_SALT_BYTES = 16;

    private CriptografiaUtil() {
        // Classe utilitária com métodos estáticos; não deve ser instanciada.
    }

    // Gera um Salt criptograficamente seguro em formato Hexadecimal (32 caracteres).
    public static String gerarSalt() {
        byte[] saltBytes = new byte[TAMANHO_SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        return HexFormat.of().formatHex(saltBytes);
    }

    // Gera o Hash SHA-256 da senha combinada com o Salt. Retorna a representação Hexadecimal (64 caracteres).
    public static String gerarHash(String senhaTexto, String salt) {
        if (senhaTexto == null || salt == null) {
            throw new IllegalArgumentException("Senha e salt não podem ser nulos.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combinacao = senhaTexto + salt;
            byte[] hashBytes = digest.digest(combinacao.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível no ambiente Java.", e);
        }
    }

    // Verifica se a senha em texto fornecida corresponde ao hash esperado usando comparação de tempo constante.
    public static boolean verificarSenha(String senhaTexto, String salt, String hashEsperado) {
        if (senhaTexto == null || salt == null || hashEsperado == null) {
            return false;
        }
        String hashCalculado = gerarHash(senhaTexto, salt);
        return MessageDigest.isEqual(
                hashCalculado.getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8)
        );
    }
}
