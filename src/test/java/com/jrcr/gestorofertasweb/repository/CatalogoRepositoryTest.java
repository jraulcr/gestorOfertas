package com.jrcr.gestorofertasweb.repository;

import com.jrcr.gestorofertasweb.model.Catalogo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para {@link CatalogoRepository} usando BBDD en memoria (H2).
 *
 * Verifica que el método findByTipoAndActivoTrueOrderByOrdenAsc funcione correctamente
 * con datos reales en base de datos.
 */
@DataJpaTest
class CatalogoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CatalogoRepository catalogoRepository;

    @BeforeEach
    void setUp() {
        entityManager.clear();
    }

    // ==========================================
    // TESTS DE findByTipoAndActivoTrueOrderByOrdenAsc
    // ==========================================

    @Test
    @DisplayName("Debe devolver catálogos activos ordenados por el campo 'orden'")
    void findByTipoAndActivoTrueOrderByOrdenAsc_conDatos_deberiaDevolverOrdenados() {
        // Given
        persistirCatalogo("modalidad", "Teletrabajo", 1, true);
        persistirCatalogo("modalidad", "Presencial", 2, true);
        persistirCatalogo("modalidad", "Híbrido", 3, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Catalogo> resultado = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("modalidad");

        // Then
        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertEquals("Teletrabajo", resultado.get(0).getValor());
        assertEquals("Presencial", resultado.get(1).getValor());
        assertEquals("Híbrido", resultado.get(2).getValor());
    }

    @Test
    @DisplayName("Debe excluir catálogos inactivos (activo=false)")
    void findByTipoAndActivoTrueOrderByOrdenAsc_conInactivos_deberiaExcluirlos() {
        // Given
        persistirCatalogo("estado", "Inscrito", 1, true);
        persistirCatalogo("estado", "Descartado", 2, false);  // Inactivo
        persistirCatalogo("estado", "Entrevista", 3, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Catalogo> resultado = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("estado");

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Inscrito", resultado.get(0).getValor());
        assertEquals("Entrevista", resultado.get(1).getValor());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay catálogos del tipo solicitado")
    void findByTipoAndActivoTrueOrderByOrdenAsc_conTipoInexistente_deberiaDevolverVacia() {
        // Given
        persistirCatalogo("modalidad", "Teletrabajo", 1, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Catalogo> resultado = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("tipo_inexistente");

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Debe ordenar correctamente con valores de 'orden' no secuenciales")
    void findByTipoAndActivoTrueOrderByOrdenAsc_conOrdenNoSecuencial_deberiaOrdenarCorrectamente() {
        // Given
        persistirCatalogo("sector", "IT", 10, true);
        persistirCatalogo("sector", "Finanzas", 5, true);
        persistirCatalogo("sector", "Marketing", 15, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Catalogo> resultado = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("sector");

        // Then
        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertEquals("Finanzas", resultado.get(0).getValor());    // orden=5
        assertEquals("IT", resultado.get(1).getValor());           // orden=10
        assertEquals("Marketing", resultado.get(2).getValor());    // orden=15
    }

    @Test
    @DisplayName("Debe manejar múltiples tipos de catálogo simultáneamente")
    void findByTipoAndActivoTrueOrderByOrdenAsc_conMultiplesTipos_deberiaFiltrarCorrectamente() {
        // Given
        persistirCatalogo("modalidad", "Teletrabajo", 1, true);
        persistirCatalogo("modalidad", "Presencial", 2, true);
        persistirCatalogo("estado", "Inscrito", 1, true);
        persistirCatalogo("estado", "Entrevista", 2, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Catalogo> modalidades = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("modalidad");
        List<Catalogo> estados = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("estado");

        // Then
        assertEquals(2, modalidades.size());
        assertEquals(2, estados.size());
        assertEquals("Teletrabajo", modalidades.get(0).getValor());
        assertEquals("Inscrito", estados.get(0).getValor());
    }

    @Test
    @DisplayName("Debe ignorar mayúsculas/minúsculas en el tipo de catálogo")
    void findByTipoAndActivoTrueOrderByOrdenAsc_deberiaSerCaseSensitive() {
        // Given
        persistirCatalogo("Modalidad", "Teletrabajo", 1, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Catalogo> resultadoMinusculas = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("modalidad");
        List<Catalogo> resultadoMayusculas = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("Modalidad");

        // Then
        assertTrue(resultadoMinusculas.isEmpty());  // No coincide
        assertEquals(1, resultadoMayusculas.size());  // Sí coincide
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    /**
     * Crea y persiste un catálogo en la BBDD.
     */
    private void persistirCatalogo(String tipo, String valor, Integer orden, Boolean activo) {
        Catalogo catalogo = new Catalogo();
        catalogo.setTipo(tipo);
        catalogo.setValor(valor);
        catalogo.setOrden(orden);
        catalogo.setActivo(activo);
        entityManager.persist(catalogo);
    }
}