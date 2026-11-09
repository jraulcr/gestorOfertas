package com.jrcr.gestorofertasweb.model;

import java.util.Objects;

/**
 * Enum que representa los posibles estados de una oferta de empleo.
 * Centraliza los valores para evitar 'magic strings' y permite asociar
 * lógica de negocio (como colores o validaciones) a cada estado.
 */
public enum Estado {

    INSCRITO("Inscrito"),
    EN_REVISION("En revisión"),
    ENTREVISTA("Entrevista"),
    DESCARTADO("Descartado"),
    CERRADO("Cerrado");

    private final String valor;

    Estado(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    /**
     * Busca el enum correspondiente a partir del valor guardado en BBDD.
     * @param valor texto del estado (ej: "Inscrito")
     * @return el enum correspondiente, o null si no existe
     */
    public static Estado fromValor(String valor) {
        if (Objects.isNull(valor)) return null;
        for (Estado estado : values()) {
            if (estado.valor.equalsIgnoreCase(valor.trim())) {
                return estado;
            }
        }
        return null;
    }

    /**
     * Método de negocio: ¿Requiere fecha de entrevista este estado?
     */
    public boolean requiereEntrevista() {
        return this == ENTREVISTA;
    }
}