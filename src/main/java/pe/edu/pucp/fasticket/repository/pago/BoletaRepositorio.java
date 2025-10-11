package pe.edu.pucp.fasticket.repository.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.pago.Boleta;

@Repository
public interface BoletaRepositorio extends JpaRepository<Boleta, Integer> {
}

