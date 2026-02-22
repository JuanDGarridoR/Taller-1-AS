package com.universidad.transferencias_distribuidas.repository.internacional;

import com.universidad.transferencias_distribuidas.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoInternacionalRepository extends JpaRepository<Movimiento, Long> {
}