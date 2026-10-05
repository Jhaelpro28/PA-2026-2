package proyecto_aula.PA.controlador;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import proyecto_aula.PA.modelo.Zona;
import proyecto_aula.PA.repositorio.ZonaRepository;
import proyecto_aula.PA.servicio.OcupacionService;

@Controller
public class OcupacionController {
    
    @Autowired
    private OcupacionService ocupacionService;
    @Autowired
    private ZonaRepository zonaRepository;

    @GetMapping("/ocupacion")
    private String ocupacion(Model model){
        List<Zona> zonas = zonaRepository.findAll();

        model.addAttribute("totalEspacio", ocupacionService.contarTotal());
        model.addAttribute("espaciosOcupados", ocupacionService.contarOcupados());
        model.addAttribute("espaciosDisponibles", ocupacionService.contarDisponibles());
        model.addAttribute("zonas", zonas);
        return "ocupacion";
    }
}
