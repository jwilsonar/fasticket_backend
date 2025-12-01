package pe.edu.pucp.fasticket.repository.geografia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, Integer> {
    List<Distrito> findByProvincia_IdProvinciaOrderByNombreAsc(Integer idProvincia);

    @Query(value = """
        SELECT COUNT(*) > 0 FROM evento e 
        WHERE e.id_local = :idLocal
          AND e.estado_evento != 'CANCELADO'
          AND e.activo = true  -- <--- ¡ESTA ES LA LÍNEA NUEVA!
          AND (
            (e.fecha_evento + e.hora_inicio) < CAST(:fin AS TIMESTAMP)
            AND
            (e.fecha_fin_evento + e.hora_fin) > CAST(:inicio AS TIMESTAMP)
          )
    """, nativeQuery = true)
    Optional<Distrito> buscarPorNombres(
            @Param("departamento") String departamento,
            @Param("provincia") String provincia,
            @Param("distrito") String distrito
    );
}

