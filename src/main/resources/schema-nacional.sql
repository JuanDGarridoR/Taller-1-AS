-- Schema para PostgreSQL (Banco Nacional)
-- IMPORTANTE: Sintaxis diferente a MySQL

CREATE TABLE IF NOT EXISTS cuenta (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(20) UNIQUE NOT NULL,
    titular VARCHAR(100) NOT NULL,
    saldo DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    activa BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movimiento (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL REFERENCES cuenta(id),
    tipo VARCHAR(20) NOT NULL, -- DEBITO, CREDITO
    monto DECIMAL(15, 2) NOT NULL,
    saldo_anterior DECIMAL(15, 2) NOT NULL,
    saldo_nuevo DECIMAL(15, 2) NOT NULL,
    descripcion VARCHAR(255),
    referencia_transferencia VARCHAR(50),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cuenta_numero ON cuenta(numero_cuenta);
CREATE INDEX idx_movimiento_cuenta ON movimiento(cuenta_id);
CREATE INDEX idx_movimiento_referencia ON movimiento(referencia_transferencia);

-- Tabla del SAGA: registra el estado de cada transferencia en cada paso.
-- Permite al orquestador saber exactamente qué ocurrió y compensar si es necesario.
CREATE TABLE IF NOT EXISTS transferencia (
    id BIGSERIAL PRIMARY KEY,
    id_transaccion VARCHAR(50) UNIQUE NOT NULL,
    cuenta_origen VARCHAR(20) NOT NULL,
    cuenta_destino VARCHAR(20) NOT NULL,
    monto DECIMAL(15, 2) NOT NULL,
    estado VARCHAR(30) NOT NULL,           -- INICIADA | DEBITO_COMPLETADO | COMPLETADA | COMPENSANDO | REVERTIDA | FALLIDA
    mensaje_error VARCHAR(500),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transferencia_id ON transferencia(id_transaccion);
CREATE INDEX idx_transferencia_estado ON transferencia(estado);

-- Cuentas del Banco Nacional
INSERT INTO cuenta (numero_cuenta, titular, saldo) VALUES
('BN-001', 'Juan Pérez', 5000.00),
('BN-002', 'María García', 10000.00),
('BN-003', 'Carlos Rodríguez', 2500.00),
('BN-004', 'Ana Martínez', 15000.00);

Commit;