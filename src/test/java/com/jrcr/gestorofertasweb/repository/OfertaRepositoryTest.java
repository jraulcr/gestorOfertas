package com.jrcr.gestorofertasweb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.jrcr.gestorofertasweb.model.Estado;
import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.model.Usuario;

/**
 * Tests de integración para {@link OfertaRepository} usando BBDD en memoria (H2).
 *
 * Verifica que las queries nativas y métodos derivados funcionen correctamente
 * con datos reales en base de datos.
 */
@DataJpaTest
class OfertaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OfertaRepository ofertaRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // Crear usuario de prueba
        usuario = new Usuario();
        usuario.setUsername("testuser");
        usuario.setPassword("encoded123");
        usuario.setRole("USER");
        entityManager.persist(usuario);
        entityManager.flush();
    }

    // ==========================================
    // TESTS DE findByUsuarioIdOrderByFechaPublicacionDesc
    // ==========================================

    @Test
    @DisplayName("Debe devolver ofertas de un usuario ordenadas por fecha descendente")
    void findByUsuarioIdOrderByFechaPublicacionDesc_conOfertas_deberiaDevolverOrdenadas() {
        // Given
        persistirOferta(usuario, "Google", "2026-10-15");
        persistirOferta(usuario, "Amazon", "2026-10-20");
        persistirOferta(usuario, "Inditex", "2026-10-10");
        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.findByUsuarioIdOrderByFechaPublicacionDesc(usuario.getId());

        // Then
        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertEquals("Amazon", resultado.get(0).getEmpresa());   // Más reciente
        assertEquals("Google", resultado.get(1).getEmpresa());
        assertEquals("Inditex", resultado.get(2).getEmpresa());  // Más antigua
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando el usuario no tiene ofertas")
    void findByUsuarioIdOrderByFechaPublicacionDesc_sinOfertas_deberiaDevolverVacia() {
        // Given
        Usuario otroUsuario = new Usuario();
        otroUsuario.setUsername("otrouser");
        otroUsuario.setPassword("encoded");
        otroUsuario.setRole("USER");
        entityManager.persist(otroUsuario);
        entityManager.flush();

        // When
        List<Oferta> resultado = ofertaRepository.findByUsuarioIdOrderByFechaPublicacionDesc(otroUsuario.getId());

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ==========================================
    // TESTS DE buscarConFiltros (query nativa con ILIKE)
    // ==========================================

    @Test
    @DisplayName("Debe buscar ofertas por texto en empresa (case-insensitive)")
    void buscarConFiltros_porTextoEnEmpresa_deberiaEncontrar() {
        // Given
        persistirOferta(usuario, "Google", "2026-10-15");
        persistirOferta(usuario, "Amazon", "2026-10-20");
        persistirOferta(usuario, "Microsoft", "2026-10-10");
        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), "goo", null, null, null);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Google", resultado.get(0).getEmpresa());
    }

    @Test
    @DisplayName("Debe buscar ofertas por texto en puesto (case-insensitive)")
    void buscarConFiltros_porTextoEnPuesto_deberiaEncontrar() {
        // Given
        Oferta oferta1 = crearOferta(usuario, "Google", "2026-10-15");
        oferta1.setPuesto("Desarrollador Java");
        entityManager.persist(oferta1);

        Oferta oferta2 = crearOferta(usuario, "Amazon", "2026-10-20");
        oferta2.setPuesto("Backend Developer");
        entityManager.persist(oferta2);

        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), "java", null, null, null);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Desarrollador Java", resultado.get(0).getPuesto());
    }

    @Test
    @DisplayName("Debe filtrar por sector exacto")
    void buscarConFiltros_porSector_deberiaFiltrarCorrectamente() {
        // Given
        Oferta oferta1 = crearOferta(usuario, "Google", "2026-10-15");
        oferta1.setSector("IT");
        entityManager.persist(oferta1);

        Oferta oferta2 = crearOferta(usuario, "Inditex", "2026-10-20");
        oferta2.setSector("Retail");
        entityManager.persist(oferta2);

        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), null, "IT", null, null);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("IT", resultado.get(0).getSector());
    }

    @ParameterizedTest
    @EnumSource(Estado.class) // Esto ejecutará el test una vez por cada valor del Enum (Inscrito, En revisión, Entrevista, Descartado, Cerrado)
    @DisplayName("Debe filtrar correctamente por CUALQUIER estado del Enum")
    void buscarConFiltros_porEstado_deberiaFiltrarCorrectamente(Estado estadoObjetivo) {
        // Given: Creamos una oferta con el estado que queremos probar
        Oferta ofertaCorrecta = crearOferta(usuario, "Google", "2026-10-15");
        ofertaCorrecta.setEstado(estadoObjetivo.getValor()); // Usamos el valor del Enum, no un string "mágico"
        entityManager.persist(ofertaCorrecta);

        // Given: Creamos una oferta con un estado DIFERENTE para asegurar que el filtro la excluye
        // (Si el objetivo es INSCRITO, usamos ENTREVISTA como control, y viceversa)
        Estado estadoControl = (estadoObjetivo == Estado.INSCRITO) ? Estado.ENTREVISTA : Estado.INSCRITO;

        Oferta ofertaIncorrecta = crearOferta(usuario, "Amazon", "2026-10-20");
        ofertaIncorrecta.setEstado(estadoControl.getValor());
        entityManager.persist(ofertaIncorrecta);

        entityManager.flush();
        entityManager.clear();

        // When: Filtramos usando el valor exacto del Enum
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(
                usuario.getId(),
                null,
                null,
                estadoObjetivo.getValor(), // Aquí pasamos "Inscrito", "Cerrado", etc.
                null
        );

        // Then: Verificamos que solo devuelve 1 resultado y que es el correcto
        assertNotNull(resultado);
        assertEquals(1, resultado.size(), "Debería devolver exactamente 1 oferta");
        assertEquals(estadoObjetivo.getValor(), resultado.get(0).getEstado(), "El estado de la oferta devuelta no coincide");
    }

    @Test
    @DisplayName("Debe filtrar por método de envío exacto")
    void buscarConFiltros_porMetodo_deberiaFiltrarCorrectamente() {
        // Given
        Oferta oferta1 = crearOferta(usuario, "Google", "2026-10-15");
        oferta1.setMetodoEnvio("LinkedIn");
        entityManager.persist(oferta1);

        Oferta oferta2 = crearOferta(usuario, "Amazon", "2026-10-20");
        oferta2.setMetodoEnvio("Web propia");
        entityManager.persist(oferta2);

        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), null, null, null, "LinkedIn");

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("LinkedIn", resultado.get(0).getMetodoEnvio());
    }

    @Test
    @DisplayName("Debe combinar múltiples filtros")
    void buscarConFiltros_conMultiplesFiltros_deberiaCombinarCorrectamente() {
        // Given
        Oferta oferta1 = crearOferta(usuario, "Google", "2026-10-15");
        oferta1.setPuesto("Desarrollador Java");
        oferta1.setEstado("Inscrito");
        oferta1.setMetodoEnvio("LinkedIn");
        entityManager.persist(oferta1);

        Oferta oferta2 = crearOferta(usuario, "Amazon", "2026-10-20");
        oferta2.setPuesto("Backend Developer");
        oferta2.setEstado("Entrevista");
        oferta2.setMetodoEnvio("LinkedIn");
        entityManager.persist(oferta2);

        entityManager.flush();
        entityManager.clear();

        // When - Buscar ofertas de LinkedIn con "java" en el texto
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), "java", null, "Inscrito", "LinkedIn");

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Google", resultado.get(0).getEmpresa());
    }

    @Test
    @DisplayName("Debe devolver todas las ofertas cuando todos los filtros son null")
    void buscarConFiltros_sinFiltros_deberiaDevolverTodas() {
        // Given
        persistirOferta(usuario, "Google", "2026-10-15");
        persistirOferta(usuario, "Amazon", "2026-10-20");
        persistirOferta(usuario, "Microsoft", "2026-10-10");
        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), null, null, null, null);

        // Then
        assertNotNull(resultado);
        assertEquals(3, resultado.size());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay coincidencias")
    void buscarConFiltros_sinCoincidencias_deberiaDevolverVacia() {
        // Given
        persistirOferta(usuario, "Google", "2026-10-15");
        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), "texto_inexistente", null, null, null);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Debe ordenar resultados por fecha de publicación descendente")
    void buscarConFiltros_deberiaOrdenarPorFechaDesc() {
        // Given
        persistirOferta(usuario, "Google", "2026-10-15");
        persistirOferta(usuario, "Amazon", "2026-10-20");
        persistirOferta(usuario, "Microsoft", "2026-10-10");
        entityManager.flush();
        entityManager.clear();

        // When
        List<Oferta> resultado = ofertaRepository.buscarConFiltros(usuario.getId(), null, null, null, null);

        // Then
        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertEquals("Amazon", resultado.get(0).getEmpresa());    // 20 oct
        assertEquals("Google", resultado.get(1).getEmpresa());     // 15 oct
        assertEquals("Microsoft", resultado.get(2).getEmpresa());  // 10 oct
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    /**
     * Crea una oferta con datos por defecto (NO la persiste).
     */
    private Oferta crearOferta(Usuario usuario, String empresa, String fechaStr) {
        Oferta oferta = new Oferta();
        oferta.setUsuario(usuario);
        oferta.setEmpresa(empresa);
        oferta.setPuesto("Desarrollador");
        oferta.setEstado("Inscrito");
        oferta.setModalidad("Teletrabajo");
        oferta.setMetodoEnvio("LinkedIn");
        oferta.setFechaPublicacion(LocalDate.parse(fechaStr));
        oferta.setWebPropia(false);
        return oferta;
    }

    /**
     * Crea Y persiste una oferta en la BBDD.
     */
    private void persistirOferta(Usuario usuario, String empresa, String fechaStr) {
        Oferta oferta = crearOferta(usuario, empresa, fechaStr);
        entityManager.persist(oferta);
    }
}