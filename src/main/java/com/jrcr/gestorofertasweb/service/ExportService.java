package com.jrcr.gestorofertasweb.service;

import com.jrcr.gestorofertasweb.model.Estado;
import com.jrcr.gestorofertasweb.model.Oferta;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Servicio para la exportación de ofertas a diferentes formatos (CSV, Excel,
 * PDF).
 * <p>
 * Utiliza el Enum {@link Estado} para determinar los colores de las filas y
 * constantes para cabeceras, nombres de archivo y formatos.
 * </p>
 * <p>
 * Orden de columnas: ID, Empresa, Puesto, Sector, Contacto, Teléfono,
 * Enlace/Web, Fecha publicación, Fecha envío CV, Método, Estado, Modalidad,
 * Fecha entrevista, Hora entrevista, Notas, Web propia
 * </p>
 */
@Service
public class ExportService {

	private static final Log logger = LogFactory.getLog(ExportService.class);

	// ==========================================
	// CONSTANTES DE FORMATO Y ARCHIVO
	// ==========================================
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final String NOMBRE_ARCHIVO_EXCEL = "ofertas.xlsx";
	private static final String NOMBRE_ARCHIVO_CSV = "ofertas.csv";
	private static final String NOMBRE_ARCHIVO_PDF = "ofertas.pdf";
	private static final String NOMBRE_HOJA_EXCEL = "Ofertas";
	private static final String TITULO_PDF = "Gestor de Ofertas de Empleo";

	// ==========================================
	// CONSTANTES DE CABECERAS (16 columnas)
	// ==========================================
	private static final String[] HEADERS_EXCEL_CSV = { "ID", "Empresa", "Puesto", "Sector", "Contacto", "Teléfono",
			"Enlace/Web", "Fecha publicación", "Fecha envío CV", "Método", "Estado", "Modalidad", "Fecha entrevista",
			"Hora entrevista", "Notas", "Web propia" };
	private static final String[] HEADERS_PDF = { "ID", "Empresa", "Puesto", "Sector", "Contacto", "Teléfono",
			"Enlace/Web", "F. Pub.", "F. Envío", "Método", "Estado", "Modalidad", "F. Entrevista", "H. Entrevista",
			"Notas", "Web" };

	// Índices de columnas
	private static final int COL_FECHA_PUBLICACION = 7;
	private static final int COL_FECHA_ENTREGA = 8;
	private static final int COL_FECHA_ENTREVISTA = 12;
	// índice de la columna Notas
	private static final int COL_NOTAS = 14;

	// ==========================================
	// CONSTANTES DE COLORES (RGB) - Sombreado suave
	// ==========================================
	private static final int[] COLOR_INSCRITO = { 221, 235, 247 }; // #DDEBF7 Azul suave
	private static final int[] COLOR_EN_REVISION = { 255, 242, 204 }; // #FFF2CC Amarillo suave
	private static final int[] COLOR_ENTREVISTA = { 226, 239, 218 }; // #E2EFDA Verde suave
	private static final int[] COLOR_DESCARTADO = { 248, 215, 218 }; // #F8D7DA Rosa suave
	private static final int[] COLOR_CERRADO = { 235, 235, 235, 1 }; // #EBEBEB Gris claro

