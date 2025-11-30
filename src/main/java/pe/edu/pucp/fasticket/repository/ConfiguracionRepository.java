package pe.edu.pucp.fasticket.repository;

import java.time.LocalTime;

import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.Column;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;

public interface ConfiguracionRepository extends JpaRepository<ConfiguracionGlobal, String> {
}