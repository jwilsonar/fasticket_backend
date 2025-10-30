package pe.edu.pucp.fasticket.events;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;

@Getter
public class TicketTransferidoEvent extends ApplicationEvent{

    private final TransferenciaEntrada historial;

    public TicketTransferidoEvent(TransferenciaEntrada historial) {
        super(historial);
        this.historial = historial;
    }
}
