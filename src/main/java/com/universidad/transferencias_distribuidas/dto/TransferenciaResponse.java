package com.universidad.transferencias_distribuidas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferenciaResponse {
    private String mensaje;
    private String idTransaccion;
    private String estado;
    private Double monto;
    private Long cuentaOrigenId;
    private Long cuentaDestinoId;
    private LocalDateTime fecha;
}
