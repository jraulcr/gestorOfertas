package com.jrcr.gestorofertasweb.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad para almacenar archivos adjuntos en la base de datos.
 *
 * <p>Cada archivo se guarda como una fila independiente en la tabla
 * {@code archivos_adjuntos}, relacionado con una oferta mediante {@code oferta_id}.
 *
 * <p>El contenido binario se almacena en la columna {@code contenido} de tipo
 * {@code BYTEA} (nativo de PostgreSQL), evitando conversiones a Base64.
 */
@Entity
@Table(name = "archivos_adjuntos")
@Data
public class ArchivoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id", nullable = false)
    private Oferta oferta;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "tipo_mime", nullable = false, length = 150)
    private String tipoMime;

    @Column(name = "tamano", nullable = false)
    private Long tamano;

    /**
     * Contenido binario del archivo.
     * <p>Se mapea a {@code BYTEA} en PostgreSQL mediante {@code columnDefinition}.
     */
    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;
}