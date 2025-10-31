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
    
    /**
     * Actualiza únicamente la URL de la imagen de una zona.
     * 
     * @param id ID de la zona
     * @param imagenUrl URL de la imagen a guardar
     * @return Zona actualizada con la nueva URL de imagen
     */
    Zona actualizarImagenUrl(Integer id, String imagenUrl);
}