	// ==========================================
	// EXPORTAR A CSV
	// ==========================================
	/**
	 * Exporta la lista de ofertas a formato CSV con codificación UTF-8.
	 */
	public void exportarCSV(List<Oferta> ofertas, HttpServletResponse response) throws IOException {
		logger.info("=== INICIO EXPORTACIÓN CSV === ofertas: " + (ofertas == null ? 0 : ofertas.size()));
		if (ofertas == null || ofertas.isEmpty()) {
			logger.warn("La lista de ofertas está vacía: se generará un CSV solo con cabeceras");
		}
		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + NOMBRE_ARCHIVO_CSV + "\"");
		try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
				CSVWriter csvWriter = new CSVWriter(writer)) {
			csvWriter.writeNext(HEADERS_EXCEL_CSV);
			for (Oferta oferta : ofertas) {
				csvWriter.writeNext(extraerDatosOferta(oferta));
			}
		}
		logger.info("=== FIN EXPORTACIÓN CSV ===");
	}

	// ==========================================
	// EXPORTAR A EXCEL
	// ==========================================
	/**
	 * Exporta la lista de ofertas a formato Excel (.xlsx) con:
	 * <ul>
	 * <li>Cabecera con estilo destacado</li>
	 * <li>Filas coloreadas según el estado de la oferta</li>
	 * <li>Fechas con formato reconocible por Excel (agrupables por año/mes)</li>
	 * <li>Filtros automáticos en todas las columnas</li>
	 * <li>Columna Notas con ancho fijo y salto de línea (no se expande)</li>
	 * </ul>
	 */
	public void exportarExcel(List<Oferta> ofertas, HttpServletResponse response) throws IOException {
		logger.info("=== INICIO EXPORTACIÓN EXCEL === ofertas: " + (ofertas == null ? 0 : ofertas.size()));
		if (ofertas == null || ofertas.isEmpty()) {
			logger.warn("La lista de ofertas está vacía: se generará un Excel solo con cabeceras");
		}
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + NOMBRE_ARCHIVO_EXCEL + "\"");
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet(NOMBRE_HOJA_EXCEL);
			// Estilos base
			XSSFCellStyle headerStyle = crearEstiloCabecera(workbook);
			short dateFormatCode = workbook.createDataFormat().getFormat("dd/mm/yyyy");
			// Estilos de fila según estado
			XSSFCellStyle styleInscrito = crearEstiloFilaRGB(workbook, COLOR_INSCRITO);
			XSSFCellStyle styleEnRevision = crearEstiloFilaRGB(workbook, COLOR_EN_REVISION);
			XSSFCellStyle styleEntrevista = crearEstiloFilaRGB(workbook, COLOR_ENTREVISTA);
			XSSFCellStyle styleDescartado = crearEstiloFilaRGB(workbook, COLOR_DESCARTADO);
			XSSFCellStyle styleCerrado = crearEstiloFilaRGB(workbook, COLOR_CERRADO);
			// Estilos de fila con formato de fecha
			XSSFCellStyle dateInscrito = crearEstiloFechaRGB(workbook, COLOR_INSCRITO, dateFormatCode);
			XSSFCellStyle dateEnRevision = crearEstiloFechaRGB(workbook, COLOR_EN_REVISION, dateFormatCode);
			XSSFCellStyle dateEntrevista = crearEstiloFechaRGB(workbook, COLOR_ENTREVISTA, dateFormatCode);
			XSSFCellStyle dateDescartado = crearEstiloFechaRGB(workbook, COLOR_DESCARTADO, dateFormatCode);
			XSSFCellStyle dateCerrado = crearEstiloFechaRGB(workbook, COLOR_CERRADO, dateFormatCode);
			// Cabecera
			XSSFRow headerRow = sheet.createRow(0);
			for (int i = 0; i < HEADERS_EXCEL_CSV.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(HEADERS_EXCEL_CSV[i]);
				cell.setCellStyle(headerStyle);
			}
			// Datos
			int rowNum = 1;
			for (Oferta oferta : ofertas) {
				XSSFRow row = sheet.createRow(rowNum++);
				poblarFilaExcel(row, oferta);
				// Estilos según estado
				XSSFCellStyle rowStyle = obtenerEstiloSegunEstado(oferta.getEstado(), styleInscrito, styleEnRevision,
						styleEntrevista, styleDescartado, styleCerrado);
				XSSFCellStyle dateStyle = obtenerEstiloSegunEstado(oferta.getEstado(), dateInscrito, dateEnRevision,
						dateEntrevista, dateDescartado, dateCerrado);
				// Aplicar estilos a cada celda
				for (int i = 0; i < HEADERS_EXCEL_CSV.length; i++) {
					XSSFCell cell = row.getCell(i);
					if ((i == COL_FECHA_PUBLICACION || i == COL_FECHA_ENTREGA || i == COL_FECHA_ENTREVISTA)
							&& cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
						cell.setCellStyle(dateStyle);
					} else if (Objects.nonNull(rowStyle)) {
						cell.setCellStyle(rowStyle);
					}
				}

				// Altura: SOLO se fija en filas con notas largas (tope 3 líneas).
				// El resto de filas no se toca y conservan su altura natural.
				String notas = oferta.getNotas();
				if (Objects.nonNull(notas) && !notas.isBlank()) {
					int lineas = (int) Math.ceil(notas.length() / 50.0); // ~50 caracteres por línea
					if (lineas > 3) {
						row.setHeightInPoints(3 * sheet.getDefaultRowHeightInPoints());
					}
				}
			}
			// Filtros automáticos
			sheet.setAutoFilter(
					new org.apache.poi.ss.util.CellRangeAddress(0, rowNum - 1, 0, HEADERS_EXCEL_CSV.length - 1));
			// CAMBIO: auto-ajuste de ancho en TODAS las columnas EXCEPTO Notas
			for (int i = 0; i < HEADERS_EXCEL_CSV.length; i++) {
				if (i != COL_NOTAS) {
					sheet.autoSizeColumn(i);
				}
			}
			// CAMBIO: columna Notas con ancho fijo (55 caracteres) para que no se expanda
			sheet.setColumnWidth(COL_NOTAS, 55 * 256);
			workbook.write(response.getOutputStream());
		}
		logger.info("=== FIN EXPORTACIÓN EXCEL ===");
	}

	// ==========================================
	// EXPORTAR A PDF
	// ==========================================
	/**
	 * Exporta la lista de ofertas a formato PDF en orientación horizontal (A4
	 * landscape).
	 */
	public void exportarPDF(List<Oferta> ofertas, HttpServletResponse response) throws IOException, DocumentException {
		logger.info("=== INICIO EXPORTACIÓN PDF === ofertas: " + (ofertas == null ? 0 : ofertas.size()));

		// 1. Blindamos la lista para evitar NullPointerException en el bucle for
		List<Oferta> ofertasSeguras = (ofertas == null) ? java.util.Collections.emptyList() : ofertas;

		if (ofertasSeguras.isEmpty()) {
			logger.warn("La lista de ofertas está vacía: se generará un PDF solo con cabeceras");
		}

		Document document = null;
		try {

			if (ofertas == null || ofertas.isEmpty()) {
				logger.warn("La lista de ofertas está vacía: se generará un PDF solo con cabeceras");
			}
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + NOMBRE_ARCHIVO_PDF + "\"");

			document = new Document(PageSize.A4.rotate());
			PdfWriter.getInstance(document, response.getOutputStream());
			document.open();

			Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
			Paragraph title = new Paragraph(TITULO_PDF, titleFont);
			title.setAlignment(Element.ALIGN_CENTER);
			title.setSpacingAfter(20);
			document.add(title);

			PdfPTable table = new PdfPTable(16);
			table.setWidthPercentage(100);
			table.setWidths(new float[] { 0.8f, 2, 2.5f, 1.5f, 1.5f, 1.2f, 2, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 0.8f,
					3, 0.8f });

			Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD);
			Color headerColor = new Color(102, 126, 234);
			for (String header : HEADERS_PDF) {
				PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
				cell.setBackgroundColor(headerColor);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.addCell(cell);
			}

			Font dataFont = new Font(Font.HELVETICA, 7, Font.NORMAL);
			for (Oferta oferta : ofertas) {
				Color rowColor = obtenerColorSegunEstado(oferta.getEstado());
				String[] datos = extraerDatosOferta(oferta);
				for (String dato : datos) {
					table.addCell(createCellWithColor(dato, dataFont, rowColor));
				}
			}
			document.add(table);

		} catch (DocumentException de) {
			logger.error("ERROR: No se ha generado el PDF", de);
		} finally {
			if (document != null && document.isOpen()) {
				document.close();
			}
			logger.info("=== FIN EXPORTACIÓN PDF ===");
		}
	}

	// ==========================================
	// MÉTODOS AUXILIARES PRIVADOS
	// ==========================================
	/**
	 * Extrae los datos de una oferta en el orden definido por
	 * {@link #HEADERS_EXCEL_CSV}.
	 */
	private String[] extraerDatosOferta(Oferta oferta) {
		return new String[] { Objects.nonNull(oferta.getId()) ? oferta.getId().toString() : "", // 0: ID
				Objects.nonNull(oferta.getEmpresa()) ? oferta.getEmpresa() : "", // 1: Empresa
				Objects.nonNull(oferta.getPuesto()) ? oferta.getPuesto() : "", // 2: Puesto
				Objects.nonNull(oferta.getSector()) ? oferta.getSector() : "", // 3: Sector
				Objects.nonNull(oferta.getContacto()) ? oferta.getContacto() : "", // 4: Contacto
				Objects.nonNull(oferta.getTelefono()) ? oferta.getTelefono() : "", // 5: Teléfono
				Objects.nonNull(oferta.getEnlaceOferta()) ? oferta.getEnlaceOferta() : "", // 6: Enlace/Web
				Objects.nonNull(oferta.getFechaPublicacion()) ? oferta.getFechaPublicacion().format(DATE_FORMAT) : "", // 7: F. Publicacion
				Objects.nonNull(oferta.getFechaEnvio()) ? oferta.getFechaEnvio().format(DATE_FORMAT) : "", // 8: F. Entrega
				Objects.nonNull(oferta.getMetodoEnvio()) ? oferta.getMetodoEnvio() : "", // 9: Método
				Objects.nonNull(oferta.getEstado()) ? oferta.getEstado() : "", // 10: Estado
				Objects.nonNull(oferta.getModalidad()) ? oferta.getModalidad() : "", // 11: Modalidad
				Objects.nonNull(oferta.getFechaEntrevista()) ? oferta.getFechaEntrevista().format(DATE_FORMAT) : "", // 12: F. Entrevista
				Objects.nonNull(oferta.getHoraEntrevista()) ? oferta.getHoraEntrevista().toString() : "", // 13: H.Entrevista
				Objects.nonNull(oferta.getNotas()) ? oferta.getNotas() : "", // 14: Notas
				Objects.nonNull(oferta.getWebPropia()) && oferta.getWebPropia() ? "Sí" : "No" // 15: Web propia
		};
	}

	/**
	 * Rellena una fila de Excel con los datos de la oferta.
	 */
	private void poblarFilaExcel(XSSFRow row, Oferta oferta) {
		row.createCell(0).setCellValue(Objects.nonNull(oferta.getId()) ? oferta.getId() : 0);
		row.createCell(1).setCellValue(Objects.nonNull(oferta.getEmpresa()) ? oferta.getEmpresa() : "");
		row.createCell(2).setCellValue(Objects.nonNull(oferta.getPuesto()) ? oferta.getPuesto() : "");
		row.createCell(3).setCellValue(Objects.nonNull(oferta.getSector()) ? oferta.getSector() : "");
		row.createCell(4).setCellValue(Objects.nonNull(oferta.getContacto()) ? oferta.getContacto() : "");
		row.createCell(5).setCellValue(Objects.nonNull(oferta.getTelefono()) ? oferta.getTelefono() : "");
		row.createCell(6).setCellValue(Objects.nonNull(oferta.getEnlaceOferta()) ? oferta.getEnlaceOferta() : "");
		XSSFCell cellFechaPub = row.createCell(COL_FECHA_PUBLICACION);
		if (Objects.nonNull(oferta.getFechaPublicacion())) {
			cellFechaPub.setCellValue(oferta.getFechaPublicacion());
		}
		XSSFCell cellFechaEnvio = row.createCell(COL_FECHA_ENTREGA);
		if (Objects.nonNull(oferta.getFechaEnvio())) {
			cellFechaEnvio.setCellValue(oferta.getFechaEnvio());
		}
		row.createCell(9).setCellValue(Objects.nonNull(oferta.getMetodoEnvio()) ? oferta.getMetodoEnvio() : "");
		row.createCell(10).setCellValue(Objects.nonNull(oferta.getEstado()) ? oferta.getEstado() : "");
		row.createCell(11).setCellValue(Objects.nonNull(oferta.getModalidad()) ? oferta.getModalidad() : "");
		XSSFCell cellFechaEntrevista = row.createCell(COL_FECHA_ENTREVISTA);
		if (Objects.nonNull(oferta.getFechaEntrevista())) {
			cellFechaEntrevista.setCellValue(oferta.getFechaEntrevista());
		}
		row.createCell(13)
				.setCellValue(Objects.nonNull(oferta.getHoraEntrevista()) ? oferta.getHoraEntrevista().toString() : "");
		row.createCell(14).setCellValue(Objects.nonNull(oferta.getNotas()) ? oferta.getNotas() : "");
		row.createCell(15).setCellValue(Objects.nonNull(oferta.getWebPropia()) && oferta.getWebPropia() ? "Sí" : "No");
	}

	/**
	 * Obtiene el color de fondo según el estado, usando el Enum {@link Estado}.
	 */
	private Color obtenerColorSegunEstado(String estado) {
		Estado estadoEnum = Estado.fromValor(estado);
		if (Objects.isNull(estadoEnum))
			return null;
		switch (estadoEnum) {
		case INSCRITO:
			return new Color(COLOR_INSCRITO[0], COLOR_INSCRITO[1], COLOR_INSCRITO[2]);
		case EN_REVISION:
			return new Color(COLOR_EN_REVISION[0], COLOR_EN_REVISION[1], COLOR_EN_REVISION[2]);
		case ENTREVISTA:
			return new Color(COLOR_ENTREVISTA[0], COLOR_ENTREVISTA[1], COLOR_ENTREVISTA[2]);
		case DESCARTADO:
			return new Color(COLOR_DESCARTADO[0], COLOR_DESCARTADO[1], COLOR_DESCARTADO[2]);
		case CERRADO:
			return new Color(COLOR_CERRADO[0], COLOR_CERRADO[1], COLOR_CERRADO[2]);
		default:
			return null;
		}
	}

	private XSSFCellStyle crearEstiloCabecera(XSSFWorkbook workbook) {
		XSSFCellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(font);
		style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}

	private XSSFCellStyle crearEstiloFilaRGB(XSSFWorkbook workbook, int[] rgb) {
		XSSFCellStyle style = workbook.createCellStyle();
		XSSFColor color = new XSSFColor(new Color(rgb[0], rgb[1], rgb[2]), null);
		style.setFillForegroundColor(color);
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		// CAMBIO: salto de línea dentro de la celda (clave para que Notas no se
		// expanda)
		style.setWrapText(true);
		return style;
	}

	private XSSFCellStyle crearEstiloFechaRGB(XSSFWorkbook workbook, int[] rgb, short dateFormatCode) {
		XSSFCellStyle style = crearEstiloFilaRGB(workbook, rgb);
		style.setDataFormat(dateFormatCode);
		return style;
	}

	/**
	 * Obtiene el estilo de celda según el estado, usando el Enum {@link Estado}.
	 */
	private XSSFCellStyle obtenerEstiloSegunEstado(String estado, XSSFCellStyle inscrito, XSSFCellStyle enRevision,
			XSSFCellStyle entrevista, XSSFCellStyle descartado, XSSFCellStyle cerrado) {
		Estado estadoEnum = Estado.fromValor(estado);
		if (Objects.isNull(estadoEnum))
			return null;
		switch (estadoEnum) {
		case INSCRITO:
			return inscrito;
		case EN_REVISION:
			return enRevision;
		case ENTREVISTA:
			return entrevista;
		case DESCARTADO:
			return descartado;
		case CERRADO:
			return cerrado;
		default:
			return null;
		}
	}

	private PdfPCell createCellWithColor(String text, Font font, Color bgColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		if (Objects.nonNull(bgColor)) {
			cell.setBackgroundColor(bgColor);
		}
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		return cell;
	}
}