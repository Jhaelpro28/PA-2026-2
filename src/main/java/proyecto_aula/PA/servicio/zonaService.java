package proyecto_aula.PA.servicio;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import proyecto_aula.PA.modelo.Zona;
import proyecto_aula.PA.repositorio.ZonaRepository;
@Service
public class ZonaService {
    @Autowired
    private ZonaRepository zonaRepository;

    public List<Zona> listarTodas(){
        return zonaRepository.findAll();
    }
    public Zona guardar(Zona zona){
        return zonaRepository.save(zona);
    }
    public Zona buscarId(Long id){
        return zonaRepository.findById(id).orElse(null);
    }
}
