package com.jrcr.gestorofertasweb.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.jrcr.gestorofertasweb.model.Catalogo;
import com.jrcr.gestorofertasweb.repository.CatalogoRepository;
import java.util.Collections;

@Service
public class CatalogoService {

    private static final Log logger = LogFactory.getLog(CatalogoService.class);

    private final CatalogoRepository catalogoRepository;

    public CatalogoService(CatalogoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
        logger.info("=== CATALOGO SERVICE INICIALIZADO ===");
    }

    /**
     * Obtiene la lista de valores únicos para un tipo de catálogo dado.
     *
     * @param tipo el tipo de catálogo (ej: "SECTOR", "ESTADO")
     * @return lista de valores (strings) para el desplegable
     */
    public List<String> obtenerValoresPorTipo(String tipo) {
        logger.info("=== CARGAR CATÁLOGO === Tipo solicitado: '" + tipo + "'");

        List<Catalogo> catalogos = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc(tipo);

        if (Objects.isNull(catalogos)) {
            return Collections.emptyList();
        }

        List<String> valores =  catalogos.stream()
                .map(Catalogo::getValor)
                .collect(Collectors.toList());

        logger.info("Catálogo '" + tipo + "' cargado: " + valores.size() + " valores");  // ← AÑADIR


        return valores;
    }
}