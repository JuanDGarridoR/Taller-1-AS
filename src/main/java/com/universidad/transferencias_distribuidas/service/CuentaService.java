package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import com.universidad.transferencias_distribuidas.model.Movimiento;
import com.universidad.transferencias_distribuidas.repository.internacional.CuentaInternacionalRepository;
import com.universidad.transferencias_distribuidas.repository.internacional.MovimientoInternacionalRepository;
import com.universidad.transferencias_distribuidas.repository.nacional.CuentaNacionalRepository;
import com.universidad.transferencias_distribuidas.repository.nacional.MovimientoNacionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CuentaService {

    @Autowired
    private CuentaNacionalRepository cuentaNacionalRepository;
    
    @Autowired
    private CuentaInternacionalRepository cuentaInternacionalRepository;
    
    @Autowired
    private MovimientoNacionalRepository movimientoNacionalRepository;
    
    @Autowired
    private MovimientoInternacionalRepository movimientoInternacionalRepository;

    @Transactional(value = "nacionalTransactionManager", readOnly = true)
    public Optional<Cuenta> buscarCuentaNacional(String numeroCuenta) {
        return cuentaNacionalRepository.findAll().stream()
                .filter(c -> c.getNumeroCuenta().equals(numeroCuenta))
                .findFirst();
    }

    @Transactional(value = "internacionalTransactionManager", readOnly = true)
    public Optional<Cuenta> buscarCuentaInternacional(String numeroCuenta) {
        return cuentaInternacionalRepository.findAll().stream()
                .filter(c -> c.getNumeroCuenta().equals(numeroCuenta))
                .findFirst();
    }

    @Transactional(value = "nacionalTransactionManager", readOnly = true)
    public List<Cuenta> obtenerCuentasNacionales() {
        return cuentaNacionalRepository.findAll();
    }

    @Transactional(value = "internacionalTransactionManager", readOnly = true)
    public List<Cuenta> obtenerCuentasInternacionales() {
        return cuentaInternacionalRepository.findAll();
    }

    @Transactional(value = "nacionalTransactionManager", readOnly = true)
    public List<Movimiento> obtenerMovimientosNacionales() {
        return movimientoNacionalRepository.findAll();
    }

    @Transactional(value = "internacionalTransactionManager", readOnly = true)
    public List<Movimiento> obtenerMovimientosInternacionales() {
        return movimientoInternacionalRepository.findAll();
    }
}
