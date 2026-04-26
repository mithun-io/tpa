package com.tpa.mapper;

import com.tpa.dto.request.PatientRequest;
import com.tpa.dto.response.PatientResponse;
import com.tpa.entity.Patient;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    Patient toPatient(PatientRequest patientRequest);

    PatientResponse toPatientResponse(Patient patient);

    List<PatientResponse> toPatientResponses(List<Patient> patients);
}