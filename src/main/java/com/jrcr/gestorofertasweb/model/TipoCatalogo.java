package com.jrcr.gestorofertasweb.model;

/**
 * Enum que representa los tipos de listas desplegables (catálogos)
 * gestionados en la base de datos.
 */
public enum TipoCatalogo {

    MODALIDAD("modalidad"),
    ESTADO("estado"),
    METODO_ENVIO("metodo_envio"),
    SECTOR("sector");

    private final String valor;

    TipoCatalogo(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}