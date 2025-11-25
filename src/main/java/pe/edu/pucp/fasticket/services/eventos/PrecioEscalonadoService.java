package pe.edu.pucp.fasticket.services.eventos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.eventos.PrecioEscalonadoDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.model.eventos.PrecioEscalonado;
import pe.edu.pucp.fasticket.repository.eventos.PrecioEscalonadoRepositorio;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrecioEscalonadoService {
    private final PrecioEscalonadoRepositorio precioRepositorio;

    @Transactional
    public PrecioEscalonado crear(PrecioEscalonadoDTO dto){
        PrecioEscalonado precioEscalonado = new PrecioEscalonado();
        precioEscalonado.setNombreEtapa(dto.getNombreEtapa());
        precioEscalonado.setFechaInicio(dto.getFechaInicio());
        precioEscalonado.setFechaFin(dto.getFechaFin());
        precioEscalonado.setActivo(dto.getActivo());

        if(precioEscalonado.getFechaFin().isBefore(precioEscalonado.getFechaInicio())){
            throw new BusinessException("La fecha final no puede ser antes de la fecha inicial");
        }

        return precioRepositorio.save(precioEscalonado);
    }
}
