package com.jrcr.gestorofertasweb.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para el enum {@link TipoMensaje}.
 *
 * Verifica que todos los tipos de mensaje estén correctamente definidos.
 */
class TipoMensajeTest {

    @Test
    @DisplayName("Debe tener 4 tipos de mensaje: SUCCESS, INFO, WARNING, ERROR")
    void values_deberiaTener4Tipos() {
        assertEquals(4, TipoMensaje.values().length);
    }

    @Test
    @DisplayName("SUCCESS debe existir")
    void success_deberiaExistir() {
        assertNotNull(TipoMensaje.SUCCESS);
        assertEquals("SUCCESS", TipoMensaje.SUCCESS.name());
    }

    @Test
    @DisplayName("INFO debe existir")
    void info_deberiaExistir() {
        assertNotNull(TipoMensaje.INFO);
        assertEquals("INFO", TipoMensaje.INFO.name());
    }

    @Test
    @DisplayName("WARNING debe existir")
    void warning_deberiaExistir() {
        assertNotNull(TipoMensaje.WARNING);
        assertEquals("WARNING", TipoMensaje.WARNING.name());
    }

    @Test
    @DisplayName("ERROR debe existir")
    void error_deberiaExistir() {
        assertNotNull(TipoMensaje.ERROR);
        assertEquals("ERROR", TipoMensaje.ERROR.name());
    }

    @Test
    @DisplayName("valueOf debe encontrar cada tipo por su nombre")
    void valueOf_deberiaEncontrarTodosLosTipos() {
        assertEquals(TipoMensaje.SUCCESS, TipoMensaje.valueOf("SUCCESS"));
        assertEquals(TipoMensaje.INFO, TipoMensaje.valueOf("INFO"));
        assertEquals(TipoMensaje.WARNING, TipoMensaje.valueOf("WARNING"));
        assertEquals(TipoMensaje.ERROR, TipoMensaje.valueOf("ERROR"));
    }

    @Test
    @DisplayName("valueOf con nombre inexistente debe lanzar IllegalArgumentException")
    void valueOf_conNombreInexistente_deberiaLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            TipoMensaje.valueOf("INEXISTENTE");
        });
    }

    @Test
    @DisplayName("valueOf con null debe lanzar NullPointerException")
    void valueOf_conNull_deberiaLanzarExcepcion() {
        assertThrows(NullPointerException.class, () -> {
            TipoMensaje.valueOf(null);
        });
    }
}