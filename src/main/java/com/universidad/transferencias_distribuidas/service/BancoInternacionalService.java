package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import com.universidad.transferencias_distribuidas.model.Movimiento;
import com.universidad.transferencias_distribuidas.repository.internacional.CuentaInternacionalRepository;
import com.universidad.transferencias_distribuidas.repository.internacional.MovimientoInternacionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BancoInternacionalService {

    @Autowired
    private CuentaInternacionalRepository cuentaRepo;

    @Autowired
    private MovimientoInternacionalRepository movRepo;

    /**
     * Paso 2 del SAGA: Acreditar en MySQL
     * Se marca como @Transactional usando el manager de Internacional
     */
    @Transactional(transactionManager = "internacionalTransactionManager")
    public void acreditar(String numeroCuenta, BigDecimal monto, String referencia) {
        // 1. Buscar la cuenta con Lock Pesimista (definido en el Repo)
        Cuenta cuenta = cuentaRepo.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta internacional " + numeroCuenta + " no encontrada"));

        // 2. Actualizar Saldo
        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.add(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepo.save(cuenta);

        // 3. Registrar Movimiento en MySQL
        registrarMovimiento(
            cuenta, 
            "CREDITO", 
            monto, 
            saldoAnterior, 
            cuenta.getSaldo(), 
            "Transferencia recibida desde Banco Nacional", 
            referencia
        );
    }

    /**
     * Método privado para persistir el historial en MySQL
     */
    private void registrarMovimiento(Cuenta c, String tipo, BigDecimal m, BigDecimal ant, BigDecimal nue, String desc, String ref) {
        Movimiento mov = new Movimiento();
        mov.setCuentaId(c.getId());
        mov.setTipo(tipo);
        mov.setMonto(m);
        mov.setSaldoAnterior(ant);
        mov.setSaldoNuevo(nue);
        mov.setDescripcion(desc);
        mov.setReferenciaTransferencia(ref);
        mov.setFecha(LocalDateTime.now());
        
        movRepo.save(mov);
    }
}