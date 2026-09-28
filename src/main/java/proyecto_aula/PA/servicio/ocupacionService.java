package proyecto_aula.PA.servicio;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import proyecto_aula.PA.modelo.Espacio;
import proyecto_aula.PA.repositorio.EspacioRepository;

@Service 
public class OcupacionService {
    
    @Autowired
    private EspacioRepository espacioRepository;
    
    public Long contarTotal(){
        return espacioRepository.count();
    }
    public int contarOcupados(){
        List<Espacio> espacios = espacioRepository.findAll();
        int contador=0;
        for(Espacio e : espacios){
            if(e.isOcupado()){
                contador++;
            }
        }
        return contador;
    }
    public Long contarDisponibles(){
        return contarTotal() - contarOcupados();
    }
}
