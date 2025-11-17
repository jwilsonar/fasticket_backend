package pe.edu.pucp.fasticket.repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

import java.util.Optional;

@Repository
public interface CarroComprasRepository extends JpaRepository<CarroCompras, Integer> {
    
    Optional<CarroCompras> findByCliente(Cliente cliente);
    Optional<CarroCompras> findByCliente_IdPersonaAndActivoTrue(Integer idCliente);
    @Query("SELECT c FROM CarroCompras c " +
            "LEFT JOIN FETCH c.items i " +
            "LEFT JOIN FETCH i.tickets " +
            "WHERE c.idCarro = :idCarro")
    Optional<CarroCompras> findByIdWithItemsAndTickets(@Param("idCarro") Integer idCarro);
    @Modifying
    @Query("UPDATE CarroCompras c SET c.activo = false WHERE c.idCarro = :carritoId")
    void desactivarCarrito(@Param("carritoId") Integer carritoId);
}

