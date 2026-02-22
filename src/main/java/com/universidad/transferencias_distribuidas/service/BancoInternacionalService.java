package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.model.Cuenta; 
import com.universidad.transferencias_distribuidas.repository.internacional.CuentaInternacionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class BancoInternacionalService {

    @Autowired 
    private CuentaInternacionalRepository repository;

    @Transactional("internacionalTransactionManager")
    public void acreditar(String cuentaId, BigDecimal monto, String referencia) {
        Cuenta cuenta = repository.findById(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta internacional no encontrada: " + cuentaId));

        cuenta.setSaldo(cuenta.getSaldo().add(monto));
        repository.save(cuenta);
    }
}