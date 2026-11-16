package loja.dominio;

import java.util.HashMap;
import java.util.Map;

/** Endereço de entrega de um cliente. */
public class Endereco {

    public enum Regiao { SUDESTE, SUL, CENTRO_OESTE, NORDESTE, NORTE }

    private static final Map<String, Regiao> REGIOES = new HashMap<>();

    static {
        registrar(Regiao.SUDESTE, "SP", "RJ", "MG", "ES");
        registrar(Regiao.SUL, "PR", "SC", "RS");
        registrar(Regiao.CENTRO_OESTE, "DF", "GO", "MT", "MS");
        registrar(Regiao.NORDESTE, "BA", "SE", "AL", "PE", "PB", "RN", "CE", "PI", "MA");
        registrar(Regiao.NORTE, "AM", "RR", "AP", "PA", "TO", "RO", "AC");
    }

    private final String rua;
    private final String numero;
    private final String bairro;
    private final String cidade;
    private final String estado;
    private final String cep;

    public Endereco(String rua, String numero, String bairro, String cidade, String estado, String cep) {
        this.rua = rua;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.cep = cep;
    }

    private static void registrar(Regiao regiao, String... ufs) {
        for (String uf : ufs) {
            REGIOES.put(uf, regiao);
        }
    }

    public String getRua() { return rua; }
    public String getNumero() { return numero; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
    public String getCep() { return cep; }

    /** CEP somente com dígitos. */
    public String cepNumerico() {
        return cep == null ? "" : cep.replaceAll("\\D", "");
    }

    public boolean cepValido() {
        return cepNumerico().length() == 8;
    }

    /** Região do país a partir da UF; usada para calcular o frete. */
    public Regiao regiao() {
        Regiao r = estado == null ? null : REGIOES.get(estado.trim().toUpperCase());
        if (r == null) {
            throw new IllegalArgumentException("UF inválida: " + estado);
        }
        return r;
    }

    public String enderecoCompleto() {
        return rua + ", " + numero + " - " + bairro + ", " + cidade + "/" + estado + " - CEP " + cep;
    }

    @Override
    public String toString() {
        return enderecoCompleto();
    }
}
