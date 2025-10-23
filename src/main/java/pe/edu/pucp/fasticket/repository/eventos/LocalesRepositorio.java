package pe.edu.pucp.fasticket.repository.eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.eventos.Local;

/**
 * Repositorio para la entidad Local.
 * Implementa queries personalizadas para búsqueda y filtrado según RF-005.
 */
@Repository
public interface LocalesRepositorio extends JpaRepository<Local, Integer> {
    
    /**
     * Encuentra todos los locales activos.
     */
    List<Local> findByActivoTrue();
    
    /**
     * Verifica si existe un local con el nombre especificado (ignora mayúsculas).
     */
    boolean existsByNombreIgnoreCase(String nombre);
    
    /**
     * RF-005: Busca locales por nombre parcial (ignora mayúsculas).
     */
    List<Local> findByNombreContainingIgnoreCase(String nombre);
    
    /**
     * RF-005: Busca locales activos por nombre parcial.
     */
    List<Local> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
    
    /**
     * RF-005: Filtra locales activos por distrito.
     */
    List<Local> findByDistritoIdDistritoAndActivoTrue(Integer idDistrito);
}