package pe.edu.pucp.fasticket.services;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import sibApi.TransactionalEmailsApi;
import sibModel.CreateSmtpEmail;
import sibModel.SendSmtpEmail;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TransactionalEmailsApi transactionalEmailsApi;

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setup() {

        // Retornar un nuevo MimeMessage por cada invocación
        lenient().when(mailSender.createMimeMessage()).thenAnswer(invocation -> {
            Properties props = new Properties();
            Session session = Session.getInstance(props);
            return new MimeMessage(session);
        });
        // Por defecto, que no haya configs en BD para usar valores por defecto
        lenient().when(configuracionRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void enviarCorreoBienvenida_enviaACliente() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNombres("Juan");
        cliente.setEmail("juan@example.com");

        emailService.enviarCorreoBienvenida(cliente);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        MimeMessage message = captor.getValue();
        assertNotNull(message);
        assertArrayEquals(new InternetAddress[]{new InternetAddress("juan@example.com")},
                message.getRecipients(MimeMessage.RecipientType.TO));
        assertNotNull(message.getSubject());
        assertTrue(message.getSubject().length() > 0);
    }

    @Test
    void enviarCorreoConfirmacionCompra_usaApiBrevo() throws Exception {
        // 1. Datos de prueba
        OrdenCompra orden = new OrdenCompra();
        orden.setIdOrdenCompra(100);
        Cliente cliente = new Cliente();
        cliente.setNombres("Ana");
        cliente.setEmail("ana@example.com");
        orden.setCliente(cliente);

        // Item dummy para que no falle por lista vacía
        ItemCarrito item = new ItemCarrito();
        item.setTipoTicket(new TipoTicket());
        item.getTipoTicket().setEvento(new Evento());
        item.getTipoTicket().getEvento().setNombre("Evento Test");
        orden.getItems().add(item);
        when(transactionalEmailsApi.sendTransacEmail(any())).thenReturn(new CreateSmtpEmail());
        // 2. Ejecutar
        emailService.enviarCorreoConfirmacionCompra(orden);

        // 3. VERIFICACIÓN (Lo que cambió)
        // Ya no verificamos mailSender.send(), sino la API de Brevo
        ArgumentCaptor<SendSmtpEmail> captor = ArgumentCaptor.forClass(SendSmtpEmail.class);
        verify(transactionalEmailsApi, times(1)).sendTransacEmail(captor.capture());

        SendSmtpEmail emailEnviado = captor.getValue();

        // Validar destinatario en el objeto de Brevo
        assertEquals("ana@example.com", emailEnviado.getTo().get(0).getEmail());
        assertTrue(emailEnviado.getHtmlContent().contains("Evento Test"));
    }

    @Test
    void enviarCorreoTransferencia_enviaAEmisorYReceptor() throws Exception {
        Cliente emisor = new Cliente();
        emisor.setNombres("Carlos");
        emisor.setEmail("carlos@example.com");

        Cliente receptor = new Cliente();
        receptor.setNombres("Bea");
        receptor.setEmail("bea@example.com");

        Evento evento = new Evento();
        evento.setNombre("Concierto Pop");

        Ticket ticket = new Ticket();
        ticket.setIdTicket(123);
        ticket.setEvento(evento);

        emailService.enviarCorreoTransferencia(emisor, receptor, ticket);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        List<MimeMessage> sent = captor.getAllValues();
        assertEquals(2, sent.size());

        // Uno al emisor
        boolean toEmisor = Arrays.equals(
                new InternetAddress[]{new InternetAddress("carlos@example.com")},
                sent.get(0).getRecipients(MimeMessage.RecipientType.TO))
                || Arrays.equals(
                new InternetAddress[]{new InternetAddress("carlos@example.com")},
                sent.get(1).getRecipients(MimeMessage.RecipientType.TO));
        // Uno al receptor
        boolean toReceptor = Arrays.equals(
                new InternetAddress[]{new InternetAddress("bea@example.com")},
                sent.get(0).getRecipients(MimeMessage.RecipientType.TO))
                || Arrays.equals(
                new InternetAddress[]{new InternetAddress("bea@example.com")},
                sent.get(1).getRecipients(MimeMessage.RecipientType.TO));

        assertTrue(toEmisor, "Debe enviarse un correo al emisor");
        assertTrue(toReceptor, "Debe enviarse un correo al receptor");
    }

    @Test
    void enviarCorreoCancelacionEvento_usaBCC() throws Exception {
        Evento evento = new Evento();
        evento.setNombre("Tech Summit");

        // Forzar asunto/cuerpo desde configuración para asegurarnos que se substituyen placeholders
        ConfiguracionGlobal asuntoCfg = new ConfiguracionGlobal();
        asuntoCfg.setKey("EMAIL_CANCELACION_ASUNTO");
        asuntoCfg.setValue("Cancelación de evento: ${eventoNombre}");
        when(configuracionRepository.findById("EMAIL_CANCELACION_ASUNTO"))
                .thenReturn(Optional.of(asuntoCfg));

        ConfiguracionGlobal cuerpoCfg = new ConfiguracionGlobal();
        cuerpoCfg.setKey("EMAIL_CANCELACION_CUERPO");
        cuerpoCfg.setValue("El evento ${eventoNombre} ha sido cancelado.");
        when(configuracionRepository.findById("EMAIL_CANCELACION_CUERPO"))
                .thenReturn(Optional.of(cuerpoCfg));

        List<String> afectados = List.of("a1@example.com", "a2@example.com", "a3@example.com");

        emailService.enviarCorreoCancelacionEvento(evento, afectados);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        MimeMessage message = captor.getValue();

        // Debe ir por BCC (sin TO visible)
        assertNull(message.getRecipients(MimeMessage.RecipientType.TO));
        InternetAddress[] bcc = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.BCC);
        assertNotNull(bcc);
        assertEquals(3, bcc.length);
        assertEquals("a1@example.com", bcc[0].getAddress());
        assertEquals("a2@example.com", bcc[1].getAddress());
        assertEquals("a3@example.com", bcc[2].getAddress());
        assertNotNull(message.getSubject());
        assertTrue(message.getSubject().contains("Tech Summit"));
    }

    @Test
    void enviarCorreoResetContrasena_enviaEmail() {
        emailService.enviarCorreoResetContrasena("user@example.com", "Reset", "<b>hola</b>");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}


