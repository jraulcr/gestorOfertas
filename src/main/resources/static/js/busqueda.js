/**
 * Autocomplete para búsqueda de empresas en móviles
 */
document.addEventListener('DOMContentLoaded', function() {
    const inputBusqueda = document.querySelector('input[name="texto"]');
    const datalist = document.getElementById('listaEmpresas');

    if (!inputBusqueda || !datalist) return;

    // Obtener lista de empresas del datalist
    const empresas = Array.from(datalist.querySelectorAll('option'))
        .map(opt => opt.value)
        .filter(v => v);

    if (empresas.length === 0) return;

    // Crear contenedor de sugerencias
    const sugerenciasDiv = document.createElement('div');
    sugerenciasDiv.className = 'autocomplete-sugerencias';
    sugerenciasDiv.style.cssText = `
        position: absolute;
        background: white;
        border: 1px solid #ddd;
        border-radius: 8px;
        max-height: 200px;
        overflow-y: auto;
        z-index: 1000;
        width: 100%;
        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        display: none;
    `;

    // Insertar después del input
    inputBusqueda.parentNode.style.position = 'relative';
    inputBusqueda.parentNode.appendChild(sugerenciasDiv);

    // Función para mostrar sugerencias
    function mostrarSugerencias(valor) {
        if (!valor || valor.length < 2) {
            sugerenciasDiv.style.display = 'none';
            return;
        }

        const filtro = valor.toLowerCase();
        const coincidencias = empresas.filter(emp =>
            emp.toLowerCase().includes(filtro)
        );

        if (coincidencias.length === 0) {
            sugerenciasDiv.style.display = 'none';
            return;
        }

        sugerenciasDiv.innerHTML = coincidencias.map(emp => `
            <div class="sugerencia-item" data-valor="${emp}" style="
                padding: 12px 15px;
                cursor: pointer;
                border-bottom: 1px solid #f0f0f0;
                transition: background 0.2s;
            ">${emp}</div>
        `).join('');

        sugerenciasDiv.style.display = 'block';

        // Añadir eventos click a cada sugerencia
        sugerenciasDiv.querySelectorAll('.sugerencia-item').forEach(item => {
            item.addEventListener('click', function() {
                inputBusqueda.value = this.dataset.valor;
                sugerenciasDiv.style.display = 'none';
            });

            item.addEventListener('mouseenter', function() {
                this.style.background = '#f8f9fa';
            });

            item.addEventListener('mouseleave', function() {
                this.style.background = 'white';
            });
        });
    }

    // Escuchar cambios en el input
    inputBusqueda.addEventListener('input', function() {
        mostrarSugerencias(this.value);
    });

    // Cerrar sugerencias al hacer clic fuera
    document.addEventListener('click', function(e) {
        if (!inputBusqueda.contains(e.target) && !sugerenciasDiv.contains(e.target)) {
            sugerenciasDiv.style.display = 'none';
        }
    });

    // Cerrar con tecla Escape
    inputBusqueda.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            sugerenciasDiv.style.display = 'none';
        }
    });
});