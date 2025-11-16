package pe.edu.pucp.fasticket.services.notificaciones;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationManager {
	private final List<NotificationChannel> channels;

	public void notifyAllChannels(NotificationRequest req) {
		for (NotificationChannel c : channels) {
			c.send(req);
		}
	}
}


