package pe.edu.pucp.fasticket.repository.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.pago.Pago;

@Repository
public interface PagoRepositorio extends JpaRepository<Pago, Integer> {

}