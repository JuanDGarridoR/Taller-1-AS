package com.universidad.transferencias_distribuidas.repository.nacional;

import com.universidad.transferencias_distribuidas.model.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio del SAGA: persiste el log de estado de cada transferencia
 * en la base de datos del Banco Nacional (PostgreSQL).
 *
 * Al estar en el paquete 'repository.nacional' queda vinculado automáticamente
 * al nacionalEntityManagerFactory y nacionalTransactionManager.
 */
@Repository
public interface TransferenciaNacionalRepository extends JpaRepository<Transferencia, Long> {

    Optional<Transferencia> findByIdTransaccion(String idTransaccion);
}
