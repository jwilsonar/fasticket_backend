package pe.edu.pucp.fasticket.services.excel;

import com.poiji.bind.Poiji;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.fasticket.dto.excel.ExcelEventoDTO;
import pe.edu.pucp.fasticket.dto.excel.ExcelLocalDTO;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.*;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CargaMasivaService {

    private final LocalesRepositorio localRepository;
    private final EventosRepositorio eventosRepositorio;
    private final DistritoRepository distritoRepository;

    @Transactional
    public String cargarLocales(MultipartFile file) throws IOException {
        List<ExcelLocalDTO> dtos = Poiji.fromExcel(file.getInputStream(), com.poiji.exception.PoijiExcelType.XLSX, ExcelLocalDTO.class);
        List<Local> nuevosLocales = new ArrayList<>();
        int errores = 0;

        for (ExcelLocalDTO dto : dtos) {
            if (dto.getNombre() == null || dto.getAforoTotal() == null ||
                    dto.getDepartamento() == null || dto.getProvincia() == null || dto.getDistrito() == null) {
                errores++;
                continue;
            }
            Local local = new Local();
            local.setNombre(dto.getNombre());
            local.setDireccion(dto.getDireccion());
            local.setAforoTotal(dto.getAforoTotal());
            local.setActivo(true);
            local.setFechaCreacion(LocalDate.now());
            local.setFechaActualizacion(LocalDate.now());
            try {
                Distrito distrito = distritoRepository.buscarPorNombres(
                        dto.getDepartamento().trim(),
                        dto.getProvincia().trim(),
                        dto.getDistrito().trim()
                ).orElseThrow(() -> new ResourceNotFoundException("Ubicación no encontrada"));

                local.setDistrito(distrito);
            } catch (Exception e) {
                log.warn("No se encontró el distrito: {} / {} / {}. Saltando local '{}'",
                        dto.getDepartamento(), dto.getProvincia(), dto.getDistrito(), dto.getNombre());
                errores++;
                continue;
            }
            nuevosLocales.add(local);
        }

        localRepository.saveAll(nuevosLocales);
        return String.format("Carga finalizada: %d locales registrados. (Errores/No encontrados: %d)",
                nuevosLocales.size(), errores);
    }

    @Transactional
    public String cargarEventos(MultipartFile file) throws IOException {
        List<ExcelEventoDTO> dtos = Poiji.fromExcel(file.getInputStream(), com.poiji.exception.PoijiExcelType.XLSX, ExcelEventoDTO.class);
        List<Evento> nuevosEventos = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        int errores = 0;
        int conflictos = 0;

        for (ExcelEventoDTO dto : dtos) {
            if (dto.getNombre() == null || dto.getNombreLocal() == null || dto.getFechaInicio() == null || dto.getHoraInicio() == null) {
                log.warn("Fila saltada por datos incompletos: {}", dto);
                errores++;
                continue;
            }

            Evento evento = new Evento();
            evento.setNombre(dto.getNombre());
            evento.setDescripcion(dto.getDescripcion());
            evento.setAforoDisponible(dto.getAforoDisponible());
            evento.setRestricciones(dto.getRestricciones());
            evento.setPoliticasDevolucion(dto.getPoliticasDevolucion());
            evento.setMenoresDeEdadPermitidos(dto.getMenoresDeEdadPermitidos());

            evento.setEstadoEvento(EstadoEvento.PUBLICADO);
            evento.setActivo(true);
            evento.setFechaCreacion(LocalDate.now());
            evento.setMaxTransferenciasPermitidas(1);
            evento.setHorasCooldownTransferencia(12);

            try {
                LocalDate fInicio = LocalDate.parse(dto.getFechaInicio(), dateFormatter);
                evento.setFechaEvento(fInicio);

                if (dto.getFechaFin() != null) {
                    evento.setFechaFinEvento(LocalDate.parse(dto.getFechaFin(), dateFormatter));
                } else {
                    evento.setFechaFinEvento(fInicio);
                }

                evento.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
                if (dto.getHoraFin() != null) {
                    evento.setHoraFin(LocalTime.parse(dto.getHoraFin()));
                } else {
                    evento.setHoraFin(evento.getHoraInicio().plusHours(2)); // Default duración
                }
                if (evento.getFechaFinEvento().isBefore(evento.getFechaEvento())) {
                    log.warn("Fecha fin anterior a inicio en evento '{}'", dto.getNombre());
                    errores++;
                    continue;
                }
            } catch (Exception e) {
                log.warn("Error formato fecha/hora en '{}': {}", dto.getNombre(), e.getMessage());
                errores++;
                continue;
            }
            if (dto.getTipoEvento() != null) {
                try {
                    evento.setTipoEvento(TipoEvento.valueOf(dto.getTipoEvento().toUpperCase().trim()));
                } catch (IllegalArgumentException e) {
                    log.warn("TipoEvento desconocido: {}", dto.getTipoEvento());
                }
            }
            try {
                Local local = localRepository.findByNombreIgnoreCaseAndActivoTrue(dto.getNombreLocal().trim())
                        .orElseThrow(() -> new ResourceNotFoundException("No existe local con nombre: " + dto.getNombreLocal()));
                evento.setLocal(local);
            } catch (Exception e) {
                log.error("Error vinculando local '{}': {}", dto.getNombreLocal(), e.getMessage());
                errores++;
                continue;
            }

            LocalDateTime inicioNuevo = LocalDateTime.of(evento.getFechaEvento(), evento.getHoraInicio());
            LocalDateTime finNuevo = LocalDateTime.of(evento.getFechaFinEvento(), evento.getHoraFin());

            boolean hayCruce = eventosRepositorio.existeCruceDeHorario(
                    evento.getLocal().getIdLocal(),
                    inicioNuevo,
                    finNuevo
            );

            boolean cruceMemoria = nuevosEventos.stream().anyMatch(e ->
                    e.getLocal().getIdLocal().equals(evento.getLocal().getIdLocal()) &&
                            LocalDateTime.of(e.getFechaEvento(), e.getHoraInicio()).isBefore(finNuevo) &&
                            LocalDateTime.of(e.getFechaFinEvento(), e.getHoraFin()).isAfter(inicioNuevo)
            );

            if (hayCruce || cruceMemoria) {
                log.warn("CONFLICTO: El evento '{}' se cruza con otro en {}", dto.getNombre(), evento.getLocal().getNombre());
                conflictos++;
                continue;
            }

            nuevosEventos.add(evento);
        }

        eventosRepositorio.saveAll(nuevosEventos);

        return String.format("Carga finalizada: %d eventos creados. (Errores: %d, Conflictos: %d)",
                nuevosEventos.size(), errores, conflictos);
    }
}