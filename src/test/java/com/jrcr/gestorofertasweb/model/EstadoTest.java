package com.jrcr.gestorofertasweb.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para el enum {@link Estado}.
 *
 * Verifica los valores, el método getValor() y la conversión fromValor().
 */
class EstadoTest {

    // ==========================================
    // TESTS DE getValor()
    // ==========================================

    @Test
    @DisplayName("INSCRITO debe devolver el valor correcto")
    void getValor_INSCRITO_deberiaDevolverInscrito() {
        assertEquals("Inscrito", Estado.INSCRITO.getValor());
    }

    @Test
    @DisplayName("ENTREVISTA debe devolver el valor correcto")
    void getValor_ENTREVISTA_deberiaDevolverEntrevista() {
        assertEquals("Entrevista", Estado.ENTREVISTA.getValor());
    }

    @Test
    @DisplayName("EN_REVISION debe devolver el valor correcto")
    void getValor_EN_REVISION_deberiaDevolverEnRevision() {
        assertEquals("En revisión", Estado.EN_REVISION.getValor());
    }

    @Test
    @DisplayName("DESCARTADO debe devolver el valor correcto")
    void getValor_DESCARTADO_deberiaDevolverDescartado() {
        assertEquals("Descartado", Estado.DESCARTADO.getValor());
    }


    @Test
    @DisplayName("CERRADO debe devolver el valor correcto")
    void getValor_RECHAZADO_deberiaDevolverCerrado() {
        assertEquals("Cerrado", Estado.CERRADO.getValor());
    }

    // ==========================================
    // TESTS DE fromValor()
    // ==========================================

    @Test
    @DisplayName("fromValor debe encontrar INSCRITO por su valor")
    void fromValor_conInscrito_deberiaDevolverINSCRITO() {
        assertEquals(Estado.INSCRITO, Estado.fromValor("Inscrito"));
    }

    @Test
    @DisplayName("fromValor debe encontrar ENTREVISTA por su valor")
    void fromValor_conEntrevista_deberiaDevolverENTREVISTA() {
        assertEquals(Estado.ENTREVISTA, Estado.fromValor("Entrevista"));
    }

    @Test
    @DisplayName("fromValor debe devolver null cuando el valor no existe")
    void fromValor_conValorInexistente_deberiaDevolverNull() {
        assertNull(Estado.fromValor("ValorInexistente"));
    }

    @Test
    @DisplayName("fromValor con null debe devolver null")
    void fromValor_conNull_deberiaDevolverNull() {
        assertNull(Estado.fromValor(null));
    }

    // ==========================================
    // TESTS DE values()
    // ==========================================

    @Test
    @DisplayName("Debe tener al menos 6 estados definidos")
    void values_deberiaTenerAlMenos6Estados() {
        assertTrue(Estado.values().length >= 5);
    }

    @Test
    @DisplayName("Todos los estados deben tener un valor no nulo")
    void values_todosDebenTenerValorNoNulo() {
        for (Estado estado : Estado.values()) {
            assertNotNull(estado.getValor(), "El estado " + estado.name() + " tiene valor null");
            assertFalse(estado.getValor().isEmpty(), "El estado " + estado.name() + " tiene valor vacío");
        }
    }
}