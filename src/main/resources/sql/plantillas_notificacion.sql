-- Tabla de plantillas de notificación
CREATE TABLE IF NOT EXISTS plantillas_notificacion (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(64) NOT NULL UNIQUE,
    asunto VARCHAR(255) NOT NULL,
    html TEXT NOT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Asegurar que el CHECK constraint admita todos los valores actuales
ALTER TABLE plantillas_notificacion
  DROP CONSTRAINT IF EXISTS plantillas_notificacion_tipo_check;

ALTER TABLE plantillas_notificacion
  ADD CONSTRAINT plantillas_notificacion_tipo_check CHECK (
    tipo IN (
      'CAMBIO_CONTRASENA',
      'VERIFICAR_CUENTA',
      'CONFIRMACION_COMPRA',
      'RECORDATORIO_EVENTO_48H',
      'TRANSFERENCIA_OK',
      'TRANSFERENCIA_FALLIDA',
      'CONFIRMACION_RECUPERACION_CONTRASENA',
      'OLVIDO_CONTRASENA_CODIGO'
    )
  );

-- Semillas (UPSERT) para asegurar contenido correcto aunque ya existan filas
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('CAMBIO_CONTRASENA', 'Tu contraseña ha sido actualizada', '<h2>Hola {{nombre}}</h2><p>Tu contraseña fue cambiada correctamente.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('VERIFICAR_CUENTA', 'Verifica tu cuenta', '<h2>Hola {{nombre}}</h2><p>Verifica tu cuenta haciendo clic <a href="{{linkVerificacion}}">aquí</a>.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('CONFIRMACION_COMPRA', 'Confirmación de compra', '<h2>Hola {{nombre}}</h2><p>Tu compra fue confirmada. Orden #{{idOrden}}. Total S/ {{total}}.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('RECORDATORIO_EVENTO_48H', 'Recordatorio: tu evento inicia pronto', '<h2>Hola {{nombre}}</h2><p>Te recordamos que el evento {{eventoNombre}} inicia en 48 horas.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('TRANSFERENCIA_OK', 'Transferencia realizada', '<h2>Hola {{nombre}}</h2><p>La transferencia del ticket {{ticketId}} para el evento {{eventoNombre}} fue exitosa.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('TRANSFERENCIA_FALLIDA', 'Transferencia no realizada', '<h2>Hola {{nombre}}</h2><p>No se pudo realizar la transferencia del ticket {{ticketId}}. Motivo: {{motivo}}.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('CONFIRMACION_RECUPERACION_CONTRASENA', 'Recuperación de contraseña', '<h2>Hola {{nombre}}</h2><p>Para recuperar tu contraseña ingresa <a href="{{linkRecuperacion}}">aquí</a>.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('OLVIDO_CONTRASENA_CODIGO', 'Tu código de verificación', '<h2>Hola {{nombre}}</h2><p>Usa este código para continuar con el proceso de recuperación:</p><p style="font-size:24px;letter-spacing:4px"><strong>{{codigo}}</strong></p><p>Este código expira en 10 minutos.</p>', TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();


