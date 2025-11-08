package pe.edu.pucp.fasticket.services.geografia;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.geografia.DepartamentoDTO;
import pe.edu.pucp.fasticket.dto.geografia.DistritoDTO;
import pe.edu.pucp.fasticket.dto.geografia.ProvinciaDTO;
import pe.edu.pucp.fasticket.repository.geografia.DepartamentoRepository;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.geografia.ProvinciaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeografiaService {
    private final DepartamentoRepository departamentoRepository;
    private final ProvinciaRepository provinciaRepository;
    private final DistritoRepository distritoRepository;

    // Lista todos los departamentos, ordenados por nombre.
    public List<DepartamentoDTO> listarDepartamentos() {
        return departamentoRepository.findAll(Sort.by("nombre"))
                .stream().map(DepartamentoDTO::new).collect(Collectors.toList());
    }

    //Lista todas las provincias (ciudades) de un departamento específico.

    public List<ProvinciaDTO> listarProvinciasPorDepartamento(Integer idDepartamento) {
        return provinciaRepository.findByDepartamento_IdDepartamentoOrderByNombreAsc(idDepartamento)
                .stream().map(ProvinciaDTO::new).collect(Collectors.toList());
    }

    //Lista todos los distritos de una provincia específica.

    public List<DistritoDTO> listarDistritosPorProvincia(Integer idProvincia) {
        return distritoRepository.findByProvincia_IdProvinciaOrderByNombreAsc(idProvincia)
                .stream().map(DistritoDTO::new).collect(Collectors.toList());
    }
}
