package com.jrcr.gestorofertasweb.repository;

import com.jrcr.gestorofertasweb.model.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByUsuarioIdOrderByFechaPublicacionDesc(Long usuarioId);

    // Método para filtros avanzados
    @Query(value = "SELECT * FROM ofertas o WHERE o.usuario_id = :usuarioId " +
    	       "AND (:texto IS NULL OR o.empresa ILIKE CONCAT('%', :texto, '%') OR o.puesto ILIKE CONCAT('%', :texto, '%')) " +
    	       "AND (:sector IS NULL OR o.sector = :sector) " +
    	       "AND (:estado IS NULL OR o.estado = :estado) " +
    	       "AND (:metodo IS NULL OR o.metodo_envio = :metodo) " +
    	       "ORDER BY o.fecha_publicacion DESC",
    	       nativeQuery = true)
    	List<Oferta> buscarConFiltros(
    	    @Param("usuarioId") Long usuarioId,
    	    @Param("texto") String texto,
    	    @Param("sector") String sector,
    	    @Param("estado") String estado,
    	    @Param("metodo") String metodo
    	);
}