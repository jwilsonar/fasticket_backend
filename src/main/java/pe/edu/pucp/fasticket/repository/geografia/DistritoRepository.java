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

    @Query("SELECT d FROM Distrito d " +
            "JOIN d.provincia p " +
            "JOIN p.departamento dep " +
            "WHERE UPPER(d.nombre) = UPPER(:distrito) " +
            "AND UPPER(p.nombre) = UPPER(:provincia) " +
            "AND UPPER(dep.nombre) = UPPER(:departamento)")
    Optional<Distrito> buscarPorNombres(
            @Param("departamento") String departamento,
            @Param("provincia") String provincia,
            @Param("distrito") String distrito
    );
}

