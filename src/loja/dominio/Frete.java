package loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Regras de cálculo de frete por região, peso, modalidade e perfil do cliente. */
public class Frete {

    public enum Modalidade { PADRAO, EXPRESSO, RETIRADA }

    private static final BigDecimal LIMITE_PESO_KG = new BigDecimal("5");

    private final Map<Endereco.Regiao, BigDecimal> tarifaBase = new EnumMap<>(Endereco.Regiao.class);
    private final BigDecimal adicionalPorKg = new BigDecimal("3.50");
    private final BigDecimal fatorExpresso = new BigDecimal("1.80");
    private final BigDecimal fatorVip = new BigDecimal("0.80");
    private final BigDecimal limiteFreteGratis = new BigDecimal("300.00");

    public Frete() {
        tarifaBase.put(Endereco.Regiao.SUDESTE, new BigDecimal("15.90"));
        tarifaBase.put(Endereco.Regiao.SUL, new BigDecimal("19.90"));
        tarifaBase.put(Endereco.Regiao.CENTRO_OESTE, new BigDecimal("24.90"));
        tarifaBase.put(Endereco.Regiao.NORDESTE, new BigDecimal("29.90"));
        tarifaBase.put(Endereco.Regiao.NORTE, new BigDecimal("39.90"));
    }

    public BigDecimal getLimiteFreteGratis() { return limiteFreteGratis; }
    public BigDecimal getAdicionalPorKg() { return adicionalPorKg; }

    public BigDecimal tarifaDaRegiao(Endereco.Regiao regiao) {
        return tarifaBase.get(regiao);
    }

    /**
     * Calcula o valor do frete.
     * <ul>
     *   <li>tarifa base conforme a região do destino;</li>
     *   <li>adicional por kg acima de 5 kg;</li>
     *   <li>modalidade expressa custa 80% a mais; retirada na loja é gratuita;</li>
     *   <li>clientes de nível OURO recebem 20% de desconto no frete;</li>
     *   <li>pedidos a partir de R$ 300,00 têm frete grátis.</li>
     * </ul>
     */
    public BigDecimal calcular(Endereco destino, BigDecimal pesoKg, BigDecimal valorPedido,
                               Modalidade modalidade, boolean clienteVip) {
        if (destino == null || !destino.cepValido()) {
            throw new IllegalArgumentException("Endereço de entrega inválido");}
        if (pesoKg == null || pesoKg.signum() <= 0) {
            throw new IllegalArgumentException("Peso do pedido deve ser positivo");}
        Objects.requireNonNull(valorPedido, "valorPedido");
        Objects.requireNonNull(modalidade, "modalidade");
        BigDecimal custo = tarifaBase.get(destino.regiao());
        if (pesoKg.compareTo(LIMITE_PESO_KG) > 0) {
            custo = custo.add(pesoKg.subtract(LIMITE_PESO_KG).multiply(adicionalPorKg));}
        if (modalidade == Modalidade.EXPRESSO) {
            custo = custo.multiply(fatorExpresso);
        } else if (modalidade == Modalidade.RETIRADA) {
            return BigDecimal.ZERO.setScale(2);}
        if (clienteVip) {
            custo = custo.multiply(fatorVip);}
        if (valorPedido.compareTo(limiteFreteGratis) >= 0) {
            return BigDecimal.ZERO.setScale(2);}
        return custo.setScale(2, RoundingMode.HALF_UP);
    }

    /** Prazo estimado de entrega em dias úteis. */
    public int prazoEstimadoDias(Endereco destino, Modalidade modalidade) {
        if (modalidade == Modalidade.RETIRADA) {
            return 0;}
        int prazo = destino.regiao() == Endereco.Regiao.SUDESTE ? 3 : 7;
        return modalidade == Modalidade.EXPRESSO ? Math.max(1, prazo / 2) : prazo;
    }

    @Override
    public String toString() {
        return "Frete[grátis a partir de R$ " + limiteFreteGratis + "]";
    }
}
