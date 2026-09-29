package proyecto_aula.PA.servicio;
import java.util.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import proyecto_aula.PA.modelo.Espacio;
import proyecto_aula.PA.modelo.Registro;
import proyecto_aula.PA.modelo.TipoVehiculo;
import proyecto_aula.PA.modelo.Vehiculo;
import proyecto_aula.PA.modelo.Zona;
import proyecto_aula.PA.repositorio.EspacioRepository;
import proyecto_aula.PA.repositorio.RegistroRepository;
@Service 
public class SalidaService {
    

    @Autowired 
    public RegistroRepository registroRepository;

    @Autowired 
    public EspacioRepository espacioRepository;

    public Registro registrarSalida(String placa){
        Registro registro = registroRepository.findByVehiculo_PlacaAndHoraSalidaIsNull(placa);
        if(registro == null){
            return null;
        }
        
        LocalDateTime ahora = LocalDateTime.now();
        Duration duracion = Duration.between(registro.getHoraEntrada(), ahora);


        long minutos = duracion.toMinutes();
        long horasCobro = (minutos / 60 ) + (minutos % 60 == 0 ? 0 : 1);
        if(horasCobro == 0){
            horasCobro = 1;
        }


        double tarifaPorHora = obtenerTarifa(registro.getVehiculo().getTipo());
        double valorPagar = horasCobro * tarifaPorHora;

        registro.setHoraSalida(ahora);
        registro.setValorPagado(valorPagar);
        registroRepository.save(registro);

        Espacio espacio = registro.getEspacio();
        espacio.setOcupado(false);
        espacioRepository.save(espacio);

        return registro;
    }
    private double obtenerTarifa(proyecto_aula.PA.modelo.TipoVehiculo tipo){
        switch(tipo){
            case CARRO:
                return 3000.0;
            case MOTO:
                return 2000.0;
            case BICICLETA:
                return 1000.0;
            default:
                return 0.0;
        }
    }
}
