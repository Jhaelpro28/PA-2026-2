"""
Reconocimiento de placas vehiculares colombianas.

Pasos:
  1. Se buscan regiones candidatas a placa con OpenCV (color amarillo y bordes).
  2. Cada region se endereza (perspectiva) y se lee con EasyOCR.
  3. El texto leido se valida contra los formatos colombianos:
        carro  -> 3 letras + 3 numeros   (ABC123)
        moto   -> 3 letras + 2 numeros + 1 letra (ABC12D)
  4. Si nada cumple el formato NO se inventa nada: se devuelve ok=False.
"""
import re

import cv2
import numpy as np

PERMITIDOS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

# Confusiones tipicas del OCR, solo se usan segun la posicion esperada.
A_DIGITO = {"O": "0", "Q": "0", "D": "0", "I": "1", "L": "1", "Z": "2", "S": "5", "G": "6", "B": "8"}
A_LETRA = {"0": "O", "1": "I", "2": "Z", "5": "S", "6": "G", "8": "B"}

CONF_MINIMA = 0.30          # confianza minima de EasyOCR por lectura
CONF_BUENA = 0.60           # si se alcanza, se deja de buscar
MAX_CANDIDATOS = 5
MAX_LADO = 1280             # la imagen se reduce a este ancho/alto maximo

_reader = None


def obtener_lector():
    """Carga EasyOCR una sola vez (la primera vez descarga los modelos)."""
    global _reader
    if _reader is None:
        import easyocr
        _reader = easyocr.Reader(["en"], gpu=False, verbose=False)
    return _reader


# --------------------------------------------------------------------------
# Calidad de imagen
# --------------------------------------------------------------------------
def evaluar_calidad(gris):
    nitidez = float(cv2.Laplacian(gris, cv2.CV_64F).var())
    brillo = float(gris.mean())
    return {"nitidez": round(nitidez, 1), "brillo": round(brillo, 1)}


def advertencias_calidad(calidad):
    avisos = []
    if calidad["brillo"] < 55:
        avisos.append("La imagen esta muy oscura.")
    elif calidad["brillo"] > 215:
        avisos.append("La imagen esta sobreexpuesta (demasiada luz).")
    if calidad["nitidez"] < 40:
        avisos.append("La imagen esta borrosa.")
    return avisos


# --------------------------------------------------------------------------
# Deteccion de regiones candidatas
# --------------------------------------------------------------------------
def _ordenar_puntos(pts):
    pts = np.array(pts, dtype="float32")
    s = pts.sum(axis=1)
    d = np.diff(pts, axis=1).ravel()
    return np.array([pts[np.argmin(s)], pts[np.argmin(d)], pts[np.argmax(s)], pts[np.argmax(d)]], dtype="float32")


def _enderezar(img, rect):
    """Recorta la region rotada y la deja horizontal."""
    caja = _ordenar_puntos(cv2.boxPoints(rect))
    tl, tr, br, bl = caja
    ancho = int(max(np.linalg.norm(br - bl), np.linalg.norm(tr - tl)))
    alto = int(max(np.linalg.norm(tr - br), np.linalg.norm(tl - bl)))
    if ancho < 20 or alto < 10:
        return None
    destino = np.array([[0, 0], [ancho - 1, 0], [ancho - 1, alto - 1], [0, alto - 1]], dtype="float32")
    M = cv2.getPerspectiveTransform(caja, destino)
    recorte = cv2.warpPerspective(img, M, (ancho, alto))
    if recorte.shape[0] > recorte.shape[1]:          # quedo vertical
        recorte = cv2.rotate(recorte, cv2.ROTATE_90_CLOCKWISE)
    return recorte


