package com.universidad.transferencias_distribuidas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Slf4j
public class TransferenciaService {
    @Autowired private BancoNacionalService nacionalService;
    @Autowired private BancoInternacionalService internacionalService;

    public void realizarTransferencia(String origen, String destino, BigDecimal monto) {
        log.info("Iniciando SAGA: {} -> {}", origen, destino);
        
        // 1. Paso en Postgres
        String referencia = nacionalService.debitar(origen, monto);
        
        try {
            // 2. Paso en MySQL (Debes crear un método similar en BancoInternacionalService)
            internacionalService.acreditar(destino, monto, referencia);
            log.info("SAGA COMPLETADA. Ref: {}", referencia);
        } catch (Exception e) {
            log.error("Fallo en destino. Ejecutando compensación con Ref: {}", referencia);
            // 3. Compensación
            nacionalService.compensarDebito(origen, monto, referencia);
            throw new RuntimeException("Transferencia fallida y revertida automáticamente.");
        }
    }
}