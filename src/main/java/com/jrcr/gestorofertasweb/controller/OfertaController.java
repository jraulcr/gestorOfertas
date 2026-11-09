package com.jrcr.gestorofertasweb.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jrcr.gestorofertasweb.model.ArchivoAdjunto;
import com.jrcr.gestorofertasweb.model.Estado;
import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.model.TipoCatalogo;
import com.jrcr.gestorofertasweb.model.TipoMensaje;
import com.jrcr.gestorofertasweb.model.Usuario;
import com.jrcr.gestorofertasweb.repository.ArchivoAdjuntoRepository;
import com.jrcr.gestorofertasweb.repository.UsuarioRepository;
import com.jrcr.gestorofertasweb.service.CatalogoService;
import com.jrcr.gestorofertasweb.service.ExportService;
import com.jrcr.gestorofertasweb.service.ImportService;
import com.jrcr.gestorofertasweb.service.OfertaService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Controlador para la gestión de ofertas de empleo.
 *
 * <p>
 * Endpoints principales:
 * <ul>
 * <li>{@code GET /home} - Página principal con listado de ofertas</li>
 * <li>{@code GET /oferta/nueva} - Formulario de nueva oferta</li>
 * <li>{@code POST /oferta/guardar} - Guardar oferta (crear o actualizar)</li>
 * <li>{@code GET /oferta/editar/{id}} - Formulario de edición</li>
 * <li>{@code GET /oferta/eliminar/{id}} - Eliminar oferta</li>
 * <li>{@code GET /oferta/ics/{id}} - Generar archivo ICS para entrevista</li>
 * <li>{@code GET /archivos/{archivoId}} - Servir archivo adjunto desde
 * BBDD</li>
 * <li>{@code GET /export/csv|excel|pdf} - Exportar ofertas</li>
 * <li>{@code GET /importar} - Formulario de importación CSV</li>
 * <li>{@code POST /importar} - Procesar importación CSV</li>
 * </ul>
 */
@Controller
public class OfertaController {

	private static final Log logger = LogFactory.getLog(OfertaController.class);
	private static final String FLASH_MENSAJE = "mensaje";
	private static final String FLASH_TIPO = "tipoMensaje";

	private final OfertaService ofertaService;
	private final ExportService exportService;
	private final ImportService importService;
	private final CatalogoService catalogoService;
	private final UsuarioRepository usuarioRepository;
	private final ArchivoAdjuntoRepository archivoRepository;
	private final MessageSource messageSource;

	public String pato;

	public OfertaController(OfertaService ofertaService, ExportService exportService, ImportService importService,
			CatalogoService catalogoService, UsuarioRepository usuarioRepository,
			ArchivoAdjuntoRepository archivoRepository, MessageSource messageSource) {
		this.ofertaService = ofertaService;
		this.exportService = exportService;
		this.importService = importService;
		this.catalogoService = catalogoService;
		this.usuarioRepository = usuarioRepository;
		this.archivoRepository = archivoRepository;
		this.messageSource = messageSource;
	}

	private String msg(String clave, Object... args) {
		return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
	}

	// ==========================================
	// HOME - Listado de ofertas
	// ==========================================

	@GetMapping("/home")
	public String home(Model model, Authentication authentication, @RequestParam(required = false) String texto,
			@RequestParam(required = false) String sector, @RequestParam(required = false) String estado,
			@RequestParam(required = false) String metodo) {

		logger.info("Accediendo al home - usuario: " + authentication.getName());

		String username = authentication.getName();
		Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

		if (Objects.isNull(usuario)) {
			logger.warn("Usuario no encontrado: " + username);
			model.addAttribute("mensaje", msg("user.notFound"));
			model.addAttribute("tipoMensaje", TipoMensaje.ERROR);
			return "redirect:/login";
		}

		List<Oferta> ofertas = ofertaService.buscarConFiltros(usuario.getId(), texto, sector, estado, metodo);

		// 1. CALCULAR CONTADORES
		long total = ofertas.size();
		long entregadas = ofertas.stream().filter(o -> "Inscrito".equals(o.getEstado())).count();
		long enRevision = ofertas.stream().filter(o -> "En revisión".equals(o.getEstado())).count();
		long entrevistas = ofertas.stream().filter(o -> "Entrevista".equals(o.getEstado())).count();
		long descartadas = ofertas.stream().filter(o -> "Descartado".equals(o.getEstado())).count();
		long cerrados = ofertas.stream().filter(o -> "Cerrado".equals(o.getEstado())).count();

		// 2. AÑADIR DATOS AL MODELO
		model.addAttribute("ofertas", ofertas);
		model.addAttribute("username", username);
		model.addAttribute("texto", texto);
		model.addAttribute("sector", sector);
		model.addAttribute("estado", estado);
		model.addAttribute("metodo", metodo);

		model.addAttribute("total", total);
		model.addAttribute("entregadas", entregadas);
		model.addAttribute("enRevision", enRevision);
		model.addAttribute("entrevistas", entrevistas);
		model.addAttribute("descartadas", descartadas);
		model.addAttribute("cerrados", cerrados);

		// 3. CARGAR EMPRESAS PARA AUTOCOMPLETADO
		List<String> empresas = ofertas.stream().map(Oferta::getEmpresa).filter(Objects::nonNull).distinct().sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(Collectors.toList());
		model.addAttribute("empresas", empresas);
		logger.info("Empresas únicas para autocompletado: " + empresas.size());

		// 4. CARGAR CATÁLOGOS PARA LOS DESPLEGABLES
		cargarCatalogos(model);

		return "home";
	}

