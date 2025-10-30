package pe.edu.pucp.fasticket.repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrdenCompraRepositorio extends JpaRepository<OrdenCompra, Integer> {
    List<OrdenCompra> findByEstadoAndFechaExpiracionBefore(EstadoCompra estado, LocalDateTime fechaExpiracion);
    List<OrdenCompra> findByItems_TipoTicket_Evento_IdEventoAndEstado(Integer idEvento, EstadoCompra estado);
}