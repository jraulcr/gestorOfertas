package com.jrcr.gestorofertasweb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.model.Usuario;
import com.jrcr.gestorofertasweb.repository.OfertaRepository;

/**
 * Tests unitarios para {@link ImportService}.
 *
 * Verifica la correcta importación de ofertas desde archivos CSV,
 * incluyendo la creación de nuevas ofertas, actualización de existentes,
 * manejo de errores y validación de formatos.
 */
@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    private ImportService importService;

    @Mock
    private OfertaRepository ofertaRepository;

    @Mock
    private MessageSource messageSource;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // Pasar los mocks al constructor (inyección manual)
        importService = new ImportService(ofertaRepository, messageSource);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");
    }

    // ==========================================
    // TESTS DE IMPORTACIÓN CSV
    // ==========================================

    @Test
    @DisplayName("Debe importar ofertas nuevas desde CSV válido")
    void importarCSV_conCSVValido_deberiaCrearOfertas() throws IOException {
        // Given
        String csvContent = "ID,Empresa,Puesto,Sector,Contacto,Teléfono,Enlace/Web,Fecha publicación,Fecha envío CV,Método,Estado,Modalidad,Fecha entrevista,Hora entrevista,Notas,Web propia\n" +
                ",Google,Desarrollador Java,Programación,Carlos López,+34912345678,https://google.com,15/10/2026,,LinkedIn,Inscrito,Teletrabajo,,,Proceso en marcha,No\n" +
                ",Amazon,Backend Developer,Programación,Ana Martín,+34987654321,https://amazon.com,16/10/2026,17/10/2026,Web propia,Entrevista,Híbrido,20/10/2026,10:00,Entrevista técnica,Sí";

        MultipartFile file = new MockMultipartFile("file", "ofertas.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // Mockear el repositorio para simular guardado
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mockear MessageSource para devolver los mensajes
        when(messageSource.getMessage(eq("import.completed"), any(), any()))
                .thenReturn("Importación completada: 2 nuevas, 0 actualizadas, 0 errores.");

        // When
        String resultado = importService.importarCSV(file, usuario);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.contains("2 nuevas"));
        verify(ofertaRepository, times(2)).save(any(Oferta.class));
    }

    @Test
    @DisplayName("Debe actualizar ofertas existentes cuando se proporciona ID")
    void importarCSV_conIDExistente_deberiaActualizarOferta() throws IOException {
        // Given
        String csvContent = "ID,Empresa,Puesto,Sector,Contacto,Teléfono,Enlace/Web,Fecha publicación,Fecha envío CV,Método,Estado,Modalidad,Fecha entrevista,Hora entrevista,Notas,Web propia\n" +
                "1,Google,Desarrollador Senior,Programación,Carlos López,+34912345678,https://google.com,15/10/2026,,LinkedIn,Inscrito,Teletrabajo,,,Actualizado,No";

        MultipartFile file = new MockMultipartFile("file", "ofertas.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        Oferta ofertaExistente = new Oferta();
        ofertaExistente.setId(1L);
        ofertaExistente.setEmpresa("Google");
        ofertaExistente.setPuesto("Desarrollador Java");

        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(ofertaExistente));
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageSource.getMessage(eq("import.completed"), any(), any()))
                .thenReturn("Importación completada: 0 nuevas, 1 actualizadas, 0 errores.");

        // When
        String resultado = importService.importarCSV(file, usuario);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.contains("1 actualizadas"));
        verify(ofertaRepository).findById(1L);
        verify(ofertaRepository).save(any(Oferta.class));
    }

    @Test
    @DisplayName("Debe manejar archivo CSV vacío")
    void importarCSV_conArchivoVacio_deberiaDevolverMensajeError() throws IOException {
        // Given
        String csvContent = "";
        MultipartFile file = new MockMultipartFile("file", "ofertas.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        when(messageSource.getMessage(eq("import.emptyFile"), any(), any()))
                .thenReturn("El archivo está vacío.");

        // When
        String resultado = importService.importarCSV(file, usuario);

        // Then
        assertEquals("El archivo está vacío.", resultado);
        verify(ofertaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe manejar CSV solo con cabecera (sin datos)")
    void importarCSV_soloCabecera_deberiaDevolverCeroImportados() throws IOException {
        // Given
        String csvContent = "ID,Empresa,Puesto,Sector,Contacto,Teléfono,Enlace/Web,Fecha publicación,Fecha envío CV,Método,Estado,Modalidad,Fecha entrevista,Hora entrevista,Notas,Web propia";
        MultipartFile file = new MockMultipartFile("file", "ofertas.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        when(messageSource.getMessage(eq("import.completed"), any(), any()))
                .thenReturn("Importación completada: 0 nuevas, 0 actualizadas, 0 errores.");

        // When
        String resultado = importService.importarCSV(file, usuario);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.contains("0 nuevas"));
        verify(ofertaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe manejar filas con columnas insuficientes")
    void importarCSV_conFilasIncompletas_deberiaContarErrores() throws IOException {
        // Given
        String csvContent = "ID,Empresa,Puesto,Sector,Contacto,Teléfono,Enlace/Web,Fecha publicación,Fecha envío CV,Método,Estado,Modalidad,Fecha entrevista,Hora entrevista,Notas,Web propia\n" +
                ",Google,Desarrollador"; // Solo 3 columnas en lugar de 16

        MultipartFile file = new MockMultipartFile("file", "ofertas.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        when(messageSource.getMessage(eq("import.completed"), any(), any()))
                .thenReturn("Importación completada: 0 nuevas, 0 actualizadas, 1 errores.");

        // When
        String resultado = importService.importarCSV(file, usuario);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.contains("1 errores"));
        verify(ofertaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe parsear fechas correctamente en formato dd/MM/yyyy")
    void importarCSV_conFechasValidas_deberiaParsearCorrectamente() throws IOException {
        // Given
        String csvContent = "ID,Empresa,Puesto,Sector,Contacto,Teléfono,Enlace/Web,Fecha publicación,Fecha envío CV,Método,Estado,Modalidad,Fecha entrevista,Hora entrevista,Notas,Web propia\n" +
                ",Google,Desarrollador,Programación,Carlos,+34912345678,https://google.com,15/10/2026,16/10/2026,LinkedIn,Entrevista,Teletrabajo,20/10/2026,10:00,Nota de prueba,No";

        MultipartFile file = new MockMultipartFile("file", "ofertas.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<Oferta> ofertaCaptor = ArgumentCaptor.forClass(Oferta.class);
        when(ofertaRepository.save(ofertaCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageSource.getMessage(eq("import.completed"), any(), any()))
                .thenReturn("Importación completada: 1 nuevas, 0 actualizadas, 0 errores.");

        // When
        importService.importarCSV(file, usuario);

        // Then
        Oferta ofertaGuardada = ofertaCaptor.getValue();
        assertNotNull(ofertaGuardada);
        assertEquals(LocalDate.of(2026, 10, 15), ofertaGuardada.getFechaPublicacion());
        assertEquals(LocalDate.of(2026, 10, 16), ofertaGuardada.getFechaEnvio());
        assertEquals(LocalDate.of(2026, 10, 20), ofertaGuardada.getFechaEntrevista());
        assertEquals(LocalTime.of(10, 0), ofertaGuardada.getHoraEntrevista());
    }

    @Test
    @DisplayName("Debe manejar valores nulos y vacíos en campos opcionales")
    void importarCSV_conCamposVacios_deberiaManejarCorrectamente() throws IOException {
        // Given
        String csvContent = "ID,Empresa,Puesto,Sector,Contacto,Teléfono,Enlace/Web,Fecha publicación,Fecha envío CV,Método,Estado,Modalidad,Fecha entrevista,Hora entrevista,Notas,Web propia\n" +
                ",Google,Desarrollador,,,,,,15/10/2026,,LinkedIn,Inscrito,,,,,";

        MultipartFile file = new MockMultipartFile("file", "ofertas.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<Oferta> ofertaCaptor = ArgumentCaptor.forClass(Oferta.class);
        when(ofertaRepository.save(ofertaCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageSource.getMessage(eq("import.completed"), any(), any()))
                .thenReturn("Importación completada: 1 nuevas, 0 actualizadas, 0 errores.");

        // When
        importService.importarCSV(file, usuario);

        // Then
        Oferta ofertaGuardada = ofertaCaptor.getValue();
        assertNotNull(ofertaGuardada);
        assertEquals("Google", ofertaGuardada.getEmpresa());
        assertEquals("Desarrollador", ofertaGuardada.getPuesto());
        assertNull(ofertaGuardada.getSector());
        assertNull(ofertaGuardada.getContacto());
        assertNull(ofertaGuardada.getTelefono());
        assertNull(ofertaGuardada.getEnlaceOferta());
        assertNull(ofertaGuardada.getNotas());
        assertFalse(ofertaGuardada.getWebPropia());
    }

    // ==========================================
    // NOTA IMPORTANTE
    // ==========================================
    // Para que estos tests funcionen, ImportService necesita tener
    // los campos inyectados vía constructor o setters.
    //
    // Opción recomendada: Modificar ImportService para usar inyección por constructor:
    //
    // @Service
    // public class ImportService {
    //     private final OfertaRepository ofertaRepository;
    //     private final MessageSource messageSource;
    //
    //     @Autowired
    //     public ImportService(OfertaRepository ofertaRepository, MessageSource messageSource) {
    //         this.ofertaRepository = ofertaRepository;
    //         this.messageSource = messageSource;
    //     }
    //     ...
    // }
    //
    // Y en el test:
    // @BeforeEach
    // void setUp() {
    //     importService = new ImportService(ofertaRepository, messageSource);
    // }
}