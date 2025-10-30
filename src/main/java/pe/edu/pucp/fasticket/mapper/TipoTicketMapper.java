package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Mapper(componentModel = "spring")
public interface TipoTicketMapper {

    @Mapping(target = "idZona", source = "zona.idZona")
    @Mapping(target = "nombreZona", source = "zona.nombre")
    TipoTicketDTO toDTO(TipoTicket tipoTicket);

    @Mapping(target = "idTipoTicket", ignore = true)
    @Mapping(target = "zona", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "cantidadDisponible", source = "stock")
    @Mapping(target = "cantidadVendida", constant = "0")
    @Mapping(target = "fechaInicioVenta", ignore = true)
    @Mapping(target = "fechaFinVenta", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    TipoTicket toEntity(TipoTicketDTO dto);
}
