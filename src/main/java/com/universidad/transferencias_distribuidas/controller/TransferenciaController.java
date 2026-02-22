package com.universidad.transferencias_distribuidas.controller;

import com.universidad.transferencias_distribuidas.dto.TransferenciaRequest;
import com.universidad.transferencias_distribuidas.service.TransferenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transferencias")
public class TransferenciaController {

    @Autowired
    private TransferenciaService transferenciaService;

    @PostMapping("/ejecutar")
    public ResponseEntity<String> ejecutar(@RequestBody TransferenciaRequest request) {
        try {
            // Cambié el nombre a realizarTransferencia para que coincida con el service
            transferenciaService.realizarTransferencia(
                request.getCuentaOrigen(), 
                request.getCuentaDestino(), 
                request.getMonto()
            );
            return ResponseEntity.ok("Transferencia exitosa procesada por el Orquestador SAGA");
        } catch (Exception e) {
            // Esto devolverá el error de "Transferencia fallida y revertida" si falla el SAGA
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}