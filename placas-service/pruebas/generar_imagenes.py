"""Genera fotos SINTETICAS de placas colombianas para probar el pipeline.
No reemplazan fotos reales: sirven para comprobar deteccion, OCR y validaciones."""
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

FUENTE = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
SALIDA = Path(sys.argv[1] if len(sys.argv) > 1 else "../imagenes_prueba")
SALIDA.mkdir(exist_ok=True, parents=True)
rng = np.random.default_rng(7)


def placa_carro(texto, color=(255, 205, 0)):
    im = Image.new("RGB", (520, 260), color)
    d = ImageDraw.Draw(im)
    d.rectangle([4, 4, 515, 255], outline=(0, 0, 0), width=6)
    d.text((260, 38), "COLOMBIA", font=ImageFont.truetype(FUENTE, 34), fill=(0, 0, 0), anchor="mm")
    d.text((260, 140), texto, font=ImageFont.truetype(FUENTE, 120), fill=(0, 0, 0), anchor="mm")
    d.text((260, 222), "BOGOTA D.C.", font=ImageFont.truetype(FUENTE, 30), fill=(0, 0, 0), anchor="mm")
    return np.array(im)[:, :, ::-1]


def placa_moto(texto):
    im = Image.new("RGB", (300, 260), (255, 205, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([4, 4, 295, 255], outline=(0, 0, 0), width=6)
    d.text((150, 85), texto[:3], font=ImageFont.truetype(FUENTE, 105), fill=(0, 0, 0), anchor="mm")
    d.text((150, 185), texto[3:], font=ImageFont.truetype(FUENTE, 105), fill=(0, 0, 0), anchor="mm")
    return np.array(im)[:, :, ::-1]


def escena(placa, ancho_placa=360, centro=(640, 470), angulo=0, perspectiva=0, brillo=1.0, desenfoque=0, ruido=6):
    fondo = np.full((800, 1280, 3), (140, 135, 130), np.uint8)
    fondo = cv2.GaussianBlur(fondo + rng.integers(0, 40, fondo.shape, dtype=np.uint8), (0, 0), 4)
    cv2.rectangle(fondo, (180, 180), (1100, 700), (60, 40, 120), -1)       # carroceria
    cv2.rectangle(fondo, (260, 230), (1020, 400), (190, 190, 200), -1)     # vidrio/parrilla
    cv2.rectangle(fondo, (300, 600), (980, 650), (30, 30, 30), -1)         # parachoques
    ph, pw = placa.shape[:2]
    esc = ancho_placa / pw
    placa = cv2.resize(placa, None, fx=esc, fy=esc, interpolation=cv2.INTER_AREA)
    ph, pw = placa.shape[:2]
    src = np.float32([[0, 0], [pw, 0], [pw, ph], [0, ph]])
    dx = perspectiva * pw
    dst = np.float32([[dx, 0], [pw - dx * 0.3, dy := perspectiva * ph * 0.4], [pw - dx * 0.3, ph - dy], [dx, ph]])
    M = cv2.getPerspectiveTransform(src, dst)
    M2 = cv2.getRotationMatrix2D((pw / 2, ph / 2), angulo, 1.0)
    placa_t = cv2.warpPerspective(placa, M, (pw, ph), borderValue=(0, 0, 0))
    mascara = cv2.warpPerspective(np.full((ph, pw), 255, np.uint8), M, (pw, ph))
    placa_t = cv2.warpAffine(placa_t, M2, (pw, ph))
    mascara = cv2.warpAffine(mascara, M2, (pw, ph))
    x0, y0 = centro[0] - pw // 2, centro[1] - ph // 2
    roi = fondo[y0:y0 + ph, x0:x0 + pw]
    roi[mascara > 128] = placa_t[mascara > 128]
    img = fondo.astype(np.float32) * brillo
    img += rng.normal(0, ruido, img.shape)
    img = np.clip(img, 0, 255).astype(np.uint8)
    if desenfoque:
        img = cv2.GaussianBlur(img, (0, 0), desenfoque)
    return img


casos = {
    "carro_frontal_ABC123": escena(placa_carro("ABC123")),
    "carro_inclinado_XYZ789": escena(placa_carro("XYZ789"), angulo=-9, perspectiva=0.12),
    "carro_blanca_JKL456": escena(placa_carro("JKL456", (245, 245, 245)), ancho_placa=380),
    "moto_FGH45K": escena(placa_moto("FGH45K"), ancho_placa=230),
    "carro_oscuro_QWE321": escena(placa_carro("QWE321"), brillo=0.28),
    "carro_borroso_RTY654": escena(placa_carro("RTY654"), desenfoque=14),
    "sin_placa": escena(np.full((260, 520, 3), (90, 90, 95), np.uint8)),
}
for nombre, img in casos.items():
    cv2.imwrite(str(SALIDA / f"{nombre}.jpg"), img, [cv2.IMWRITE_JPEG_QUALITY, 90])
print("generadas", len(casos), "imagenes en", SALIDA.resolve())
