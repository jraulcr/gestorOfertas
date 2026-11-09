/**
 * JavaScript global para Gestor de Ofertas
 */
document.addEventListener('DOMContentLoaded', function() {

    // ==========================================
    // VALIDACIÓN DE CONTRASEÑAS
    // ==========================================
    const formPassword = document.getElementById('formPassword');
    if (formPassword) {
        const pass1 = document.getElementById('passwordNueva');
        const pass2 = document.getElementById('passwordConfirmar');

        formPassword.addEventListener('submit', function(e) {
            if (pass1.value !== pass2.value) {
                e.preventDefault();
                alert('Las contraseñas nuevas no coinciden');
                return false;
            }
            if (pass1.value.length < 6) {
                e.preventDefault();
                alert('La contraseña debe tener al menos 6 caracteres');
                return false;
            }
        });
    }

    // ==========================================
    // TOOLTIPS DE BOOTSTRAP
    // ==========================================
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    if (tooltipTriggerList.length > 0) {
        [...tooltipTriggerList].map(el => new bootstrap.Tooltip(el));
    }

    // ==========================================
    // AUTO-CIERRE DE ALERTAS
    // ==========================================
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000); // 5 segundos
    });
});