def _contornos_a_rects(mascara, area_img, relajado=False):
    contornos, _ = cv2.findContours(mascara, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    rects = []
    for c in contornos:
        area = cv2.contourArea(c)
        if area < area_img * 0.0015 or area > area_img * 0.35:
            continue
        rect = cv2.minAreaRect(c)
        (_, _), (w, h), _ = rect
        if w < 1 or h < 1:
            continue
        razon = max(w, h) / min(w, h)
        # carro ~ 2-3.5 ; moto ~ 1.1-1.6
        if not (1.05 <= razon <= 6.0):
            continue
        relleno = area / (w * h)
        if relleno < (0.45 if relajado else 0.6):
            continue
        forma = 1.0 if (1.8 <= razon <= 4.2 or 1.1 <= razon <= 1.7) else 0.6
        rects.append((relleno * forma, rect))
    return rects


def encontrar_candidatos(img):
    h, w = img.shape[:2]
    area_img = h * w
    gris = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    cand = []

    # (a) placas amarillas (particulares y motos en Colombia)
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    amarillo = cv2.inRange(hsv, (15, 90, 110), (38, 255, 255))
    amarillo = cv2.morphologyEx(amarillo, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_RECT, (15, 9)))
    amarillo = cv2.morphologyEx(amarillo, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5)))
    for puntaje, r in _contornos_a_rects(amarillo, area_img):
        cand.append((puntaje * 1.5, r))

    # (b) bordes: sirve tambien para placas blancas
    suave = cv2.bilateralFilter(gris, 9, 40, 40)
    bordes = cv2.Canny(suave, 60, 160)
    bordes = cv2.morphologyEx(bordes, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_RECT, (25, 9)))
    bordes = cv2.dilate(bordes, None, iterations=1)
    for puntaje, r in _contornos_a_rects(bordes, area_img, relajado=True):
        cand.append((puntaje, r))

    cand.sort(key=lambda x: x[0], reverse=True)

    # quitar duplicados (centros muy cercanos)
    finales = []
    for _, r in cand:
        (cx, cy), (rw, rh), _ = r
        a = rw * rh
        repetido = False
        for (fx, fy), (fw, fh), _ in finales:
            if np.hypot(cx - fx, cy - fy) < 0.04 * max(h, w) and 0.5 < a / (fw * fh) < 2.0:
                repetido = True
                break
        if not repetido:
            finales.append(r)
        if len(finales) >= MAX_CANDIDATOS:
            break

    recortes = []
    for r in finales:
        (cx, cy), (rw, rh), ang = r
        r_pad = ((cx, cy), (rw * 1.08 + 6, rh * 1.15 + 6), ang)
        rec = _enderezar(img, r_pad)
        if rec is not None:
            recortes.append(rec)
    return recortes


# --------------------------------------------------------------------------
# OCR y validacion de formato
# --------------------------------------------------------------------------
def _normalizar_luz(img):
    """Estira el contraste cuando la imagen esta oscura o lavada."""
    gris = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY) if img.ndim == 3 else img
    lo, hi = np.percentile(gris, (2, 98))
    if hi - lo < 1:
        return img
    out = (img.astype(np.float32) - lo) * (255.0 / (hi - lo))
    return np.clip(out, 0, 255).astype(np.uint8)


def _preparar(recorte, modo):
    recorte = _normalizar_luz(recorte)
    ancho_objetivo = 480
    f = ancho_objetivo / recorte.shape[1]
    r = cv2.resize(recorte, None, fx=f, fy=f, interpolation=cv2.INTER_CUBIC)
    if modo == "color":
        return r
    g = cv2.cvtColor(r, cv2.COLOR_BGR2GRAY)
    g = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(4, 4)).apply(g)
    if modo == "umbral":
        g = cv2.GaussianBlur(g, (3, 3), 0)
        g = cv2.adaptiveThreshold(g, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 12)
    return g


def _leer(imagen):
    """Devuelve (texto_en_orden_de_lectura, confianza_promedio, lecturas)."""
    lecturas = obtener_lector().readtext(imagen, allowlist=PERMITIDOS, detail=1, paragraph=False)
    if not lecturas:
        return "", 0.0, []
    items = []
    for caja, texto, conf in lecturas:
        ys = [p[1] for p in caja]
        xs = [p[0] for p in caja]
        items.append({"x": min(xs), "yc": (min(ys) + max(ys)) / 2, "alto": max(ys) - min(ys), "t": texto, "c": float(conf)})
    alto_med = float(np.median([i["alto"] for i in items])) or 1.0
    items.sort(key=lambda i: i["yc"])
    filas, actual = [], [items[0]]
    for it in items[1:]:
        if abs(it["yc"] - actual[-1]["yc"]) <= 0.6 * alto_med:
            actual.append(it)
        else:
            filas.append(actual)
            actual = [it]
    filas.append(actual)
    orden = []
    for fila in filas:
        orden.extend(sorted(fila, key=lambda i: i["x"]))
    texto = "".join(re.sub(r"[^A-Z0-9]", "", i["t"].upper()) for i in orden)
    conf = float(np.mean([i["c"] for i in orden]))
    return texto, conf, [(i["t"], round(i["c"], 2)) for i in orden]


def _corregir(ventana, patron):
    """patron: cadena de 'L' (letra) y 'N' (numero). Devuelve (texto, cambios) o None."""
    salida, cambios = [], 0
    for ch, tipo in zip(ventana, patron):
        if tipo == "L":
            if ch.isalpha():
                salida.append(ch)
            elif ch in A_LETRA:
                salida.append(A_LETRA[ch]); cambios += 1
            else:
                return None
        else:
            if ch.isdigit():
                salida.append(ch)
            elif ch in A_DIGITO:
                salida.append(A_DIGITO[ch]); cambios += 1
            else:
                return None
    return "".join(salida), cambios


PATRONES = {"CARRO": "LLLNNN", "MOTO": "LLLNNL"}


