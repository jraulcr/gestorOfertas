/**
 * Modal para visualizar notas completas
 */
function abrirModalNotas(elemento) {
    const notas = elemento.getAttribute('data-notas');
    document.getElementById('notasContenido').textContent = notas;
    const modal = new bootstrap.Modal(document.getElementById('notasModal'));
    modal.show();
}