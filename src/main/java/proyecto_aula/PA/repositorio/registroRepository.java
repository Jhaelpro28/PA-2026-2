package proyecto_aula.PA.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import proyecto_aula.PA.modelo.Registro;

public interface  RegistroRepository extends JpaRepository<Registro, Long>{
    Registro findByVehiculo_PlacaAndHoraSalidaIsNull(String placa);
}
