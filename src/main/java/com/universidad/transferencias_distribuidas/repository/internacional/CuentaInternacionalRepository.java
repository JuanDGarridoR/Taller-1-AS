package com.universidad.transferencias_distribuidas.repository.internacional;

import com.universidad.transferencias_distribuidas.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CuentaInternacionalRepository extends JpaRepository<Cuenta, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
}