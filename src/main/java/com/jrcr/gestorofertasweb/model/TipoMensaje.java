package com.jrcr.gestorofertasweb.model;

/**
 * Enum que representa los tipos de notificación del sistema.
 * Se utiliza para determinar el icono, color y título del modal de notificación en el frontend.
 *
 * Uso en controladores:
 *   redirectAttributes.addFlashAttribute("tipoMensaje", TipoMensaje.SUCCESS);
 *
 * Uso en JavaScript (notificaciones.js):
 *   Se mapea cada valor a su configuración visual (icono Bootstrap, color, título).
 */
public enum TipoMensaje {

    /** Operación completada con éxito (verde) */
    SUCCESS,

    /** Información informativa (azul) */
    INFO,

    /** Advertencia o aviso que requiere atención (amarillo) */
    WARNING,

    /** Error en la operación (rojo) */
    ERROR
}