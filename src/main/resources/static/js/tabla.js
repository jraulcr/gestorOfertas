/**
 * Inicialización de DataTables y tooltips
 */

//Numero de filas a mostrar en la tabla
$(document).ready(function() {
    if ($('#tablaOfertas').length) {
        $('#tablaOfertas').DataTable({
            searching: false,       // ← Desactiva el buscador nativo
            paging: true,           // Mantiene la paginación (10, 25, 50...)
            info: true,             // Mantiene el texto "Mostrando X de Y"
            language: {
                url: '//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json'
            },
			lengthMenu: [ [5, 10, 25, 50, -1], [5, 10, 25, 50, "Todos"] ],
            pageLength: 5,
            order: [[0, 'desc']],
            columnDefs: [
                { orderable: false, targets: [16] }
            ]
        });
    }

    // Activar tooltips de Bootstrap
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl, {
            html: false,
            delay: { show: 300, hide: 100 }
        });
    });
});

// Al hacer click en cualquier fila del cuerpo de la tabla
$(document).ready(function() {
    $('#tablaOfertas tbody').on('click', 'tr', function() {
        // 1. Quitar la clase de selección de TODAS las filas
        $('#tablaOfertas tbody tr').removeClass('fila-seleccionada');

        // 2. Añadir la clase de selección SOLO a la fila que se ha hecho click
        $(this).addClass('fila-seleccionada');
    });
});

