package com.jrcr.gestorofertasweb.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para el enum {@link TipoCatalogo}.
 *
 * Verifica los valores de cada tipo de catálogo y el método getValor().
 */
class TipoCatalogoTest {

    // ==========================================
    // TESTS DE getValor()
    // ==========================================

    @Test
    @DisplayName("MODALIDAD debe devolver el valor correcto")
    void getValor_MODALIDAD_deberiaDevolverModalidad() {
        assertEquals("modalidad", TipoCatalogo.MODALIDAD.getValor());
    }

    @Test
    @DisplayName("ESTADO debe devolver el valor correcto")
    void getValor_ESTADO_deberiaDevolverEstado() {
        assertEquals("estado", TipoCatalogo.ESTADO.getValor());
    }

    @Test
    @DisplayName("METODO_ENVIO debe devolver el valor correcto")
    void getValor_METODO_ENVIO_deberiaDevolverMetodoEnvio() {
        assertEquals("metodo_envio", TipoCatalogo.METODO_ENVIO.getValor());
    }

    @Test
    @DisplayName("SECTOR debe devolver el valor correcto")
    void getValor_SECTOR_deberiaDevolverSector() {
        assertEquals("sector", TipoCatalogo.SECTOR.getValor());
    }

    // ==========================================
    // TESTS DE values()
    // ==========================================

    @Test
    @DisplayName("Debe tener al menos 4 tipos de catálogo")
    void values_deberiaTenerAlMenos4Tipos() {
        assertTrue(TipoCatalogo.values().length >= 4);
    }

    @Test
    @DisplayName("Todos los tipos deben tener un valor no nulo")
    void values_todosDebenTenerValorNoNulo() {
        for (TipoCatalogo tipo : TipoCatalogo.values()) {
            assertNotNull(tipo.getValor(), "El tipo " + tipo.name() + " tiene valor null");
            assertFalse(tipo.getValor().isEmpty(), "El tipo " + tipo.name() + " tiene valor vacío");
        }
    }

    @Test
    @DisplayName("Todos los valores deben ser únicos")
    void values_todosLosValoresDebenSerUnicos() {
        TipoCatalogo[] tipos = TipoCatalogo.values();
        for (int i = 0; i < tipos.length; i++) {
            for (int j = i + 1; j < tipos.length; j++) {
                assertNotEquals(tipos[i].getValor(), tipos[j].getValor(),
                        "Los tipos " + tipos[i].name() + " y " + tipos[j].name() + " tienen el mismo valor");
            }
        }
    }

    @Test
    @DisplayName("valueOf debe encontrar cada tipo por su nombre")
    void valueOf_deberiaEncontrarTodosLosTipos() {
        assertEquals(TipoCatalogo.MODALIDAD, TipoCatalogo.valueOf("MODALIDAD"));
        assertEquals(TipoCatalogo.ESTADO, TipoCatalogo.valueOf("ESTADO"));
        assertEquals(TipoCatalogo.METODO_ENVIO, TipoCatalogo.valueOf("METODO_ENVIO"));
        assertEquals(TipoCatalogo.SECTOR, TipoCatalogo.valueOf("SECTOR"));
    }
}