/**
 * Sistema de notificaciones modales
 * Muestra mensajes flash del servidor con iconos según tipo
 */
document.addEventListener('DOMContentLoaded', function() {
    const flashData = document.getElementById('flashData');
    if (flashData) {
        const mensaje = flashData.getAttribute('data-mensaje');
        const tipo = flashData.getAttribute('data-tipo') || 'INFO';
        if (mensaje) {
            mostrarNotificacion(mensaje, tipo);
        }
    }
});

function mostrarNotificacion(mensaje, tipo) {
    const modal = new bootstrap.Modal(document.getElementById('notificacionModal'));
    const icono = document.getElementById('modalIcono');
    const titulo = document.getElementById('modalTitulo');
    const msg = document.getElementById('modalMensaje');

    const configuraciones = {
        'SUCCESS': { icono: '<i class="bi bi-check-circle-fill text-success"></i>', titulo: '¡Operación exitosa!', clase: 'mb-3 fw-bold text-success' },
        'INFO':    { icono: '<i class="bi bi-info-circle-fill text-primary"></i>',   titulo: 'Información',        clase: 'mb-3 fw-bold text-primary' },
        'WARNING': { icono: '<i class="bi bi-exclamation-triangle-fill text-warning"></i>', titulo: 'Atención', clase: 'mb-3 fw-bold text-warning' },
        'ERROR':   { icono: '<i class="bi bi-x-circle-fill text-danger"></i>',        titulo: 'Error',              clase: 'mb-3 fw-bold text-danger' }
    };

    const config = configuraciones[tipo] || configuraciones['INFO'];

    icono.innerHTML = config.icono;
    titulo.textContent = config.titulo;
    titulo.className = config.clase;
    msg.textContent = mensaje;

    modal.show();
}