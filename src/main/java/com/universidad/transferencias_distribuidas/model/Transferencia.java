package com.universidad.transferencias_distribuidas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa el registro persistente de un SAGA de transferencia.
 *
 * SAGA pattern: cada instancia de esta clase ES el "log de estado" del orquestador.
 * Permite saber exactamente en qué paso falló una transferencia y si ya fue
 * compensada, sin depender de la memoria de la JVM.
 *
 * Ciclo de vida de estados:
 *   INICIADA → DEBITO_COMPLETADO → COMPLETADA          (happy path)
 *   INICIADA → FALLIDA                                 (débito falló)
 *   DEBITO_COMPLETADO → COMPENSANDO → REVERTIDA        (crédito falló, compensación ok)
 */
@Entity
@Table(name = "transferencia")
@Data
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador único del SAGA, generado por el orquestador (UUID). */
    @Column(name = "id_transaccion", unique = true, nullable = false, length = 50)
    private String idTransaccion;

    @Column(name = "cuenta_origen", nullable = false, length = 20)
    private String cuentaOrigen;

    @Column(name = "cuenta_destino", nullable = false, length = 20)
    private String cuentaDestino;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    /**
     * Estado actual del SAGA. Se actualiza atómicamente en cada paso del orquestador,
     * garantizando que siempre refleja el último paso ejecutado con éxito.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTransferencia estado;

    /** Descripción del error en caso de fallo o compensación. */
    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
