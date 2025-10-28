package pe.edu.pucp.fasticket.services.eventos;

import java.util.List;
import java.util.Optional;

import pe.edu.pucp.fasticket.model.eventos.Zona;

public interface ZonaServicio {
    
    List<Zona> listarTodas();
    
    Optional<Zona> buscarPorId(Integer id);
    
    List<Zona> buscarPorLocal(Integer idLocal);
    
    Zona crear(Zona zona, Integer idLocal);
    
    Zona actualizar(Zona zona, Integer idLocal);
    
    void eliminar(Integer id);
    
    List<Zona> buscarActivas();
}
