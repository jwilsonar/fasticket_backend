-- ============================================================================
-- SCRIPT SEEDER PARA FASTICKET
-- Datos de prueba para la base de datos
-- ============================================================================

-- Deshabilitar restricciones temporalmente para facilitar la carga
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. GEOGRAFÍA: DEPARTAMENTOS, PROVINCIAS Y DISTRITOS
-- ============================================================================

-- Departamentos
INSERT INTO departamento (id_departamento, nombre, activo) VALUES
(1, 'Lima', true),
(2, 'Arequipa', true),
(3, 'Cusco', true),
(4, 'La Libertad', true),
(5, 'Piura', true);

-- Provincias de Lima
INSERT INTO provincia (id_provincia, nombre, activo, idDepartamento) VALUES
(1, 'Lima', true, 1),
(2, 'Callao', true, 1),
(3, 'Huaral', true, 1),
(4, 'Barranca', true, 1),
(5, 'Cañete', true, 1);

-- Provincias de otros departamentos
INSERT INTO provincia (id_provincia, nombre, activo, idDepartamento) VALUES
(6, 'Arequipa', true, 2),
(7, 'Cusco', true, 3),
(8, 'Trujillo', true, 4),
(9, 'Piura', true, 5);

-- Distritos de Lima
INSERT INTO distrito (id_distrito, nombre, activo, idProvincia) VALUES
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
(15, 'La Perla', true, 2);

-- ============================================================================
-- 2. USUARIOS: PERSONAS, CLIENTES Y ADMINISTRADORES
-- ============================================================================

-- Contraseña encriptada (BCrypt): "password123" para todos
-- Hash: $2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i

