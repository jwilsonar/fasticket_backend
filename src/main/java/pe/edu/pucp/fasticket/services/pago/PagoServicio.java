package pe.edu.pucp.fasticket.services.pago;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.dto.pago.ComprobanteDTO;
import pe.edu.pucp.fasticket.dto.pago.PagoResumenDTO;
import pe.edu.pucp.fasticket.dto.pago.RegistrarPagoDTO;
import pe.edu.pucp.fasticket.model.pago.Boleta;
import pe.edu.pucp.fasticket.model.pago.ComprobantePago;
import pe.edu.pucp.fasticket.model.pago.EstadoPago;
import pe.edu.pucp.fasticket.model.pago.Pago;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.pago.BoletaRepositorio;
import pe.edu.pucp.fasticket.repository.pago.ComprobanteDePagoRepositorio;
import pe.edu.pucp.fasticket.repository.pago.PagoRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagoServicio {

    @Autowired
    private PagoRepositorio pagoRepository;
    @Autowired
    private OrdenCompraRepositorio ordenRepository;
    @Autowired
    private OrdenServicio ordenServicio;
    @Autowired
    private ComprobanteDePagoRepositorio comprobantePagoRepositorio;
    @Autowired
    private PersonasRepositorio personaRepositorio;
    @Autowired
    private BoletaRepositorio boletaRepositorio;

    public ComprobanteDTO registrarPagoFinal(RegistrarPagoDTO dto) {
        var orden = ordenRepository.findById(dto.getIdOrden())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        if (dto.getNumeroTarjeta() == null || dto.getNumeroTarjeta().length() < 4) {
            throw new RuntimeException("Número de tarjeta inválido");
        }
        var usuario = personaRepositorio.findById(orden.getUsuarioCreacion()).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        String ultimos4 = dto.getNumeroTarjeta().substring(dto.getNumeroTarjeta().length() - 4);
        Pago pago = new Pago();
        pago.setMetodo("Tarjeta (" + ultimos4 + ")");
        pago.setMonto(dto.getMonto());
        pago.setEstado(EstadoPago.APROBADO);
        pago.setFechaPago(LocalDate.now());
        pago.setActivo(true);
        pago.setFechaCreacion(LocalDate.now());
        pago.setUsuarioCreacion(orden.getUsuarioCreacion());
        pago.setOrdenCompra(orden);
        pagoRepository.save(pago);
        ordenServicio.confirmarPagoOrden(orden.getIdOrdenCompra());
        ComprobantePago comprobante = new ComprobantePago();
        comprobante.setNumeroSerie(String.format("CP-%05d", pago.getIdPago()));
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setTotal(dto.getMonto());
        comprobante.setActivo(true);
        comprobante.setUsuarioCreacion(orden.getUsuarioCreacion());
        comprobante.setFechaCreacion(LocalDate.now());
        comprobante.setDni(usuario.getDocIdentidad());
        comprobante.setPago(pago);
        comprobantePagoRepositorio.save(comprobante);
        Boleta boleta = new Boleta();
        boleta.setDni(usuario.getDocIdentidad());
        boleta.setNombreCliente(usuario.getNombres() + " " + usuario.getApellidos());
        boleta.setComprobantePago(comprobante);
        boletaRepositorio.save(boleta);
        OrdenResumenDTO ordenDTO = new OrdenResumenDTO(orden);
        List<DatosAsistenteDTO> asistentes = orden.getItems().stream().flatMap(item -> item.getTickets().stream()).map(e -> new DatosAsistenteDTO(
                        e.getTipoDocumentoAsistente(),
                        e.getDocumentoAsistente(),
                        e.getNombreAsistente(),
                        e.getApellidoAsistente()
                ))
                .collect(Collectors.toList());
        return new ComprobanteDTO(
                comprobante.getNumeroSerie(),
                "ORD-" + orden.getIdOrdenCompra(),
                ordenDTO.getNombreEvento(),
                ordenDTO.getNombreLocal(),
                ordenDTO.getFecha(),
                ordenDTO.getHora(),
                orden.getFechaOrden(),
                orden.getFechaOrden().atStartOfDay().toLocalTime(),
                ordenDTO.getItems().stream().mapToInt(ItemResumenDTO::getCantidad).sum(),
                ordenDTO.getItems(),
                asistentes,
                ordenDTO.getTotal(),
                pago.getMetodo(),
                "XXXX-XXXX-XXXX-" + ultimos4,
                pago.getEstado().toString(),
                comprobante.getFechaEmision()
        );
    }
}
