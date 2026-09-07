package com.karim.gdmr_backend.medicalhistory.adapter.in.web;

import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.doctor.domain.port.in.GetDoctorIdByUserIdUseCase;
import com.karim.gdmr_backend.medicalhistory.adapter.in.web.dto.AddMedicalHistoryEntryRequest;
import com.karim.gdmr_backend.medicalhistory.adapter.in.web.dto.MedicalHistoryEntryResponse;
import com.karim.gdmr_backend.medicalhistory.domain.port.in.AddMedicalHistoryEntryUseCase;
import com.karim.gdmr_backend.medicalhistory.domain.port.in.ListMedicalHistoryUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-history")
@Tag(name = "Medical History", description = "Doctor-only medical history entries per employee")
public class MedicalHistoryController {

    private final AddMedicalHistoryEntryUseCase addMedicalHistoryEntryUseCase;
    private final ListMedicalHistoryUseCase listMedicalHistoryUseCase;
    private final GetDoctorIdByUserIdUseCase getDoctorIdByUserIdUseCase;

    public MedicalHistoryController(AddMedicalHistoryEntryUseCase addMedicalHistoryEntryUseCase,
                                     ListMedicalHistoryUseCase listMedicalHistoryUseCase,
                                     GetDoctorIdByUserIdUseCase getDoctorIdByUserIdUseCase) {
        this.addMedicalHistoryEntryUseCase = addMedicalHistoryEntryUseCase;
        this.listMedicalHistoryUseCase = listMedicalHistoryUseCase;
        this.getDoctorIdByUserIdUseCase = getDoctorIdByUserIdUseCase;
    }

    @PostMapping
    public ResponseEntity<MedicalHistoryEntryResponse> addEntry(
            Authentication authentication, @RequestBody AddMedicalHistoryEntryRequest request) {
        Long doctorId = resolveDoctorId(authentication);
        var entry = addMedicalHistoryEntryUseCase.addEntry(
                new AddMedicalHistoryEntryUseCase.AddMedicalHistoryEntryCommand(
                        request.employeeId(), doctorId, request.visitId(), request.category(), request.description()));
        return ResponseEntity.status(201).body(MedicalHistoryEntryResponse.from(entry));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<MedicalHistoryEntryResponse>> listForEmployee(@PathVariable Long employeeId) {
        List<MedicalHistoryEntryResponse> entries = listMedicalHistoryUseCase.listForEmployee(employeeId).stream()
                .map(MedicalHistoryEntryResponse::from)
                .toList();
        return ResponseEntity.ok(entries);
    }

    private Long resolveDoctorId(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).userId();
        return getDoctorIdByUserIdUseCase.getDoctorId(userId);
    }
}
