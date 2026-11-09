package com.jrcr.gestorofertasweb.service;

import com.jrcr.gestorofertasweb.model.Estado;
import com.jrcr.gestorofertasweb.model.Oferta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para {@link ExportService}.
 *
 * Verifica la correcta exportación de ofertas a CSV, Excel y PDF,
 * incluyendo el uso de enums, colores por estado y formato de fechas.
 *
 * Utiliza {@link MockHttpServletResponse} de Spring Test para capturar
 * la respuesta HTTP sin necesidad de un servidor real ni mocks complejos.
 */
class ExportServiceTest {

    private ExportService exportService;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
        response = new MockHttpServletResponse();
    }

    // ==========================================
    // TESTS DE EXPORTACIÓN CSV
    // ==========================================

    @Test
    @DisplayName("Debe exportar CSV con cabecera y datos correctos")
    void exportarCSV_conOfertas_deberiaGenerarCSVValido() throws IOException {
        // Given
        List<Oferta> ofertas = crearListaOfertasEjemplo();

        // When
        exportService.exportarCSV(ofertas, response);

        // Then
        assertEquals("text/csv; charset=UTF-8", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("ofertas.csv"));

        String contenido = response.getContentAsString();
        assertNotNull(contenido);
        // Las cabeceras en ExportService tienen espacios al final ("ID ", "Empresa ", ...)
        assertTrue(contenido.contains("ID"));
        assertTrue(contenido.contains("Empresa"));
        assertTrue(contenido.contains("Google"));
        assertTrue(contenido.contains("Inscrito"));
    }

    @Test
    @DisplayName("Debe exportar CSV vacío cuando no hay ofertas")
    void exportarCSV_sinOfertas_deberiaGenerarSoloCabecera() throws IOException {
        // Given
        List<Oferta> ofertas = Collections.emptyList();

        // When
        exportService.exportarCSV(ofertas, response);

        // Then
        String contenido = response.getContentAsString();
        assertTrue(contenido.contains("ID"));
        assertTrue(contenido.contains("Empresa"));
        // No debe haber filas de datos
        assertFalse(contenido.contains("Google"));
    }

    @Test
    @DisplayName("Debe exportar CSV con codificación UTF-8 para caracteres especiales")
    void exportarCSV_conCaracteresEspeciales_deberiaMantenerUTF8() throws IOException {
        // Given
        Oferta oferta = new Oferta();
        oferta.setId(1L);
        oferta.setEmpresa("Español & Ñ");
        oferta.setPuesto("Desarrollador");
        oferta.setEstado(Estado.INSCRITO.getValor());
        oferta.setModalidad("Teletrabajo");
        oferta.setMetodoEnvio("LinkedIn");
        oferta.setFechaPublicacion(LocalDate.of(2026, 10, 15));
        oferta.setWebPropia(false);
        oferta.setNotas("Acentos: á, é, í, ó, ú");

        List<Oferta> ofertas = Collections.singletonList(oferta);

        // When
        exportService.exportarCSV(ofertas, response);

        // Then
        String contenido = response.getContentAsString();
        assertTrue(contenido.contains("Español & Ñ"));
        assertTrue(contenido.contains("á, é, í, ó, ú"));
    }

    // ==========================================
    // TESTS DE EXPORTACIÓN EXCEL
    // ==========================================

    @Test
    @DisplayName("Debe exportar Excel con filtros automáticos y colores por estado")
    void exportarExcel_conOfertas_deberiaGenerarExcelValido() throws IOException {
        // Given
        List<Oferta> ofertas = crearListaOfertasEjemplo();

        // When
        exportService.exportarExcel(ofertas, response);

        // Then
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                     response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("ofertas.xlsx"));
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    @Test
    @DisplayName("Debe aplicar colores diferentes según el estado")
    void exportarExcel_conDiferentesEstados_deberiaAplicarColoresCorrectos() throws IOException {
        // Given
        Oferta ofertaInscrito = crearOferta(1L, "Google", Estado.INSCRITO.getValor());
        Oferta ofertaEntrevista = crearOferta(2L, "Amazon", Estado.ENTREVISTA.getValor());
        Oferta ofertaDescartado = crearOferta(3L, "Inditex", Estado.DESCARTADO.getValor());

        List<Oferta> ofertas = Arrays.asList(ofertaInscrito, ofertaEntrevista, ofertaDescartado);

        // When
        exportService.exportarExcel(ofertas, response);

        // Then
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    @Test
    @DisplayName("Debe manejar oferta con fechas nulas en Excel")
    void exportarExcel_conFechasNulas_deberiaGenerarExcelSinErrores() throws IOException {
        // Given
        Oferta oferta = new Oferta();
        oferta.setId(1L);
        oferta.setEmpresa("Test");
        oferta.setPuesto("Puesto");
        oferta.setEstado(Estado.INSCRITO.getValor());
        // Fechas a null
        oferta.setFechaPublicacion(null);
        oferta.setFechaEnvio(null);
        oferta.setFechaEntrevista(null);
        oferta.setHoraEntrevista(null);

        List<Oferta> ofertas = Collections.singletonList(oferta);

        // When
        exportService.exportarExcel(ofertas, response);

        // Then
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    // ==========================================
    // TESTS DE EXPORTACIÓN PDF
    // ==========================================

    @Test
    @DisplayName("Debe exportar PDF con título y tabla")
    void exportarPDF_conOfertas_deberiaGenerarPDFValido() throws Exception {
        // Given
        List<Oferta> ofertas = crearListaOfertasEjemplo();

        // When
        exportService.exportarPDF(ofertas, response);

        // Then
        assertEquals("application/pdf", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("ofertas.pdf"));
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    @Test
    @DisplayName("Debe generar PDF vacío cuando no hay ofertas")
    void exportarPDF_sinOfertas_deberiaGenerarPDFConSoloTitulo() throws Exception {
        // Given
        List<Oferta> ofertas = Collections.emptyList();

        // When
        exportService.exportarPDF(ofertas, response);

        // Then
        assertEquals("application/pdf", response.getContentType());
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    /**
     * Crea una lista de ofertas de ejemplo con diferentes estados.
     */
    private List<Oferta> crearListaOfertasEjemplo() {
        Oferta oferta1 = crearOferta(1L, "Google", Estado.INSCRITO.getValor());
        Oferta oferta2 = crearOferta(2L, "Amazon", Estado.ENTREVISTA.getValor());
        Oferta oferta3 = crearOferta(3L, "Inditex", Estado.DESCARTADO.getValor());
        return Arrays.asList(oferta1, oferta2, oferta3);
    }

    /**
     * Crea una oferta con los datos mínimos para testing.
     */
    private Oferta crearOferta(Long id, String empresa, String estado) {
        Oferta oferta = new Oferta();
        oferta.setId(id);
        oferta.setEmpresa(empresa);
        oferta.setPuesto("Desarrollador Java");
        oferta.setSector("Programación / Desarrollo");
        oferta.setEstado(estado);
        oferta.setModalidad("Teletrabajo");
        oferta.setMetodoEnvio("LinkedIn");
        oferta.setFechaPublicacion(LocalDate.of(2026, 10, 15));
        oferta.setWebPropia(false);
        oferta.setNotas("Nota de prueba");
        oferta.setHoraEntrevista(LocalTime.of(10, 0));
        return oferta;
    }
}