package com.universidad.transferencias_distribuidas.controller;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import com.universidad.transferencias_distribuidas.model.Movimiento;
import com.universidad.transferencias_distribuidas.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @GetMapping("/internacional")
    public List<Cuenta> getCuentasInternacionales() {
        return cuentaService.obtenerCuentasInternacionales();
    }
    
    @GetMapping("/nacional")
    public List<Cuenta> getCuentasNacionales() {
        return cuentaService.obtenerCuentasNacionales();
    }
    
    @GetMapping("/todas")
    public List<Cuenta> getTodasLasCuentas() {
        List<Cuenta> todas = new ArrayList<>();
        todas.addAll(cuentaService.obtenerCuentasNacionales());
        todas.addAll(cuentaService.obtenerCuentasInternacionales());
        return todas;
    }
    
    @GetMapping("/saldo/{numeroCuenta}")
    public ResponseEntity<?> getSaldo(@PathVariable String numeroCuenta) {
        Optional<Cuenta> cuentaNacional = cuentaService.buscarCuentaNacional(numeroCuenta);
        if (cuentaNacional.isPresent()) {
            return ResponseEntity.ok(cuentaNacional.get());
        }
        
        Optional<Cuenta> cuentaInternacional = cuentaService.buscarCuentaInternacional(numeroCuenta);
        if (cuentaInternacional.isPresent()) {
            return ResponseEntity.ok(cuentaInternacional.get());
        }
        
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/movimientos/{numeroCuenta}")
    public ResponseEntity<List<Movimiento>> getMovimientos(@PathVariable String numeroCuenta) {
        Optional<Cuenta> cuentaNacional = cuentaService.buscarCuentaNacional(numeroCuenta);
        if (cuentaNacional.isPresent()) {
            return ResponseEntity.ok(cuentaService.obtenerMovimientosNacionales());
        }
        
        Optional<Cuenta> cuentaInternacional = cuentaService.buscarCuentaInternacional(numeroCuenta);
        if (cuentaInternacional.isPresent()) {
            return ResponseEntity.ok(cuentaService.obtenerMovimientosInternacionales());
        }
        
        return ResponseEntity.notFound().build();
    }
}