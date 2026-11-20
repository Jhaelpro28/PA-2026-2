

create table usuario(
	id bigint not null auto_increment,
    nombre varchar(255) not null,
    documento varchar(255) not null,
    password varchar(255) not null,
    username varchar(255) not null,
    primary key(id),
    constraint uq_usuario_username unique(username)
);
create table zona(
	id bigint not null auto_increment,
    nombre varchar(255) not null,
    tipo varchar(255) not null,
    capacidad int not null,
    primary key(id)
);
create table espacio(
	id bigint not null auto_increment,
    numero int not null,
    ocupado boolean not null default false,
    zona_id bigint not null,
    primary key(id),
    constraint fk_espacio_zona foreign key(zona_id) references zona(id)
);
create table vehiculo(
	id bigint not null auto_increment,
    placa varchar(10) not null,
    tipo enum('CARRO', 'MOTO') not null,
    primary key(id),
    constraint uq_vehiculo_placa unique(placa)
);
create table registro(
	id bigint not null auto_increment,
    hora_entrada datetime(6) not null,
    hora_salida datetime(6),
    valor_pagado double,
    vehiculo_id bigint not null,
    espacio_id bigint not null,
    primary key(id),
    constraint fk_registro_vehiculo foreign key(vehiculo_id) references vehiculo(id),
    constraint fk_registro_espacio foreign key(espacio_id) references espacio(id)
);
insert into usuario(nombre, documento, password, username) values
('Administrador', '1142917184', '1234', 'admin');

insert into zona(nombre, tipo, capacidad) values
('Zona Norte', 'Comercial', 5),
('Zona Sur', 'Residencial', 3);

insert into espacio(numero, ocupado, zona_id) values
	(1, FALSE, 1),
    (2, FALSE, 1),
    (3, FALSE, 1),
    (4, FALSE, 1),
    (5, FALSE, 1);
    
    insert into espacio(numero, ocupado, zona_id) values
    (1, FALSE, 2),
    (2, FALSE, 2),
    (3, FALSE, 2);


select * from usuario;
select * from zona;
select * from espacio;
select * from vehiculo;
select * from registro;