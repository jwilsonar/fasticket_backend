package pe.edu.pucp.fasticket.services.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TipoTicketServicio {
    @Autowired
    private TipoTicketRepository repo_tipoTicket;

    public List<TipoTicket> ListarTiposTicket(){
        return repo_tipoTicket.findAll();
    }

    public Optional<TipoTicket> BuscarId(Integer id){
        return repo_tipoTicket.findById(id);
    }

    public TipoTicket Guardar(TipoTicket tipoTicket){
        return (TipoTicket) repo_tipoTicket.save(tipoTicket);
    }

    public void Eliminar(Integer id){
        repo_tipoTicket.deleteById(id);
    }
}
