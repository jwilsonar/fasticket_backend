-- ============================================================================
-- SCRIPT SEEDER COMPLETO PARA FASTICKET - PostgreSQL
-- Este script elimina todas las tablas y crea datos de prueba completos
-- Incluye todas las entidades y relaciones del modelo
-- ATENCIÓN: Este script hace DROP de todas las tablas - usar con precaución
-- ============================================================================

-- ============================================================================
-- PARTE 1: DROP COMPLETO DE TODAS LAS TABLAS
-- ============================================================================

-- Desactivar temporalmente restricciones de clave foránea
SET session_replication_role = 'replica';

-- Eliminar todas las tablas en orden inverso de dependencias
DROP TABLE IF EXISTS transferencias CASCADE;
DROP TABLE IF EXISTS auditoria_admin CASCADE;
DROP TABLE IF EXISTS boleta CASCADE;
DROP TABLE IF EXISTS descuentos_realizados CASCADE;
DROP TABLE IF EXISTS canje CASCADE;
DROP TABLE IF EXISTS puntos CASCADE;
DROP TABLE IF EXISTS ticket CASCADE;
DROP TABLE IF EXISTS item_carrito CASCADE;
DROP TABLE IF EXISTS orden_compra CASCADE;
DROP TABLE IF EXISTS carro_compras CASCADE;
DROP TABLE IF EXISTS comprobantepago CASCADE;
DROP TABLE IF EXISTS pago CASCADE;
DROP TABLE IF EXISTS codigo_promocional CASCADE;
DROP TABLE IF EXISTS reglapuntos CASCADE;
DROP TABLE IF EXISTS tipo_ticket CASCADE;
DROP TABLE IF EXISTS zona CASCADE;
DROP TABLE IF EXISTS evento CASCADE;
DROP TABLE IF EXISTS local CASCADE;
DROP TABLE IF EXISTS cliente CASCADE;
DROP TABLE IF EXISTS administrador CASCADE;
DROP TABLE IF EXISTS persona CASCADE;
DROP TABLE IF EXISTS distrito CASCADE;
DROP TABLE IF EXISTS provincia CASCADE;
DROP TABLE IF EXISTS departamento CASCADE;

-- Reactivar restricciones
SET session_replication_role = 'origin';

-- ============================================================================
-- NOTA IMPORTANTE: 
-- Este script hace DROP de todas las tablas. Las tablas serán recreadas por 
-- JPA/Hibernate cuando se ejecute la aplicación Spring Boot.
-- 
-- IMPORTANTE: Después de ejecutar este script, DEBE iniciar la aplicación
-- Spring Boot para que JPA/Hibernate recree las tablas con el esquema correcto
-- según el modelo Java. Luego ejecute solo la parte de inserción de datos.
-- ============================================================================

-- Verificar que las tablas existen (después de que la aplicación las haya recreado)
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'departamento';
    
    IF table_count = 0 THEN
        RAISE WARNING 'Las tablas no existen después del DROP. Las tablas serán recreadas automáticamente por JPA/Hibernate cuando inicie la aplicación Spring Boot. Este script continuará pero las inserciones fallarán si las tablas no existen.';
    END IF;
END $$;

-- ============================================================================
-- PARTE 2: LIMPIEZA DE DATOS EXISTENTES (si las tablas existen)
-- ============================================================================

-- Desactivar temporalmente restricciones de clave foránea
SET session_replication_role = 'replica';

-- Borrar todas las filas (en orden inverso de dependencias) - solo si las tablas existen
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'transferencias') THEN
        DELETE FROM transferencias;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'auditoria_admin') THEN
        DELETE FROM auditoria_admin;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'boleta') THEN
        DELETE FROM boleta;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'descuentos_realizados') THEN
        DELETE FROM descuentos_realizados;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'canje') THEN
        DELETE FROM canje;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'puntos') THEN
        DELETE FROM puntos;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ticket') THEN
        DELETE FROM ticket;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'item_carrito') THEN
        DELETE FROM item_carrito;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orden_compra') THEN
        DELETE FROM orden_compra;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'carro_compras') THEN
        DELETE FROM carro_compras;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'comprobantepago') THEN
        DELETE FROM comprobantepago;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'pago') THEN
        DELETE FROM pago;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'codigo_promocional') THEN
        DELETE FROM codigo_promocional;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reglapuntos') THEN
        DELETE FROM reglapuntos;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tipo_ticket') THEN
        DELETE FROM tipo_ticket;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'zona') THEN
        DELETE FROM zona;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'evento') THEN
        DELETE FROM evento;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'local') THEN
        DELETE FROM local;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'cliente') THEN
        DELETE FROM cliente;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'administrador') THEN
        DELETE FROM administrador;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'persona') THEN
        DELETE FROM persona;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'distrito') THEN
        DELETE FROM distrito;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'provincia') THEN
        DELETE FROM provincia;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'departamento') THEN
        DELETE FROM departamento;
    END IF;
END $$;

-- Reactivar restricciones
SET session_replication_role = 'origin';

-- Resetear todas las secuencias (si existen)
DO $$
DECLARE
    seq_name text;
BEGIN
    FOR seq_name IN 
        SELECT sequence_name FROM information_schema.sequences 
        WHERE sequence_schema = 'public'
    LOOP
        BEGIN
            EXECUTE format('SELECT setval(%L, 1, false)', seq_name);
        EXCEPTION WHEN OTHERS THEN
            -- Ignorar errores si la secuencia no existe
            NULL;
        END;
    END LOOP;
END $$;

-- ============================================================================
-- PARTE 3: INSERCIÓN DE DATOS (En orden de dependencias)
-- ============================================================================

-- ============================================================================
-- 1. GEOGRAFÍA: DEPARTAMENTOS, PROVINCIAS Y DISTRITOS
-- ============================================================================

-- Departamentos
INSERT INTO departamento (id_departamento, nombre, activo) VALUES
(1, 'Lima', true),
(2, 'Arequipa', true),
(3, 'Cusco', true),
(4, 'La Libertad', true),
(5, 'Piura', true)
ON CONFLICT DO NOTHING;

-- Provincias
INSERT INTO provincia (id_provincia, nombre, activo, id_departamento) VALUES
(1, 'Lima', true, 1),
(2, 'Callao', true, 1),
(3, 'Huaral', true, 1),
(4, 'Arequipa', true, 2),
(5, 'Cusco', true, 3),
(6, 'Trujillo', true, 4),
(7, 'Piura', true, 5)
ON CONFLICT DO NOTHING;

