package com.jrcr.gestorofertasweb.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ofertas")
@Data
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String empresa;

    @Column(nullable = false)
    private String puesto;

    private String sector;
    private String contacto;
    private String telefono;

    private Boolean webPropia = false;

    @Column(length = 1000)
    private String enlaceOferta;

    private LocalDate fechaPublicacion;
    @Column(name = "fecha_envio")
    private LocalDate fechaEnvio;

    private String modalidad;
    private String metodoEnvio;
    private String estado;

    @Column(length = 2000)
    private String notas;

    private LocalDate fechaEntrevista;
    private LocalTime horaEntrevista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}