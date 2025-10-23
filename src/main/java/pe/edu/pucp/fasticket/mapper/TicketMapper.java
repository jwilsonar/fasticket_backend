// RUTA: pe.edu.pucp.fasticket.mapper.TicketMapper.java

package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.pucp.fasticket.dto.tickets.TicketDTO;
import pe.edu.pucp.fasticket.dto.tickets.TicketCreateDTO;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.Zona;
// Remove unused imports if they cause issues
// import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
// import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    // --- Método toDTO (Revisado) ---
    // Make sure Zona has a 'nombre' field with a getter
    @Mapping(source = "zona.nombre", target = "nombreZona")
    // MapStruct should handle enum-to-string conversion if types match (String <-> String)
    // If TicketDTO.estado is String and Ticket.estado is EstadoTicket (enum),
    // you might need a custom mapping method or @ValueMapping. Let's assume String for now.
    @Mapping(source = "estado", target = "estado")
    // We assume TicketDTO doesn't need 'stock' as Ticket.java doesn't have it.
    // If TicketDTO needs stock, where should it come from? Maybe CategoriaEntrada?
    TicketDTO toDTO(Ticket ticket);

    // --- Método toEntity (CORREGIDO) ---
    @Mapping(target = "idTicket", ignore = true)
    // --- CORRECCIÓN: 'codigoQr' (lowercase q) SÍ existe ---
    @Mapping(target = "codigoQr", ignore = true) // Ignoramos porque se genera después
    // --- FIN CORRECCIÓN ---
    @Mapping(target = "qrImage", ignore = true)        // Ignorar campo extra
    @Mapping(target = "asiento", ignore = true)         // Ignorar, se asignará después si aplica
    @Mapping(target = "fila", ignore = true)            // Ignorar, se asignará después si aplica
    @Mapping(target = "estado", ignore = true)         // Se asigna en el Service ("DISPONIBLE")
    @Mapping(target = "activo", ignore = true)         // Se asigna en el Service (true)
    @Mapping(target = "tipoTicket", ignore = true)      // Ignorar relación
    @Mapping(target = "itemCarrito", ignore = true)     // Ignorar relación
    // --- CORRECCIÓN: Usar 'cliente' ---
    @Mapping(target = "cliente", ignore = true)        // Ignorar relación (se asigna al comprar/transferir)
    // --- FIN CORRECCIÓN ---
    @Mapping(target = "nombreAsistente", ignore = true) // Ignorar
    @Mapping(target = "apellidoAsistente", ignore = true)// Ignorar
    @Mapping(target = "documentoAsistente", ignore = true)// Ignorar
    @Mapping(target = "tipoDocumentoAsistente", ignore = true) // Ignorar

    // --- Ignorando campos de auditoría ---
    @Mapping(target = "usuarioCreacion", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "usuarioActualizacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)

    // --- Mapeos explícitos y automáticos ---
    // REMOVED: @Mapping(target = "nombre", ...) // 'nombre' NO existe en Ticket.java
    // REMOVED: @Mapping(target = "stock", ...)  // 'stock' NO existe en Ticket.java
    @Mapping(target = "precio", source = "dto.precio")   // 'precio' SÍ existe
    @Mapping(target = "evento", source = "evento")     // Viene del parámetro
    @Mapping(target = "zona", source = "zona")       // Viene del parámetro
    Ticket toEntity(TicketCreateDTO dto, Evento evento, Zona zona);
    // --- FIN CORRECCIÓN ---
}