-- Distritos
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
(11, 'Bellavista', true, 2),
(12, 'La Perla', true, 2),
(13, 'Arequipa Centro', true, 4),
(14, 'Yanahuara', true, 4),
(15, 'Cusco Centro', true, 5)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 2. USUARIOS: PERSONAS, ADMINISTRADORES Y CLIENTES
-- ============================================================================

-- Contraseña encriptada (BCrypt): "password123" para todos
-- Hash: $2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i

-- Personas (Base) - Administradores
INSERT INTO persona (id_persona, tipo_documento, doc_identidad, nombres, apellidos, telefono, email, fecha_nacimiento, direccion, contrasenia, rol, activo, fecha_creacion, usuario_creacion, id_distrito) VALUES
(1, 'DNI', '10000001', 'Admin', 'Principal', '999999999', 'admin@pucp.edu.pe', '1985-01-15', 'Av. Universitaria 1801', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'ADMINISTRADOR', true, '2024-01-01', 1, 1),
(2, 'DNI', '10000002', 'Carlos', 'García López', '999999998', 'carlos.admin@fasticket.com', '1988-05-20', 'Av. Larco 456', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'ADMINISTRADOR', true, '2024-01-01', 1, 2),
(3, 'DNI', '10000003', 'María', 'Fernández Silva', '999999997', 'maria.admin@fasticket.com', '1990-08-12', 'Jr. Camaná 234', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'ADMINISTRADOR', true, '2024-01-01', 1, 3)
ON CONFLICT DO NOTHING;

-- Administradores (Tabla hija)
INSERT INTO administrador (id_persona, cargo) VALUES
(1, 'Super Administrador'),
(2, 'Administrador de Eventos'),
(3, 'Administrador de Ventas')
ON CONFLICT DO NOTHING;

