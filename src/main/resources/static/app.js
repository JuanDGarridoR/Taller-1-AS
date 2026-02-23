// Variables globales
let cuentasNacionales = [];
let cuentasInternacionales = [];
let historialTransferencias = [];

// Inicializar la aplicación al cargar la página
document.addEventListener('DOMContentLoaded', function() {
    cargarCuentas();
    configurarFormulario();
    configurarSelectorCuentas();
    mostrarCuentas('nacional');
});

// Cargar cuentas desde el backend
async function cargarCuentas() {
    try {
        // Cargar cuentas nacionales
        const respuestaNacional = await fetch('/api/cuentas/nacional');
        cuentasNacionales = await respuestaNacional.json();
        
        // Cargar cuentas internacionales
        const respuestaInternacional = await fetch('/api/cuentas/internacional');
        cuentasInternacionales = await respuestaInternacional.json();
        
        // Llenar los selectores
        llenarSelector('cuentaOrigen', cuentasNacionales);
        llenarSelector('cuentaDestino', cuentasInternacionales);
        
    } catch (error) {
        console.error('Error al cargar cuentas:', error);
        mostrarMensaje('Error al cargar las cuentas. Verifica que el servidor esté corriendo.', 'error');
    }
}

// Llenar un selector con las cuentas
function llenarSelector(idSelector, cuentas) {
    const selector = document.getElementById(idSelector);
    selector.innerHTML = '<option value="">Seleccione una cuenta...</option>';
    
    cuentas.forEach(cuenta => {
        const option = document.createElement('option');
        option.value = cuenta.numeroCuenta;
        option.textContent = `${cuenta.numeroCuenta} - ${cuenta.titular} ($${cuenta.saldo.toFixed(2)})`;
        selector.appendChild(option);
    });
}

// Configurar listeners para los selectores de cuentas
function configurarSelectorCuentas() {
    document.getElementById('cuentaOrigen').addEventListener('change', function() {
        mostrarInfoCuenta(this.value, 'infoCuentaOrigen', cuentasNacionales);
    });
    
    document.getElementById('cuentaDestino').addEventListener('change', function() {
        mostrarInfoCuenta(this.value, 'infoCuentaDestino', cuentasInternacionales);
    });
}

// Mostrar información de la cuenta seleccionada
function mostrarInfoCuenta(numeroCuenta, idElemento, cuentas) {
    const elemento = document.getElementById(idElemento);
    
    if (!numeroCuenta) {
        elemento.innerHTML = '';
        return;
    }
    
    const cuenta = cuentas.find(c => c.numeroCuenta === numeroCuenta);
    if (cuenta) {
        elemento.innerHTML = `
            <strong>Titular:</strong> ${cuenta.titular}<br>
            <strong>Saldo disponible:</strong> $${cuenta.saldo.toFixed(2)}
        `;
    }
}

// Configurar el formulario de transferencia
function configurarFormulario() {
    const form = document.getElementById('formTransferencia');
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const cuentaOrigen = document.getElementById('cuentaOrigen').value;
        const cuentaDestino = document.getElementById('cuentaDestino').value;
        const monto = parseFloat(document.getElementById('monto').value);
        
        // Validaciones del cliente
        if (!cuentaOrigen || !cuentaDestino) {
            mostrarMensaje('Debe seleccionar ambas cuentas', 'error');
            return;
        }
        
        if (cuentaOrigen === cuentaDestino) {
            mostrarMensaje('La cuenta origen y destino no pueden ser la misma', 'error');
            return;
        }
        
        if (monto <= 0) {
            mostrarMensaje('El monto debe ser mayor a cero', 'error');
            return;
        }
        
        // Verificar saldo suficiente
        const cuenta = cuentasNacionales.find(c => c.numeroCuenta === cuentaOrigen);
        if (cuenta && monto > cuenta.saldo) {
            mostrarMensaje('Saldo insuficiente en la cuenta origen', 'error');
            return;
        }
        
        // Realizar la transferencia
        await realizarTransferencia(cuentaOrigen, cuentaDestino, monto);
    });
}

// Realizar la transferencia
async function realizarTransferencia(origen, destino, monto) {
    const btnSubmit = document.querySelector('.btn-primary');
    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Procesando...';
    
    try {
        const response = await fetch('/api/transferencias', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                cuentaOrigen: origen,
                cuentaDestino: destino,
                monto: monto
            })
        });
        
        const resultado = await response.json();
        
        // Mostrar resultado
        mostrarResultadoTransferencia(resultado);
        
        // Agregar al historial
        agregarAlHistorial({
            ...resultado,
            cuentaOrigen: origen,
            cuentaDestino: destino
        });
        
        // Recargar cuentas para actualizar saldos
        await cargarCuentas();
        
        // Limpiar formulario si fue exitoso
        if (resultado.estado === 'EXITOSO') {
            document.getElementById('formTransferencia').reset();
            document.getElementById('infoCuentaOrigen').innerHTML = '';
            document.getElementById('infoCuentaDestino').innerHTML = '';
        }
        
    } catch (error) {
        console.error('Error en la transferencia:', error);
        mostrarMensaje('Error al realizar la transferencia. Intente nuevamente.', 'error');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.textContent = 'Realizar Transferencia';
    }
}

