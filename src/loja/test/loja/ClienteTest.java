package loja;

import loja.dominio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Cliente.cpfValido()")
class ClienteTest {

    private Cliente criarClienteComCpf(String cpf) {
        return new Cliente(1L, "Cliente Teste", "teste@example.com", cpf);
    }

    @ParameterizedTest(name = "CPF válido formatado: {0}")
    @ValueSource(strings = {
            "529.982.247-25",
            "111.444.777-35",
            "390.533.447-05",
            "100.000.028-10"
    })
    @DisplayName("Deve retornar true para CPFs válidos com pontuação")
    void deveAceitarCpfsValidosComPontuacao(String cpf) {
        assertTrue(criarClienteComCpf(cpf).cpfValido());
    }

    @ParameterizedTest(name = "CPF válido sem pontuação: {0}")
    @ValueSource(strings = {
            "52998224725",
            "11144477735",
            "39053344705",
            "10000002810"
    })
    @DisplayName("Deve retornar true para CPFs válidos somente com dígitos")
    void deveAceitarCpfsValidosSemPontuacao(String cpf) {
        assertTrue(criarClienteComCpf(cpf).cpfValido());
    }

    @Test
    @DisplayName("Deve retornar true quando o cálculo do primeiro ou segundo dígito verificador resultar em resto 10 (convertido para 0)")
    void deveAceitarCpfsComRestoDezConvertidoParaZero() {
        // 39053344705: resto do 1º dígito == 10 -> dígito verificador 0
        assertTrue(criarClienteComCpf("39053344705").cpfValido());
        // 10000002810: resto do 2º dígito == 10 -> dígito verificador 0
        assertTrue(criarClienteComCpf("10000002810").cpfValido());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "...---"})
    @DisplayName("Deve retornar false para CPF nulo, vazio ou sem dígitos")
    void deveRejeitarCpfNuloOuVazio(String cpf) {
        assertFalse(criarClienteComCpf(cpf).cpfValido());
    }

    @ParameterizedTest(name = "CPF com quantidade incorreta de dígitos: {0}")
    @ValueSource(strings = {
            "1234567890",
            "529.982.247-2",
            "529982247250",
            "529.982.247-259",
            "123"
    })
    @DisplayName("Deve retornar false para CPF com menos ou mais de 11 dígitos")
    void deveRejeitarCpfComTamanhoInvalido(String cpf) {
        assertFalse(criarClienteComCpf(cpf).cpfValido());
    }

    @ParameterizedTest(name = "CPF com dígitos repetidos: {0}")
    @ValueSource(strings = {
            "000.000.000-00",
            "111.111.111-11",
            "22222222222",
            "999.999.999-99"
    })
    @DisplayName("Deve retornar false para CPF com todos os dígitos iguais")
    void deveRejeitarCpfComTodosDigitosIguais(String cpf) {
        assertFalse(criarClienteComCpf(cpf).cpfValido());
    }

    @Test
    @DisplayName("Deve retornar false quando o primeiro dígito verificador for inválido")
    void deveRejeitarPrimeiroDigitoVerificadorInvalido() {
        // Correto: 529.982.247-25 -> alterado 1º DV de 2 para 3
        assertFalse(criarClienteComCpf("529.982.247-35").cpfValido());
    }

    @Test
    @DisplayName("Deve retornar false quando o segundo dígito verificador for inválido")
    void deveRejeitarSegundoDigitoVerificadorInvalido() {
        // Correto: 529.982.247-25 -> alterado 2º DV de 5 para 6
        assertFalse(criarClienteComCpf("529.982.247-26").cpfValido());
    }

    @Test
    @DisplayName("Deve retornar false quando ambos os dígitos verificadores forem inválidos")
    void deveRejeitarAmbosDigitosVerificadoresInvalidos() {
        assertFalse(criarClienteComCpf("529.982.247-99").cpfValido());
    }
}
