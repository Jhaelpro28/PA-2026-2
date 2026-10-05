package proyecto_aula.PA.controlador;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import proyecto_aula.PA.modelo.Registro;
import proyecto_aula.PA.modelo.TipoVehiculo;
import proyecto_aula.PA.modelo.Zona;
import proyecto_aula.PA.repositorio.ZonaRepository;
import proyecto_aula.PA.servicio.EntradaService;

@Controller
public class EntradaController {
    @Autowired
    private EntradaService entradaService;
    @Autowired
    private ZonaRepository zonaRepository;

    @GetMapping("/entrada")
    public String mostrarFormulario(Model model){
        List<Zona> zonas = zonaRepository.findAll();
        model.addAttribute("zonas", zonas);
        return "entrada";
    }
    @PostMapping("/entrada")
    public String registrarEntrada(
        @RequestParam String placa,
        @RequestParam TipoVehiculo tipoVehiculo,
        @RequestParam Long zonaId, Model model
    ){
        Registro registro = entradaService.registrarEntrada(placa, tipoVehiculo, zonaId);
        
        List<Zona> zonas = zonaRepository.findAll();
        model.addAttribute("zonas", zonas);

        if(registro == null){
            model.addAttribute("error", "No hay espacio disponible en la zona seleccionada.");
            return "entrada";
        } else {
            model.addAttribute("mensaje", "Entrada registrada exitosamente. ID de registro: " + registro.getId());
            return "entrada";
        }
    }
    
}