// Mostrar resultado de la transferencia
function mostrarResultadoTransferencia(resultado) {
    const section = document.getElementById('resultadoSection');
    const div = document.getElementById('resultado');
    
    const esExitoso = resultado.estado === 'EXITOSO';
    const clase = esExitoso ? 'success' : 'error';
    
    div.className = `mensaje ${clase}`;
    div.innerHTML = `
        <h3>${esExitoso ? 'Transferencia Exitosa' : 'Transferencia Fallida'}</h3>
        <p><strong>Estado:</strong> ${resultado.estado}</p>
        <p><strong>Mensaje:</strong> ${resultado.mensaje}</p>
        <p><strong>ID de Transacción:</strong> ${resultado.idTransaccion}</p>
        ${resultado.monto ? `<p><strong>Monto:</strong> $${resultado.monto.toFixed(2)}</p>` : ''}
        <p><strong>Fecha:</strong> ${new Date(resultado.fecha).toLocaleString()}</p>
    `;
    
    section.style.display = 'block';
    section.scrollIntoView({ behavior: 'smooth' });
}

// Agregar transferencia al historial
function agregarAlHistorial(transferencia) {
    historialTransferencias.unshift(transferencia);
    
    const historialDiv = document.getElementById('historial');
    historialDiv.innerHTML = '';
    
    if (historialTransferencias.length === 0) {
        historialDiv.innerHTML = '<p class="text-muted">No hay transferencias realizadas</p>';
        return;
    }
    
    historialTransferencias.forEach(t => {
        const item = document.createElement('div');
        item.className = `historial-item ${t.estado === 'EXITOSO' ? 'exitoso' : 'fallido'}`;
        item.innerHTML = `
            <div class="historial-header">
                <span class="badge ${t.estado === 'EXITOSO' ? 'badge-success' : 'badge-error'}">${t.estado}</span>
                <span class="fecha">${new Date(t.fecha).toLocaleString()}</span>
            </div>
            <div class="historial-body">
                <p><strong>Origen:</strong> ${t.cuentaOrigen} → <strong>Destino:</strong> ${t.cuentaDestino}</p>
                <p><strong>Monto:</strong> $${t.monto ? t.monto.toFixed(2) : '0.00'}</p>
                <p><strong>ID:</strong> ${t.idTransaccion}</p>
                <p class="mensaje-pequeño">${t.mensaje}</p>
            </div>
        `;
        historialDiv.appendChild(item);
    });
}

// Consultar saldo de una cuenta
async function consultarSaldo() {
    const numeroCuenta = document.getElementById('buscarCuenta').value.trim();
    const resultadoDiv = document.getElementById('resultadoSaldo');
    
    if (!numeroCuenta) {
        resultadoDiv.innerHTML = '<p class="mensaje error">Ingrese un número de cuenta</p>';
        return;
    }
    
    try {
        const response = await fetch(`/api/cuentas/saldo/${numeroCuenta}`);
        
        if (!response.ok) {
            resultadoDiv.innerHTML = '<p class="mensaje error">Cuenta no encontrada</p>';
            return;
        }
        
        const cuenta = await response.json();
        resultadoDiv.innerHTML = `
            <div class="mensaje success">
                <h4>${cuenta.numeroCuenta}</h4>
                <p><strong>Titular:</strong> ${cuenta.titular}</p>
                <p><strong>Saldo:</strong> $${cuenta.saldo.toFixed(2)}</p>
                <p><strong>Estado:</strong> ${cuenta.activa ? 'Activa' : 'Inactiva'}</p>
            </div>
        `;
    } catch (error) {
        console.error('Error al consultar saldo:', error);
        resultadoDiv.innerHTML = '<p class="mensaje error">Error al consultar el saldo</p>';
    }
}

// Mostrar lista de cuentas
async function mostrarCuentas(tipo) {
    // Actualizar tabs activos
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });
    event.target.classList.add('active');
    
    const listaCuentasDiv = document.getElementById('listaCuentas');
    listaCuentasDiv.innerHTML = '<p>Cargando...</p>';
    
    try {
        const response = await fetch(`/api/cuentas/${tipo}`);
        const cuentas = await response.json();
        
        if (cuentas.length === 0) {
            listaCuentasDiv.innerHTML = '<p class="text-muted">No hay cuentas disponibles</p>';
            return;
        }
        
        let html = '<div class="cuentas-grid">';
        cuentas.forEach(cuenta => {
            html += `
                <div class="cuenta-card">
                    <div class="cuenta-numero">${cuenta.numeroCuenta}</div>
                    <div class="cuenta-titular">${cuenta.titular}</div>
                    <div class="cuenta-saldo">$${cuenta.saldo.toFixed(2)}</div>
                    <div class="cuenta-estado ${cuenta.activa ? 'activa' : 'inactiva'}">
                        ${cuenta.activa ? 'Activa' : 'Inactiva'}
                    </div>
                </div>
            `;
        });
        html += '</div>';
        
        listaCuentasDiv.innerHTML = html;
        
    } catch (error) {
        console.error('Error al cargar cuentas:', error);
        listaCuentasDiv.innerHTML = '<p class="mensaje error">Error al cargar las cuentas</p>';
    }
}

// Función auxiliar para mostrar mensajes
function mostrarMensaje(texto, tipo) {
    const resultadoSection = document.getElementById('resultadoSection');
    const resultadoDiv = document.getElementById('resultado');
    
    resultadoDiv.className = `mensaje ${tipo}`;
    resultadoDiv.innerHTML = `<p>${texto}</p>`;
    resultadoSection.style.display = 'block';
    resultadoSection.scrollIntoView({ behavior: 'smooth' });
}
