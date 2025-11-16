package pe.edu.pucp.fasticket.repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;

import java.util.List;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Integer> {
    @Modifying
    @Query("DELETE FROM ItemCarrito i WHERE i.carroCompra.idCarro = :idCarro")
    void deleteByCarroCompraId(@Param("idCarro") Integer idCarro);
    List<ItemCarrito> findByCarroCompra_IdCarro(Integer idCarro);
    @Modifying
    @Query("UPDATE ItemCarrito i SET i.carroCompra = NULL, i.ordenCompra = :orden WHERE i.idItemCarrito = :itemId")
    void transferirItemAOrden(@Param("itemId") Integer itemId, @Param("orden") OrdenCompra orden);
}