package com.jrcr.gestorofertasweb.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "catalogo")
@Data
public class Catalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 100)
    private String valor;

    private Integer orden = 0;

    private Boolean activo = true;
}