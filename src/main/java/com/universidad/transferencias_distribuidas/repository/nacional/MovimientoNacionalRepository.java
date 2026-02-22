package com.universidad.transferencias_distribuidas.repository.nacional;

import com.universidad.transferencias_distribuidas.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoNacionalRepository extends JpaRepository<Movimiento, Long> {
}