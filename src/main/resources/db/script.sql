-- Departamentos
INSERT INTO departamento (id_departamento, nombre, activo) VALUES
(1, 'Lima', true),
(2, 'Arequipa', true),
(3, 'Cusco', true),
(4, 'La Libertad', true),
(5, 'Piura', true)
ON CONFLICT (id_departamento) DO NOTHING;

-- Provincias de Lima
INSERT INTO provincia (id_provincia, nombre, activo, id_departamento) VALUES
(1, 'Lima', true, 1),
(2, 'Callao', true, 1),
(3, 'Huaral', true, 1),
(4, 'Barranca', true, 1),
(5, 'Cañete', true, 1)
ON CONFLICT (id_provincia) DO NOTHING;

-- Provincias de otros departamentos
INSERT INTO provincia (id_provincia, nombre, activo, id_departamento) VALUES
(6, 'Arequipa', true, 2),
(7, 'Cusco', true, 3),
(8, 'Trujillo', true, 4),
(9, 'Piura', true, 5)
ON CONFLICT (id_provincia) DO NOTHING;

-- Distritos de Lima
INSERT INTO distrito (id_distrito, nombre, activo, id_provincia) VALUES
(1, 'Miraflores', true, 1),
(2, 'San Isidro', true, 1),
(3, 'Barranco', true, 1),
(4, 'Surco', true, 1),
(5, 'San Borja', true, 1),
(6, 'La Molina', true, 1),
(7, 'Cercado de Lima', true, 1),
(8, 'Jesús María', true, 1),
(9, 'Lince', true, 1),
(10, 'Pueblo Libre', true, 1),
(11, 'Magdalena', true, 1),
(12, 'San Miguel', true, 1),
(13, 'Bellavista', true, 2),
(14, 'Callao', true, 2),
(15, 'La Perla', true, 2)
ON CONFLICT (id_distrito) DO NOTHING;

