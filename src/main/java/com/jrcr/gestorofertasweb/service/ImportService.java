package com.jrcr.gestorofertasweb.service;

import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.model.Usuario;
import com.jrcr.gestorofertasweb.repository.OfertaRepository;
import com.opencsv.CSVReader;

/**
 * Servicio para importar ofertas desde archivos CSV.
 *
 * Formato esperado del CSV (16 columnas): ID, Empresa, Puesto, Sector,
 * Contacto, Teléfono, Enlace/Web, Fecha publicación, Fecha envío CV, Método,
 * Estado, Modalidad, Fecha entrevista, Hora entrevista, Notas, Web propia
 *
 * Los mensajes de usuario están externalizados en messages.properties.
 */
@Service
public class ImportService {

	private static final Log logger = LogFactory.getLog(ImportService.class);

	// Formatos de fecha/hora esperados en el CSV
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	// Índices de columnas del CSV (0-based)
	private static final int COL_ID = 0;
	private static final int COL_EMPRESA = 1;
	private static final int COL_PUESTO = 2;
	private static final int COL_SECTOR = 3;
	private static final int COL_CONTACTO = 4;
	private static final int COL_TELEFONO = 5;
	private static final int COL_ENLACE = 6;
	private static final int COL_FECHA_PUBLICACION = 7;
	private static final int COL_FECHA_ENTREGA = 8;
	private static final int COL_METODO = 9;
	private static final int COL_ESTADO = 10;
	private static final int COL_MODALIDAD = 11;
	private static final int COL_FECHA_ENTREVISTA = 12;
	private static final int COL_HORA_ENTREVISTA = 13;
	private static final int COL_NOTAS = 14;
	private static final int COL_WEB_PROPIA = 15;

	// Número mínimo de columnas esperadas
	private static final int MIN_COLUMNAS = 16;

	private final OfertaRepository ofertaRepository;
	private final MessageSource messageSource;

	public ImportService(OfertaRepository ofertaRepository, MessageSource messageSource) {
		this.ofertaRepository = ofertaRepository;
		this.messageSource = messageSource;
		logger.info("=== IMPORT SERVICE INICIALIZADO ===");
	}

	/**
	 * Método auxiliar para obtener mensajes desde messages.properties.
	 */
	private String msg(String clave, Object... args) {
		return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
	}

	/**
	 * Importa ofertas desde un archivo CSV.
	 *
	 * @param file    archivo CSV subido
	 * @param usuario usuario propietario de las ofertas
	 * @return mensaje de resultado con el número de registros importados,
	 *         actualizados y errores
	 */
	public String importarCSV(MultipartFile file, Usuario usuario) {
		logger.info("=== INICIO IMPORTACIÓN CSV ===");
		logger.info("Archivo recibido: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
		logger.info("Usuario destino: " + usuario.getUsername() + " (ID: " + usuario.getId() + ")");

		int importados = 0;
		int actualizados = 0;
		int errores = 0;

		try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {

			List<String[]> rows = reader.readAll();
			logger.info("Total de filas leídas del CSV: " + rows.size());

			// Validar que el archivo no esté vacío
			if (rows.isEmpty()) {
				logger.warn("El archivo CSV está vacío");
				return msg("import.emptyFile");
			}

			// Procesar filas (saltar la cabecera, índice 0)
			for (int i = 1; i < rows.size(); i++) {
				String[] row = rows.get(i);
				int numFila = i + 1; // +1 porque la fila 1 es la cabecera

				// Validar número de columnas
				if (row.length < MIN_COLUMNAS) {
					errores++;
					continue;
				}

				try {
					Oferta oferta = procesarFila(row, usuario);
					ofertaRepository.save(oferta);

					// Contabilizar según si es nueva o actualización
					String idStr = row[COL_ID].trim();
					if (idStr.isEmpty()) {
						importados++;
					} else {
						actualizados++;
					}

				} catch (Exception e) {
					errores++;
					logger.error("ERROR en fila " + numFila + ": " + e.getMessage(), e);
				}
			}

			return msg("import.completed", importados, actualizados, errores);

		} catch (Exception e) {
			return msg("import.error", e.getMessage());
		}
	}

	/**
	 * Procesa una fila del CSV y crea/actualiza una oferta.
	 */
	private Oferta procesarFila(String[] row, Usuario usuario) {
		String idStr = row[COL_ID].trim();
		Oferta oferta;

		// Buscar oferta existente por ID (si se proporciona)
		if (!idStr.isEmpty()) {
			try {
				Long id = Long.parseLong(idStr);
				logger.debug("  Buscando oferta existente con ID: " + id);
				oferta = ofertaRepository.findById(id).orElse(null);
			} catch (NumberFormatException e) {
				logger.error("  ID no numérico ('" + idStr + "'), se tratará como nueva oferta");
				oferta = null;
			}
		} else {
			oferta = null;
		}

		// Si no existe, crear nueva
		if (Objects.isNull(oferta)) {
			oferta = new Oferta();
			oferta.setUsuario(usuario);
		}

		// Mapear campos
		oferta.setEmpresa(row[COL_EMPRESA].trim());
		oferta.setPuesto(row[COL_PUESTO].trim());
		oferta.setSector(parseString(row[COL_SECTOR]));
		oferta.setContacto(parseString(row[COL_CONTACTO]));
		oferta.setTelefono(parseString(row[COL_TELEFONO]));
		oferta.setEnlaceOferta(parseString(row[COL_ENLACE]));
		oferta.setMetodoEnvio(parseString(row[COL_METODO]));
		oferta.setEstado(parseString(row[COL_ESTADO]));
		oferta.setModalidad(parseString(row[COL_MODALIDAD]));
		oferta.setNotas(parseString(row[COL_NOTAS]));
		oferta.setWebPropia("Sí".equalsIgnoreCase(row[COL_WEB_PROPIA].trim()));

		// Parsear fechas
		oferta.setFechaPublicacion(parseDate(row[COL_FECHA_PUBLICACION]));
		oferta.setFechaEnvio(parseDate(row[COL_FECHA_ENTREGA]));
		oferta.setFechaEntrevista(parseDate(row[COL_FECHA_ENTREVISTA]));
		oferta.setHoraEntrevista(parseTime(row[COL_HORA_ENTREVISTA]));

		return oferta;
	}

	/**
	 * Parsea un string a LocalDate. Devuelve null si está vacío o es inválido.
	 */
	private LocalDate parseDate(String value) {
		if (Objects.isNull(value) || value.trim().isEmpty()) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim(), DATE_FORMAT);
		} catch (Exception e) {
			// Intentar con formato ISO por defecto
			try {
				return LocalDate.parse(value.trim());
			} catch (Exception e2) {
				logger.error("No se pudo parsear fecha " + value);
				return null;
			}
		}
	}

	/**
	 * Parsea un string a LocalTime. Devuelve null si está vacío o es inválido.
	 */
	private LocalTime parseTime(String value) {
		if (Objects.isNull(value) || value.trim().isEmpty()) {
			return null;
		}
		try {
			return LocalTime.parse(value.trim(), TIME_FORMAT);
		} catch (Exception e) {
			// Intentar con formato ISO por defecto
			try {
				return LocalTime.parse(value.trim());
			} catch (Exception e2) {
				logger.error("No se pudo parsear hora " + value);

				return null;
			}
		}
	}

	/**
	 * Parsea un string, devolviendo null si está vacío.
	 */
	private String parseString(String value) {
		if (Objects.isNull(value) || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
	}
}