def extraer_placa(texto, conf):
    """Busca en 'texto' la mejor placa con formato colombiano."""
    mejor = None
    # quitar palabras impresas en la placa para que no formen ventanas falsas
    texto = re.sub(r"COLOMBIA|BOGOTA|MEDELLIN|BARRANQUILLA|CARTAGENA|BUCARAMANGA|CUCUTA|PEREIRA|MANIZALES|ARMENIA|NEIVA", "|", texto)
    for i in range(0, len(texto) - 5):
        ventana = texto[i:i + 6]
        for formato, patron in PATRONES.items():
            r = _corregir(ventana, patron)
            if r is None:
                continue
            placa, cambios = r
            if cambios > 1:
                continue
            # la placa no puede ir pegada a otro digito (seria una lectura mal cortada)
            siguiente = texto[i + 6] if i + 6 < len(texto) else ""
            if patron.endswith("N") and siguiente.isdigit():
                continue
            puntaje = conf * (1 - 0.2 * cambios)
            # una ventana que ya cumple el formato tal cual es mas confiable
            if mejor is None or puntaje > mejor["puntaje"]:
                mejor = {"placa": placa, "formato": formato, "cambios": cambios, "puntaje": puntaje}
    return mejor


def votar(lecturas):
    """Combina varias lecturas: voto ponderado caracter por caracter dentro del formato dominante."""
    if not lecturas:
        return None
    peso_formato = {}
    for l in lecturas:
        peso_formato[l["formato"]] = peso_formato.get(l["formato"], 0) + l["puntaje"]
    formato = max(peso_formato, key=peso_formato.get)
    grupo = [l for l in lecturas if l["formato"] == formato]
    placa = ""
    for pos in range(6):
        pesos = {}
        for l in grupo:
            pesos[l["placa"][pos]] = pesos.get(l["placa"][pos], 0) + l["puntaje"]
        placa += max(pesos, key=pesos.get)
    acuerdo = sum(l["puntaje"] for l in grupo if l["placa"] == placa) / sum(l["puntaje"] for l in grupo)
    mejor_puntaje = max(l["puntaje"] for l in grupo)
    return {"placa": placa, "formato": formato, "puntaje": mejor_puntaje * acuerdo,
            "cambios": min(l["cambios"] for l in grupo if l["placa"] == placa) if acuerdo > 0 else 1,
            "acuerdo": acuerdo}


# --------------------------------------------------------------------------
# Funcion principal
# --------------------------------------------------------------------------
def reconocer_placa(datos_imagen):
    arr = np.frombuffer(datos_imagen, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        return {"ok": False, "codigo": "IMAGEN_INVALIDA",
                "mensaje": "El archivo no es una imagen valida. Suba una foto JPG o PNG."}

    h, w = img.shape[:2]
    if max(h, w) > MAX_LADO:
        f = MAX_LADO / max(h, w)
        img = cv2.resize(img, None, fx=f, fy=f, interpolation=cv2.INTER_AREA)

    calidad = evaluar_calidad(cv2.cvtColor(img, cv2.COLOR_BGR2GRAY))
    avisos = advertencias_calidad(calidad)

    candidatos = encontrar_candidatos(img)
    lecturas, vistos = [], []            # lecturas validas: dicts de extraer_placa

    def probar(recorte, modos):
        for modo in modos:
            texto, conf, _ = _leer(_preparar(recorte, modo))
            if texto:
                vistos.append(texto)
            if conf < CONF_MINIMA:
                continue
            r = extraer_placa(texto, conf)
            if r:
                lecturas.append(r)

    for rec in candidatos:               # varias variantes de cada region y se vota
        probar(rec, ["gris", "color", "umbral"])
        if len(lecturas) >= 2 and len({l["placa"] for l in lecturas}) == 1:
            break
    if not lecturas:                     # ultimo recurso: toda la imagen
        probar(img, ["color"])

    mejor = votar(lecturas)

    if mejor is None:
        if avisos:
            msg = " ".join(avisos) + " Tome otra fotografia o escriba la placa manualmente."
            codigo = "CALIDAD_BAJA"
        else:
            msg = ("No se pudo leer ninguna placa con formato valido. Acerque la camara, "
                   "enfoque la placa de frente o escriba la placa manualmente.")
            codigo = "PLACA_NO_ENCONTRADA"
        return {"ok": False, "codigo": codigo, "mensaje": msg, "advertencias": avisos,
                "calidad": calidad, "texto_detectado": vistos[:6]}

    aviso_conf = []
    if mejor["puntaje"] < CONF_BUENA:
        aviso_conf.append("La lectura tiene baja confianza: verifique cada caracter.")
    if mejor["acuerdo"] < 1:
        aviso_conf.append("Las lecturas no coincidieron del todo entre si: verifique la placa.")
    if mejor["cambios"]:
        aviso_conf.append("Se corrigieron caracteres ambiguos (por ejemplo 0/O, 1/I, 5/S). Verifique la placa.")

    return {
        "ok": True,
        "placa": mejor["placa"],
        "formato": mejor["formato"],
        "tipo_sugerido": mejor["formato"],
        "confianza": round(mejor["puntaje"], 2),
        "advertencias": avisos + aviso_conf,
        "calidad": calidad,
        "mensaje": "Placa reconocida. Verifique que sea correcta antes de continuar.",
    }
