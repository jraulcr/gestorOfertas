package com.jrcr.gestorofertasweb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.jrcr.gestorofertasweb.model.ArchivoAdjunto;
import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.repository.ArchivoAdjuntoRepository;
import com.jrcr.gestorofertasweb.repository.OfertaRepository;

/**
 * Tests unitarios para {@link OfertaService}.
 *
 * <p>Cubre la lógica de negocio principal:
 * <ul>
 *   <li>Guardar ofertas con y sin archivos adjuntos (en BBDD como byte[])</li>
 *   <li>Buscar por ID (existente e inexistente)</li>
 *   <li>Buscar con filtros combinados</li>
 *   <li>Eliminar ofertas y sus archivos asociados</li>
 *   <li>Listar ofertas por usuario</li>
 *   <li>Obtener archivos adjuntos desde BBDD</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OfertaServiceTest {

    private OfertaService ofertaService;

    @Mock
    private OfertaRepository ofertaRepository;

    @Mock
    private ArchivoAdjuntoRepository archivoRepository;

    @BeforeEach
    void setUp() {
        ofertaService = new OfertaService(ofertaRepository, archivoRepository);
    }

    // ==========================================
    // TESTS DE GUARDAR OFERTA
    // ==========================================

    @Test
    @DisplayName("Debe guardar una oferta nueva sin archivos")
    void guardar_ofertaNuevaSinArchivos_deberiaPersistirla() {
        // Given
        Oferta oferta = crearOfertaEjemplo(null, "Google", "Desarrollador Java");
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ofertaService.guardar(oferta, null);

        // Then
        verify(ofertaRepository).save(any(Oferta.class));
        verify(archivoRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Debe guardar una oferta nueva con archivos adjuntos en BBDD")
    void guardar_ofertaNuevaConArchivos_deberiaProcesarArchivos() {
        // Given
        Oferta oferta = crearOfertaEjemplo(null, "Amazon", "Backend Developer");
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(inv -> inv.getArgument(0));

        MultipartFile archivo1 = new MockMultipartFile(
                "archivos", "cv.pdf", "application/pdf", "contenido_pdf".getBytes());
        MultipartFile archivo2 = new MockMultipartFile(
                "archivos", "portfolio.png", "image/png", "contenido_png".getBytes());
        MultipartFile[] archivos = {archivo1, archivo2};

        // When
        ofertaService.guardar(oferta, archivos);

        // Then
        verify(ofertaRepository).save(any(Oferta.class));
        // Se debe haber guardado cada archivo en BBDD
        verify(archivoRepository, org.mockito.Mockito.times(2)).save(any(ArchivoAdjunto.class));
    }

    @Test
    @DisplayName("Debe actualizar una oferta existente sin archivos nuevos")
    void guardar_ofertaExistenteSinArchivosNuevos_deberiaSoloActualizarOferta() {
        // Given
        Oferta oferta = crearOfertaEjemplo(1L, "Google", "Desarrollador Senior");
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ofertaService.guardar(oferta, null);

        // Then
        verify(ofertaRepository).save(any(Oferta.class));
        verify(archivoRepository, org.mockito.Mockito.never()).save(any());
    }

    // ==========================================
    // TESTS DE BUSCAR POR ID
    // ==========================================

    @Test
    @DisplayName("Debe buscar oferta por ID existente")
    void buscarPorId_conIdExistente_deberiaDevolverOferta() {
        // Given
        Oferta oferta = crearOfertaEjemplo(1L, "Google", "Desarrollador Java");
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));

        // When
        Oferta resultado = ofertaService.buscarPorId(1L);

        // Then
        assertNotNull(resultado);
        assertEquals("Google", resultado.getEmpresa());
        verify(ofertaRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe devolver null cuando el ID no existe")
    void buscarPorId_conIdInexistente_deberiaDevolverNull() {
        // Given
        when(ofertaRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        Oferta resultado = ofertaService.buscarPorId(99L);

        // Then
        assertNull(resultado);
        verify(ofertaRepository).findById(99L);
    }

    // ==========================================
    // TESTS DE ELIMINAR OFERTA
    // ==========================================

	@Test
	@DisplayName("Debe eliminar una oferta y sus archivos adjuntos")
	void eliminar_deberiaEliminarOfertaYArchivos() {
		// El servicio ya no consulta findById: borra archivos por FK y luego la oferta
		ofertaService.eliminar(1L);

		verify(archivoRepository).deleteByOfertaId(1L);
		verify(ofertaRepository).deleteById(1L);
	}


	@Test
	@DisplayName("Debe intentar eliminar aunque el ID no exista")
	void eliminar_conIdInexistente_deberiaIntentarEliminar() {
		// Sin stubbings: deleteByOfertaId y deleteById no lanzan excepción si el ID no existe
		ofertaService.eliminar(99L);

		verify(archivoRepository).deleteByOfertaId(99L);
		verify(ofertaRepository).deleteById(99L);
	}

    // ==========================================
    // TESTS DE BUSCAR CON FILTROS
    // ==========================================

    @Test
    @DisplayName("Debe buscar ofertas con todos los filtros")
    void buscarConFiltros_conTodosLosFiltros_deberiaFiltrarCorrectamente() {
        // Given
        Long usuarioId = 1L;
        String texto = "Java";
        String sector = "Programación";
        String estado = "Inscrito";
        String metodo = "LinkedIn";

        List<Oferta> ofertasFiltradas = Arrays.asList(
                crearOfertaEjemplo(1L, "Google", "Inscrito")
        );
        when(ofertaRepository.buscarConFiltros(usuarioId, texto, sector, estado, metodo))
                .thenReturn(ofertasFiltradas);

        // When
        List<Oferta> resultado = ofertaService.buscarConFiltros(usuarioId, texto, sector, estado, metodo);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(ofertaRepository).buscarConFiltros(usuarioId, texto, sector, estado, metodo);
    }

    @Test
    @DisplayName("Debe buscar ofertas sin filtros (todos nulos)")
    void buscarConFiltros_sinFiltros_deberiaDevolverTodasLasOfertas() {
        // Given
        Long usuarioId = 1L;
        List<Oferta> todasLasOfertas = Arrays.asList(
                crearOfertaEjemplo(1L, "Google", "Inscrito"),
                crearOfertaEjemplo(2L, "Amazon", "Entrevista")
        );
        when(ofertaRepository.findByUsuarioIdOrderByFechaPublicacionDesc(usuarioId))
                .thenReturn(todasLasOfertas);

        // When
        List<Oferta> resultado = ofertaService.buscarConFiltros(usuarioId, null, null, null, null);

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay ofertas que coincidan con los filtros")
    void buscarConFiltros_sinResultados_deberiaDevolverListaVacia() {
        // Given
        when(ofertaRepository.buscarConFiltros(anyLong(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<Oferta> resultado = ofertaService.buscarConFiltros(1L, "texto", "sector", "estado", "metodo");

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ==========================================
    // TESTS DE LISTAR POR USUARIO
    // ==========================================

    @Test
    @DisplayName("Debe listar ofertas de un usuario")
    void listarPorUsuario_conUsuarioValido_deberiaDevolverOfertas() {
        // Given
        Long usuarioId = 1L;
        List<Oferta> ofertasEsperadas = Arrays.asList(
                crearOfertaEjemplo(1L, "Google", "Inscrito"),
                crearOfertaEjemplo(2L, "Amazon", "Entrevista")
        );
        when(ofertaRepository.findByUsuarioIdOrderByFechaPublicacionDesc(usuarioId))
                .thenReturn(ofertasEsperadas);

        // When
        List<Oferta> resultado = ofertaService.listarPorUsuario(usuarioId);

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(ofertaRepository).findByUsuarioIdOrderByFechaPublicacionDesc(usuarioId);
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando el usuario no tiene ofertas")
    void listarPorUsuario_sinOfertas_deberiaDevolverListaVacia() {
        // Given
        when(ofertaRepository.findByUsuarioIdOrderByFechaPublicacionDesc(99L))
                .thenReturn(Collections.emptyList());

        // When
        List<Oferta> resultado = ofertaService.listarPorUsuario(99L);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ==========================================
    // TESTS DE OBTENER ARCHIVOS ADJUNTOS
    // ==========================================

    @Test
    @DisplayName("Debe obtener archivo adjunto desde BBDD")
    void obtenerArchivo_conIdExistente_deberiaDevolverArchivo() {
        // Given
        Long archivoId = 1L;
        ArchivoAdjunto archivo = new ArchivoAdjunto();
        archivo.setId(archivoId);
        archivo.setNombre("cv.pdf");
        archivo.setTipoMime("application/pdf");
        archivo.setContenido("contenido_pdf".getBytes());

        when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));

        // When
        ArchivoAdjunto resultado = ofertaService.obtenerArchivo(archivoId);

        // Then
        assertNotNull(resultado);
        assertEquals("cv.pdf", resultado.getNombre());
        assertEquals("application/pdf", resultado.getTipoMime());
        verify(archivoRepository).findById(archivoId);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el archivo no existe")
    void obtenerArchivo_conIdInexistente_deberiaLanzarExcepcion() {
        // Given
        when(archivoRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            ofertaService.obtenerArchivo(99L);
        });
    }

    @Test
    @DisplayName("Debe listar archivos de una oferta")
    void listarArchivosDeOferta_conOfertaValida_deberiaDevolverArchivos() {
        // Given
        Long ofertaId = 1L;
        List<ArchivoAdjunto> archivosEsperados = Arrays.asList(
                crearArchivoAdjunto(1L, "cv.pdf", "application/pdf"),
                crearArchivoAdjunto(2L, "portfolio.png", "image/png")
        );
        when(archivoRepository.findByOfertaId(ofertaId)).thenReturn(archivosEsperados);

        // When
        List<ArchivoAdjunto> resultado = ofertaService.listarArchivosDeOferta(ofertaId);

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(archivoRepository).findByOfertaId(ofertaId);
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando la oferta no tiene archivos")
    void listarArchivosDeOferta_sinArchivos_deberiaDevolverListaVacia() {
        // Given
        when(archivoRepository.findByOfertaId(99L)).thenReturn(Collections.emptyList());

        // When
        List<ArchivoAdjunto> resultado = ofertaService.listarArchivosDeOferta(99L);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    private Oferta crearOfertaEjemplo(Long id, String empresa, String puesto) {
        Oferta oferta = new Oferta();
        oferta.setId(id);
        oferta.setEmpresa(empresa);
        oferta.setPuesto(puesto);
        oferta.setEstado("Inscrito");
        oferta.setModalidad("Teletrabajo");
        oferta.setMetodoEnvio("LinkedIn");
        oferta.setFechaPublicacion(LocalDate.of(2026, 10, 15));
        oferta.setWebPropia(false);
        return oferta;
    }

    private ArchivoAdjunto crearArchivoAdjunto(Long id, String nombre, String tipoMime) {
        ArchivoAdjunto archivo = new ArchivoAdjunto();
        archivo.setId(id);
        archivo.setNombre(nombre);
        archivo.setTipoMime(tipoMime);
        archivo.setTamano(1024L);
        archivo.setFechaSubida(LocalDateTime.now());
        return archivo;
    }
}