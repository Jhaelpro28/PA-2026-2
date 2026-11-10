"""Prueba el reconocedor con las imagenes de ../imagenes_prueba.
El nombre del archivo indica la placa esperada (o 'sin_placa')."""
import sys, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import reconocedor

carpeta = Path(__file__).resolve().parent.parent / "imagenes_prueba"
fallos = 0
for f in sorted(carpeta.glob("*.jpg")):
    esperado = None if f.stem.startswith("sin_placa") else f.stem.split("_")[-1]
    t = time.time()
    r = reconocedor.reconocer_placa(f.read_bytes())
    leido = r.get("placa") if r.get("ok") else None
    estado = "OK " if leido == esperado else ("~  " if leido is None else "MAL")
    # una imagen oscura/borrosa puede fallar con aviso; lo grave es devolver una placa equivocada
    if leido is not None and leido != esperado:
        fallos += 1
        dif = sum(a != b for a, b in zip(leido, esperado or ""))
        estado = f"MAL({dif} car.)"
    print(f"{estado} {f.name:32} esperado={esperado} leido={leido} conf={r.get('confianza')} "
          f"codigo={r.get('codigo','-')} {time.time()-t:.1f}s")
print("lecturas con algun caracter distinto al esperado:", fallos, "(el usuario siempre verifica antes de guardar)")
