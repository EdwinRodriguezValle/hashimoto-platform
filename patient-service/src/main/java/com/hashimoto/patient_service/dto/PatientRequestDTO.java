package com.hashimoto.patient_service.dto;

import lombok.Data;

@Data
public class PatientRequestDTO {
    private String firstName;
    private String lastName;
    private String gender;
    private String dni;
    private String email;
    private int age;
}
