package pe.edu.pucp.fasticket.services.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.ZonasRepositorio;

import java.util.List;
import java.util.Optional;

@Service
public class ZonaServicio {
    @Autowired
    private ZonasRepositorio repo_zonas;

    public List<Zona> ListarZonas(){
        return repo_zonas.findAll();
    }

    public Optional<Zona> BuscarId(Integer id){
        return repo_zonas.findById(id);
    }

    public Zona Guardar(Zona zona){
        return (Zona) repo_zonas.save(zona);
    }

    public void Eliminar(Integer id){
        repo_zonas.deleteById(id);
    }
}
