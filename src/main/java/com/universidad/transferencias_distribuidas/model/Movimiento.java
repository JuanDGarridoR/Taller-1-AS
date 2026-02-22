package com.universidad.transferencias_distribuidas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento")
@Data
public class Movimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id")
    private Long cuentaId;

    private String tipo;
    private BigDecimal monto;

    @Column(name = "saldo_anterior")
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_nuevo")
    private BigDecimal saldoNuevo;

    private String descripcion;

    @Column(name = "referencia_transferencia")
    private String referenciaTransferencia;

    private LocalDateTime fecha = LocalDateTime.now();
}