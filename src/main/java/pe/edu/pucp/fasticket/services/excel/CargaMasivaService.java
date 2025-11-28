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

        for (ExcelLocalDTO dto : dtos) {
            if (dto.getNombre() == null || dto.getAforoTotal() == null) continue;
            Local local = new Local();
            local.setNombre(dto.getNombre());
            local.setDireccion(dto.getDireccion());
            local.setAforoTotal(dto.getAforoTotal());
            local.setUrlMapa(dto.getUrlMapa());
            local.setActivo(true);
            local.setFechaCreacion(LocalDate.now());
            if (dto.getIdDistrito() != null) {
                Distrito distrito = distritoRepository.findById(dto.getIdDistrito()).orElse(null);
                local.setDistrito(distrito);
            }
            nuevosLocales.add(local);
        }
        localRepository.saveAll(nuevosLocales);
        return "Carga exitosa: " + nuevosLocales.size() + " locales registrados.";
    }

    @Transactional
    public String cargarEventos(MultipartFile file) throws IOException {
        List<ExcelEventoDTO> dtos = Poiji.fromExcel(file.getInputStream(), com.poiji.exception.PoijiExcelType.XLSX, ExcelEventoDTO.class);
        List<Evento> nuevosEventos = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int errores = 0;
        int conflictos = 0;
        for (ExcelEventoDTO dto : dtos) {
            if (dto.getNombre() == null || dto.getIdLocal() == null) continue;
            Evento evento = new Evento();
            evento.setNombre(dto.getNombre());
            evento.setDescripcion(dto.getDescripcion());
            evento.setAforoDisponible(dto.getAforoDisponible());
            evento.setRestricciones(dto.getRestricciones());
            evento.setPoliticasDevolucion(dto.getPoliticasDevolucion());
            evento.setMenoresDeEdadPermitidos(dto.getMenoresDeEdadPermitidos());
            evento.setEstadoEvento(EstadoEvento.PUBLICADO);
            evento.setActivo(true);
            evento.setImagenUrl(dto.getImagenUrl());
            evento.setFechaActualizacion(LocalDate.now());
            evento.setFechaCreacion(LocalDate.now());
            evento.setMaxTransferenciasPermitidas(1);
            evento.setHorasCooldownTransferencia(1);
            try {
                if (dto.getFechaInicio() != null) {
                    evento.setFechaEvento(LocalDate.parse(dto.getFechaInicio(), dateFormatter));
                }
                if (dto.getFechaFin() != null) {
                    evento.setFechaFinEvento(LocalDate.parse(dto.getFechaFin(), dateFormatter));
                } else {
                    evento.setFechaFinEvento(evento.getFechaEvento());
                }
                if (dto.getHoraInicio() != null) evento.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
                if (dto.getHoraFin() != null) evento.setHoraFin(LocalTime.parse(dto.getHoraFin()));
                if (evento.getFechaFinEvento() != null && evento.getFechaEvento() != null &&
                        evento.getFechaFinEvento().isBefore(evento.getFechaEvento())) {
                    log.warn("Fecha fin anterior a fecha inicio en evento '{}'", dto.getNombre());
                    errores++;
                    continue;
                }
            } catch (Exception e) {
                log.warn("Formato de fecha/hora inválido en fila '{}'", dto.getNombre());
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
                Local local = localRepository.findById(dto.getIdLocal())
                        .orElseThrow(() -> new ResourceNotFoundException("Local ID " + dto.getIdLocal() + " no existe"));
                evento.setLocal(local);
            } catch (Exception e) {
                errores++;
                continue;
            }
            LocalDateTime inicioNuevo = LocalDateTime.of(evento.getFechaEvento(), evento.getHoraInicio());
            LocalDateTime finNuevo = LocalDateTime.of(evento.getFechaFinEvento(), evento.getHoraFin());
            boolean cruceBD = eventosRepositorio.existeCruceDeHorario(
                    evento.getLocal().getIdLocal(),
                    inicioNuevo,
                    finNuevo
            );
            boolean cruceMemoria = nuevosEventos.stream().anyMatch(e ->
                    e.getLocal().getIdLocal().equals(evento.getLocal().getIdLocal()) &&
                            !e.getEstadoEvento().name().equals("CANCELADO") &&
                            (e.getFechaEvento().isBefore(evento.getFechaFinEvento().plusDays(1)) && e.getFechaFinEvento().isAfter(evento.getFechaEvento().minusDays(1))) &&
                            (e.getHoraInicio().isBefore(evento.getHoraFin()) && e.getHoraFin().isAfter(evento.getHoraInicio()))
            );
            if (cruceBD||cruceMemoria) {
                log.warn("CONFLICTO: El evento '{}' se cruza con otro en el mismo local.", dto.getNombre());
                conflictos++;
                continue;
            }
            nuevosEventos.add(evento);
        }
        eventosRepositorio.saveAll(nuevosEventos);
        return String.format("Carga finalizada: %d eventos creados. (Errores formato: %d, Conflictos horario: %d)",
                nuevosEventos.size(), errores, conflictos);
    }
}