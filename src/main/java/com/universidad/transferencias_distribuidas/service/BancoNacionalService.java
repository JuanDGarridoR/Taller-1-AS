package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import com.universidad.transferencias_distribuidas.model.Movimiento;
import com.universidad.transferencias_distribuidas.repository.nacional.CuentaNacionalRepository;
import com.universidad.transferencias_distribuidas.repository.nacional.MovimientoNacionalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class BancoNacionalService {

    @Autowired
    private CuentaNacionalRepository cuentaRepository;

    @Autowired
    private MovimientoNacionalRepository movimientoRepository;

    /**
     * PASO 1 del SAGA: Débita el monto de la cuenta origen.
     *
     * SAGA: Si este método lanza excepción el orquestador marca la transferencia
     * como FALLIDA y no avanza al paso 2. No se necesita compensación.
     */
    @Transactional("nacionalTransactionManager")
    public void debitar(String numeroCuenta, BigDecimal monto, String referencia) {
        log.info("[SAGA][{}] PASO 1 – Debitando {} de cuenta {}", referencia, monto, numeroCuenta);

        // Usamos findByNumeroCuenta (con lock pesimista) para identificar la cuenta
        // por su número de cuenta (BN-001, BN-002...) y no por el PK numérico.
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta nacional no encontrada: " + numeroCuenta));

        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new RuntimeException("Cuenta nacional inactiva: " + numeroCuenta);
        }

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new RuntimeException("Saldo insuficiente en cuenta " + numeroCuenta
                    + ". Saldo: " + cuenta.getSaldo() + ", Requerido: " + monto);
        }

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.subtract(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        // Registrar movimiento – audit trail obligatorio del SAGA
        Movimiento mov = new Movimiento();
        mov.setCuentaId(cuenta.getId());
        mov.setTipo("DEBITO");
        mov.setMonto(monto);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(cuenta.getSaldo());
        mov.setDescripcion("Débito por transferencia SAGA");
        mov.setReferenciaTransferencia(referencia);
        movimientoRepository.save(mov);

        log.info("[SAGA][{}] PASO 1 OK – Saldo anterior: {}, Saldo nuevo: {}", referencia, saldoAnterior, cuenta.getSaldo());
    }

    /**
     * COMPENSACIÓN del PASO 1: Revierte el débito devolviendo el monto a la cuenta.
     *
     * SAGA: Se llama cuando el PASO 2 (crédito internacional) falla.
     * Debe ser idempotente: si se llama dos veces el saldo termina mal,
     * por eso el orquestador debe garantizar que solo se llame una vez.
     */
    @Transactional("nacionalTransactionManager")
    public void compensarDebito(String numeroCuenta, BigDecimal monto, String referencia) {
        log.warn("[SAGA][{}] COMPENSACIÓN – Revirtiendo débito de {} en cuenta {}", referencia, monto, numeroCuenta);

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Error en compensación: Cuenta no encontrada: " + numeroCuenta));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.add(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        // Registrar movimiento de compensación – el audit trail debe reflejar la reversión
        Movimiento mov = new Movimiento();
        mov.setCuentaId(cuenta.getId());
        mov.setTipo("CREDITO");
        mov.setMonto(monto);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(cuenta.getSaldo());
        mov.setDescripcion("COMPENSACIÓN SAGA – Reversión de débito");
        mov.setReferenciaTransferencia(referencia);
        movimientoRepository.save(mov);

        log.warn("[SAGA][{}] COMPENSACIÓN OK – Saldo restaurado: {}", referencia, cuenta.getSaldo());
    }
}
