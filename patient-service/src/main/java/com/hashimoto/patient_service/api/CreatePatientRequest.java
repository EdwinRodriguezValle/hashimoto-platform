package com.hashimoto.patient_service.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // <-- ESTA ANOTACIÓN GENERA AUTOMÁTICAMENTE TODOS LOS GETTERS Y SETTERS
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientRequest {
    private String firstName;
    private String lastName;
    private String gender;
    private String dni;
    private String email;
    private int age;
}

