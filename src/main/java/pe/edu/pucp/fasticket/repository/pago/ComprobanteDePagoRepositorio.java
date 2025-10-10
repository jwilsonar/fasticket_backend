package pe.edu.pucp.fasticket.repository.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.pago.ComprobantePago;

@Repository
public interface ComprobanteDePagoRepositorio extends JpaRepository<ComprobantePago, Integer> {
}
