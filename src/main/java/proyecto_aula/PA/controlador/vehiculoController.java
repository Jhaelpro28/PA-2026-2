package proyecto_aula.PA.controlador;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import proyecto_aula.PA.modelo.Registro;
import proyecto_aula.PA.repositorio.RegistroRepository;

@Controller
public class VehiculoController {
    @Autowired
    private RegistroRepository registroRepository;

    @GetMapping("/vehiculos")
    public String listar(
        @RequestParam(required = false) String buscar,
        @RequestParam(required = false) String estado,
        Model model){
            List<Registro> todos = registroRepository.findAll();
            List<Registro> filtrados = new ArrayList<>();

            for(Registro r : todos){
                boolean coincidePlaca = (buscar == null || buscar.isBlank()) || 
            r.getVehiculo().getPlaca().toLowerCase().contains(buscar.toLowerCase());

                boolean coincideEstado = (estado == null || estado.isBlank()) ||
                (estado.equals("DENTRO") && r.getHoraSalida() == null) ||
                (estado.equals("FUERA") && r.getHoraSalida() != null);

                if(coincidePlaca && coincideEstado){
                    filtrados.add(r);
                }
            }
            model.addAttribute("registros", filtrados);
            model.addAttribute("buscar", buscar);
            model.addAttribute("estado", estado);
            return "/vehiculos";
        }
    
}
