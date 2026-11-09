package com.jrcr.gestorofertasweb.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.jrcr.gestorofertasweb.repository.ArchivoAdjuntoRepository;

import com.jrcr.gestorofertasweb.model.ArchivoAdjunto;
import com.jrcr.gestorofertasweb.model.Estado;
import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.model.TipoMensaje;
import com.jrcr.gestorofertasweb.model.Usuario;
import com.jrcr.gestorofertasweb.repository.UsuarioRepository;
import com.jrcr.gestorofertasweb.service.CatalogoService;
import com.jrcr.gestorofertasweb.service.ExportService;
import com.jrcr.gestorofertasweb.service.ImportService;
import com.jrcr.gestorofertasweb.service.OfertaService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests de integración para {@link OfertaController} usando MockMvc.
 *
 * <p>Utiliza {@code @MockitoBean} (introducido en Spring Boot 3.4) para reemplazar
 * al deprecated {@code @MockBean}, manteniendo el código moderno y sin warnings.</p>
 */
@WebMvcTest(OfertaController.class)
class OfertaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfertaService ofertaService;

    @MockitoBean
    private ExportService exportService;

    @MockitoBean
    private ImportService importService;

    @MockitoBean
    private CatalogoService catalogoService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private MessageSource messageSource;

	@MockitoBean
	private ArchivoAdjuntoRepository archivoRepository;

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> {
                    String clave = invocation.getArgument(0);
                    Object[] args = invocation.getArgument(1);
                    String mensaje = clave;
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            mensaje = mensaje.replace("{" + i + "}", String.valueOf(args[i]));
                        }
                    }
                    return mensaje;
                });

        when(catalogoService.obtenerValoresPorTipo(anyString()))
                .thenReturn(Arrays.asList("Valor1", "Valor2"));
    }

    // ==========================================
    // TESTS DEL ENDPOINT /home
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /home debe devolver 200 y mostrar la vista")
    void home_conUsuarioAutenticado_deberiaDevolverVista() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(ofertaService.buscarConFiltros(eq(1L), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("ofertas"))
                .andExpect(model().attributeExists("username"));
    }

    @Test
    @DisplayName("GET /home sin autenticación debe devolver 401")
    void home_sinAutenticacion_deberiaDevolver401() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /home debe aplicar filtros correctamente")
    void home_conFiltros_deberiaPasarlosAlServicio() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(ofertaService.buscarConFiltros(eq(1L), eq("Java"), eq("IT"), eq("Inscrito"), eq("LinkedIn")))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/home")
                        .param("texto", "Java")
                        .param("sector", "IT")
                        .param("estado", "Inscrito")
                        .param("metodo", "LinkedIn"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));

        verify(ofertaService).buscarConFiltros(1L, "Java", "IT", "Inscrito", "LinkedIn");
    }

    // ==========================================
    // TESTS DEL ENDPOINT /oferta/nueva
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /oferta/nueva debe mostrar formulario de nueva oferta")
    void mostrarFormularioNueva_deberiaDevolverFormulario() throws Exception {
        // Mock necesario: la plantilla accede a archivosAdjuntos
        when(ofertaService.listarArchivosDeOferta(anyLong()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/oferta/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("formulario-oferta"))
                .andExpect(model().attribute("modoEdicion", false))
                .andExpect(model().attributeExists("oferta"));
    }

    // ==========================================
    // TESTS DEL ENDPOINT /oferta/editar/{id}
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /oferta/editar/{id} con ID existente debe mostrar formulario de edición")
    void mostrarFormularioEditar_conIdExistente_deberiaDevolverFormulario() throws Exception {
        Oferta oferta = crearOfertaEjemplo(1L, "Google", Estado.INSCRITO.getValor());
        when(ofertaService.buscarPorId(1L)).thenReturn(oferta);

        // Mock necesario: la plantilla itera sobre archivosAdjuntos
        when(ofertaService.listarArchivosDeOferta(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/oferta/editar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("formulario-oferta"))
                .andExpect(model().attribute("modoEdicion", true))
                .andExpect(model().attribute("oferta", oferta));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /oferta/editar/{id} con ID inexistente debe redirigir al home")
    void mostrarFormularioEditar_conIdInexistente_deberiaRedirigirAHome() throws Exception {
        when(ofertaService.buscarPorId(99L)).thenReturn(null);

        mockMvc.perform(get("/oferta/editar/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    // ==========================================
    // TESTS DEL ENDPOINT /oferta/eliminar/{id}
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /oferta/eliminar/{id} debe eliminar y redirigir al home")
    void eliminarOferta_conIdValido_deberiaEliminarYRedirigir() throws Exception {
        doNothing().when(ofertaService).eliminar(1L);

        mockMvc.perform(get("/oferta/eliminar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("tipoMensaje", TipoMensaje.WARNING));

        verify(ofertaService).eliminar(1L);
    }

    // ==========================================
    // TESTS DEL ENDPOINT /oferta/ics/{id}
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /oferta/ics/{id} con estado Entrevista debe generar ICS")
    void generarICS_conEntrevista_deberiaDevolverICS() throws Exception {
        Oferta oferta = crearOfertaEjemplo(1L, "Google", Estado.ENTREVISTA.getValor());
        oferta.setFechaEntrevista(LocalDate.of(2026, 10, 20));
        oferta.setHoraEntrevista(LocalTime.of(10, 0));
        when(ofertaService.buscarPorId(1L)).thenReturn(oferta);

        mockMvc.perform(get("/oferta/ics/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/calendar; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".ics")));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /oferta/ics/{id} con ID inexistente debe devolver 404")
    void generarICS_conIdInexistente_deberiaDevolver404() throws Exception {
        when(ofertaService.buscarPorId(99L)).thenReturn(null);

        mockMvc.perform(get("/oferta/ics/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /oferta/ics/{id} sin estado Entrevista debe devolver 400")
    void generarICS_sinEntrevista_deberiaDevolver400() throws Exception {
        Oferta oferta = crearOfertaEjemplo(1L, "Google", Estado.INSCRITO.getValor());
        when(ofertaService.buscarPorId(1L)).thenReturn(oferta);

        mockMvc.perform(get("/oferta/ics/1"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // TESTS DEL ENDPOINT /archivos/{archivoId} (NUEVO - BBDD)
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /archivos/{archivoId} con ID existente debe servir el archivo")
    void servirArchivo_conIdExistente_deberiaServirArchivo() throws Exception {
        // Given
        Long archivoId = 1L;
        ArchivoAdjunto archivo = new ArchivoAdjunto();
        archivo.setId(archivoId);
        archivo.setNombre("cv.pdf");
        archivo.setTipoMime("application/pdf");
        archivo.setContenido("contenido_pdf".getBytes());

        when(ofertaService.obtenerArchivo(archivoId)).thenReturn(archivo);

        // When & Then
        mockMvc.perform(get("/archivos/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("cv.pdf")));

        verify(ofertaService).obtenerArchivo(archivoId);
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /archivos/{archivoId} con ID inexistente debe devolver 404")
    void servirArchivo_conIdInexistente_deberiaDevolver404() throws Exception {
        when(ofertaService.obtenerArchivo(99L))
                .thenThrow(new RuntimeException("Archivo no encontrado"));

        mockMvc.perform(get("/archivos/99"))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // TESTS DE EXPORTACIÓN
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /export/csv debe llamar al servicio de exportación CSV")
    void exportarCSV_deberiaLlamarAlServicio() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(ofertaService.listarPorUsuario(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/export/csv"))
                .andExpect(status().isOk());

        verify(ofertaService).listarPorUsuario(1L);
        verify(exportService).exportarCSV(anyList(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /export/excel debe llamar al servicio de exportación Excel")
    void exportarExcel_deberiaLlamarAlServicio() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(ofertaService.listarPorUsuario(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/export/excel"))
                .andExpect(status().isOk());

        verify(ofertaService).listarPorUsuario(1L);
        verify(exportService).exportarExcel(anyList(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /export/pdf debe llamar al servicio de exportación PDF")
    void exportarPDF_deberiaLlamarAlServicio() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(ofertaService.listarPorUsuario(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/export/pdf"))
                .andExpect(status().isOk());

        verify(ofertaService).listarPorUsuario(1L);
        verify(exportService).exportarPDF(anyList(), any(HttpServletResponse.class));
    }

    // ==========================================
    // TESTS DE IMPORTACIÓN
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /importar debe mostrar formulario de importación")
    void mostrarFormularioImportar_deberiaDevolverFormulario() throws Exception {
        mockMvc.perform(get("/importar"))
                .andExpect(status().isOk())
                .andExpect(view().name("importar"));
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    private Oferta crearOfertaEjemplo(Long id, String empresa, String estado) {
        Oferta oferta = new Oferta();
        oferta.setId(id);
        oferta.setEmpresa(empresa);
        oferta.setPuesto("Desarrollador Java");
        oferta.setEstado(estado);
        oferta.setModalidad("Teletrabajo");
        oferta.setMetodoEnvio("LinkedIn");
        oferta.setFechaPublicacion(LocalDate.of(2026, 10, 15));
        oferta.setWebPropia(false);
        return oferta;
    }
}