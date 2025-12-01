package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Mapper(componentModel = "spring")
public interface TipoTicketMapper {

    @Mapping(target = "idZona", source = "zona.idZona")
    @Mapping(target = "nombreZona", source = "zona.nombre")
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "cantidadDisponible", source = "cantidadDisponible")
    TipoTicketDTO toDTO(TipoTicket tipoTicket);

    @Mapping(target = "idTipoTicket", ignore = true)
    @Mapping(target = "zona", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "cantidadDisponible", source = "stock")
    @Mapping(target = "cantidadVendida", constant = "0")
    @Mapping(target = "precio", source = "precio")
    @Mapping(target = "fechaInicioVenta", source = "fechaInicioVenta")
    @Mapping(target = "fechaFinVenta", source = "fechaFinVenta")
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "evento", ignore = true)
    TipoTicket toEntity(TipoTicketDTO dto);
}
