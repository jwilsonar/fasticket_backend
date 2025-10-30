package pe.edu.pucp.fasticket.mapper;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pe.edu.pucp.fasticket.dto.eventos.LocalCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.LocalResponseDTO;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

/**
 * Mapper para convertir entre entidades Local y DTOs.
 */
@Component
@RequiredArgsConstructor
public class LocalMapper {

    public LocalResponseDTO toResponseDTO(Local local) {
        if (local == null) {
            return null;
        }
        
        return LocalResponseDTO.builder()
                .idLocal(local.getIdLocal())
                .nombre(local.getNombre())
                .direccion(local.getDireccion())
                .urlMapa(local.getUrlMapa())
                .aforoTotal(local.getAforoTotal())
                .activo(local.getActivo())
                .idDistrito(local.getDistrito() != null ? local.getDistrito().getIdDistrito() : null)
                .nombreDistrito(local.getDistrito() != null ? local.getDistrito().getNombre() : null)
                .fechaCreacion(local.getFechaCreacion())
                .build();
    }

    public Local toEntity(LocalCreateDTO dto, Distrito distrito) {
        Local local = new Local();
        local.setNombre(dto.getNombre());
        local.setDireccion(dto.getDireccion());
        local.setUrlMapa(dto.getUrlMapa());
        local.setAforoTotal(dto.getAforoTotal());
        local.setDistrito(distrito);
        local.setActivo(true);
        local.setFechaCreacion(LocalDate.now());
        return local;
    }

    public void updateEntity(Local local, LocalCreateDTO dto, Distrito distrito) {
        local.setNombre(dto.getNombre());
        local.setDireccion(dto.getDireccion());
        local.setUrlMapa(dto.getUrlMapa());
        local.setAforoTotal(dto.getAforoTotal());
        if (distrito != null) {
            local.setDistrito(distrito);
        }
        local.setFechaActualizacion(LocalDate.now());
    }
}

