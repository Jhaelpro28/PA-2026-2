package proyecto_aula.PA.controlador;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import proyecto_aula.PA.modelo.Registro;
import proyecto_aula.PA.repositorio.RegistroRepository;
import proyecto_aula.PA.servicio.SalidaService;

@Controller
public class SalidaController {
    @Autowired 
    private SalidaService salidaService;
    @Autowired 
    private RegistroRepository registroRepository;

    @GetMapping("/salida")
    public String buscar(@RequestParam(required = false) String placa, Model model) {
        if(placa != null && !placa.isBlank()){
            Registro registro = registroRepository.findByVehiculo_PlacaAndHoraSalidaIsNull(placa);
            if(registro == null){
                model.addAttribute("error", "No se encontró vehiculo con esta placa");
            } else {
                Duration duracion = Duration.between(registro.getHoraEntrada(), LocalDateTime.now());
                long minutos = duracion.toMinutes();
                long horasCobro = (minutos / 60 ) + (minutos % 60 == 0 ? 0 : 1);
                if(horasCobro == 0){
                    horasCobro = 1;
                }
                double tarifaPorHora = salidaService.obtenerTarifa(registro.getVehiculo().getTipo());
                double valorPagar = horasCobro * tarifaPorHora;
                model.addAttribute("registro", registro);
                model.addAttribute("tiempoTranscurrido", horasCobro + "h (estimada)");
                model.addAttribute("valorPagar", valorPagar);
            }
            model.addAttribute("placaBuscada", placa);

        }
        return "salida";
    }

    @PostMapping("/salida/confirmar")
    public String confirmar(@RequestParam String placa, RedirectAttributes redirectAttributes){
        Registro registro = salidaService.registrarSalida(placa);
        if(registro == null){
            redirectAttributes.addFlashAttribute("error", "No se registro la salida");
            
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "Salida registrada exitosamente. Valor pagado: " + registro.getValorPagado());
            
        }
        return "redirect:/salida";
    }
}
