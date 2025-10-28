// RUTA: pe.edu.pucp.fasticket.mapper.ZonaMapper.java

package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.edu.pucp.fasticket.dto.zonas.ZonaCreateDTO;
import pe.edu.pucp.fasticket.dto.zonas.ZonaDTO;
import pe.edu.pucp.fasticket.model.eventos.Zona;

@Mapper(componentModel = "spring")
public interface ZonaMapper {

    @Mapping(target = "idLocal", source = "local.idLocal")
    ZonaDTO toDTO(Zona zona);

    @Mapping(target = "idZona", ignore = true)
    @Mapping(target = "local", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "usuarioCreacion", ignore = true)
    @Mapping(target = "usuarioActualizacion", ignore = true)
    Zona toEntity(ZonaCreateDTO dto);
}