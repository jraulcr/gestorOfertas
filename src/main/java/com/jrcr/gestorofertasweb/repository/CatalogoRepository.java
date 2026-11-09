package com.jrcr.gestorofertasweb.repository;

import com.jrcr.gestorofertasweb.model.Catalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatalogoRepository extends JpaRepository<Catalogo, Long> {
    // Busca por tipo, que estén activos, y los ordena por el campo 'orden'
    List<Catalogo> findByTipoAndActivoTrueOrderByOrdenAsc(String tipo);
}