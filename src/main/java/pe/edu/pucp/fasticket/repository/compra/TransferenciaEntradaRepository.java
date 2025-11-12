package pe.edu.pucp.fasticket.repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;

@Repository
public interface TransferenciaEntradaRepository extends JpaRepository<TransferenciaEntrada, Integer> {
    // Por ahora no necesitamos métodos personalizados, JpaRepository es suficiente
}