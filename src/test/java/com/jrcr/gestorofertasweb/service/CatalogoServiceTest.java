package com.jrcr.gestorofertasweb.service;

import com.jrcr.gestorofertasweb.model.Catalogo;
import com.jrcr.gestorofertasweb.repository.CatalogoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link CatalogoService}.
 */
class CatalogoServiceTest {

    private CatalogoService catalogoService;
    private CatalogoRepository catalogoRepository;

    @BeforeEach
    void setUp() {
        // Crear el mock manualmente (más robusto que @Mock)
        catalogoRepository = mock(CatalogoRepository.class);
        catalogoService = new CatalogoService(catalogoRepository);
    }

    @Test
    @DisplayName("Debe devolver lista de valores para un tipo de catálogo válido")
    void obtenerValoresPorTipo_conTipoValido_deberiaDevolverLista() {
        // Given
        List<Catalogo> catalogosEsperados = Arrays.asList(
                crearCatalogo(1L, "modalidad", "Teletrabajo", 1),
                crearCatalogo(2L, "modalidad", "Presencial", 2),
                crearCatalogo(3L, "modalidad", "Híbrido", 3)
        );
        when(catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("modalidad"))
                .thenReturn(catalogosEsperados);

        // When
        List<String> resultado = catalogoService.obtenerValoresPorTipo("modalidad");

        // Then
        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertTrue(resultado.contains("Teletrabajo"));
        assertTrue(resultado.contains("Presencial"));
        assertTrue(resultado.contains("Híbrido"));
        verify(catalogoRepository).findByTipoAndActivoTrueOrderByOrdenAsc("modalidad");
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay valores para el tipo")
    void obtenerValoresPorTipo_sinValores_deberiaDevolverListaVacia() {
        // Given
        when(catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("sector"))
                .thenReturn(Collections.emptyList());

        // When
        List<String> resultado = catalogoService.obtenerValoresPorTipo("sector");

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando el repositorio devuelve null")
    void obtenerValoresPorTipo_conNull_deberiaDevolverListaVacia() {
        // Given
        when(catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("estado"))
                .thenReturn(null);

        // When
        List<String> resultado = catalogoService.obtenerValoresPorTipo("estado");

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    private Catalogo crearCatalogo(Long id, String tipo, String valor, Integer orden) {
        Catalogo catalogo = new Catalogo();
        catalogo.setId(id);
        catalogo.setTipo(tipo);
        catalogo.setValor(valor);
        catalogo.setOrden(orden);
        catalogo.setActivo(true);
        return catalogo;
    }
}