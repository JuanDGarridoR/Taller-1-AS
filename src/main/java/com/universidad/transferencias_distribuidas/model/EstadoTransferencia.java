package com.universidad.transferencias_distribuidas.model;

public enum EstadoTransferencia {
    /** Paso 0: El SAGA se registró pero aún no se ejecutó ningún paso */
    INICIADA,
    /** Paso 1 OK: El débito en Banco Nacional fue exitoso */
    DEBITO_COMPLETADO,
    /** Paso 2 OK: El crédito en Banco Internacional fue exitoso → SAGA terminado */
    COMPLETADA,
    /** Paso 2 falló: Se está ejecutando la transacción compensatoria */
    COMPENSANDO,
    /** La compensación revirtió el débito correctamente */
    REVERTIDA,
    /** Falló el débito inicial antes de que el SAGA avanzara */
    FALLIDA
}