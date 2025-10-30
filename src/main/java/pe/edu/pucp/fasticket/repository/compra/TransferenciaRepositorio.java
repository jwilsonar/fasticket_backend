package pe.edu.pucp.fasticket.repository.compra;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;
import java.util.List;

public interface TransferenciaRepositorio extends JpaRepository<TransferenciaEntrada, Integer> {

    List<TransferenciaEntrada> findByTicket_IdTicketOrderByFechaTransferenciaDesc(Integer idTicket);

}