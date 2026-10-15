package proyecto_aula.PA.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import proyecto_aula.PA.repositorio.RegistroRepository;

import java.util.*;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import proyecto_aula.PA.modelo.Registro;

import java.time.*;

import org.springframework.format.annotation.DateTimeFormat;
@Controller 
public class ResumenController {
    
    @Autowired 
    private RegistroRepository registroRepository;


    @GetMapping("/resumen")
    public String resumen(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        Model model
    ){
        List<Registro> cerrados = new ArrayList<>();
        for (Registro r : registroRepository.findAll()){
            if(r.getHoraSalida() != null){
                continue;
            }
            LocalDate dia = r.getHoraSalida().toLocalDate();
            boolean despuesDeDesde = (desde == null) || !dia.isBefore(desde);
            boolean antesDeHasta = (hasta == null) || !dia.isAfter(hasta);
            if(despuesDeDesde && antesDeHasta){
                cerrados.add(r);
            }
        }
        double totalRecaudado = 0;
        long totalMinutos = 0;
        for(Registro r : cerrados){
            totalRecaudado += r.getValorPagado();
            totalMinutos += Duration.between(r.getHoraEntrada(), r.getHoraSalida()).toMinutes();
        }
        int total = cerrados.size();
        long promedio = (total == 0) ? 0 : totalMinutos / total;
        String promedioEstancia = (promedio / 60 ) + "h " + (promedio % 60) + "m";

        model.addAttribute("totalVehiculos", total);
        model.addAttribute("totalRecaudo", (long) totalRecaudado);
        model.addAttribute("promedioEstancia", promedioEstancia);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);   
        
        return "resumen";
    }
}
