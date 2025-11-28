package pe.edu.pucp.fasticket.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;

public interface ConfiguracionRepository extends JpaRepository<ConfiguracionGlobal, String> {
}