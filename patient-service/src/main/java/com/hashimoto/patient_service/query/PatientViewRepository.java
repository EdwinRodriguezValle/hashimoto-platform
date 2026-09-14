package com.hashimoto.patient_service.query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientViewRepository extends JpaRepository<PatientView, String> {
    // Aquí puedes meter búsquedas personalizadas en el futuro, ej: findByDni
    // Ejemplo 1: Buscar por DNI exacto (Retorna un Optional por si no existe)
    // Spring genera automáticamente: SELECT * FROM patient_view WHERE dni = ?
    //Optional<PatientView> findByDni(String dni);

    // Ejemplo 2: Buscar pacientes de un país específico
    // Spring genera automáticamente: SELECT * FROM patient_view WHERE pais = ?
    // List<PatientView> findByPais(String pais); // (Si usas la variable pais en tu entidad)

    // Ejemplo 3: Buscar pacientes que tengan una edad mayor a cierta cantidad
    // Spring genera automáticamente: SELECT * FROM patient_view WHERE age > ?
    //List<PatientView> findByAgeGreaterThan(int age);

    // Ejemplo 4: Buscar por Nombre que contenga una palabra (Ignorando mayúsculas/minúsculas)
    // Spring genera automáticamente: SELECT * FROM patient_view WHERE LOWER(first_name) LIKE LOWER(?)
    //List<PatientView> findByFirstNameContainingIgnoreCase(String keyword);
}