-- Personas (Base) - Clientes
INSERT INTO persona (id_persona, tipo_documento, doc_identidad, nombres, apellidos, telefono, email, fecha_nacimiento, direccion, contrasenia, rol, activo, fecha_creacion, usuario_creacion, id_distrito) VALUES
(10, 'DNI', '72345678', 'Juan', 'Pérez García', '987654321', 'juan.perez@fasticket.com', '1990-05-15', 'Av. Larco 1234', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-15', 1, 1),
(11, 'DNI', '73456789', 'María', 'González López', '987654322', 'maria.gonzalez@fasticket.com', '1992-08-20', 'Calle Los Pinos 567', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-16', 1, 2),
(12, 'DNI', '74567890', 'Carlos', 'Rodríguez Mendoza', '987654323', 'carlos.rodriguez@fasticket.com', '1988-03-10', 'Jr. Libertad 890', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-17', 1, 3),
(13, 'DNI', '75678901', 'Ana', 'Martínez Silva', '987654324', 'ana.martinez@fasticket.com', '1995-11-25', 'Av. Primavera 234', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-18', 1, 4),
(14, 'DNI', '76789012', 'Luis', 'Sánchez Torres', '987654325', 'luis.sanchez@fasticket.com', '1991-07-30', 'Calle Las Flores 456', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-19', 1, 5),
(15, 'DNI', '77890123', 'Pedro', 'Ramírez Castro', '987654326', 'pedro.ramirez@fasticket.com', '1993-02-14', 'Av. Javier Prado 789', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-20', 1, 6),
(16, 'DNI', '78901234', 'Sofia', 'Vargas Morales', '987654327', 'sofia.vargas@fasticket.com', '1994-09-05', 'Jr. Washington 321', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-21', 1, 7),
(17, 'DNI', '79012345', 'Diego', 'Fernández Rojas', '987654328', 'diego.fernandez@fasticket.com', '1989-12-18', 'Av. Arequipa 654', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-22', 1, 8),
(18, 'PASAPORTE', 'P1234567', 'Laura', 'Herrera Jiménez', '987654329', 'laura.herrera@fasticket.com', '1996-04-08', 'Av. La Paz 987', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-23', 1, 9),
(19, 'DNI', '70123456', 'Roberto', 'Torres Vega', '987654330', 'roberto.torres@fasticket.com', '1987-10-22', 'Calle Bolognesi 123', '$2a$10$N9qo8uLOickgx2ZMRZoMye7LNc9Pl.2XYtfQwNe2LSFdj9mRQZr3i', 'CLIENTE', true, '2024-01-24', 1, 10)
ON CONFLICT DO NOTHING;

-- Clientes (Tabla hija) - Usando valores permitidos según constraint: BRONCE, PLATA, ORO
INSERT INTO cliente (id_persona, nivel, puntos_acumulados) VALUES
(10, 'BRONCE', 0),
(11, 'BRONCE', 150),
(12, 'PLATA', 2500),
(13, 'BRONCE', 50),
(14, 'ORO', 8500),
(15, 'PLATA', 3200),
(16, 'BRONCE', 200),
(17, 'PLATA', 2800),
(18, 'BRONCE', 0),
(19, 'ORO', 9200)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 3. LOCALES Y ZONAS
-- ============================================================================

-- Locales
INSERT INTO local (id_local, nombre, url_mapa, direccion, aforo_total, imagen_url, activo, usuario_creacion, fecha_creacion, id_distrito) VALUES
(1, 'Estadio Nacional', 'https://maps.google.com/estadio-nacional', 'Av. Paseo de la República s/n', 45000, 'https://example.com/locales/estadio-nacional.jpg', true, 1, '2024-01-01', 7),
(2, 'Arena Perú', 'https://maps.google.com/arena-peru', 'Av. Javier Prado Este 8212', 5000, 'https://example.com/locales/arena-peru.jpg', true, 1, '2024-01-01', 4),
(3, 'Explanada Costa Verde', 'https://maps.google.com/costa-verde', 'Malecón de la Marina', 15000, 'https://example.com/locales/costa-verde.jpg', true, 2, '2024-01-02', 1),
(4, 'Centro de Convenciones Lima', 'https://maps.google.com/centro-convenciones', 'Av. Alfredo Benavides 1555', 3000, 'https://example.com/locales/convenciones.jpg', true, 2, '2024-01-02', 1),
(5, 'Parque de la Exposición', 'https://maps.google.com/parque-exposicion', 'Av. 28 de Julio', 8000, 'https://example.com/locales/exposicion.jpg', true, 1, '2024-01-03', 7),
(6, 'Teatro Municipal', 'https://maps.google.com/teatro-municipal', 'Jr. Ica 377', 1200, 'https://example.com/locales/teatro.jpg', true, 2, '2024-01-03', 7),
(7, 'Jockey Club del Perú', 'https://maps.google.com/jockey-club', 'Av. El Derby s/n', 10000, 'https://example.com/locales/jockey.jpg', true, 1, '2024-01-04', 4),
(8, 'Centro Cultural PUCP', 'https://maps.google.com/cultural-pucp', 'Av. Camino Real 1075', 800, 'https://example.com/locales/pucp.jpg', true, 1, '2024-01-04', 5),
(9, 'Estadio Monumental', 'https://maps.google.com/monumental', 'Av. Tomás Valle', 80000, 'https://example.com/locales/monumental.jpg', true, 2, '2024-01-05', 7),
(10, 'Barranco Arena', 'https://maps.google.com/barranco-arena', 'Av. Grau 456', 2500, 'https://example.com/locales/barranco.jpg', true, 2, '2024-01-05', 3)
ON CONFLICT DO NOTHING;

-- Zonas
INSERT INTO zona (id_zona, nombre, aforo_max, imagen_url, activo, usuario_creacion, fecha_creacion, id_local) VALUES
-- Estadio Nacional (id_local=1)
(1, 'Tribuna Norte', 10000, 'https://example.com/zonas/tribuna-norte.jpg', true, 1, '2024-01-01', 1),
(2, 'Tribuna Sur', 10000, 'https://example.com/zonas/tribuna-sur.jpg', true, 1, '2024-01-01', 1),
(3, 'Tribuna Este', 8000, 'https://example.com/zonas/tribuna-este.jpg', true, 1, '2024-01-01', 1),
(4, 'Tribuna Oeste', 8000, 'https://example.com/zonas/tribuna-oeste.jpg', true, 1, '2024-01-01', 1),
(5, 'Platea VIP', 5000, 'https://example.com/zonas/platea-vip.jpg', true, 1, '2024-01-01', 1),
(6, 'Palco Preferencial', 4000, 'https://example.com/zonas/palco.jpg', true, 1, '2024-01-01', 1),
-- Arena Perú (id_local=2)
(7, 'General', 3000, 'https://example.com/zonas/general.jpg', true, 1, '2024-01-02', 2),
(8, 'Campo', 1500, 'https://example.com/zonas/campo.jpg', true, 1, '2024-01-02', 2),
(9, 'VIP', 500, 'https://example.com/zonas/vip-arena.jpg', true, 1, '2024-01-02', 2),
-- Explanada Costa Verde (id_local=3)
(10, 'Zona General', 12000, 'https://example.com/zonas/zona-general.jpg', true, 1, '2024-01-03', 3),
(11, 'Zona VIP', 3000, 'https://example.com/zonas/zona-vip.jpg', true, 1, '2024-01-03', 3),
-- Centro de Convenciones (id_local=4)
(12, 'Salón Principal', 2000, 'https://example.com/zonas/salon-principal.jpg', true, 1, '2024-01-04', 4),
(13, 'Salón VIP', 1000, 'https://example.com/zonas/salon-vip.jpg', true, 1, '2024-01-04', 4),
-- Parque de la Exposición (id_local=5)
(14, 'Zona Central', 5000, 'https://example.com/zonas/zona-central.jpg', true, 1, '2024-01-05', 5),
(15, 'Zona Lateral', 3000, 'https://example.com/zonas/zona-lateral.jpg', true, 1, '2024-01-05', 5),
-- Teatro Municipal (id_local=6)
(16, 'Platea', 800, 'https://example.com/zonas/platea.jpg', true, 1, '2024-01-06', 6),
(17, 'Balcón', 400, 'https://example.com/zonas/balcon.jpg', true, 1, '2024-01-06', 6),
-- Jockey Club (id_local=7)
(18, 'Tribuna', 6000, 'https://example.com/zonas/tribuna-jockey.jpg', true, 1, '2024-01-07', 7),
(19, 'Campo VIP', 4000, 'https://example.com/zonas/campo-vip.jpg', true, 1, '2024-01-07', 7),
-- Estadio Monumental (id_local=9)
(20, 'Occidente', 20000, 'https://example.com/zonas/occidente.jpg', true, 1, '2024-01-09', 9),
(21, 'Oriente', 20000, 'https://example.com/zonas/oriente.jpg', true, 1, '2024-01-09', 9),
(22, 'Norte/Sur', 30000, 'https://example.com/zonas/norte-sur.jpg', true, 1, '2024-01-09', 9),
(23, 'Campo', 8000, 'https://example.com/zonas/campo-monumental.jpg', true, 1, '2024-01-09', 9),
(24, 'Golden Circle', 2000, 'https://example.com/zonas/golden-circle.jpg', true, 1, '2024-01-09', 9),
-- Barranco Arena (id_local=10)
(25, 'General', 1500, 'https://example.com/zonas/general-barranco.jpg', true, 1, '2024-01-10', 10),
(26, 'Preferencial', 800, 'https://example.com/zonas/preferencial-barranco.jpg', true, 1, '2024-01-10', 10),
(27, 'VIP', 200, 'https://example.com/zonas/vip-barranco.jpg', true, 1, '2024-01-10', 10)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 4. EVENTOS (Usando valores permitidos según constraint: ROCK, POP, etc.)
-- ============================================================================

INSERT INTO evento (id_evento, nombre, fecha_evento, hora_inicio, hora_fin, aforo_disponible, imagen_url, tipo_evento, estado_evento, edad_minima, restricciones, politicas_devolucion, max_transferencias_permitidas, horas_cooldown_transferencia, activo, usuario_creacion, fecha_creacion, id_local) VALUES
(1, 'Concierto Rock Nacional', '2025-11-15', '20:00:00', '23:00:00', 45000, 'https://example.com/eventos/rock-nacional.jpg', 'ROCK', 'PUBLICADO', 0, 'No se permite el ingreso de bebidas alcohólicas ni alimentos. Cámaras profesionales requieren autorización.', 'Cambios y devoluciones hasta 48 horas antes del evento. Sujeto a disponibilidad y cargo por servicio.', 1, 12, true, 2, '2024-01-10', 1),
(2, 'Festival de Música Electrónica', '2025-11-25', '16:00:00', '02:00:00', 15000, 'https://example.com/eventos/electronica.jpg', 'ELECTRONICA', 'PUBLICADO', 18, 'Prohibido el ingreso de menores de edad. Evento para mayores de 18 años. Se permite camping. No se permiten vidrios ni latas.', 'No se permiten devoluciones. Cambios de nombre hasta 7 días antes del evento con cargo de S/ 20.', 2, 24, true, 2, '2024-01-11', 3),
(3, 'Concierto Pop Internacional', '2025-12-01', '21:00:00', '00:00:00', 80000, 'https://example.com/eventos/pop-internacional.jpg', 'POP', 'PUBLICADO', 0, 'No se permiten paraguas, selfie sticks ni mochilas grandes. Cámaras profesionales prohibidas.', 'Cambios hasta 15 días antes. Devoluciones solo en caso de cancelación del evento.', 1, 12, true, 2, '2024-01-12', 9),
(4, 'Concierto Urbano', '2025-11-20', '10:00:00', '22:00:00', 5000, 'https://example.com/eventos/urbano.jpg', 'URBANO', 'PUBLICADO', 0, 'Menores de 12 años deben ingresar acompañados de un adulto. Disfraces y accesorios permitidos (sin armas).', 'Cambios de día permitidos. Devoluciones hasta 5 días antes con cargo del 10%.', 2, 24, true, 2, '2024-01-13', 2),
(5, 'Show Reggaeton', '2025-11-18', '21:00:00', '23:00:00', 3000, 'https://example.com/eventos/reggaeton.jpg', 'REGUETON', 'PUBLICADO', 16, 'Contenido para mayores de 16 años. No se permite el uso de celulares durante el show.', 'No se permiten cambios ni devoluciones. Las entradas son personales e intransferibles.', 1, 12, true, 3, '2024-01-14', 4),
(6, 'Festival Rock & Pop', '2025-11-28', '11:00:00', '22:00:00', 8000, 'https://example.com/eventos/rock-pop.jpg', 'ROCKANDPOP', 'PUBLICADO', 0, 'Acceso familiar. Se permite el ingreso de cochecitos de bebé. Zona pet-friendly disponible.', 'Cambios hasta 3 días antes. Reembolsos solo en caso de cancelación oficial.', 1, 12, true, 3, '2024-01-15', 5),
(7, 'Concierto Metal', '2025-12-05', '19:00:00', '21:30:00', 10000, 'https://example.com/eventos/metal.jpg', 'METAL', 'PUBLICADO', 0, 'Apto para toda la familia. Recomendado para mayores de 12 años. No se permiten flashes ni grabaciones.', 'Cambios permitidos hasta 72 horas antes. Devoluciones con penalidad del 20%.', 1, 12, true, 1, '2024-01-16', 7),
(8, 'Festival Punk', '2025-11-22', '20:00:00', '22:00:00', 800, 'https://example.com/eventos/punk.jpg', 'PUNK', 'PUBLICADO', 12, 'Recomendado para mayores de 12 años. Silenciar celulares. Llegar 15 minutos antes del inicio.', 'Cambios hasta 24 horas antes. No se permiten devoluciones después de iniciada la función.', 1, 12, true, 1, '2024-01-17', 6),
(9, 'Concierto Reggae', '2025-12-08', '06:00:00', '14:00:00', 5000, 'https://example.com/eventos/reggae.jpg', 'REGGAE', 'PUBLICADO', 18, 'Solo mayores de 18 años. Certificado médico obligatorio.', 'Cambios hasta 30 días antes. No se permiten devoluciones. Transferencias permitidas.', 2, 24, true, 2, '2024-01-18', 1),
(10, 'Festival Musical Mixto', '2025-11-30', '18:00:00', '01:00:00', 2500, 'https://example.com/eventos/mixto.jpg', 'ROCK', 'PUBLICADO', 18, 'Solo mayores de 18 años. Se permite el consumo de bebidas alcohólicas. Sillas plegables permitidas.', 'Cambios hasta 7 días antes. Devoluciones no permitidas. Pases intransferibles.', 1, 12, true, 2, '2024-01-19', 10)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 5. TIPOS DE TICKETS (Relacionados con Eventos y Zonas)
-- ============================================================================

INSERT INTO tipo_ticket (id_tipo_ticket, nombre, descripcion, precio, stock, cantidad_disponible, cantidad_vendida, fecha_inicio_venta, fecha_fin_venta, activo, limite_por_persona, id_zona, id_evento) VALUES
-- Concierto Rock Nacional (id_evento=1)
(1, 'General', 'Entrada general sin asiento numerado', 80.00, 30000, 30000, 0, '2024-02-01 10:00:00', '2025-11-15 19:00:00', true, 10, 1, 1),
(2, 'Tribuna', 'Asiento numerado en tribuna', 150.00, 10000, 10000, 0, '2024-02-01 10:00:00', '2025-11-15 19:00:00', true, 8, 2, 1),
(3, 'VIP', 'Zona VIP con bar incluido', 350.00, 5000, 5000, 0, '2024-02-01 10:00:00', '2025-11-15 19:00:00', true, 4, 5, 1),
-- Festival Electrónica (id_evento=2)
(4, 'Pase 1 Día', 'Acceso a un día del festival', 120.00, 5000, 5000, 0, '2024-02-15 10:00:00', '2025-11-25 15:00:00', true, 6, 10, 2),
(5, 'Pase 2 Días', 'Acceso completo al festival', 200.00, 8000, 8000, 0, '2024-02-15 10:00:00', '2025-11-25 15:00:00', true, 4, 10, 2),
(6, 'VIP Premium', 'Acceso a zona VIP', 450.00, 2000, 2000, 0, '2024-02-15 10:00:00', '2025-11-25 15:00:00', true, 2, 11, 2),
-- Concierto Pop (id_evento=3)
(7, 'Occidente', 'Tribuna Occidente', 250.00, 20000, 20000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 10, 20, 3),
(8, 'Oriente', 'Tribuna Oriente', 280.00, 20000, 20000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 10, 21, 3),
(9, 'Norte/Sur', 'Tribuna Norte o Sur', 220.00, 30000, 30000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 10, 22, 3),
(10, 'Campo', 'Campo cerca del escenario', 400.00, 8000, 8000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 8, 23, 3),
(11, 'Golden Circle', 'Zona exclusiva frente al escenario', 650.00, 2000, 2000, 0, '2024-03-01 10:00:00', '2025-12-01 20:00:00', true, 4, 24, 3),
-- Show Urbano (id_evento=4)
(12, 'Entrada General', 'Acceso general al evento', 40.00, 4000, 4000, 0, '2024-02-20 10:00:00', '2025-11-20 09:00:00', true, 10, 7, 4),
(13, 'Fast Pass', 'Entrada con fila preferencial', 70.00, 800, 800, 0, '2024-02-20 10:00:00', '2025-11-20 09:00:00', true, 6, 8, 4),
(14, 'VIP', 'Acceso VIP', 150.00, 200, 200, 0, '2024-02-20 10:00:00', '2025-11-20 09:00:00', true, 2, 9, 4),
-- Show Reggaeton (id_evento=5)
(15, 'Platea Baja', 'Asiento en platea baja', 180.00, 1500, 1500, 0, '2024-02-10 10:00:00', '2025-11-18 20:00:00', true, 8, 12, 5),
(16, 'Platea Alta', 'Asiento en platea alta', 120.00, 1000, 1000, 0, '2024-02-10 10:00:00', '2025-11-18 20:00:00', true, 6, 13, 5),
-- Festival Rock & Pop (id_evento=6)
(17, 'Entrada General', 'Acceso general a la feria', 25.00, 7000, 7000, 0, '2024-03-01 10:00:00', '2025-11-28 10:00:00', true, 10, 14, 6),
(18, 'Fast Track', 'Entrada con fila rápida', 45.00, 800, 800, 0, '2024-03-01 10:00:00', '2025-11-28 10:00:00', true, 6, 15, 6),
-- Concierto Metal (id_evento=7)
(19, 'Tribuna Lateral', 'Vista lateral del espectáculo', 150.00, 5000, 5000, 0, '2024-03-15 10:00:00', '2025-12-05 18:00:00', true, 10, 18, 7),
(20, 'Tribuna Central', 'Vista central privilegiada', 250.00, 3000, 3000, 0, '2024-03-15 10:00:00', '2025-12-05 18:00:00', true, 8, 19, 7),
-- Festival Punk (id_evento=8)
(21, 'Platea', 'Asiento en platea', 60.00, 500, 500, 0, '2024-02-15 10:00:00', '2025-11-22 19:00:00', true, 6, 16, 8),
(22, 'Balcón', 'Asiento en balcón', 40.00, 250, 250, 0, '2024-02-15 10:00:00', '2025-11-22 19:00:00', true, 4, 17, 8),
-- Concierto Reggae (id_evento=9)
(23, 'General', 'Entrada general', 50.00, 2000, 2000, 0, '2024-03-01 10:00:00', '2025-12-07 23:59:00', true, 10, 1, 9),
(24, 'Preferencial', 'Zona preferencial', 80.00, 2000, 2000, 0, '2024-03-01 10:00:00', '2025-12-07 23:59:00', true, 8, 2, 9),
(25, 'VIP', 'Zona VIP', 120.00, 1000, 1000, 0, '2024-03-01 10:00:00', '2025-12-07 23:59:00', true, 4, 5, 9),
-- Festival Mixto (id_evento=10)
(26, 'General', 'Acceso general al festival', 90.00, 1500, 1500, 0, '2024-02-20 10:00:00', '2025-11-30 17:00:00', true, 8, 25, 10),
(27, 'Preferencial', 'Zona preferencial con asientos', 150.00, 800, 800, 0, '2024-02-20 10:00:00', '2025-11-30 17:00:00', true, 6, 26, 10),
(28, 'VIP Lounge', 'Acceso a lounge VIP con bar incluido', 280.00, 200, 200, 0, '2024-02-20 10:00:00', '2025-11-30 17:00:00', true, 4, 27, 10)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 6. REGLAS DE PUNTOS Y FIDELIZACIÓN
-- ============================================================================

-- tipo_regla es smallint: CANJE=0, COMPRA=1 (según orden del enum)
INSERT INTO reglapuntos (id_regla, soles_por_punto, tipo_regla, activo, estado) VALUES
(1, 10.00, 1, true, true),  -- COMPRA (1)
(2, 100.00, 0, true, true)  -- CANJE (0)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 7. CÓDIGOS PROMOCIONALES
-- ============================================================================

INSERT INTO codigo_promocional (id_codigo_promocional, codigo, descripcion, fecha_fin, tipo, valor, stock, cantidad_por_cliente) VALUES
(1, 'BIENVENIDA20', 'Descuento del 20% para nuevos clientes', '2025-12-31 23:59:59', 'PORCENTAJE', 20.00, 1000, 1),
(2, 'VERANO50', 'Descuento fijo de S/ 50 en compras mayores a S/ 200', '2025-12-31 23:59:59', 'MONTO_FIJO', 50.00, 500, 2),
(3, 'VIP100', 'Descuento de S/ 100 para membresías VIP', '2025-12-31 23:59:59', 'MONTO_FIJO', 100.00, 200, 1),
(4, 'ESTUDIANTE15', '15% de descuento para estudiantes', '2025-12-31 23:59:59', 'PORCENTAJE', 15.00, 2000, 1)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 8. CARROS DE COMPRAS
-- ============================================================================

-- Crear 5 carros (uno para cada orden, ya que id_carro_compra es NOT NULL)
INSERT INTO carro_compras (id_carro, fecha_creacion, fecha_actualizacion, subtotal, total, activo, id_cliente, id_evento_actual) VALUES
(1, '2024-11-01 10:00:00', '2024-11-01 10:00:00', 0.0, 0.0, true, 10, 1),
(2, '2024-11-01 11:00:00', '2024-11-01 11:00:00', 0.0, 0.0, true, 11, 2),
(3, '2024-11-01 12:00:00', '2024-11-01 12:00:00', 0.0, 0.0, true, 12, NULL),
(4, '2024-11-04 10:00:00', '2024-11-04 10:00:00', 0.0, 0.0, true, 13, NULL),
(5, '2024-11-05 10:00:00', '2024-11-05 10:00:00', 0.0, 0.0, true, 14, NULL)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 9. ÓRDENES DE COMPRA
-- ============================================================================

-- NOTA: id_carro_compra es NOT NULL según el esquema actual, por lo que todas
-- las órdenes deben tener un carro asociado. Ya creamos 5 carros (1-5).
INSERT INTO orden_compra (id_orden_compra, fecha_orden, subtotal, descuento_por_membrecia, descuento_por_canje, igv, total, estado, codigo_seguimiento, metodo_pago, activo, usuario_creacion, fecha_creacion, id_cliente, id_carro_compra, fecha_expiracion) VALUES
(1, '2024-11-01', 300.00, 0.00, 0.00, 45.76, 345.76, 'APROBADO', 'OC-2024-0001', 'TARJETA', true, 10, '2024-11-01', 10, 1, '2024-11-02 23:59:59'),
(2, '2024-11-02', 480.00, 24.00, 0.00, 69.66, 525.66, 'APROBADO', 'OC-2024-0002', 'TRANSFERENCIA', true, 11, '2024-11-02', 11, 2, '2024-11-03 23:59:59'),
(3, '2024-11-03', 600.00, 60.00, 0.00, 91.86, 631.86, 'PENDIENTE', 'OC-2024-0003', NULL, true, 12, '2024-11-03', 12, 3, '2024-11-04 23:59:59'),
(4, '2024-11-04', 800.00, 80.00, 0.00, 122.03, 842.03, 'APROBADO', 'OC-2024-0004', 'YAPE', true, 13, '2024-11-04', 13, 4, '2024-11-05 23:59:59'),
(5, '2024-11-05', 1200.00, 180.00, 0.00, 172.88, 1192.88, 'APROBADO', 'OC-2024-0005', 'TARJETA', true, 14, '2024-11-05', 14, 5, '2024-11-06 23:59:59')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 10. ITEMS DE CARRITO (Relacionados con Órdenes de Compra y Tipos de Ticket)
-- ============================================================================

INSERT INTO item_carrito (id_item_carrito, cantidad, precio, descuento, precio_final, fecha_agregado, activo, id_carro_compra, id_orden_compra, id_tipo_ticket) VALUES
-- Items de Orden 1 (Cliente 10)
(1, 2, 150.00, 0.00, 300.00, '2024-11-01', true, 1, 1, 2),
-- Items de Orden 2 (Cliente 11)
(2, 3, 120.00, 0.00, 360.00, '2024-11-02', true, 2, 2, 4),
(3, 1, 120.00, 0.00, 120.00, '2024-11-02', true, 2, 2, 4),
-- Items de Orden 3 (Cliente 12)
(4, 2, 280.00, 0.00, 560.00, '2024-11-03', true, 3, 3, 8),
(5, 1, 40.00, 0.00, 40.00, '2024-11-03', true, 3, 3, 12),
-- Items de Orden 4 (Cliente 13)
(6, 4, 180.00, 0.00, 720.00, '2024-11-04', true, 4, 4, 15),
(7, 1, 80.00, 0.00, 80.00, '2024-11-04', true, 4, 4, 24),
-- Items de Orden 5 (Cliente 14)
(8, 3, 400.00, 0.00, 1200.00, '2024-11-05', true, 5, 5, 10)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 11. TICKETS (Relacionados con Eventos, TipoTicket, ItemCarrito, OrdenCompra y Cliente)
-- ============================================================================

INSERT INTO ticket (id_ticket, codigo_qr, asiento, fila, precio, estado, activo, usuario_creacion, fecha_creacion, id_evento, id_tipo_ticket, id_item_carrito, id_orden_compra, id_cliente, nombre_asistente, apellido_asistente, documento_asistente, tipo_documento_asistente, contador_transferencias, fecha_ultima_transferencia) VALUES
-- Tickets de Orden 1 (Cliente 10)
(1, 'QR-ROCK-001', 'A12', 'A', 150.00, 'VENDIDA', true, 10, '2024-11-01', 1, 2, 1, 1, 10, 'Juan', 'Pérez García', '72345678', 'DNI', 0, NULL),
(2, 'QR-ROCK-002', 'A13', 'A', 150.00, 'VENDIDA', true, 10, '2024-11-01', 1, 2, 1, 1, 10, 'María', 'Pérez López', '73456789', 'DNI', 0, NULL),
-- Tickets de Orden 2 (Cliente 11)
(3, 'QR-ELEC-001', NULL, NULL, 120.00, 'VENDIDA', true, 11, '2024-11-02', 2, 4, 2, 2, 11, 'María', 'González López', '73456789', 'DNI', 0, NULL),
(4, 'QR-ELEC-002', NULL, NULL, 120.00, 'VENDIDA', true, 11, '2024-11-02', 2, 4, 2, 2, 11, 'Carlos', 'González Mendoza', '74567890', 'DNI', 0, NULL),
(5, 'QR-ELEC-003', NULL, NULL, 120.00, 'VENDIDA', true, 11, '2024-11-02', 2, 4, 2, 2, 11, 'Ana', 'González Silva', '75678901', 'DNI', 0, NULL),
(6, 'QR-ELEC-004', NULL, NULL, 120.00, 'VENDIDA', true, 11, '2024-11-02', 2, 4, 3, 2, 11, 'Luis', 'González Torres', '76789012', 'DNI', 0, NULL),
-- Tickets de Orden 3 (Cliente 12)
(7, 'QR-POP-001', 'B25', 'B', 280.00, 'RESERVADA', true, 12, '2024-11-03', 3, 8, 4, 3, 12, 'Carlos', 'Rodríguez Mendoza', '74567890', 'DNI', 0, NULL),
(8, 'QR-POP-002', 'B26', 'B', 280.00, 'RESERVADA', true, 12, '2024-11-03', 3, 8, 4, 3, 12, 'Sofia', 'Rodríguez Vargas', '78901234', 'DNI', 0, NULL),
-- Tickets de Orden 4 (Cliente 13)
(9, 'QR-REG-001', 'C10', 'C', 180.00, 'VENDIDA', true, 13, '2024-11-04', 5, 15, 6, 4, 13, 'Ana', 'Martínez Silva', '75678901', 'DNI', 0, NULL),
(10, 'QR-REG-002', 'C11', 'C', 180.00, 'VENDIDA', true, 13, '2024-11-04', 5, 15, 6, 4, 13, 'Pedro', 'Martínez Ramírez', '77890123', 'DNI', 0, NULL),
(11, 'QR-REG-003', 'C12', 'C', 180.00, 'VENDIDA', true, 13, '2024-11-04', 5, 15, 6, 4, 13, 'Diego', 'Martínez Fernández', '79012345', 'DNI', 0, NULL),
(12, 'QR-REG-004', 'C13', 'C', 180.00, 'VENDIDA', true, 13, '2024-11-04', 5, 15, 6, 4, 13, 'Laura', 'Martínez Herrera', 'P1234567', 'PASAPORTE', 0, NULL),
-- Tickets de Orden 5 (Cliente 14)
(13, 'QR-POP-CAMP-001', NULL, NULL, 400.00, 'VENDIDA', true, 14, '2024-11-05', 3, 10, 8, 5, 14, 'Luis', 'Sánchez Torres', '76789012', 'DNI', 0, NULL),
(14, 'QR-POP-CAMP-002', NULL, NULL, 400.00, 'VENDIDA', true, 14, '2024-11-05', 3, 10, 8, 5, 14, 'Sofia', 'Sánchez Vargas', '78901234', 'DNI', 0, NULL),
(15, 'QR-POP-CAMP-003', NULL, NULL, 400.00, 'VENDIDA', true, 14, '2024-11-05', 3, 10, 8, 5, 14, 'Roberto', 'Sánchez Torres', '70123456', 'DNI', 0, NULL)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 12. PAGOS Y COMPROBANTES
-- ============================================================================

INSERT INTO pago (id_pago, metodo, monto, estado, fecha_pago, activo, usuario_creacion, fecha_creacion, id_orden) VALUES
(1, 'TARJETA', 345.76, 'APROBADO', '2024-11-01', true, 10, '2024-11-01', 1),
(2, 'TRANSFERENCIA', 525.66, 'APROBADO', '2024-11-02', true, 11, '2024-11-02', 2),
(3, NULL, NULL, 'PENDIENTE', NULL, true, 12, '2024-11-03', 3),
(4, 'YAPE', 842.03, 'APROBADO', '2024-11-04', true, 13, '2024-11-04', 4),
(5, 'TARJETA', 1192.88, 'APROBADO', '2024-11-05', true, 14, '2024-11-05', 5)
ON CONFLICT DO NOTHING;

INSERT INTO comprobantepago (id_comprobante, numero_serie, fecha_emision, total, activo, usuario_creacion, fecha_creacion, dni, id_pago) VALUES
(1, 'B001-000001', '2024-11-01 14:30:00', 345.76, true, 10, '2024-11-01', '72345678', 1),
(2, 'B001-000002', '2024-11-02 15:45:00', 525.66, true, 11, '2024-11-02', '73456789', 2),
(3, 'B001-000003', '2024-11-04 16:20:00', 842.03, true, 13, '2024-11-04', '75678901', 4),
(4, 'B001-000004', '2024-11-05 17:10:00', 1192.88, true, 14, '2024-11-05', '76789012', 5)
ON CONFLICT DO NOTHING;

INSERT INTO boleta (id_boleta, dni, nombre_cliente, id_comprobante) VALUES
(1, '72345678', 'Juan Pérez García', 1),
(2, '73456789', 'María González López', 2),
(3, '75678901', 'Ana Martínez Silva', 3),
(4, '76789012', 'Luis Sánchez Torres', 4)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 13. PUNTOS Y CANJES
-- ============================================================================

-- tipo_transaccion es smallint: GANADO=0, PERDIDO=1 (según orden del enum)
-- estado es VARCHAR según constraint: ACTIVO, VENCIDO, CANJEADO
INSERT INTO puntos (id_puntos, cant_puntos, fecha_vencimiento, fecha_transaccion, tipo_transaccion, activo, estado, id_regla, id_cliente) VALUES
-- Puntos ganados por cliente 11 (compra de S/ 480)
(1, 48, '2025-11-02', '2024-11-02', 0, true, 'ACTIVO', 1, 11),  -- GANADO=0
-- Puntos ganados por cliente 12 (compra de S/ 600)
(2, 60, '2025-11-03', '2024-11-03', 0, true, 'ACTIVO', 1, 12),  -- GANADO=0
-- Puntos ganados por cliente 13 (compra de S/ 800)
(3, 80, '2025-11-04', '2024-11-04', 0, true, 'ACTIVO', 1, 13),  -- GANADO=0
-- Puntos ganados por cliente 14 (compra de S/ 1200)
(4, 120, '2025-11-05', '2024-11-05', 0, true, 'ACTIVO', 1, 14),  -- GANADO=0
-- Puntos canjeados (ejemplo: cliente 14 canjea puntos)
(5, 5000, '2025-11-06', '2024-11-06', 1, true, 'CANJEADO', 2, 14)  -- PERDIDO=1
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 14. DESCUENTOS REALIZADOS (Códigos Promocionales Aplicados)
-- ============================================================================

INSERT INTO descuentos_realizados (id_descuento_realizado, id_codigo_promocional, id_orden_compra, valor) VALUES
(1, 1, 2, 96.00),  -- 20% de descuento en orden 2 (S/ 480 * 0.20 = S/ 96)
(2, 2, 4, 50.00)   -- Descuento fijo de S/ 50 en orden 4
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 15. TRANSFERENCIAS DE ENTRADAS
-- ============================================================================

INSERT INTO transferencias (id_transferencia, id_ticket, id_emisor, id_receptor, fecha_transferencia) VALUES
(1, 3, 11, 12, '2024-11-10 10:00:00'),  -- María transfiere ticket a Carlos
(2, 4, 11, 13, '2024-11-10 11:00:00')   -- María transfiere otro ticket a Ana
ON CONFLICT DO NOTHING;

-- Actualizar contadores de transferencias en tickets
UPDATE ticket SET contador_transferencias = 1, fecha_ultima_transferencia = '2024-11-10 10:00:00' WHERE id_ticket = 3;
UPDATE ticket SET contador_transferencias = 1, fecha_ultima_transferencia = '2024-11-10 11:00:00' WHERE id_ticket = 4;

-- ============================================================================
-- 16. AUDITORÍA ADMINISTRADOR
-- ============================================================================

INSERT INTO auditoria_admin (id_auditoria, accion, modulo, fecha, ip_origen, descripcion, navegador, sistema_operativo, activo, usuario) VALUES
(1, 'CREAR', 'Eventos', '2024-01-10 10:00:00', '192.168.1.100', 'Creación del evento "Concierto Rock Nacional"', 'Chrome 120', 'Windows 11', true, 1),
(2, 'ACTUALIZAR', 'Eventos', '2024-01-11 14:30:00', '192.168.1.101', 'Actualización del evento "Festival de Música Electrónica"', 'Firefox 121', 'Windows 10', true, 2),
(3, 'CREAR', 'Locales', '2024-01-01 09:00:00', '192.168.1.102', 'Creación del local "Estadio Nacional"', 'Edge 120', 'Windows 11', true, 1),
(4, 'LEER', 'Clientes', '2024-11-05 16:00:00', '192.168.1.100', 'Consulta de lista de clientes', 'Chrome 120', 'Windows 11', true, 1),
(5, 'ELIMINAR', 'Eventos', '2024-11-20 11:00:00', '192.168.1.103', 'Eliminación lógica del evento cancelado', 'Safari 17', 'macOS 14', true, 2)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- ACTUALIZAR SECUENCIAS
-- ============================================================================

SELECT setval(pg_get_serial_sequence('departamento', 'id_departamento'), COALESCE((SELECT MAX(id_departamento) FROM departamento), 1));
SELECT setval(pg_get_serial_sequence('provincia', 'id_provincia'), COALESCE((SELECT MAX(id_provincia) FROM provincia), 1));
SELECT setval(pg_get_serial_sequence('distrito', 'id_distrito'), COALESCE((SELECT MAX(id_distrito) FROM distrito), 1));
SELECT setval(pg_get_serial_sequence('persona', 'id_persona'), COALESCE((SELECT MAX(id_persona) FROM persona), 1));
SELECT setval(pg_get_serial_sequence('local', 'id_local'), COALESCE((SELECT MAX(id_local) FROM local), 1));
SELECT setval(pg_get_serial_sequence('zona', 'id_zona'), COALESCE((SELECT MAX(id_zona) FROM zona), 1));
SELECT setval(pg_get_serial_sequence('evento', 'id_evento'), COALESCE((SELECT MAX(id_evento) FROM evento), 1));
SELECT setval(pg_get_serial_sequence('tipo_ticket', 'id_tipo_ticket'), COALESCE((SELECT MAX(id_tipo_ticket) FROM tipo_ticket), 1));
SELECT setval(pg_get_serial_sequence('orden_compra', 'id_orden_compra'), COALESCE((SELECT MAX(id_orden_compra) FROM orden_compra), 1));
SELECT setval(pg_get_serial_sequence('carro_compras', 'id_carro'), COALESCE((SELECT MAX(id_carro) FROM carro_compras), 1));
SELECT setval(pg_get_serial_sequence('item_carrito', 'id_item_carrito'), COALESCE((SELECT MAX(id_item_carrito) FROM item_carrito), 1));
SELECT setval(pg_get_serial_sequence('ticket', 'id_ticket'), COALESCE((SELECT MAX(id_ticket) FROM ticket), 1));
SELECT setval(pg_get_serial_sequence('pago', 'id_pago'), COALESCE((SELECT MAX(id_pago) FROM pago), 1));
SELECT setval(pg_get_serial_sequence('comprobantepago', 'id_comprobante'), COALESCE((SELECT MAX(id_comprobante) FROM comprobantepago), 1));
SELECT setval(pg_get_serial_sequence('boleta', 'id_boleta'), COALESCE((SELECT MAX(id_boleta) FROM boleta), 1));
SELECT setval(pg_get_serial_sequence('reglapuntos', 'id_regla'), COALESCE((SELECT MAX(id_regla) FROM reglapuntos), 1));
SELECT setval(pg_get_serial_sequence('puntos', 'id_puntos'), COALESCE((SELECT MAX(id_puntos) FROM puntos), 1));
SELECT setval(pg_get_serial_sequence('codigo_promocional', 'id_codigo_promocional'), COALESCE((SELECT MAX(id_codigo_promocional) FROM codigo_promocional), 1));
SELECT setval(pg_get_serial_sequence('descuentos_realizados', 'id_descuento_realizado'), COALESCE((SELECT MAX(id_descuento_realizado) FROM descuentos_realizados), 1));
SELECT setval(pg_get_serial_sequence('transferencias', 'id_transferencia'), COALESCE((SELECT MAX(id_transferencia) FROM transferencias), 1));
SELECT setval(pg_get_serial_sequence('auditoria_admin', 'id_auditoria'), COALESCE((SELECT MAX(id_auditoria) FROM auditoria_admin), 1));

-- ============================================================================
-- RESUMEN DE DATOS INSERTADOS
-- ============================================================================
-- 
-- GEOGRAFÍA:
--   - Departamentos: 5
--   - Provincias: 7
--   - Distritos: 15
--
-- USUARIOS:
--   - Personas: 13 (3 Administradores + 10 Clientes)
--   - Administradores: 3
--   - Clientes: 10 (con diferentes niveles: BRONCE, PLATA, ORO)
--
-- EVENTOS:
--   - Locales: 10
--   - Zonas: 27
--   - Eventos: 10 (usando tipos permitidos: ROCK, POP, ELECTRONICA, etc.)
--   - Tipos de Tickets: 28
--
-- COMPRAS:
--   - Carros de Compras: 5
--   - Órdenes de Compra: 5
--   - Items de Carrito: 8
--   - Tickets: 15
--
-- PAGOS:
--   - Pagos: 5
--   - Comprobantes de Pago: 4
--   - Boletas: 4
--
-- FIDELIZACIÓN:
--   - Reglas de Puntos: 2 (tipo_regla como smallint: 0=CANJE, 1=COMPRA)
--   - Puntos: 5 (tipo_transaccion como smallint: 0=GANADO, 1=PERDIDO)
--   - Códigos Promocionales: 4
--   - Descuentos Realizados: 2
--
-- OTROS:
--   - Transferencias: 2
--   - Registros de Auditoría: 5
--
-- ============================================================================
-- INFORMACIÓN IMPORTANTE SOBRE DISCREPANCIAS
-- ============================================================================
--
-- PROBLEMA IDENTIFICADO:
-- El esquema de la base de datos tiene diferencias con el modelo Java:
--
-- 1. TIPO EVENTO:
--    - Modelo Java tiene: CONCIERTO, FESTIVAL, OBRA_TEATRAL, etc.
--    - BD solo permite: ROCK, POP, ELECTRONICA, URBANO, METAL, PUNK, REGGAE, ROCKANDPOP, REGUETON
--    - SOLUCIÓN: El script usa los valores permitidos por la BD actual
--
-- 2. TIPO MEMBRESÍA:
--    - Modelo Java tiene: BRONCE, PLATA, ORO
--    - BD permite: BRONCE, PLATA, ORO (coincide con el modelo)
--    - SOLUCIÓN: El script usa los valores del modelo Java (BRONCE, PLATA, ORO)
--
-- 3. TIPO REGLA Y TIPO TRANSACCIÓN:
--    - Modelo Java los define como @Enumerated(EnumType.STRING)
--    - BD los tiene como smallint (ORDINAL)
--    - SOLUCIÓN: El script usa valores numéricos (0=CANJE/GANADO, 1=COMPRA/PERDIDO)
--
-- 4. ESTADO COMPRA:
--    - Modelo Java tiene: APROBADO, RECHAZADO, PENDIENTE, ANULADO
--    - BD solo permite: APROBADO, RECHAZADO, PENDIENTE
--    - SOLUCIÓN: El script usa solo los 3 estados permitidos
--
-- RECOMENDACIÓN:
-- Para alinear el esquema con el modelo, se debe:
-- 1. Ejecutar las migraciones de JPA/Hibernate con spring.jpa.hibernate.ddl-auto=update
-- 2. O modificar manualmente las constraints en la BD para que coincidan con el modelo
--
-- ============================================================================
-- INFORMACIÓN DE ACCESO
-- ============================================================================
--
-- Usuario Administrador de prueba:
--   Email: admin@pucp.edu.pe
--   Contraseña: password123
--
-- Usuarios Cliente de prueba:
--   Email: juan.perez@fasticket.com
--   Contraseña: password123
--
--   Email: maria.gonzalez@fasticket.com
--   Contraseña: password123
--
--   Email: carlos.rodriguez@fasticket.com
--   Contraseña: password123
--
-- Todos los usuarios tienen la misma contraseña para facilitar pruebas.
-- Las contraseñas están encriptadas con BCrypt.
--
-- ============================================================================
