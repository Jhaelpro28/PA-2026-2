package proyecto_aula.PA.servicio;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import proyecto_aula.PA.modelo.Registro;
import proyecto_aula.PA.repositorio.RegistroRepository;

@Service
public class ResumenService {
    @Autowired
    private RegistroRepository registroRepository;

    public List<Registro> obtenerRegistroCerradosFiltrados(LocalDate desde, LocalDate hasta){
        List<Registro> todos = registroRepository.findAll();
        List<Registro> cerrados = new ArrayList<>();

        for(Registro r : todos){
            if(r.getHoraSalida() == null){
                continue;
            }
            LocalDate dia = r.getHoraSalida().toLocalDate();
            boolean despuesDeDesde = (desde == null) || !dia.isBefore(desde);
            boolean antesDeHasta = (hasta == null) || !dia.isAfter(hasta);

            if(despuesDeDesde && antesDeHasta){
                cerrados.add(r);
            }
        }
        return cerrados;
        
    }
    public double calcularTotalRecaudado(List<Registro> registros){
        double total = 0;
        for(Registro r : registros){
            total += r.getValorPagado();
        }
        return total;
    }
    public String calcularPromedioEstancia(List<Registro> registros){
        long totalMinutos = 0;
        int total = registros.size();

        for(Registro r : registros){
            if(r.getHoraEntrada() != null && r.getHoraSalida() != null){
                totalMinutos += Duration.between(r.getHoraEntrada(), r.getHoraSalida()).toMinutes();
            }
        }
        long promedio = (total == 0) ? 0 : totalMinutos / total;
        return(promedio / 60) + "h " + (promedio % 60) + "m";
    }
}
