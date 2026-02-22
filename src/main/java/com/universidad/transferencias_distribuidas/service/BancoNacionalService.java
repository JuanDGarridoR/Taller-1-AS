package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import com.universidad.transferencias_distribuidas.repository.nacional.CuentaNacionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class BancoNacionalService {

    @Autowired 
    private CuentaNacionalRepository repository;

    @Transactional("nacionalTransactionManager")
    public void debitar(String cuentaId, BigDecimal monto, String referencia) {
        
        Cuenta cuenta = repository.findById(cuentaId) 
                .orElseThrow(() -> new RuntimeException("Cuenta nacional no encontrada: " + cuentaId));

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new RuntimeException("Saldo insuficiente. Ref: " + referencia);
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        repository.save(cuenta);
    }

    @Transactional("nacionalTransactionManager")
    public void compensarDebito(String cuentaId, BigDecimal monto, String referencia) {
        Cuenta cuenta = repository.findById(cuentaId)
                .orElseThrow(() -> new RuntimeException("Error en compensación: Cuenta no encontrada"));
        
        cuenta.setSaldo(cuenta.getSaldo().add(monto));
        repository.save(cuenta);
    }
}