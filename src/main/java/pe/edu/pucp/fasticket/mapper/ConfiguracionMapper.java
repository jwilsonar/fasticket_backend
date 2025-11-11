package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import pe.edu.pucp.fasticket.dto.ConfiguracionDTO;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConfiguracionMapper {

    ConfiguracionDTO toDTO(ConfiguracionGlobal configuracion);

    ConfiguracionGlobal toEntity(ConfiguracionDTO dto);

    List<ConfiguracionDTO> toDTOList(List<ConfiguracionGlobal> configuraciones);
}