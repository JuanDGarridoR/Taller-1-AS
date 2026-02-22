package com.universidad.transferencias_distribuidas.controller;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import com.universidad.transferencias_distribuidas.repository.internacional.CuentaInternacionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    @Autowired
    private CuentaInternacionalRepository repository;

    @GetMapping("/internacional")
    public List<Cuenta> getCuentasInternacionales() {
        
        return repository.findAll();
    }
}