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
import pe.edu.pucp.fasticket.model.eventos.*; // Evento, Local, EstadoEvento, TipoEvento
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
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
            local.setImagenUrl(dto.getImagenUrl());
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
            evento.setFechaCreacion(LocalDate.now());
            evento.setMaxTransferenciasPermitidas(1);
            evento.setHorasCooldownTransferencia(12);
            try {
                if (dto.getFechaEvento() != null) evento.setFechaEvento(LocalDate.parse(dto.getFechaEvento()));
                if (dto.getHoraInicio() != null) evento.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
                if (dto.getHoraFin() != null) evento.setHoraFin(LocalTime.parse(dto.getHoraFin()));
            } catch (Exception e) {
                log.warn("Fecha inválida en evento '{}', saltando.", dto.getNombre());
                continue;
            }

            if (dto.getTipoEvento() != null) {
                try {
                    evento.setTipoEvento(TipoEvento.valueOf(dto.getTipoEvento().toUpperCase().trim()));
                } catch (IllegalArgumentException e) {
                    log.warn("TipoEvento desconocido: {}", dto.getTipoEvento());
                }
            }
            Local local = localRepository.findById(dto.getIdLocal())
                    .orElseThrow(() -> new ResourceNotFoundException("Local ID " + dto.getIdLocal() + " no existe"));
            evento.setLocal(local);

            nuevosEventos.add(evento);
        }
        eventosRepositorio.saveAll(nuevosEventos);
        return "Carga exitosa: " + nuevosEventos.size() + " eventos registrados como BORRADOR.";
    }
}