package proyecto_aula.PA.controlador;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import proyecto_aula.PA.modelo.Registro;
import proyecto_aula.PA.servicio.ResumenService; 

@Controller 
public class ResumenController {
    
    @Autowired 
    private ResumenService resumenService;

    @GetMapping("/resumen")
    public String resumen(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        Model model
    ){
        
        List<Registro> cerrados = resumenService.obtenerRegistroCerradosFiltrados(desde, hasta);
        
        double totalRecaudado = resumenService.calcularTotalRecaudado(cerrados);
        
        int total = cerrados.size();
        String promedioEstancia = resumenService.calcularPromedioEstancia(cerrados);

        model.addAttribute("totalVehiculos", total);
        model.addAttribute("totalRecaudo", (long) totalRecaudado);
        model.addAttribute("promedioEstancia", promedioEstancia);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);   
        
        return "view/resumen";
    }
}