package loja.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cliente da loja, com endereços e programa de fidelidade. */
public class Cliente {

    public enum Nivel { BRONZE, PRATA, OURO }

    private final long id;
    private final String nome;
    private final String email;
    private final String cpf;
    private final LocalDate dataCadastro = LocalDate.now();
    private final List<Endereco> enderecos = new ArrayList<>();
    private int pontos;

    public Cliente(long id, String nome, String email, String cpf) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCpf() { return cpf; }
    public LocalDate getDataCadastro() { return dataCadastro; }
    public int getPontos() { return pontos; }

    public List<Endereco> getEnderecos() {
        return Collections.unmodifiableList(enderecos);
    }

    public void adicionarEndereco(Endereco endereco) {
        if (endereco == null || !endereco.cepValido()) {
            throw new IllegalArgumentException("Endereço inválido");
        }
        enderecos.add(endereco);
    }

    public Endereco enderecoPrincipal() {
        if (enderecos.isEmpty()) {
            throw new IllegalStateException("Cliente sem endereço cadastrado");
        }
        return enderecos.get(0);
    }

    public boolean emailValido() {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    /** Valida o CPF pelos dois dígitos verificadores. */
    public boolean cpfValido() {
        if (cpf == null) {
            return false;
        }
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public void adicionarPontos(int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("Pontos não podem ser negativos");
        }
        pontos += quantidade;
    }

    public void removerPontos(int quantidade) {
        pontos = Math.max(0, pontos - Math.max(0, quantidade));
    }

    /** Nível de fidelidade calculado a partir dos pontos acumulados. */
    public Nivel getNivel() {
        if (pontos >= 2000) {
            return Nivel.OURO;
        }
        return pontos >= 500 ? Nivel.PRATA : Nivel.BRONZE;
    }

    @Override
    public String toString() {
        return nome + " <" + email + ">";
    }
}
