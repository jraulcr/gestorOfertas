/**
 * Validaciones de formularios
 */
document.addEventListener('DOMContentLoaded', function() {
    // Toggle campos de entrevista según estado
    const estadoSelect = document.getElementById('estado');
    const fechaEntrevista = document.getElementById('fechaEntrevista');
    const horaEntrevista = document.getElementById('horaEntrevista');

    if (estadoSelect && fechaEntrevista && horaEntrevista) {
        function toggleEntrevistaFields() {
            const esEntrevista = estadoSelect.value === 'Entrevista';
            fechaEntrevista.disabled = !esEntrevista;
            horaEntrevista.disabled = !esEntrevista;
            if (!esEntrevista) {
                fechaEntrevista.value = '';
                horaEntrevista.value = '';
            }
        }
        estadoSelect.addEventListener('change', toggleEntrevistaFields);
        toggleEntrevistaFields();
    }

    // Validación de contraseña en formulario de cambio
    const formPassword = document.getElementById('formPassword');
    if (formPassword) {
        formPassword.addEventListener('submit', function(event) {
            const nueva = document.getElementById('passwordNueva').value;
            const confirmar = document.getElementById('passwordConfirmar').value;

            if (nueva !== confirmar) {
                event.preventDefault();
                alert('Las contraseñas nuevas no coinciden.');
                document.getElementById('passwordConfirmar').focus();
                document.getElementById('passwordConfirmar').style.borderColor = 'red';
            }
        });
    }

    // Toggle mostrar/ocultar contraseña
    window.togglePassword = function(inputId) {
        const input = document.getElementById(inputId);
        const icon = input.nextElementSibling.querySelector('i');

        if (input.type === 'password') {
            input.type = 'text';
            icon.classList.remove('bi-eye');
            icon.classList.add('bi-eye-slash');
        } else {
            input.type = 'password';
            icon.classList.remove('bi-eye-slash');
            icon.classList.add('bi-eye');
        }
    };
});

document.addEventListener('click', function (evento) {
    var boton = evento.target.closest('.btn-eliminar-archivo');
    if (!boton) { return; }

    var id = boton.getAttribute('data-id');
    var nombre = boton.getAttribute('data-nombre');
    var checkbox = document.getElementById('check-' + id);
    var badge = boton.closest('.file-badge');
    if (!checkbox) { return; }

    if (checkbox.checked) {
        // Ya estaba marcado: se desmarca
        checkbox.checked = false;
        if (badge) { badge.style.opacity = '1'; }
        boton.classList.remove('btn-danger');
        boton.classList.add('btn-outline-danger');
        boton.title = 'Eliminar archivo';
    } else {
        if (confirm('¿Estás seguro de que quieres eliminar el archivo "' + nombre + '"?\n\nSe eliminará definitivamente al pulsar "Guardar oferta".')) {
            checkbox.checked = true;
            if (badge) { badge.style.opacity = '0.45'; }
            boton.classList.remove('btn-outline-danger');
            boton.classList.add('btn-danger');
            boton.title = 'Marcado para eliminar: pulsa Guardar para confirmar';
        }
    }
});