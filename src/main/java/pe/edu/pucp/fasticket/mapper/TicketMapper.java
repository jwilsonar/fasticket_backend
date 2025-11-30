// RUTA: pe.edu.pucp.fasticket.mapper.TicketMapper.java

package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.pucp.fasticket.dto.tickets.TicketDTO;
import pe.edu.pucp.fasticket.dto.tickets.TicketCreateDTO;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
// Remove unused imports if they cause issues
// import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
// import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    // --- Método toDTO (Corregido) ---
    @Mapping(source = "estado", target = "estado")
    // --- INICIO CORRECCIÓN ERROR 1 ---
    // Ignora los campos que están en el DTO pero no en la Entidad
    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "nombreZona", ignore = true)
    // --- FIN CORRECCIÓN ERROR 1 ---
    TicketDTO toDTO(Ticket ticket);

    // --- Método toEntity (Corregido) ---
    @Mapping(target = "idTicket", ignore = true)
    @Mapping(target = "codigoQr", ignore = true)
    @Mapping(target = "qrImageUrl", ignore = true)
    @Mapping(target = "asiento", ignore = true)
    @Mapping(target = "fila", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "tipoTicket", ignore = true)
    @Mapping(target = "itemCarrito", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "nombreAsistente", ignore = true)
    @Mapping(target = "apellidoAsistente", ignore = true)
    @Mapping(target = "documentoAsistente", ignore = true)
    @Mapping(target = "tipoDocumentoAsistente", ignore = true)

    // --- INICIO CORRECCIÓN ERROR 2 ---
    // Ignora los nuevos campos de Transferencia que no vienen en el CreateDTO
    @Mapping(target = "ordenCompra", ignore = true)
    @Mapping(target = "contadorTransferencias", ignore = true)
    @Mapping(target = "fechaUltimaTransferencia", ignore = true)
    @Mapping(target = "historialTransferencias", ignore = true)
    @Mapping(target = "solicitudesTransferencia", ignore = true)
    // --- FIN CORRECCIÓN ERROR 2 ---

    // --- Ignorando campos de auditoría ---
    @Mapping(target = "usuarioCreacion", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "usuarioActualizacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)

    // --- Mapeos explícitos y automáticos ---
    @Mapping(target = "precio", source = "dto.precio")
    @Mapping(target = "evento", source = "evento")
    Ticket toEntity(TicketCreateDTO dto, Evento evento);
}