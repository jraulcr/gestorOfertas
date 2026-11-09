package com.jrcr.gestorofertasweb.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jrcr.gestorofertasweb.model.ArchivoAdjunto;
import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.repository.ArchivoAdjuntoRepository;
import com.jrcr.gestorofertasweb.repository.OfertaRepository;

@Service
public class OfertaService {

    private static final Log logger = LogFactory.getLog(OfertaService.class);

    private final OfertaRepository ofertaRepository;
    private final ArchivoAdjuntoRepository archivoRepository;

    public OfertaService(OfertaRepository ofertaRepository, ArchivoAdjuntoRepository archivoRepository) {
        this.ofertaRepository = ofertaRepository;
        this.archivoRepository = archivoRepository;
        logger.info("=== OFERTA SERVICE INICIALIZADO ===");
    }

    public List<Oferta> listarPorUsuario(Long usuarioId) {
        logger.info("Listando ofertas para usuario ID: " + usuarioId);
        return ofertaRepository.findByUsuarioIdOrderByFechaPublicacionDesc(usuarioId);
    }

    public Oferta buscarPorId(Long id) {
        logger.info("Buscando oferta por ID: " + id);
        return ofertaRepository.findById(id).orElse(null);
    }

    public Oferta guardar(Oferta oferta, MultipartFile[] archivos) {
        logger.info("=== INICIO MÉTODO GUARDAR ===");
        logger.info("Empresa: " + oferta.getEmpresa() + ", Puesto: " + oferta.getPuesto());
        logger.info("Archivos recibidos: " + (archivos != null ? archivos.length : 0));

        // 1. Guardar la oferta primero para obtener su ID
        Oferta guardada = ofertaRepository.save(oferta);
        logger.info("Oferta guardada con ID: " + guardada.getId());

        // 2. Procesar archivos adjuntos si existen
        if (archivos != null && archivos.length > 0) {
            try {
                logger.info("--- Procesando " + archivos.length + " archivo(s) ---");

                for (int i = 0; i < archivos.length; i++) {
                    MultipartFile archivo = archivos[i];
                    logger.info("--- Procesando archivo " + (i + 1) + " de " + archivos.length + " ---");
                    logger.info("Nombre original: " + archivo.getOriginalFilename());
                    logger.info("Tamaño: " + archivo.getSize() + " bytes");
                    logger.info("Tipo MIME: " + archivo.getContentType());
                    logger.info("¿Está vacío? " + archivo.isEmpty());

                    if (archivo != null && !archivo.isEmpty()) {
                        ArchivoAdjunto archivoAdjunto = new ArchivoAdjunto();
                        archivoAdjunto.setOferta(guardada);
                        archivoAdjunto.setNombre(archivo.getOriginalFilename());
                        archivoAdjunto.setTipoMime(archivo.getContentType() != null
                                ? archivo.getContentType()
                                : "application/octet-stream");
                        archivoAdjunto.setTamano(archivo.getSize());
                        archivoAdjunto.setFechaSubida(LocalDateTime.now());

                        // Guardar contenido binario directamente (BYTEA)
                        archivoAdjunto.setContenido(archivo.getBytes());

                        archivoRepository.save(archivoAdjunto);
                        logger.info("Archivo guardado en BBDD: " + archivo.getOriginalFilename() + " (" + archivo.getSize() + " bytes)");
                    } else {
                        logger.warn("Archivo vacío o nulo, saltando...");
                    }
                }
            } catch (Exception e) {
                logger.error("ERROR AL GUARDAR ARCHIVOS: " + e.getMessage(), e);
                throw new RuntimeException("Error al guardar los archivos adjuntos en BBDD", e);
            }
        } else {
            logger.info("No se recibieron archivos nuevos");
        }

        logger.info("=== FIN MÉTODO GUARDAR ===");
        return guardada;
    }

    public List<Oferta> buscarConFiltros(Long usuarioId, String texto, String sector,
                                          String estado, String metodo) {
        texto = normalizar(texto);
        sector = normalizar(sector);
        estado = normalizar(estado);
        metodo = normalizar(metodo);

        if (Objects.isNull(texto) && Objects.isNull(sector) &&
            Objects.isNull(estado) && Objects.isNull(metodo)) {
            return listarPorUsuario(usuarioId);
        }

        return ofertaRepository.buscarConFiltros(usuarioId, texto, sector, estado, metodo);
    }

    private String normalizar(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }

    public void eliminar(Long id) {
        logger.info("Eliminando oferta ID: " + id);
        archivoRepository.deleteByOfertaId(id);
        ofertaRepository.deleteById(id);
    }

    public ArchivoAdjunto obtenerArchivo(Long archivoId) {
        logger.info("Obteniendo archivo adjunto ID: " + archivoId);
        return archivoRepository.findById(archivoId)
                .orElseThrow(() -> {
                    logger.warn("Archivo no encontrado con ID: " + archivoId);
                    return new RuntimeException("Archivo no encontrado con ID: " + archivoId);
                });
    }

    public List<ArchivoAdjunto> listarArchivosDeOferta(Long ofertaId) {
        logger.info("Listando archivos de oferta ID: " + ofertaId);
        return archivoRepository.findByOfertaId(ofertaId);
    }
}