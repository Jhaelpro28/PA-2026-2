# Servicio lector de placas (Python)

Servicio local que usa **OpenCV + EasyOCR** para leer placas colombianas (carro `ABC123`, moto `ABC12D`).
Spring Boot le envía la foto (`/api/placas/reconocer`) y el navegador nunca habla con Python directamente.

## Instalar (una sola vez)
```bash
cd placas-service
python -m venv .venv
.venv\Scripts\activate            # Windows   (Linux/Mac: source .venv/bin/activate)
pip install -r requirements.txt   # instala torch (~200 MB-2 GB según el equipo)
```
La primera ejecución descarga los modelos de EasyOCR (~100 MB, requiere internet una vez).

## Iniciar
```bash
python app.py        # queda en http://127.0.0.1:5001  (probar: http://127.0.0.1:5001/salud)
```
Debe estar corriendo **antes** de usar "Escanear placa". Si no lo está, la página avisa y permite escribir la placa a mano.

## Probar
```bash
cd pruebas
python generar_imagenes.py ../imagenes_prueba   # fotos sintéticas
python probar_reconocedor.py
```