	// ==========================================
	// NUEVA OFERTA
	// ==========================================

	@GetMapping("/oferta/nueva")
	public String mostrarFormularioNueva(Model model) {
		logger.info("Mostrando formulario de nueva oferta");
		model.addAttribute("oferta", new Oferta());
		model.addAttribute("modoEdicion", false);
		model.addAttribute("archivosAdjuntos", List.of());
		cargarCatalogos(model);
		return "formulario-oferta";
	}

	// ==========================================
	// EDITAR OFERTA
	// ==========================================

	@GetMapping("/oferta/editar/{id}")
	public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
		logger.info("Mostrando formulario de edición para oferta ID: " + id);

		Oferta oferta = ofertaService.buscarPorId(id);

		if (Objects.isNull(oferta)) {
			logger.warn("Oferta no encontrada con ID: " + id);
			return "redirect:/home";
		}

		model.addAttribute("oferta", oferta);
		model.addAttribute("modoEdicion", true);

		// Cargar archivos adjuntos desde BBDD
		List<ArchivoAdjunto> archivos = ofertaService.listarArchivosDeOferta(id);
		model.addAttribute("archivosAdjuntos", archivos);
		logger.info("Archivos adjuntos encontrados: " + archivos.size());

		cargarCatalogos(model);
		return "formulario-oferta";
	}

	// ==========================================
	// GUARDAR OFERTA (Crear o Actualizar)
	// ==========================================

	@PostMapping("/oferta/guardar")
	public String guardarOferta(@RequestParam(required = false) Long id, @RequestParam String empresa,
			@RequestParam String puesto, @RequestParam(required = false) String sector,
			@RequestParam(required = false) String contacto, @RequestParam(required = false) String telefono,
			@RequestParam(required = false) String enlaceOferta, @RequestParam String fechaPublicacion,
			@RequestParam(required = false) String fechaEnvio, @RequestParam String modalidad,
			@RequestParam String metodoEnvio, @RequestParam String estado,
			@RequestParam(defaultValue = "false") boolean webPropia,
			@RequestParam(required = false) String fechaEntrevista,
			@RequestParam(required = false) String horaEntrevista, @RequestParam(required = false) String notas,
			@RequestParam(required = false) MultipartFile[] archivos,
			@RequestParam(required = false) List<Long> archivosAEliminar, Authentication authentication,
			RedirectAttributes redirectAttributes) {

		logger.info("=== INICIO GUARDAR OFERTA ===");
		logger.info("ID: " + id + ", Empresa: " + empresa + ", Puesto: " + puesto);
		logger.info("Archivos nuevos recibidos: " + (archivos != null ? archivos.length : 0));
		logger.info("Archivos a eliminar: " + (archivosAEliminar != null ? archivosAEliminar.size() : 0));

		try {
			String username = authentication.getName();
			Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

			if (Objects.isNull(usuario)) {
				logger.error("Usuario no encontrado: " + username);
				redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.notFound"));
				redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
				return "redirect:/home";
			}

			Oferta oferta;
			if (Objects.nonNull(id) && id > 0) {
				// Actualizar oferta existente
				logger.info("Actualizando oferta existente ID: " + id);
				oferta = ofertaService.buscarPorId(id);
				if (Objects.isNull(oferta)) {
					logger.warn("Oferta a actualizar no encontrada: " + id);
					redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("oferta.notFound"));
					redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
					return "redirect:/home";
				}
			} else {
				// Crear nueva oferta
				logger.info("Creando nueva oferta");
				oferta = new Oferta();
				oferta.setUsuario(usuario);
			}

			// Mapear campos
			oferta.setEmpresa(empresa);
			oferta.setPuesto(puesto);
			oferta.setSector(sector);
			oferta.setContacto(contacto);
			oferta.setTelefono(telefono);
			oferta.setEnlaceOferta(enlaceOferta);
			oferta.setModalidad(modalidad);
			oferta.setMetodoEnvio(metodoEnvio);
			oferta.setEstado(estado);
			oferta.setWebPropia(webPropia);
			oferta.setNotas(notas);

			// Parsear fechas
			if (Objects.nonNull(fechaPublicacion) && !fechaPublicacion.isEmpty()) {
				oferta.setFechaPublicacion(LocalDate.parse(fechaPublicacion));
			}
			if (Objects.nonNull(fechaEnvio) && !fechaEnvio.isEmpty()) {
				oferta.setFechaEnvio(LocalDate.parse(fechaEnvio));
			}
			if (Objects.nonNull(fechaEntrevista) && !fechaEntrevista.isEmpty()) {
				oferta.setFechaEntrevista(LocalDate.parse(fechaEntrevista));
			}
			if (Objects.nonNull(horaEntrevista) && !horaEntrevista.isEmpty()) {
				oferta.setHoraEntrevista(LocalTime.parse(horaEntrevista));
			}

			// 1. Eliminar archivos marcados para borrar
			if (archivosAEliminar != null && !archivosAEliminar.isEmpty()) {
				for (Long archivoId : archivosAEliminar) {
					logger.info("Eliminando archivo ID: " + archivoId);
					archivoRepository.deleteById(archivoId);
				}
			}

			// 2. Guardar oferta con nuevos archivos (en BBDD)
			ofertaService.guardar(oferta, archivos);

			String mensaje = Objects.nonNull(id) && id > 0 ? msg("oferta.updated") : msg("oferta.saved");

			logger.info("Oferta guardada correctamente. Mensaje: " + mensaje);
			redirectAttributes.addFlashAttribute(FLASH_MENSAJE, mensaje);
			redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.SUCCESS);

			logger.info("=== FIN GUARDAR OFERTA ===");
			return "redirect:/home";

		} catch (Exception e) {
			logger.error("Error al guardar la oferta: " + e.getMessage(), e);
			redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("oferta.errorSave", e.getMessage()));
			redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
			return "redirect:/home";
		}
	}

	// ==========================================
	// ELIMINAR OFERTA
	// ==========================================

	@GetMapping("/oferta/eliminar/{id}")
	public String eliminarOferta(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		logger.info("Eliminando oferta ID: " + id);
		try {
			ofertaService.eliminar(id);
			redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("oferta.deleted"));
			redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.WARNING);
			logger.info("Oferta eliminada correctamente");
		} catch (Exception e) {
			logger.error("Error al eliminar la oferta: " + e.getMessage(), e);
			redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("oferta.errorDelete"));
			redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
		}
		return "redirect:/home";
	}

	// ==========================================
	// GENERAR ICS PARA ENTREVISTA
	// ==========================================

	// ==========================================
	// GENERAR ICS PARA ENTREVISTA
	// ==========================================
	@GetMapping("/oferta/ics/{id}")
	public ResponseEntity<String> generarICS(@PathVariable Long id) {
		logger.info("Solicitando generación de ICS para oferta ID: " + id);
		Oferta oferta = ofertaService.buscarPorId(id);

		if (Objects.isNull(oferta)) {
			logger.warn("No se encontró la oferta con ID: " + id);
			return ResponseEntity.notFound().build();
		}

		// Validaciones usando Enums y mensajes externalizados
		if (!Estado.ENTREVISTA.getValor().equals(oferta.getEstado())) {
			logger.warn("Intento de generar ICS para oferta no entrevista. ID: " + id + ", Estado actual: "
					+ oferta.getEstado());
			return ResponseEntity.badRequest().body(msg("oferta.icsNoEntrevista"));
		}

		if (oferta.getFechaEntrevista() == null) {
			logger.warn("Intento de generar ICS para oferta sin fecha de entrevista. ID: " + id);
			return ResponseEntity.badRequest().body(msg("oferta.icsNoFecha"));
		}

		try {
			DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
			DateTimeFormatter icsDateFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
			DateTimeFormatter icsStampFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
			DateTimeFormatter fileNameDateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // ✅ Para el nombre del
																							// archivo

			LocalTime hora = oferta.getHoraEntrevista() != null ? oferta.getHoraEntrevista() : LocalTime.of(10, 0);
			LocalTime horaFin = hora.plusHours(1);

			String dtStart = oferta.getFechaEntrevista().atTime(hora).format(icsDateFmt);
			String dtEnd = oferta.getFechaEntrevista().atTime(horaFin).format(icsDateFmt);
			String dtStamp = java.time.LocalDateTime.now().format(icsStampFmt);
			String uid = "oferta-" + oferta.getId() + "-" + System.currentTimeMillis() + "@gestorofertas.local";

			String fechaEntrevistaStr = oferta.getFechaEntrevista().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
					+ " a las " + hora.format(timeFmt);

			StringBuilder desc = new StringBuilder();
			desc.append("Empresa: ").append(oferta.getEmpresa() != null ? oferta.getEmpresa() : "N/A").append("\\n");
			desc.append("Puesto: ").append(oferta.getPuesto() != null ? oferta.getPuesto() : "N/A").append("\\n");
			desc.append("Sector: ").append(oferta.getSector() != null ? oferta.getSector() : "N/A").append("\\n");
			desc.append("Contacto: ").append(oferta.getContacto() != null ? oferta.getContacto() : "N/A").append("\\n");
			desc.append("ENTREVISTA: ").append(fechaEntrevistaStr).append("\\n");
			desc.append("Estado: ").append(oferta.getEstado() != null ? oferta.getEstado() : "N/A").append("\\n");
			desc.append("Método: ").append(oferta.getMetodoEnvio() != null ? oferta.getMetodoEnvio() : "N/A")
					.append("\\n");
			desc.append("Modalidad: ").append(oferta.getModalidad() != null ? oferta.getModalidad() : "N/A")
					.append("\\n");
			desc.append("Web propia: ").append(oferta.getWebPropia() != null && oferta.getWebPropia() ? "Sí" : "No")
					.append("\\n");

			if (oferta.getEnlaceOferta() != null && !oferta.getEnlaceOferta().isEmpty()) {
				desc.append("Enlace: ").append(oferta.getEnlaceOferta()).append("\\n");
			}
			desc.append("\\nNotas: ").append(oferta.getNotas() != null ? oferta.getNotas() : "");

			StringBuilder ics = new StringBuilder();
			ics.append("BEGIN:VCALENDAR\r\n");
			ics.append("VERSION:2.0\r\n");
			ics.append("PRODID:-//GestorOfertas//ES\r\n");
			ics.append("CALSCALE:GREGORIAN\r\n");
			ics.append("METHOD:PUBLISH\r\n");
			ics.append("BEGIN:VEVENT\r\n");
			ics.append("UID:").append(uid).append("\r\n");
			ics.append("DTSTAMP:").append(dtStamp).append("\r\n");
			ics.append("DTSTART:").append(dtStart).append("\r\n");
			ics.append("DTEND:").append(dtEnd).append("\r\n");
			ics.append("SUMMARY:ENTREVISTA: ").append(oferta.getPuesto()).append(" - ").append(oferta.getEmpresa())
					.append("\r\n");
			ics.append("DESCRIPTION:").append(desc.toString()).append("\r\n");
			ics.append("STATUS:CONFIRMED\r\n");
			ics.append("SEQUENCE:0\r\n");
			ics.append("BEGIN:VALARM\r\n");
			ics.append("ACTION:DISPLAY\r\n");
			ics.append("DESCRIPTION:Recordatorio de entrevista\r\n");
			ics.append("TRIGGER:-PT1H\r\n");
			ics.append("END:VALARM\r\n");
			ics.append("END:VEVENT\r\n");
			ics.append("END:VCALENDAR\r\n");

			String nombreEmpresaSeguro = oferta.getEmpresa() != null
					? oferta.getEmpresa().replaceAll("[^a-zA-Z0-9]", "_")
					: "Empresa";

			// Formateo explícito para garantizar "yyyy-MM-dd" en el nombre del archivo
			String fechaFormateada = oferta.getFechaEntrevista().format(fileNameDateFmt);
			String filename = "entrevista_" + nombreEmpresaSeguro + "_" + fechaFormateada + ".ics";

			logger.info("ICS generado exitosamente para la oferta: " + oferta.getEmpresa() + " (ID: " + id + ")");

			return ResponseEntity.ok().header("Content-Type", "text/calendar; charset=UTF-8")
					.header("Content-Disposition", "attachment; filename=\"" + filename + "\"").body(ics.toString());

		} catch (Exception e) {
			logger.error("Error al generar el archivo ICS para la oferta ID: " + id, e);
			return ResponseEntity.internalServerError().body(msg("oferta.icsError"));
		}
	}

	// ==========================================
	// SERVIR ARCHIVOS ADJUNTOS (desde BBDD)
	// ==========================================

	@GetMapping("/archivos/{archivoId}")
	public ResponseEntity<byte[]> servirArchivo(@PathVariable Long archivoId) {
		logger.info("Solicitando archivo adjunto ID: " + archivoId);

		try {
			ArchivoAdjunto archivo = ofertaService.obtenerArchivo(archivoId);

			logger.info("Archivo encontrado: " + archivo.getNombre() + " (" + archivo.getTamano() + " bytes, tipo: "
					+ archivo.getTipoMime() + ")");

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(archivo.getTipoMime()))
					.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivo.getNombre() + "\"")
					.body(archivo.getContenido());
		} catch (RuntimeException e) {
			// Un archivo no encontrado es un 404 esperado, no un error crítico del servidor
			logger.warn("Archivo no encontrado con ID: " + archivoId);
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			// Solo logueamos como ERROR si es un fallo inesperado del sistema (ej: error de
			// BBDD)
			logger.error("Error inesperado al servir archivo ID " + archivoId + ": " + e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	// ==========================================
	// EXPORTAR OFERTAS
	// ==========================================

	@GetMapping("/export/csv")
	public void exportarCSV(HttpServletResponse response, Authentication authentication) throws IOException {
		logger.info("Exportando ofertas a CSV - usuario: " + authentication.getName());

		String username = authentication.getName();
		Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

		if (Objects.nonNull(usuario)) {
			List<Oferta> ofertas = ofertaService.listarPorUsuario(usuario.getId());
			exportService.exportarCSV(ofertas, response);
			logger.info("Exportación CSV completada: " + ofertas.size() + " ofertas");
		}
	}

	@GetMapping("/export/excel")
	public void exportarExcel(HttpServletResponse response, Authentication authentication) throws IOException {
		logger.info("Exportando ofertas a Excel - usuario: " + authentication.getName());

		String username = authentication.getName();
		Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

		if (Objects.nonNull(usuario)) {
			List<Oferta> ofertas = ofertaService.listarPorUsuario(usuario.getId());
			exportService.exportarExcel(ofertas, response);
			logger.info("Exportación Excel completada: " + ofertas.size() + " ofertas");
		}
	}

	@GetMapping("/export/pdf")
	public void exportarPDF(HttpServletResponse response, Authentication authentication) throws IOException {
		logger.info("Exportando ofertas a PDF - usuario: " + authentication.getName());

		String username = authentication.getName();
		Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

		if (Objects.nonNull(usuario)) {
			List<Oferta> ofertas = ofertaService.listarPorUsuario(usuario.getId());
			exportService.exportarPDF(ofertas, response);
			logger.info("Exportación PDF completada: " + ofertas.size() + " ofertas");
		}
	}

	// ==========================================
	// IMPORTAR OFERTAS
	// ==========================================

	@GetMapping("/importar")
	public String mostrarFormularioImportar() {
		logger.info("Mostrando formulario de importación");
		return "importar";
	}

	@PostMapping("/importar")
	public String procesarImportacion(MultipartFile file, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		logger.info("Iniciando proceso de importación");

		if (file.isEmpty()) {
			logger.warn("Archivo de importación vacío");
			redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("import.fileRequired"));
			redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.WARNING);
			return "redirect:/importar";
		}

		try {
			String username = authentication.getName();
			Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

			if (Objects.isNull(usuario)) {
				logger.error("Usuario no encontrado para importación: " + username);
				redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.notFound"));
				redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
				return "redirect:/importar";
			}

			String resultado = importService.importarCSV(file, usuario);
			logger.info("Importación completada: " + resultado);

			redirectAttributes.addFlashAttribute(FLASH_MENSAJE, resultado);
			redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.SUCCESS);

		} catch (Exception e) {
			logger.error("Error en la importación: " + e.getMessage(), e);
			redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("import.error", e.getMessage()));
			redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
		}

		return "redirect:/home";
	}

	// ==========================================
	// MÉTODOS AUXILIARES
	// ==========================================

	/**
	 * Carga los catálogos en el modelo para los formularios.
	 */
	private void cargarCatalogos(Model model) {
		model.addAttribute("modalidades", catalogoService.obtenerValoresPorTipo(TipoCatalogo.MODALIDAD.getValor()));
		model.addAttribute("metodosEnvio", catalogoService.obtenerValoresPorTipo(TipoCatalogo.METODO_ENVIO.getValor()));
		model.addAttribute("estados", catalogoService.obtenerValoresPorTipo(TipoCatalogo.ESTADO.getValor()));
		model.addAttribute("sectores", catalogoService.obtenerValoresPorTipo(TipoCatalogo.SECTOR.getValor()));
	}
}