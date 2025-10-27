package pe.edu.pucp.fasticket.services.eventos;

import pe.edu.pucp.fasticket.model.eventos.Zona;

import java.util.List;
import java.util.Optional;

public interface ZonaServicio {
    
    List<Zona> listarTodas();
    
    Optional<Zona> buscarPorId(Integer id);
    
    List<Zona> buscarPorLocal(Integer idLocal);
    
    Zona crear(Zona zona);
    
    Zona actualizar(Zona zona);
    
    void eliminar(Integer id);
    
    List<Zona> buscarActivas();
}
