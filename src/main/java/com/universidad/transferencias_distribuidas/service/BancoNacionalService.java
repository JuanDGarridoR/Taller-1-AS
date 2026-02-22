package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.model.*;
import com.universidad.transferencias_distribuidas.repository.nacional.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BancoNacionalService {
    @Autowired private CuentaNacionalRepository cuentaRepo;
    @Autowired private MovimientoNacionalRepository movRepo;

    @Transactional(transactionManager = "nacionalTransactionManager")
    public String debitar(String numeroCuenta, BigDecimal monto) {
        Cuenta cuenta = cuentaRepo.findByNumeroCuenta(numeroCuenta)
            .orElseThrow(() -> new RuntimeException("Cuenta nacional no encontrada"));

        if (cuenta.getSaldo().compareTo(monto) < 0) throw new RuntimeException("Saldo insuficiente");

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.subtract(monto));
        cuentaRepo.save(cuenta);

        String ref = UUID.randomUUID().toString().substring(0, 8);
        registrarMovimiento(cuenta, "DEBITO", monto, saldoAnterior, cuenta.getSaldo(), "Transferencia enviada", ref);
        return ref;
    }

    @Transactional(transactionManager = "nacionalTransactionManager")
    public void compensarDebito(String numeroCuenta, BigDecimal monto, String ref) {
        Cuenta cuenta = cuentaRepo.findByNumeroCuenta(numeroCuenta).orElseThrow();
        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.add(monto));
        cuentaRepo.save(cuenta);

        registrarMovimiento(cuenta, "CREDITO", monto, saldoAnterior, cuenta.getSaldo(), "Reversión por fallo en destino", ref);
    }

    private void registrarMovimiento(Cuenta c, String tipo, BigDecimal m, BigDecimal ant, BigDecimal nue, String desc, String ref) {
        Movimiento mov = new Movimiento();
        mov.setCuentaId(c.getId());
        mov.setTipo(tipo);
        mov.setMonto(m);
        mov.setSaldoAnterior(ant);
        mov.setSaldoNuevo(nue);
        mov.setDescripcion(desc);
        mov.setReferenciaTransferencia(ref);
        movRepo.save(mov);
    }
}