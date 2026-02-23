package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import com.universidad.transferencias_distribuidas.model.Movimiento;
import com.universidad.transferencias_distribuidas.repository.internacional.CuentaInternacionalRepository;
import com.universidad.transferencias_distribuidas.repository.internacional.MovimientoInternacionalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class BancoInternacionalService {

    @Autowired
    private CuentaInternacionalRepository cuentaRepository;

    @Autowired
    private MovimientoInternacionalRepository movimientoRepository;

    /**
     * PASO 2 del SAGA: Acredita el monto en la cuenta destino.
     *
     * SAGA: Si este método lanza excepción, el orquestador ejecutará la
     * compensación del PASO 1 (compensarDebito en BancoNacionalService).
     */
    @Transactional("internacionalTransactionManager")
    public void acreditar(String numeroCuenta, BigDecimal monto, String referencia) {
        log.info("[SAGA][{}] PASO 2 – Acreditando {} en cuenta {}", referencia, monto, numeroCuenta);

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta internacional no encontrada: " + numeroCuenta));

        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new RuntimeException("Cuenta internacional inactiva: " + numeroCuenta);
        }

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.add(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        // Registrar movimiento – audit trail del SAGA paso 2
        Movimiento mov = new Movimiento();
        mov.setCuentaId(cuenta.getId());
        mov.setTipo("CREDITO");
        mov.setMonto(monto);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(cuenta.getSaldo());
        mov.setDescripcion("Crédito por transferencia SAGA");
        mov.setReferenciaTransferencia(referencia);
        movimientoRepository.save(mov);

        log.info("[SAGA][{}] PASO 2 OK – Saldo anterior: {}, Saldo nuevo: {}", referencia, saldoAnterior, cuenta.getSaldo());
    }

    /**
     * COMPENSACIÓN del PASO 2: Revierte el crédito (caso avanzado).
     *
     * SAGA: Necesario si en el futuro se agregan pasos posteriores al crédito
     * que puedan fallar. Garantiza que el patrón es extensible.
     */
    @Transactional("internacionalTransactionManager")
    public void compensarCredito(String numeroCuenta, BigDecimal monto, String referencia) {
        log.warn("[SAGA][{}] COMPENSACIÓN PASO 2 – Revirtiendo crédito de {} en cuenta {}", referencia, monto, numeroCuenta);

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Error en compensación internacional: Cuenta no encontrada: " + numeroCuenta));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.subtract(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        Movimiento mov = new Movimiento();
        mov.setCuentaId(cuenta.getId());
        mov.setTipo("DEBITO");
        mov.setMonto(monto);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(cuenta.getSaldo());
        mov.setDescripcion("COMPENSACIÓN SAGA – Reversión de crédito");
        mov.setReferenciaTransferencia(referencia);
        movimientoRepository.save(mov);

        log.warn("[SAGA][{}] COMPENSACIÓN PASO 2 OK – Saldo restaurado: {}", referencia, cuenta.getSaldo());
    }
}
