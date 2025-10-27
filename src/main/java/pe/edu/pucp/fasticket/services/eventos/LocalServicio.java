package pe.edu.pucp.fasticket.services.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;

import java.util.List;
import java.util.Optional;

@Service
public class LocalServicio {
    @Autowired
    private LocalesRepositorio repo_locales;

    public List<Local> ListarLocales() {
        return repo_locales.findAll();
    }

    public Optional<Local> BuscarId(Integer id) { return repo_locales.findById(id);}

    // En teoría, se usa este metodo para guardar un Local
    public Local Guardar(Local local) { return (Local) repo_locales.save(local);}

    public void Eliminar(Integer id) { repo_locales.deleteById(id);}

}
