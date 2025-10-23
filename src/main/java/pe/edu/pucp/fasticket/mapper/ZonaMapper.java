// RUTA: pe.edu.pucp.fasticket.mapper.ZonaMapper.java

package pe.edu.pucp.fasticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// --- IMPORTACIONES CORRECTAS ---
// Apuntando a tu nueva carpeta 'dto.zonas'
import pe.edu.pucp.fasticket.dto.zonas.ZonaDTO;
import pe.edu.pucp.fasticket.dto.zonas.ZonaCreateDTO;
// --- FIN IMPORTACIONES ---

import pe.edu.pucp.fasticket.model.eventos.Zona;

@Mapper(componentModel = "spring") // Le dice a Spring que esto es un Bean
public interface ZonaMapper {

    // 1. Convierte la Entidad -> al DTO
    // (Asegúrate que tu ZonaDTO tenga un constructor vacío @NoArgsConstructor)
    ZonaDTO toDTO(Zona zona);

    // 2. Convierte el CreateDTO -> a la Entidad
    @Mapping(target = "idZona", ignore = true)
    @Mapping(target = "local", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "usuarioCreacion", ignore = true)
    @Mapping(target = "usuarioActualizacion", ignore = true)
    @Mapping(target = "categoriasEntrada", ignore = true) // Asumiendo que esta es una relación
    Zona toEntity(ZonaCreateDTO dto);
}