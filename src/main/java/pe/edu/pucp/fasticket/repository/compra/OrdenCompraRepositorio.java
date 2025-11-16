package pe.edu.pucp.fasticket.repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepositorio extends JpaRepository<OrdenCompra, Integer> {

    List<OrdenCompra> findByEstadoAndFechaExpiracionBefore(EstadoCompra estado, LocalDateTime fechaExpiracion);

    List<OrdenCompra> findByItems_TipoTicket_Evento_IdEventoAndEstado(Integer idEvento, EstadoCompra estado);

    @Query("SELECT o FROM OrdenCompra o WHERE o.carroCompras.idCarro = :carritoId AND o.activo = true")
    List<OrdenCompra> findByCarroComprasIdCarroAndActivoTrue(@Param("carritoId") Integer carritoId);


    @Query("""
                SELECT DISTINCT o
                FROM OrdenCompra o
                LEFT JOIN FETCH o.pago p
                LEFT JOIN FETCH o.items i
                LEFT JOIN FETCH i.tipoTicket
                WHERE o.idOrdenCompra = :idOrden
                  AND (p.activo = true OR p IS NULL)
            """)
    Optional<OrdenCompra> findByIdWithPagoActivo(@Param("idOrden") Integer idOrden);
    
    List<OrdenCompra> findByCliente_IdPersona(Integer idCliente);
    
    List<OrdenCompra> findByCliente_IdPersonaAndEstado(Integer idCliente, EstadoCompra estado);
    
    List<OrdenCompra> findByEstado(EstadoCompra estado);
    
    List<OrdenCompra> findByCliente_IdPersonaOrderByFechaOrdenDesc(Integer idCliente);
    
    List<OrdenCompra> findAllByOrderByFechaOrdenDesc();

    @Query("SELECT DISTINCT o FROM OrdenCompra o " +
            "LEFT JOIN FETCH o.cliente c " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.tipoTicket tt " +
            "WHERE o.idOrdenCompra = :id")
    Optional<OrdenCompra> findByIdWithAllDetails(@Param("id") Integer id);
}