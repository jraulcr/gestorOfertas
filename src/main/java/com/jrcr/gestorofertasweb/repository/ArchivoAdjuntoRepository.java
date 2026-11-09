package com.jrcr.gestorofertasweb.repository;

import com.jrcr.gestorofertasweb.model.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio para la gestión de archivos adjuntos en BBDD.
 */
public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {
    List<ArchivoAdjunto> findByOfertaId(Long ofertaId);
    void deleteByOfertaId(Long ofertaId);
}