-- Personas (Base)
INSERT INTO Persona (idPersona, tipoDocumento, docIdentidad, nombres, apellidos, telefono, email, fechaNacimiento, direccion, contrasenia, rol, activo, fechaCreacion, idDistrito) VALUES
(1, 'DNI', '72345678', 'Juan', 'Pérez García', '987654321', 'juan.perez@fasticket.com', '1990-05-15', 'Av. Larco 1234', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-15', 1),
(2, 'DNI', '73456789', 'María', 'González López', '987654322', 'maria.gonzalez@fasticket.com', '1992-08-20', 'Calle Los Pinos 567', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-16', 2),
(3, 'DNI', '74567890', 'Carlos', 'Rodríguez Mendoza', '987654323', 'carlos.rodriguez@fasticket.com', '1988-03-10', 'Jr. Libertad 890', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-17', 3),
(4, 'DNI', '75678901', 'Ana', 'Martínez Silva', '987654324', 'ana.martinez@fasticket.com', '1995-11-25', 'Av. Primavera 234', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-18', 4),
(5, 'DNI', '76789012', 'Luis', 'Sánchez Torres', '987654325', 'luis.sanchez@fasticket.com', '1991-07-30', 'Calle Las Flores 456', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-19', 5),
(6, 'DNI', '77890123', 'Pedro', 'Ramírez Castro', '987654326', 'pedro.ramirez@fasticket.com', '1993-02-14', 'Av. Javier Prado 789', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-20', 6),
(7, 'DNI', '78901234', 'Sofia', 'Vargas Morales', '987654327', 'sofia.vargas@fasticket.com', '1994-09-05', 'Jr. Washington 321', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-21', 7),
(8, 'DNI', '79012345', 'Diego', 'Fernández Rojas', '987654328', 'diego.fernandez@fasticket.com', '1989-12-18', 'Av. Arequipa 654', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-22', 8),
(9, 'DNI', '70123456', 'Valeria', 'Jiménez Paredes', '987654329', 'valeria.jimenez@fasticket.com', '1996-06-22', 'Calle Schell 987', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-23', 9),
(10, 'DNI', '71234567', 'Roberto', 'Cruz Delgado', '987654330', 'roberto.cruz@fasticket.com', '1987-04-08', 'Av. Benavides 432', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-24', 10),
(11, 'DNI', '45678901', 'Administrador', 'Principal', '987654331', 'admin@pucp.edu.pe', '1985-01-01', 'Av. Universitaria 1801', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'ADMINISTRADOR', true, '2024-01-01', 5),
(12, 'DNI', '45678902', 'Jorge', 'Administrador Eventos', '987654332', 'jorge.admin@pucp.edu.pe', '1986-02-15', 'Av. Universitaria 1801', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'ADMINISTRADOR', true, '2024-01-02', 5),
(13, 'DNI', '45678903', 'Patricia', 'Administrador Ventas', '987654333', 'patricia.admin@pucp.edu.pe', '1987-03-20', 'Av. Universitaria 1801', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'ADMINISTRADOR', true, '2024-01-03', 5);

-- Clientes (hereda de Persona)
INSERT INTO Cliente (idPersona, nivel, puntosAcumulados) VALUES
(1, 'CLASICO', 0),
(2, 'CLASICO', 150),
(3, 'PLUS', 350),
(4, 'CLASICO', 50),
(5, 'PREMIUM', 850),
(6, 'PLUS', 420),
(7, 'CLASICO', 180),
(8, 'PREMIUM', 1200),
(9, 'PLUS', 520),
(10, 'CLASICO', 90);

-- Administradores (hereda de Persona)
INSERT INTO Administrador (idPersona, cargo) VALUES
(11, 'Administrador General'),
(12, 'Gestor de Eventos'),
(13, 'Supervisor de Ventas');

-- ============================================================================
-- 3. LOCALES
-- ============================================================================

INSERT INTO Local (idLocal, nombre, direccion, aforoTotal, activo, usuarioCreacion, fechaCreacion, idDistrito) VALUES
(1, 'Estadio Nacional', 'Av. Paseo de la República s/n', 45000, true, 11, '2024-01-01', 7),
(2, 'Arena Perú', 'Av. Javier Prado Este 8212', 5000, true, 11, '2024-01-01', 4),
(3, 'Explanada Costa Verde', 'Malecón de la Marina', 15000, true, 11, '2024-01-02', 1),
(4, 'Centro de Convenciones Lima', 'Av. Alfredo Benavides 1555', 3000, true, 11, '2024-01-02', 1),
(5, 'Parque de la Exposición', 'Av. 28 de Julio', 8000, true, 11, '2024-01-03', 7),
(6, 'Teatro Municipal', 'Jr. Ica 377', 1200, true, 11, '2024-01-03', 7),
(7, 'Jockey Club del Perú', 'Av. El Derby s/n', 10000, true, 11, '2024-01-04', 4),
(8, 'Centro Cultural PUCP', 'Av. Camino Real 1075', 800, true, 11, '2024-01-04', 5),
(9, 'Estadio Monumental', 'Av. Tomás Valle', 80000, true, 11, '2024-01-05', 7),
(10, 'Barranco Arena', 'Av. Grau 456', 2500, true, 11, '2024-01-05', 3);

-- ============================================================================
-- 4. ZONAS (RF-004: Zonas del local)
-- ============================================================================

-- Zonas del Estadio Nacional (idLocal=1)
INSERT INTO Zona (idZona, nombre, aforoMax, activo, usuarioCreacion, fechaCreacion, idLocal) VALUES
(1, 'Tribuna Norte', 10000, true, 11, '2024-01-01', 1),
(2, 'Tribuna Sur', 10000, true, 11, '2024-01-01', 1),
(3, 'Tribuna Este', 8000, true, 11, '2024-01-01', 1),
(4, 'Tribuna Oeste', 8000, true, 11, '2024-01-01', 1),
(5, 'Platea VIP', 5000, true, 11, '2024-01-01', 1),
(6, 'Palco Preferencial', 4000, true, 11, '2024-01-01', 1);

-- Zonas de Arena Perú (idLocal=2)
INSERT INTO Zona (idZona, nombre, aforoMax, activo, usuarioCreacion, fechaCreacion, idLocal) VALUES
(7, 'General', 3000, true, 11, '2024-01-02', 2),
(8, 'Campo', 1500, true, 11, '2024-01-02', 2),
(9, 'VIP', 500, true, 11, '2024-01-02', 2);

-- Zonas de Explanada Costa Verde (idLocal=3)
INSERT INTO Zona (idZona, nombre, aforoMax, activo, usuarioCreacion, fechaCreacion, idLocal) VALUES
(10, 'Zona General', 12000, true, 11, '2024-01-03', 3),
(11, 'Zona VIP', 3000, true, 11, '2024-01-03', 3);

-- Zonas del Centro de Convenciones (idLocal=4)
INSERT INTO Zona (idZona, nombre, aforoMax, activo, usuarioCreacion, fechaCreacion, idLocal) VALUES
(12, 'Salón Principal', 2000, true, 11, '2024-01-04', 4),
(13, 'Salón VIP', 1000, true, 11, '2024-01-04', 4);

-- ============================================================================
-- 5. EVENTOS (RF-007, RF-072, RF-073)
-- ============================================================================

INSERT INTO Evento (idEvento, nombre, descripcion, fechaEvento, horaInicio, horaFin, aforoDisponible, imagenUrl, tipoEvento, estadoEvento, edadMinima, restricciones, politicasDevolucion, activo, usuarioCreacion, fechaCreacion, idLocal) VALUES
(1, 'Concierto de Gian Marco', 'El reconocido cantautor peruano Gian Marco presenta su nuevo tour con todos sus éxitos y canciones inéditas.', '2025-11-15', '20:00:00', '23:00:00', 45000, 'https://example.com/eventos/gian-marco.jpg', 'CONCIERTO', 'PROGRAMADO', 0, 'No se permite el ingreso de bebidas alcohólicas ni alimentos. Cámaras profesionales requieren autorización.', 'Cambios y devoluciones hasta 48 horas antes del evento. Sujeto a disponibilidad y cargo por servicio.', true, 12, '2024-01-10', 1),

(2, 'Festival Selvámonos 2025', 'El festival de música más esperado del año con artistas nacionales e internacionales de rock, indie y alternativo.', '2025-11-25', '16:00:00', '02:00:00', 15000, 'https://example.com/eventos/selvamonos.jpg', 'FESTIVAL', 'PROGRAMADO', 18, 'Prohibido el ingreso de menores de edad. Evento para mayores de 18 años. Se permite camping. No se permiten vidrios ni latas.', 'No se permiten devoluciones. Cambios de nombre hasta 7 días antes del evento con cargo de S/ 20.', true, 12, '2024-01-11', 3),

(3, 'Coldplay - Music of the Spheres World Tour', 'La banda británica Coldplay regresa a Lima con su espectacular show lleno de música, luces y efectos especiales.', '2025-12-01', '21:00:00', '00:00:00', 80000, 'https://example.com/eventos/coldplay.jpg', 'CONCIERTO', 'PROGRAMADO', 0, 'No se permiten paraguas, selfie sticks ni mochilas grandes. Cámaras profesionales prohibidas.', 'Cambios hasta 15 días antes. Devoluciones solo en caso de cancelación del evento.', true, 12, '2024-01-12', 9),

(4, 'Peru Game Show 2025', 'El evento de videojuegos, esports y cultura geek más grande del Perú. Torneos, cosplay, streamers y mucho más.', '2025-11-20', '10:00:00', '22:00:00', 5000, 'https://example.com/eventos/peru-game-show.jpg', 'FERIA', 'PROGRAMADO', 0, 'Menores de 12 años deben ingresar acompañados de un adulto. Disfraces y accesorios permitidos (sin armas).', 'Cambios de día permitidos. Devoluciones hasta 5 días antes con cargo del 10%.', true, 12, '2024-01-13', 2),

(5, 'Stand Up Comedy - Franco Escamilla', 'El famoso comediante mexicano Franco Escamilla trae su show "Voyager" lleno de humor negro y situaciones cotidianas.', '2025-11-18', '21:00:00', '23:00:00', 3000, 'https://example.com/eventos/franco-escamilla.jpg', 'TEATRO', 'PROGRAMADO', 16, 'Contenido para mayores de 16 años. Humor para adultos con lenguaje explícito. No se permite el uso de celulares durante el show.', 'No se permiten cambios ni devoluciones. Las entradas son personales e intransferibles.', true, 12, '2024-01-14', 4),

(6, 'Feria Gastronómica Mistura 2025', 'El festival gastronómico más importante de América Latina. Degustaciones, talleres y presentaciones de los mejores chefs del país.', '2025-11-28', '11:00:00', '22:00:00', 8000, 'https://example.com/eventos/mistura.jpg', 'FERIA', 'PROGRAMADO', 0, 'Acceso familiar. Se permite el ingreso de cochecitos de bebé. Zona pet-friendly disponible.', 'Cambios hasta 3 días antes. Reembolsos solo en caso de cancelación oficial.', true, 12, '2024-01-15', 5),

(7, 'Circo del Sol - Alegría', 'El famoso Cirque du Soleil presenta su espectáculo Alegría con acrobacias impresionantes y efectos visuales de clase mundial.', '2025-12-05', '19:00:00', '21:30:00', 10000, 'https://example.com/eventos/cirque-du-soleil.jpg', 'CIRCO', 'PROGRAMADO', 0, 'Apto para toda la familia. Recomendado para niños mayores de 5 años. No se permiten flashes ni grabaciones.', 'Cambios permitidos hasta 72 horas antes. Devoluciones con penalidad del 20%.', true, 12, '2024-01-16', 7),

(8, 'Obra de Teatro: El Avaro', 'Adaptación moderna de la obra clásica de Molière a cargo de la Compañía Nacional de Teatro.', '2025-11-22', '20:00:00', '22:00:00', 800, 'https://example.com/eventos/el-avaro.jpg', 'TEATRO', 'PROGRAMADO', 12, 'Recomendado para mayores de 12 años. Silenciar celulares. Llegar 15 minutos antes del inicio.', 'Cambios hasta 24 horas antes. No se permiten devoluciones después de iniciada la función.', true, 12, '2024-01-17', 6),

(9, 'Maratón Lima 2025', 'Maratón internacional 42K con categorías para todos los niveles: 10K, 21K y 42K.', '2025-12-08', '06:00:00', '14:00:00', 5000, 'https://example.com/eventos/maraton-lima.jpg', 'DEPORTIVO', 'PROGRAMADO', 18, 'Solo mayores de 18 años para 42K. Categorías 10K y 21K desde 16 años. Certificado médico obligatorio.', 'Cambios de categoría hasta 30 días antes. No se permiten devoluciones. Transferencias permitidas.', true, 12, '2024-01-18', 1),

(10, 'Festival de Jazz de Lima', 'Tres días de jazz con artistas nacionales e internacionales en un ambiente único frente al mar.', '2025-11-30', '18:00:00', '01:00:00', 2500, 'https://example.com/eventos/jazz-festival.jpg', 'FESTIVAL', 'PROGRAMADO', 18, 'Solo mayores de 18 años. Se permite el consumo de bebidas alcohólicas. Sillas plegables permitidas.', 'Cambios hasta 7 días antes. Devoluciones no permitidas. Pases intransferibles.', true, 12, '2024-01-19', 10);

-- ============================================================================
-- 6. TIPOS DE TICKETS
-- ============================================================================

-- Tickets para Concierto de Gian Marco
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(1, 'General', 'Entrada general sin asiento numerado', 80.00, 30000, 30000, 0, '2024-02-01 10:00:00', '2025-11-15 19:00:00', true, 1),
(2, 'Tribuna', 'Asiento numerado en tribuna', 150.00, 10000, 10000, 0, '2024-02-01 10:00:00', '2025-11-15 19:00:00', true, 1),
(3, 'VIP', 'Zona VIP con bar incluido y meet & greet', 350.00, 500, 500, 0, '2024-02-01 10:00:00', '2025-11-15 19:00:00', true, 1);

-- Tickets para Festival Selvámonos
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(4, 'Pase 1 Día', 'Acceso a un día del festival', 120.00, 5000, 5000, 0, '2024-02-15 10:00:00', '2025-11-25 15:00:00', true, 2),
(5, 'Pase 2 Días', 'Acceso completo al festival', 200.00, 8000, 8000, 0, '2024-02-15 10:00:00', '2025-11-25 15:00:00', true, 2),
(6, 'VIP Premium', 'Acceso a zona VIP, bar abierto y baños exclusivos', 450.00, 300, 300, 0, '2024-02-15 10:00:00', '2025-11-25 15:00:00', true, 2);

-- Tickets para Coldplay
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(7, 'Occidente', 'Tribuna Occidente', 250.00, 20000, 20000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 3),
(8, 'Oriente', 'Tribuna Oriente', 280.00, 20000, 20000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 3),
(9, 'Norte/Sur', 'Tribuna Norte o Sur', 220.00, 30000, 30000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 3),
(10, 'Campo', 'Campo cerca del escenario', 400.00, 8000, 8000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 3),
(11, 'Golden Circle', 'Zona exclusiva frente al escenario', 650.00, 1000, 1000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 3);

-- Tickets para Peru Game Show
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(12, 'Entrada General', 'Acceso general al evento', 40.00, 4000, 4000, 0, '2024-02-20 10:00:00', '2025-11-20 09:00:00', true, 4),
(13, 'Fast Pass', 'Entrada con fila preferencial', 70.00, 800, 800, 0, '2024-02-20 10:00:00', '2025-11-20 09:00:00', true, 4),
(14, 'Meet & Greet Streamers', 'Incluye acceso a meet & greet con streamers', 150.00, 200, 200, 0, '2024-02-20 10:00:00', '2025-11-20 09:00:00', true, 4);

-- Tickets para Franco Escamilla
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(15, 'Platea Baja', 'Asiento en platea baja', 180.00, 1500, 1500, 0, '2024-02-10 10:00:00', '2025-11-18 20:00:00', true, 5),
(16, 'Platea Alta', 'Asiento en platea alta', 120.00, 1000, 1000, 0, '2024-02-10 10:00:00', '2025-11-18 20:00:00', true, 5),
(17, 'VIP Front Row', 'Primera fila con foto incluida', 350.00, 50, 50, 0, '2024-02-10 10:00:00', '2025-11-18 20:00:00', true, 5);

-- Tickets para Mistura
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(18, 'Entrada General', 'Acceso general a la feria', 25.00, 7000, 7000, 0, '2024-03-01 10:00:00', '2025-11-28 10:00:00', true, 6),
(19, 'Fast Track', 'Entrada con fila rápida', 45.00, 800, 800, 0, '2024-03-01 10:00:00', '2025-11-28 10:00:00', true, 6),
(20, 'Talleres Gastronómicos', 'Incluye acceso a talleres con chefs', 80.00, 200, 200, 0, '2024-03-01 10:00:00', '2025-11-28 10:00:00', true, 6);

-- Tickets para Circo del Sol
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(21, 'Tribuna Lateral', 'Vista lateral del espectáculo', 150.00, 5000, 5000, 0, '2024-03-15 10:00:00', '2025-12-05 18:00:00', true, 7),
(22, 'Tribuna Central', 'Vista central privilegiada', 250.00, 3000, 3000, 0, '2024-03-15 10:00:00', '2025-12-05 18:00:00', true, 7),
(23, 'Premium', 'Mejores asientos + bebida de cortesía', 400.00, 500, 500, 0, '2024-03-15 10:00:00', '2025-12-05 18:00:00', true, 7);

-- Tickets para El Avaro
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(24, 'Platea', 'Asiento en platea', 60.00, 500, 500, 0, '2024-02-15 10:00:00', '2025-11-22 19:00:00', true, 8),
(25, 'Balcón', 'Asiento en balcón', 40.00, 250, 250, 0, '2024-02-15 10:00:00', '2025-11-22 19:00:00', true, 8),
(26, 'Estudiante', 'Entrada con descuento estudiante', 30.00, 50, 50, 0, '2024-02-15 10:00:00', '2025-11-22 19:00:00', true, 8);

-- Tickets para Maratón Lima
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(27, '10K', 'Inscripción para carrera 10K', 50.00, 2000, 2000, 0, '2024-03-01 10:00:00', '2025-12-07 23:59:00', true, 9),
(28, '21K', 'Inscripción para media maratón 21K', 80.00, 2000, 2000, 0, '2024-03-01 10:00:00', '2025-12-07 23:59:00', true, 9),
(29, '42K', 'Inscripción para maratón completo 42K', 120.00, 1000, 1000, 0, '2024-03-01 10:00:00', '2025-12-07 23:59:00', true, 9);

-- Tickets para Festival de Jazz
INSERT INTO TipoTicket (idTipoTicket, nombre, descripcion, precio, stock, cantidadDisponible, cantidadVendida, fechaInicioVenta, fechaFinVenta, activo, idEvento) VALUES
(30, 'General', 'Acceso general al festival', 90.00, 1500, 1500, 0, '2024-02-20 10:00:00', '2025-11-30 17:00:00', true, 10),
(31, 'Preferencial', 'Zona preferencial con asientos', 150.00, 800, 800, 0, '2024-02-20 10:00:00', '2025-11-30 17:00:00', true, 10),
(32, 'VIP Lounge', 'Acceso a lounge VIP con bar incluido', 280.00, 200, 200, 0, '2024-02-20 10:00:00', '2025-11-30 17:00:00', true, 10);

-- ============================================================================
-- RESUMEN DE DATOS INSERTADOS
-- ============================================================================
-- Departamentos: 5
-- Provincias: 9
-- Distritos: 15
-- Personas: 13 (10 Clientes + 3 Administradores)
-- Clientes: 10
-- Administradores: 3
-- Locales: 10
-- Zonas: 13 (con relación a locales - RF-004)
-- Eventos: 10 (con edad mínima, restricciones y políticas - RF-072, RF-073)
-- Tipos de Tickets: 32
-- ============================================================================

-- Habilitar restricciones nuevamente
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- INFORMACIÓN IMPORTANTE
-- ============================================================================
-- Usuario de prueba cliente:
--   Email: juan.perez@fasticket.com
--   Contraseña: password123
--
-- Usuario de prueba administrador:
--   Email: admin@pucp.edu.pe
--   Contraseña: password123
--
-- Todos los usuarios tienen la misma contraseña para facilitar pruebas.
-- ============================================================================

