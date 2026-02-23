package com.universidad.transferencias_distribuidas.service;

import com.universidad.transferencias_distribuidas.dto.TransferenciaResponse;
import com.universidad.transferencias_distribuidas.model.EstadoTransferencia;
import com.universidad.transferencias_distribuidas.model.Transferencia;
import com.universidad.transferencias_distribuidas.repository.nacional.TransferenciaNacionalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orquestador del patrón SAGA para transferencias interbancarias.
 *
 * Responsabilidades:
 *  1. Crear y persistir el registro del SAGA antes de ejecutar cualquier paso.
 *  2. Actualizar el estado de la transferencia después de CADA paso.
 *  3. Invocar la transacción compensatoria si algún paso falla.
 *  4. Garantizar que el estado final siempre quede persistido (COMPLETADA o REVERTIDA).
 *
 * Flujo de estados:
 *   INICIADA → DEBITO_COMPLETADO → COMPLETADA
 *   INICIADA → FALLIDA
 *   DEBITO_COMPLETADO → COMPENSANDO → REVERTIDA
 */
@Service
@Slf4j
public class TransferenciaService {

    @Autowired private BancoNacionalService nacionalService;
    @Autowired private BancoInternacionalService internacionalService;
    @Autowired private TransferenciaNacionalRepository transferenciaRepository;

    public TransferenciaResponse realizarTransferencia(String cuentaOrigen, String cuentaDestino, BigDecimal monto) {

        String idTransaccion = UUID.randomUUID().toString();
        log.info("╔══ SAGA INICIADA [{}] ══╗", idTransaccion);
        log.info("  Origen: {} | Destino: {} | Monto: ${}", cuentaOrigen, cuentaDestino, monto);

        // ── PASO 0: Registrar el SAGA como INICIADA ────────────────────────────
        // Esto es fundamental: el registro existe en BD antes de mover dinero.
        // Si la JVM muere después de este punto podremos detectar SAGAs incompletos.
        Transferencia saga = new Transferencia();
        saga.setIdTransaccion(idTransaccion);
        saga.setCuentaOrigen(cuentaOrigen);
        saga.setCuentaDestino(cuentaDestino);
        saga.setMonto(monto);
        saga.setEstado(EstadoTransferencia.INICIADA);
        transferenciaRepository.save(saga);
        log.info("[SAGA][{}] Estado persistido: INICIADA", idTransaccion);

        // ── PASO 1: Débito en Banco Nacional (PostgreSQL) ──────────────────────
        try {
            nacionalService.debitar(cuentaOrigen, monto, idTransaccion);

            // Actualizar estado: el débito fue exitoso, el SAGA puede continuar
            saga.setEstado(EstadoTransferencia.DEBITO_COMPLETADO);
            transferenciaRepository.save(saga);
            log.info("[SAGA][{}] Estado persistido: DEBITO_COMPLETADO", idTransaccion);

        } catch (Exception e) {
            // El débito falló: no se movió dinero, no se necesita compensar
            log.error("[SAGA][{}] PASO 1 FALLIDO: {}", idTransaccion, e.getMessage());
            saga.setEstado(EstadoTransferencia.FALLIDA);
            saga.setMensajeError("Débito fallido: " + e.getMessage());
            transferenciaRepository.save(saga);

            return TransferenciaResponse.builder()
                    .mensaje("Transferencia fallida: " + e.getMessage())
                    .idTransaccion(idTransaccion)
                    .estado("FALLIDA")
                    .monto(monto.doubleValue())
                    .cuentaOrigenId(null)
                    .cuentaDestinoId(null)
                    .fecha(LocalDateTime.now())
                    .build();
        }

        // ── PASO 2: Crédito en Banco Internacional (MySQL) ────────────────────
        try {
            internacionalService.acreditar(cuentaDestino, monto, idTransaccion);

            // Actualizar estado: SAGA completado exitosamente
            saga.setEstado(EstadoTransferencia.COMPLETADA);
            transferenciaRepository.save(saga);
            log.info("[SAGA][{}] Estado persistido: COMPLETADA", idTransaccion);
            log.info("╚══ SAGA COMPLETADA [{}] ══╝", idTransaccion);

            return TransferenciaResponse.builder()
                    .mensaje("Transferencia internacional exitosa")
                    .idTransaccion(idTransaccion)
                    .estado("COMPLETADA")
                    .monto(monto.doubleValue())
                    .cuentaOrigenId(null)
                    .cuentaDestinoId(null)
                    .fecha(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            // El crédito falló: hay que compensar el débito ya realizado
            log.error("[SAGA][{}] PASO 2 FALLIDO: {}. Iniciando compensación...", idTransaccion, e.getMessage());

            // Marcar como COMPENSANDO antes de ejecutar la compensación
            saga.setEstado(EstadoTransferencia.COMPENSANDO);
            saga.setMensajeError("Crédito fallido: " + e.getMessage());
            transferenciaRepository.save(saga);
            log.warn("[SAGA][{}] Estado persistido: COMPENSANDO", idTransaccion);

            // ── COMPENSACIÓN: Revertir el débito ──────────────────────────────
            try {
                nacionalService.compensarDebito(cuentaOrigen, monto, idTransaccion);

                saga.setEstado(EstadoTransferencia.REVERTIDA);
                transferenciaRepository.save(saga);
                log.warn("[SAGA][{}] Estado persistido: REVERTIDA", idTransaccion);
                log.warn("╚══ SAGA REVERTIDA [{}] ══╝", idTransaccion);

                return TransferenciaResponse.builder()
                        .mensaje("Fallo en destino. Débito revertido. Error: " + e.getMessage())
                        .idTransaccion(idTransaccion)
                        .estado("REVERTIDA")
                        .monto(monto.doubleValue())
                        .cuentaOrigenId(null)
                        .cuentaDestinoId(null)
                        .fecha(LocalDateTime.now())
                        .build();

            } catch (Exception compensacionEx) {
                // La compensación también falló: estado crítico, requiere intervención manual
                log.error("╚══ CRÍTICO [{}]: Compensación fallida: {} ══╝", idTransaccion, compensacionEx.getMessage());
                saga.setMensajeError("CRÍTICO - Compensación fallida: " + compensacionEx.getMessage());
                transferenciaRepository.save(saga);

                return TransferenciaResponse.builder()
                        .mensaje("ESTADO CRÍTICO: el débito no pudo revertirse. Referencia: " + idTransaccion)
                        .idTransaccion(idTransaccion)
                        .estado("COMPENSANDO")
                        .monto(monto.doubleValue())
                        .cuentaOrigenId(null)
                        .cuentaDestinoId(null)
                        .fecha(LocalDateTime.now())
                        .build();
            }
        }
    }
}
