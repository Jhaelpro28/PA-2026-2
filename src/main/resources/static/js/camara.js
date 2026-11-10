/*
 * Escanear placa: abre la camara (o carga una imagen), la envia a /api/placas/reconocer,
 * muestra la placa para que el usuario la verifique y la usa en la pagina actual.
 * Se activa solo en paginas que incluyan el fragmento fragments/escaner.
 */
(function () {
    'use strict';

    var bloque = document.querySelector('.escaner-placa');
    if (!bloque) { return; }

    var modo = bloque.dataset.modo;
    var destino = document.querySelector(bloque.dataset.destino);
    var video = bloque.querySelector('#video-camara');
    var contCamara = bloque.querySelector('#camara-contenedor');
    var estado = bloque.querySelector('#camara-estado');
    var zonaResultado = bloque.querySelector('[data-zona="resultado"]');
    var vista = bloque.querySelector('[data-zona="vista-previa"]');
    var campoPlaca = bloque.querySelector('[data-zona="placa-reconocida"]');
    var listaAvisos = bloque.querySelector('[data-zona="avisos"]');
    var info = bloque.querySelector('[data-zona="info"]');
    var inputArchivo = bloque.querySelector('[data-accion="subir"]');
    var flujo = null;
    var tipoSugerido = null;
    var ocupado = false;

    function mensaje(texto, clase) {
        estado.textContent = texto;
        estado.className = 'camara-estado' + (clase ? ' ' + clase : '');
    }

    function normalizar(t) {
        return (t || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
    }

    function bloquear(si) {
        ocupado = si;
        bloque.querySelectorAll('button').forEach(function (b) { b.disabled = si; });
    }

    /* ---------------- camara ---------------- */
    function abrirCamara() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            mensaje('Este navegador no permite usar la cámara (se necesita HTTPS o localhost). ' +
                'Use "Subir imagen" o escriba la placa manualmente.', 'error');
            return;
        }
        navigator.mediaDevices.getUserMedia({
            video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 } }, audio: false
        }).then(function (s) {
            flujo = s;
            video.srcObject = s;
            contCamara.hidden = false;
            mensaje('Cámara lista. Encuadre la placa dentro del recuadro y pulse "Tomar foto".');
        }).catch(function (err) {
            var m = 'No se pudo abrir la cámara.';
            if (err && (err.name === 'NotAllowedError' || err.name === 'SecurityError')) {
                m = 'Permiso de cámara denegado. Habilítelo en el navegador o use "Subir imagen".';
            } else if (err && (err.name === 'NotFoundError' || err.name === 'OverconstrainedError')) {
                m = 'No se encontró ninguna cámara en este dispositivo. Use "Subir imagen".';
            } else if (err && err.name === 'NotReadableError') {
                m = 'La cámara está siendo usada por otra aplicación.';
            }
            mensaje(m, 'error');
        });
    }

    function cerrarCamara() {
        if (flujo) { flujo.getTracks().forEach(function (t) { t.stop(); }); }
        flujo = null;
        video.srcObject = null;
        contCamara.hidden = true;
    }

    function tomarFoto() {
        if (!flujo || !video.videoWidth) {
            mensaje('La cámara aún no está lista.', 'error');
            return;
        }
        var c = document.createElement('canvas');
        c.width = video.videoWidth;
        c.height = video.videoHeight;
        c.getContext('2d').drawImage(video, 0, 0, c.width, c.height);
        c.toBlob(function (blob) {
            if (blob) { procesar(blob); } else { mensaje('No se pudo capturar la imagen.', 'error'); }
        }, 'image/jpeg', 0.92);
    }

    /* ---------------- imagen subida ---------------- */
    function reducir(archivo) {
        // Reduce fotos muy grandes (celulares) para enviar menos datos
        return new Promise(function (resolve) {
            if (!window.createImageBitmap) { resolve(archivo); return; }
            createImageBitmap(archivo).then(function (bmp) {
                var max = 1600, lado = Math.max(bmp.width, bmp.height);
                if (lado <= max) { resolve(archivo); return; }
                var f = max / lado, c = document.createElement('canvas');
                c.width = Math.round(bmp.width * f);
                c.height = Math.round(bmp.height * f);
                c.getContext('2d').drawImage(bmp, 0, 0, c.width, c.height);
                c.toBlob(function (b) { resolve(b || archivo); }, 'image/jpeg', 0.92);
            }).catch(function () { resolve(archivo); });
        });
    }

    function archivoElegido() {
        var f = inputArchivo.files && inputArchivo.files[0];
        inputArchivo.value = '';
        if (!f) { return; }
        if (f.type && f.type.indexOf('image/') !== 0) {
            mensaje('El archivo seleccionado no es una imagen.', 'error');
            return;
        }
        if (f.size > 10 * 1024 * 1024) {
            mensaje('La imagen supera 10 MB. Elija una más pequeña.', 'error');
            return;
        }
        reducir(f).then(procesar);
    }

    /* ---------------- reconocimiento ---------------- */
    function limpiarResultado() {
        listaAvisos.textContent = '';
        info.hidden = true;
        info.textContent = '';
        info.className = 'escaner-info';
    }

    function avisar(lista) {
        (lista || []).forEach(function (a) {
            var li = document.createElement('li');
            li.textContent = a;
            listaAvisos.appendChild(li);
        });
    }

    function procesar(blob) {
        if (ocupado) { return; }
        limpiarResultado();
        tipoSugerido = null;
        vista.src = URL.createObjectURL(blob);
        zonaResultado.hidden = false;
        campoPlaca.value = '';
        bloquear(true);
        mensaje('Procesando imagen… (puede tardar unos segundos)');

        var datos = new FormData();
        datos.append('imagen', blob, 'placa.jpg');

        fetch(bloque.dataset.urlReconocer, { method: 'POST', body: datos, credentials: 'same-origin' })
            .then(function (r) {
                var tipo = r.headers.get('content-type') || '';
                if (r.redirected || tipo.indexOf('json') === -1) {
                    throw { sesion: true };
                }
                return r.json();
            })
            .then(function (d) {
                if (d.ok && d.placa) {
                    campoPlaca.value = d.placa;
                    tipoSugerido = d.tipo_sugerido || null;
                    mensaje(d.mensaje + ' (confianza ' + Math.round((d.confianza || 0) * 100) + '%)', 'ok');
                    avisar(d.advertencias);
                } else {
                    mensaje(d.mensaje || 'No se pudo reconocer la placa.', 'error');
                    avisar(d.advertencias && d.mensaje ? [] : d.advertencias);
                }
                campoPlaca.focus();
            })
            .catch(function (e) {
                mensaje(e && e.sesion ? 'Su sesión expiró. Recargue la página e inicie sesión de nuevo.'
                    : 'No se pudo contactar al lector de placas. Escriba la placa manualmente.', 'error');
                campoPlaca.focus();
            })
            .then(function () { bloquear(false); });
    }

    /* ---------------- confirmar y usar la placa ---------------- */
    function enlace(texto, url) {
        var a = document.createElement('a');
        a.href = url;
        a.textContent = texto;
        return a;
    }

    function boton(texto, fn) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'btn-secundario';
        b.textContent = texto;
        b.addEventListener('click', fn);
        return b;
    }

    function parrafo(texto) {
        var p = document.createElement('p');
        p.style.margin = '0 0 .3rem';
        p.textContent = texto;
        return p;
    }

    function enviarFormulario() {
        var f = destino && destino.closest('form');
        if (f) { f.submit(); }
    }

    function confirmar() {
        var placa = normalizar(campoPlaca.value);
        campoPlaca.value = placa;
        limpiarResultado();
        if (placa.length < 5) {
            info.className = 'escaner-info error';
            info.textContent = 'Escriba una placa válida (por ejemplo ABC123 o ABC12D).';
            info.hidden = false;
            return;
        }
        bloquear(true);
        fetch(bloque.dataset.urlConsultar + '?placa=' + encodeURIComponent(placa), { credentials: 'same-origin' })
            .then(function (r) {
                if (r.redirected || (r.headers.get('content-type') || '').indexOf('json') === -1) { throw {}; }
                return r.json();
            })
            .then(function (v) { aplicar(placa, v); })
            .catch(function () {
                info.className = 'escaner-info error';
                info.textContent = 'No se pudo consultar la placa. Recargue la página e intente de nuevo.';
                info.hidden = false;
            })
            .then(function () { bloquear(false); });
    }

    function aplicar(placa, v) {
        if (destino) { destino.value = placa; }
        info.hidden = false;

        var historial = enlace('Ver historial del vehículo', bloque.dataset.urlVehiculos + '?buscar=' + encodeURIComponent(placa));

        if (v.existe) {
            info.appendChild(parrafo('Vehículo registrado: ' + placa + ' · tipo ' + v.tipo +
                (v.dentro ? ' · actualmente DENTRO (zona ' + v.zona + ', espacio ' + v.espacio + ')' : ' · actualmente fuera.')));
        } else {
            info.className = 'escaner-info aviso';
            info.appendChild(parrafo('La placa ' + placa + ' no está registrada todavía.'));
        }

        if (modo === 'entrada') {
            var tipo = document.querySelector('#tipoVehiculo');
            if (v.existe && tipo) {
                tipo.value = v.tipo;
            } else if (!v.existe && tipo && !tipo.value && tipoSugerido) {
                tipo.value = tipoSugerido;
                info.appendChild(parrafo('Tipo sugerido por el formato de la placa (' + tipoSugerido + '): confírmelo.'));
            }
            if (v.dentro) {
                info.className = 'escaner-info error';
                info.appendChild(parrafo('Este vehículo ya está dentro: no se puede registrar otra entrada.'));
                info.appendChild(enlace('Ir a registrar la salida', bloque.dataset.urlSalida + '?placa=' + encodeURIComponent(placa)));
            } else {
                info.appendChild(parrafo('Complete tipo y zona y pulse "Registrar Entrada" para confirmar.'));
                var zona = document.querySelector('#zonaId');
                if (zona) { zona.focus(); }
            }
        } else if (modo === 'salida') {
            if (v.dentro) {
                info.appendChild(boton('Buscar salida de ' + placa, enviarFormulario));
            } else {
                info.className = 'escaner-info error';
                info.appendChild(parrafo('Este vehículo no está dentro del parqueadero, no hay salida por registrar.'));
            }
        } else {
            info.appendChild(boton('Filtrar listado por ' + placa, enviarFormulario));
        }
        info.appendChild(historial);
    }

    /* ---------------- eventos ---------------- */
    bloque.addEventListener('click', function (e) {
        var accion = e.target && e.target.dataset ? e.target.dataset.accion : null;
        if (!accion || ocupado) { return; }
        if (accion === 'abrir-camara') { abrirCamara(); }
        else if (accion === 'cerrar-camara') { cerrarCamara(); mensaje('Cámara cerrada.'); }
        else if (accion === 'tomar-foto') { tomarFoto(); }
        else if (accion === 'confirmar') { confirmar(); }
    });
    inputArchivo.addEventListener('change', archivoElegido);
    campoPlaca.addEventListener('input', function () { campoPlaca.value = normalizar(campoPlaca.value); });
    campoPlaca.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); confirmar(); }
    });
    window.addEventListener('pagehide', cerrarCamara);
})();