-- ============================================
-- PLANTILLAS DE NOTIFICACIÓN POR EMAIL
-- ============================================
-- Crear tabla si no existe
CREATE TABLE IF NOT EXISTS plantillas_notificacion (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(64) NOT NULL UNIQUE,
    asunto VARCHAR(255) NOT NULL,
    html TEXT NOT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Asegurar constraint de tipos válidos
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

-- Plantilla: VERIFICAR_CUENTA
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('VERIFICAR_CUENTA', 
 '¡Bienvenido a Fasticket! Verifica tu cuenta',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verifica tu cuenta</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">¡Bienvenido a Fasticket!</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                Gracias por registrarte en Fasticket. Para completar tu registro y activar tu cuenta, 
                                necesitamos verificar tu dirección de correo electrónico.
                            </p>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                Haz clic en el botón siguiente para verificar tu cuenta:
                            </p>
                            <table width="100%" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td align="center" style="padding: 20px 0;">
                                        <a href="{{linkVerificacion}}" style="display: inline-block; padding: 14px 40px; background-color: #667eea; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;">Verificar mi cuenta</a>
                                    </td>
                                </tr>
                            </table>
                            <p style="color: #999999; font-size: 14px; line-height: 1.6; margin: 30px 0 0 0;">
                                Si el botón no funciona, copia y pega el siguiente enlace en tu navegador:<br>
                                <a href="{{linkVerificacion}}" style="color: #667eea; word-break: break-all;">{{linkVerificacion}}</a>
                            </p>
                            <p style="color: #999999; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                <strong>Importante:</strong> Este enlace expira en 24 horas. Si no verificas tu cuenta en ese tiempo, 
                                deberás solicitar un nuevo enlace de verificación.
                            </p>
                            <p style="color: #999999; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                Si no te registraste en Fasticket, puedes ignorar este correo de forma segura.
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

-- Plantilla: OLVIDO_CONTRASENA_CODIGO
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('OLVIDO_CONTRASENA_CODIGO',
 'Tu código de verificación - Fasticket',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Código de verificación</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">Recuperación de Contraseña</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                Recibimos una solicitud para restablecer la contraseña de tu cuenta en Fasticket.
                            </p>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                Usa el siguiente código de verificación para continuar:
                            </p>
                            <table width="100%" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td align="center" style="padding: 20px 0;">
                                        <div style="background-color: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; display: inline-block;">
                                            <p style="color: #333333; font-size: 36px; font-weight: bold; letter-spacing: 8px; margin: 0; font-family: ''Courier New'', monospace;">{{codigo}}</p>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                            <p style="color: #ff6b6b; font-size: 14px; line-height: 1.6; margin: 30px 0 0 0; font-weight: bold;">
                                ⏰ Este código expira en 10 minutos.
                            </p>
                            <p style="color: #999999; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                Si no solicitaste este código, puedes ignorar este correo de forma segura. 
                                Tu cuenta permanecerá segura.
                            </p>
                            <p style="color: #999999; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">
                                <strong>Consejo de seguridad:</strong> Nunca compartas este código con nadie. 
                                El equipo de Fasticket nunca te pedirá tu código de verificación.
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

-- Plantilla: CONFIRMACION_RECUPERACION_CONTRASENA
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('CONFIRMACION_RECUPERACION_CONTRASENA',
 'Tu contraseña ha sido restablecida - Fasticket',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Contraseña restablecida</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #51cf66 0%, #40c057 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">✓ Contraseña Restablecida</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                Tu contraseña ha sido restablecida exitosamente. Ahora puedes iniciar sesión con tu nueva contraseña.
                            </p>
                            <div style="background-color: #f8f9fa; border-left: 4px solid #51cf66; padding: 15px; margin: 20px 0;">
                                <p style="color: #333333; font-size: 14px; margin: 0; font-weight: bold;">Información importante:</p>
                                <ul style="color: #666666; font-size: 14px; margin: 10px 0 0 0; padding-left: 20px;">
                                    <li>Tu cuenta ha sido desbloqueada automáticamente</li>
                                    <li>Los intentos fallidos de inicio de sesión han sido reseteados</li>
                                    <li>Puedes iniciar sesión inmediatamente con tu nueva contraseña</li>
                                </ul>
                            </div>
                            <p style="color: #ff6b6b; font-size: 14px; line-height: 1.6; margin: 30px 0 0 0; font-weight: bold;">
                                ⚠️ Si no realizaste este cambio, contacta inmediatamente con nuestro equipo de soporte.
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

-- Plantilla: CAMBIO_CONTRASENA
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('CAMBIO_CONTRASENA',
 'Tu contraseña ha sido actualizada - Fasticket',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Contraseña actualizada</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #51cf66 0%, #40c057 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">✓ Contraseña Actualizada</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                Tu contraseña ha sido cambiada exitosamente. Tu cuenta ahora está protegida con tu nueva contraseña.
                            </p>
                            <p style="color: #ff6b6b; font-size: 14px; line-height: 1.6; margin: 30px 0 0 0; font-weight: bold;">
                                ⚠️ Si no realizaste este cambio, contacta inmediatamente con nuestro equipo de soporte.
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

-- Plantilla: CONFIRMACION_COMPRA
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('CONFIRMACION_COMPRA',
 'Confirmación de compra - Fasticket',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Confirmación de compra</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">¡Compra Confirmada!</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                Tu compra ha sido confirmada exitosamente. Aquí están los detalles:
                            </p>
                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;">
                                <p style="color: #333333; font-size: 16px; margin: 0 0 10px 0;"><strong>Orden #{{idOrden}}</strong></p>
                                <p style="color: #666666; font-size: 18px; margin: 10px 0 0 0;"><strong>Total: S/ {{total}}</strong></p>
                            </div>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="{{pdfUrl}}" target="_blank" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; text-decoration: none; padding: 15px 30px; border-radius: 6px; font-size: 16px; font-weight: bold; box-shadow: 0 2px 4px rgba(0,0,0,0.2);">
                                    📄 Descargar Comprobante PDF
                                </a>
                            </div>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 20px 0 0 0;">
                                Gracias por tu compra. Te enviaremos más detalles sobre tus tickets próximamente.
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

-- Plantilla: RECORDATORIO_EVENTO_48H
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('RECORDATORIO_EVENTO_48H',
 'Recordatorio: Tu evento inicia pronto - Fasticket',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recordatorio de evento</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #ffd43b 0%, #fcc419 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #333333; margin: 0; font-size: 28px; font-weight: bold;">⏰ Recordatorio de Evento</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                Te recordamos que el evento <strong>{{eventoNombre}}</strong> inicia en 48 horas.
                            </p>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                ¡No olvides asistir y disfrutar de esta experiencia única!
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

-- Plantilla: TRANSFERENCIA_OK
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('TRANSFERENCIA_OK',
 'Transferencia de ticket exitosa - Fasticket',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Transferencia exitosa</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #51cf66 0%, #40c057 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">✓ Transferencia Exitosa</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                La transferencia del ticket <strong>#{{ticketId}}</strong> para el evento <strong>{{eventoNombre}}</strong> fue realizada exitosamente.
                            </p>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                El nuevo propietario del ticket recibirá la confirmación correspondiente.
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();

-- Plantilla: TRANSFERENCIA_FALLIDA
INSERT INTO plantillas_notificacion (tipo, asunto, html, habilitado, actualizado_en) VALUES
('TRANSFERENCIA_FALLIDA',
 'Transferencia de ticket no realizada - Fasticket',
 '<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Transferencia fallida</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;">✗ Transferencia No Realizada</h1>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola {{nombre}},</h2>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                Lamentamos informarte que no se pudo realizar la transferencia del ticket <strong>#{{ticketId}}</strong> para el evento <strong>{{eventoNombre}}</strong>.
                            </p>
                            <div style="background-color: #fff5f5; border-left: 4px solid #ff6b6b; padding: 15px; margin: 20px 0;">
                                <p style="color: #333333; font-size: 14px; margin: 0; font-weight: bold;">Motivo:</p>
                                <p style="color: #666666; font-size: 14px; margin: 10px 0 0 0;">{{motivo}}</p>
                            </div>
                            <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 20px 0 0 0;">
                                Si tienes alguna pregunta o necesitas asistencia, por favor contacta con nuestro equipo de soporte.
                            </p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; font-size: 12px; margin: 0 0 10px 0;">
                                © 2025 Fasticket. Todos los derechos reservados.
                            </p>
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                Este es un correo automático, por favor no respondas.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
 TRUE, NOW())
ON CONFLICT (tipo) DO UPDATE SET asunto = EXCLUDED.asunto, html = EXCLUDED.html, habilitado = EXCLUDED.habilitado, actualizado_en = NOW();


INSERT INTO persona (
    id_persona, activo, apellidos, contrasenia, direccion,
    doc_identidad, email, failed_attempts,
    fecha_creacion, fecha_nacimiento,
    nombres, rol, telefono, tipo_documento,
    id_distrito
) VALUES
-- Cliente 14
(14, true, 'Gómez Torres',
 '$2a$10$/8Qpw6CGx81zvqcdr.QskO9xDmbqNc1t2Ehq3AF4hgc92syA.7ub2',
 'Av. Los Próceres 123',
 '99887766', 'felipe.gomez@fasticket.com',
 0,
 CURRENT_DATE, '1993-07-10',
 'Felipe', 'CLIENTE', '987654350', 'DNI',
 5),

-- Cliente 15
(15, true, 'Ramírez Soto',
 '$2a$10$/8Qpw6CGx81zvqcdr.QskO9xDmbqNc1t2Ehq3AF4hgc92syA.7ub2',
 'Calle Los Eucaliptos 800',
 '88776655', 'lucia.ramirez@fasticket.com',
 0,
 CURRENT_DATE, '1998-12-01',
 'Lucía', 'CLIENTE', '987654351', 'DNI',
 6),

-- Admin 16
(16, true, 'Administrador',
 '$2a$10$/8Qpw6CGx81zvqcdr.QskO9xDmbqNc1t2Ehq3AF4hgc92syA.7ub2',
 'Av. Universitaria 1801',
 '77665544', 'superadmin@pucp.edu.pe',
 0,
 CURRENT_DATE, '1980-01-01',
 'Super', 'ADMINISTRADOR', '987654352', 'DNI',
 5);

INSERT INTO cliente (id_persona, nivel, puntos_acumulados) VALUES
(14, 'BRONCE', 0),
(15, 'ORO', 500);

INSERT INTO administrador (id_persona, cargo) VALUES
(16, 'Super Administrador General');
