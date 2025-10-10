package pe.edu.pucp.fasticket.services.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;

import java.util.List;
import java.util.Optional;

@Service
public class EventoServicio {
    @Autowired
    private EventosRepositorio repo_eventos;

    public List<Evento> ListarEventos(){
        return repo_eventos.findAll();
    }

    public Optional<Evento> BuscarID(Integer id){
        return repo_eventos.findById(id);
    }

    public Evento Guardar(Evento evento){
        return (Evento) repo_eventos.save(evento);
    }

    public void Eliminar(Integer id){
        repo_eventos.deleteById(id);
    }
}
