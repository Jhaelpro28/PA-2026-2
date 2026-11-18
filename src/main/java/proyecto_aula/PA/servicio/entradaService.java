package proyecto_aula.PA.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import proyecto_aula.PA.modelo.Espacio;
import proyecto_aula.PA.modelo.Registro;
import proyecto_aula.PA.modelo.TipoVehiculo;
import proyecto_aula.PA.modelo.Vehiculo;
import proyecto_aula.PA.modelo.Zona;
import proyecto_aula.PA.repositorio.EspacioRepository;
import proyecto_aula.PA.repositorio.RegistroRepository;
import proyecto_aula.PA.repositorio.VehiculoRepository;
import proyecto_aula.PA.repositorio.ZonaRepository;
@Service 
public class EntradaService {
    



    

    @Autowired 
    private VehiculoRepository vehiculoRepository;

    @Autowired 
    private RegistroRepository registroRepository;

    @Autowired 
    private EspacioRepository espacioRepository;

    @Autowired
    private ZonaRepository zonaRepository;

    public void registrarEntrada(String placaPlana, String tipoSeleccionado, Long zonaID){

        String placa = placaPlana != null ? placaPlana.toUpperCase().trim() : "";

        boolean esCarroValido = placa.matches("^[A-Z]{3}[0-9]{3}$");
        boolean esMotoValida = placa.matches("^[A-Z]{3}[0-9]{2}[A-Z]$");

        if(!esCarroValido && esMotoValida){
            throw new IllegalArgumentException("La placa ingresada no es válida para un carro o moto.");

        }
        Vehiculo vehiculo = vehiculoRepository.findByPlaca(placa).orElse(null);

        if(vehiculo != null){
            if(!vehiculo.getTipo().name().equalsIgnoreCase(tipoSeleccionado)){
                throw new IllegalArgumentException("El tipo de vehículo no coincide con la placa ingresada.");
            }
        }else{
            vehiculo = new Vehiculo();
            vehiculo.setPlaca(placaPlana);
            vehiculo.setTipo(TipoVehiculo.valueOf(tipoSeleccionado.toUpperCase()));
            vehiculoRepository.save(vehiculo);
        }
        boolean vehiculoAdentro = registroRepository.existsByVehiculoPlacaAndHoraSalidaIsNull(placaPlana);

            if (vehiculoAdentro) {
                throw new IllegalStateException("Esta placa ya ha sido ingresada.");
        }
    }

    public Registro registrarEntrada(String placa, TipoVehiculo tipo, Long zonaId){
        Registro activo = registroRepository.findByVehiculo_PlacaAndHoraSalidaIsNull(placa);
        if(activo != null){
            return null;
        }
        
        Zona zona = zonaRepository.findById(zonaId).orElse(null);
        if(zona == null){
            return null;
        }

        List<Espacio> libres = espacioRepository.findByZonaAndOcupadoFalse(zona);
        if(libres.isEmpty()){
            return null;
        }

        Espacio espacioAsignado = libres.get(0);

        Vehiculo vehiculo = vehiculoRepository.findByPlaca(placa);
        if(vehiculo == null){
            vehiculo = new Vehiculo(placa, tipo);
            vehiculo = vehiculoRepository.save(vehiculo);
        }
        
        Registro registro = new Registro(vehiculo, espacioAsignado, LocalDateTime.now());
        registro = registroRepository.save(registro);

        espacioAsignado.setOcupado(true);
        espacioRepository.save(espacioAsignado);

        return registro;
    }


}
