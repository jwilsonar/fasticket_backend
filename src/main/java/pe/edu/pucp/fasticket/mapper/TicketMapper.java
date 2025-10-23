package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
// Importamos los DTOs de su nuevo paquete 'tickets'
import pe.edu.pucp.fasticket.dto.tickets.TicketDTO;
import pe.edu.pucp.fasticket.dto.tickets.TicketCreateDTO;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.Zona;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(source = "zona.nombre", target = "nombreZona") // Mapea el nombre de la zona
    TicketDTO toDTO(Ticket ticket);

    @Mapping(target = "idTicket", ignore = true)
    @Mapping(target = "codigoQR", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "ordenCompra", ignore = true)
    @Mapping(target = "clienteActual", ignore = true)
    @Mapping(target = "evento", source = "evento") // Asigna el evento
    @Mapping(target = "zona", source = "zona")   // Asigna la zona
    Ticket toEntity(TicketCreateDTO dto, Evento evento, Zona zona);
}