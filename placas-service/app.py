"""Servicio HTTP local de reconocimiento de placas (lo consume Spring Boot)."""
from flask import Flask, jsonify, request

import reconocedor

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 10 * 1024 * 1024   # 10 MB


@app.get("/salud")
def salud():
    return jsonify({"estado": "ok"})


@app.post("/reconocer")
def reconocer():
    archivo = request.files.get("imagen")
    if archivo is None or archivo.filename == "":
        return jsonify({"ok": False, "codigo": "SIN_IMAGEN", "mensaje": "No se recibio ninguna imagen."}), 400
    try:
        return jsonify(reconocedor.reconocer_placa(archivo.read()))
    except Exception as e:                      # nunca devolver una placa inventada
        app.logger.exception("Error reconociendo placa")
        return jsonify({"ok": False, "codigo": "ERROR_INTERNO",
                        "mensaje": "Ocurrio un error al procesar la imagen. Intente de nuevo o escriba la placa."}), 500


@app.errorhandler(413)
def demasiado_grande(_):
    return jsonify({"ok": False, "codigo": "IMAGEN_GRANDE", "mensaje": "La imagen supera 10 MB."}), 413


if __name__ == "__main__":
    reconocedor.obtener_lector()                # descarga/carga modelos al iniciar
    app.run(host="127.0.0.1", port=5001)
