package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.dto.TransferenciaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class TransferenciaService {

    @Autowired private BancoNacionalService nacionalService;
    @Autowired private BancoInternacionalService internacionalService;

    public TransferenciaResponse realizarTransferencia(String origen, String destino, BigDecimal monto) {
        String idTransaccion = UUID.randomUUID().toString();
        log.info("SAGA INICIADA [{}]: {} -> {} por ${}", idTransaccion, origen, destino, monto);

        try {
            
            nacionalService.debitar(origen, monto, idTransaccion);
            log.info("PASO 1 EXITOSO: Débito en Banco Nacional completado");

            try {
                
                internacionalService.acreditar(destino, monto, idTransaccion);
                log.info("PASO 2 EXITOSO: Crédito en Banco Internacional completado");

                return TransferenciaResponse.builder()
                        .mensaje("Transferencia internacional exitosa")
                        .idTransaccion(idTransaccion)
                        .estado("EXITOSO")
                        .monto(monto.doubleValue())
                        .cuentaOrigenId(Long.parseLong(origen))
                        .cuentaDestinoId(Long.parseLong(destino))
                        .fecha(LocalDateTime.now())
                        .build();

            } catch (Exception e) {
                log.error("PASO 2 FALLIDO: {}. Ejecutando Compensación...", e.getMessage());
                
                
                nacionalService.compensarDebito(origen, monto, idTransaccion);
                
                return TransferenciaResponse.builder()
                        .mensaje("Fallo en destino: " + e.getMessage() + ". Dinero revertido a origen.")
                        .idTransaccion(idTransaccion)
                        .estado("COMPENSADO")
                        .monto(monto.doubleValue())
                        .fecha(LocalDateTime.now())
                        .build();
            }

        } catch (Exception e) {
            log.error("ERROR INICIAL: No se pudo realizar el débito. {}", e.getMessage());
            throw new RuntimeException("Transferencia no iniciada: " + e.getMessage());
        }
    }
}