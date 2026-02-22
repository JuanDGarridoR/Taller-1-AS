package com.universidad.transferencias_distribuidas.controller;

import com.universidad.transferencias_distribuidas.dto.TransferenciaRequest;
import com.universidad.transferencias_distribuidas.dto.TransferenciaResponse;
import com.universidad.transferencias_distribuidas.service.TransferenciaService;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transferencias")
public class TransferenciaController {

    @Autowired
    private TransferenciaService transferenciaService;

    @PostMapping
    public ResponseEntity<TransferenciaResponse> realizarTransferencia(@RequestBody TransferenciaRequest request) {
        try {
            TransferenciaResponse response = transferenciaService.realizarTransferencia(
                request.getCuentaOrigen(), 
                request.getCuentaDestino(), 
                request.getMonto()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                TransferenciaResponse.builder()
                    .mensaje("Error crítico: " + e.getMessage())
                    .estado("FALLIDO")
                    .fecha(LocalDateTime.now())
                    .build()
            );
        }
    }

    @PostMapping("/ejecutar")
    public ResponseEntity<String> ejecutar(@RequestBody TransferenciaRequest request) {
        try {
            transferenciaService.realizarTransferencia(
                request.getCuentaOrigen(), 
                request.getCuentaDestino(), 
                request.getMonto()
            );
            return ResponseEntity.ok("Transferencia exitosa procesada por el Orquestador SAGA");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}