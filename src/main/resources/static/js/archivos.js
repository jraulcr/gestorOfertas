/**
 * Gestión de archivos adjuntos en formularios
 * - Muestra archivos existentes desde BBDD (List<ArchivoAdjunto>)
 * - Permite añadir nuevos archivos (se acumulan)
 * - Permite marcar archivos existentes para eliminar
 * - Envía todo al servidor correctamente
 */

// Array para mantener los archivos nuevos seleccionados
let archivosNuevos = [];

/**
 * Devuelve el icono HTML según la extensión del archivo
 */
function getIconoArchivo(nombreArchivo) {
    if (!nombreArchivo) return '<i class="bi bi-file-earmark text-secondary"></i>';

    const ext = nombreArchivo.toLowerCase().split('.').pop();

    if (ext === 'pdf') {
        return '<i class="bi bi-file-earmark-pdf text-danger"></i>';
    } else if (ext === 'doc' || ext === 'docx') {
        return '<i class="bi bi-file-earmark-word text-primary"></i>';
    } else if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) {
        return '<i class="bi bi-file-earmark-image text-success"></i>';
    } else if (ext === 'xls' || ext === 'xlsx') {
        return '<i class="bi bi-file-earmark-excel text-success"></i>';
    } else if (ext === 'txt') {
        return '<i class="bi bi-file-earmark-text text-info"></i>';
    } else {
        return '<i class="bi bi-file-earmark text-secondary"></i>';
    }
}

/**
 * Añade visualmente un archivo nuevo seleccionado
 */
function añadirArchivoVisual(nombreArchivo) {
    const container = document.getElementById('archivosContainer');
    const mensajeVacio = document.getElementById('mensajeVacio');

    if (mensajeVacio) mensajeVacio.style.display = 'none';

    const badge = document.createElement('div');
    badge.className = 'file-badge archivo-nuevo';
    badge.setAttribute('data-archivo', nombreArchivo);
    badge.setAttribute('title', nombreArchivo);
    badge.innerHTML = `${getIconoArchivo(nombreArchivo)}<span>${nombreArchivo}</span><span class="remove-file" onclick="eliminarArchivo(this)" title="Eliminar archivo">×</span>`;
    container.appendChild(badge);
}

/**
 * Elimina un archivo nuevo de la lista visual y del array
 */
function eliminarArchivo(elemento) {
    const badge = elemento.parentElement;
    const nombreArchivo = badge.getAttribute('data-archivo');

    const index = archivosNuevos.findIndex(a => a.name === nombreArchivo);
    if (index > -1) {
        archivosNuevos.splice(index, 1);
    }

    badge.remove();

    // Si no quedan badges, mostrar mensaje vacío
    const container = document.getElementById('archivosContainer');
    const badgesRestantes = container.querySelectorAll('.file-badge');
    const archivosExistentesVisibles = container.querySelectorAll('.archivo-existente:not(.d-none)');

    if (badgesRestantes.length === 0 && archivosExistentesVisibles.length === 0) {
        const mensajeVacio = document.getElementById('mensajeVacio');
        if (mensajeVacio) {
            mensajeVacio.style.display = 'block';
        }
    }
}

/**
 * Inicialización al cargar la página
 */
document.addEventListener('DOMContentLoaded', function() {

    // --- Input de nuevos archivos ---
    const inputArchivos = document.getElementById('nuevosArchivos');
    if (inputArchivos) {
        inputArchivos.addEventListener('change', function(event) {
            const files = event.target.files;
            for (let i = 0; i < files.length; i++) {
                // Evitar duplicados
                const yaExiste = archivosNuevos.some(a => a.name === files[i].name && a.size === files[i].size);
                if (!yaExiste) {
                    archivosNuevos.push(files[i]);
                    añadirArchivoVisual(files[i].name);
                }
            }
            // Limpiar el input para poder seleccionar el mismo archivo de nuevo
            inputArchivos.value = '';
        });
    }

    // --- Al enviar el formulario, asignar los archivos acumulados al input ---
    const formOferta = document.getElementById('formOferta');
    if (formOferta) {
        formOferta.addEventListener('submit', function(event) {
            if (archivosNuevos.length > 0) {
                // Usar DataTransfer para asignar múltiples archivos al input
                const dataTransfer = new DataTransfer();
                archivosNuevos.forEach(archivo => {
                    dataTransfer.items.add(archivo);
                });
                inputArchivos.files = dataTransfer.files;
            }
            // El formulario se envía de forma normal (NO fetch) para que funcionen los flash attributes
        });
    }
});

// Asignar iconos a archivos existentes al cargar la página
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.archivo-existente').forEach(function(badge) {
        const nombre = badge.getAttribute('data-nombre');
        if (nombre) {
            const icono = badge.querySelector('i.bi-file-earmark');
            if (icono) {
                const ext = nombre.toLowerCase().split('.').pop();
                icono.className = 'bi'; // Reset

                if (ext === 'pdf') {
                    icono.classList.add('bi-file-earmark-pdf', 'text-danger');
                } else if (ext === 'doc' || ext === 'docx') {
                    icono.classList.add('bi-file-earmark-word', 'text-primary');
                } else if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) {
                    icono.classList.add('bi-file-earmark-image', 'text-success');
                } else if (ext === 'xls' || ext === 'xlsx') {
                    icono.classList.add('bi-file-earmark-excel', 'text-success');
                } else if (ext === 'txt') {
                    icono.classList.add('bi-file-earmark-text', 'text-info');
                } else {
                    icono.classList.add('bi-file-earmark', 'text-secondary');
                }
            }
        }
    });
});