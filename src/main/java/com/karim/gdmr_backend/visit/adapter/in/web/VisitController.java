package com.karim.gdmr_backend.visit.adapter.in.web;

import com.karim.gdmr_backend.auth.adapter.in.web.dto.PagedResponse;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.doctor.domain.port.in.GetDoctorIdByUserIdUseCase;
import com.karim.gdmr_backend.employee.domain.port.in.GetEmployeeIdByUserIdUseCase;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.visit.adapter.in.web.dto.AssignTimeSlotRequest;
import com.karim.gdmr_backend.visit.adapter.in.web.dto.CreateScheduledVisitRequest;
import com.karim.gdmr_backend.visit.adapter.in.web.dto.NegotiationEntryResponse;
import com.karim.gdmr_backend.visit.adapter.in.web.dto.RejectVisitRequest;
import com.karim.gdmr_backend.visit.adapter.in.web.dto.RequestVisitRequest;
import com.karim.gdmr_backend.visit.adapter.in.web.dto.VisitResponse;
import com.karim.gdmr_backend.visit.domain.exception.UnauthorizedVisitAccessException;
import com.karim.gdmr_backend.visit.domain.model.Visit;
import com.karim.gdmr_backend.visit.domain.model.VisitStatus;
import com.karim.gdmr_backend.visit.domain.port.in.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visits")
@Tag(name = "Visits", description = "Visit negotiation flow: request, propose, confirm, reject, schedule")
public class VisitController {

    private final RequestSpontaneousVisitUseCase requestSpontaneousVisitUseCase;
    private final AssignTimeSlotUseCase assignTimeSlotUseCase;
    private final CreateScheduledVisitUseCase createScheduledVisitUseCase;
    private final ConfirmVisitUseCase confirmVisitUseCase;
    private final RejectVisitByEmployeeUseCase rejectVisitByEmployeeUseCase;
    private final ConfirmVisitByDoctorUseCase confirmVisitByDoctorUseCase;
    private final RejectVisitByDoctorUseCase rejectVisitByDoctorUseCase;
    private final ListNegotiationHistoryUseCase listNegotiationHistoryUseCase;
    private final GetVisitByIdUseCase getVisitByIdUseCase;
    private final SendVisitRemindersUseCase sendVisitRemindersUseCase;
    private final UpdateVisitStatusUseCase updateVisitStatusUseCase;
    private final SubmitVisitReportUseCase submitVisitReportUseCase;
    private final ListVisitsUseCase listVisitsUseCase;
    private final GetEmployeeIdByUserIdUseCase getEmployeeIdByUserIdUseCase;
    private final GetDoctorIdByUserIdUseCase getDoctorIdByUserIdUseCase;

    public VisitController(RequestSpontaneousVisitUseCase requestSpontaneousVisitUseCase,
                            AssignTimeSlotUseCase assignTimeSlotUseCase,
                            CreateScheduledVisitUseCase createScheduledVisitUseCase,
                            ConfirmVisitUseCase confirmVisitUseCase,
                            RejectVisitByEmployeeUseCase rejectVisitByEmployeeUseCase,
                            ConfirmVisitByDoctorUseCase confirmVisitByDoctorUseCase,
                            RejectVisitByDoctorUseCase rejectVisitByDoctorUseCase,
                            ListNegotiationHistoryUseCase listNegotiationHistoryUseCase,
                            GetVisitByIdUseCase getVisitByIdUseCase,
                            SendVisitRemindersUseCase sendVisitRemindersUseCase,
                            UpdateVisitStatusUseCase updateVisitStatusUseCase,
                            SubmitVisitReportUseCase submitVisitReportUseCase,
                            ListVisitsUseCase listVisitsUseCase,
                            GetEmployeeIdByUserIdUseCase getEmployeeIdByUserIdUseCase,
                            GetDoctorIdByUserIdUseCase getDoctorIdByUserIdUseCase) {
        this.requestSpontaneousVisitUseCase = requestSpontaneousVisitUseCase;
        this.assignTimeSlotUseCase = assignTimeSlotUseCase;
        this.createScheduledVisitUseCase = createScheduledVisitUseCase;
        this.confirmVisitUseCase = confirmVisitUseCase;
        this.rejectVisitByEmployeeUseCase = rejectVisitByEmployeeUseCase;
        this.confirmVisitByDoctorUseCase = confirmVisitByDoctorUseCase;
        this.rejectVisitByDoctorUseCase = rejectVisitByDoctorUseCase;
        this.listNegotiationHistoryUseCase = listNegotiationHistoryUseCase;
        this.getVisitByIdUseCase = getVisitByIdUseCase;
        this.sendVisitRemindersUseCase = sendVisitRemindersUseCase;
        this.updateVisitStatusUseCase = updateVisitStatusUseCase;
        this.submitVisitReportUseCase = submitVisitReportUseCase;
        this.listVisitsUseCase = listVisitsUseCase;
        this.getEmployeeIdByUserIdUseCase = getEmployeeIdByUserIdUseCase;
        this.getDoctorIdByUserIdUseCase = getDoctorIdByUserIdUseCase;
    }

    // Étape 1 — Employé demande une visite avec un médecin précis
    @PostMapping("/spontaneous")
    public ResponseEntity<VisitResponse> requestSpontaneousVisit(
            Authentication authentication,
            @RequestParam Long doctorUserId,
            @RequestBody RequestVisitRequest request) {

        Long employeeId = resolveEmployeeId(authentication);
        Long doctorId = getDoctorIdByUserIdUseCase.getDoctorId(doctorUserId);

        Visit v = requestSpontaneousVisitUseCase.requestVisit(
                new RequestSpontaneousVisitUseCase.RequestVisitCommand(
                        employeeId, doctorId, request.motif(), request.proposedSlots()));
        return ResponseEntity.status(201).body(VisitResponse.fullAccess(v));
    }

    // Étape 2 — HR attribue un créneau disponible du médecin à la visite
    @PatchMapping("/{id}/assign-slot")
    public ResponseEntity<VisitResponse> assignTimeSlot(
            @PathVariable Long id, @RequestBody AssignTimeSlotRequest request) {
        Visit v = assignTimeSlotUseCase.assignTimeSlot(
                new AssignTimeSlotUseCase.AssignTimeSlotCommand(id, request.timeSlotId()));
        return ResponseEntity.ok(VisitResponse.administrativeView(v));
    }

    // HR crée directement une visite planifiée (embauche, annuelle, reprise, accident)
    @PostMapping("/scheduled")
    public ResponseEntity<VisitResponse> createScheduledVisit(
            @RequestBody CreateScheduledVisitRequest request) {
        Visit v = createScheduledVisitUseCase.createScheduledVisit(
                new CreateScheduledVisitUseCase.CreateScheduledVisitCommand(
                        request.employeeId(), request.doctorId(), request.timeSlotId()));
        return ResponseEntity.status(201).body(VisitResponse.administrativeView(v));
    }

    // Étape 3 — Employé confirme le créneau attribué, ou en suggère un autre
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<VisitResponse> confirmVisit(Authentication authentication, @PathVariable Long id) {
        Long employeeId = resolveEmployeeId(authentication);
        Visit v = confirmVisitUseCase.confirmVisit(id, employeeId);
        return ResponseEntity.ok(VisitResponse.fullAccess(v));
    }

    @PatchMapping("/{id}/employee-reject")
    public ResponseEntity<VisitResponse> rejectByEmployee(
            Authentication authentication, @PathVariable Long id, @RequestBody RejectVisitRequest request) {
        Long employeeId = resolveEmployeeId(authentication);
        Visit v = rejectVisitByEmployeeUseCase.rejectByEmployee(
                new RejectVisitByEmployeeUseCase.RejectByEmployeeCommand(
                        id, employeeId, request.reason(), request.suggestedDateTime()));
        return ResponseEntity.ok(VisitResponse.fullAccess(v));
    }

    // Étape 4 — Le médecin confirme (ou refuse, avec une alternative) le créneau accepté par l'employé
    @PatchMapping("/{id}/doctor-confirm")
    public ResponseEntity<VisitResponse> confirmByDoctor(Authentication authentication, @PathVariable Long id) {
        Long doctorId = resolveDoctorId(authentication);
        Visit v = confirmVisitByDoctorUseCase.confirmByDoctor(id, doctorId);
        return ResponseEntity.ok(VisitResponse.fullAccess(v));
    }

    @PatchMapping("/{id}/doctor-reject")
    public ResponseEntity<VisitResponse> rejectByDoctor(
            Authentication authentication, @PathVariable Long id, @RequestBody RejectVisitRequest request) {
        Long doctorId = resolveDoctorId(authentication);
        Visit v = rejectVisitByDoctorUseCase.rejectByDoctor(id, doctorId, request.reason(), request.suggestedDateTime());
        return ResponseEntity.ok(VisitResponse.fullAccess(v));
    }

    // Historique complet des allers-retours (qui a refusé, pourquoi, quelle alternative)
    @GetMapping("/{id}/negotiation-history")
    public ResponseEntity<java.util.List<NegotiationEntryResponse>> negotiationHistory(
            Authentication authentication, @PathVariable Long id) {
        Visit visit = getVisitByIdUseCase.getVisit(id);
        assertCanViewVisit(authentication, visit);

        java.util.List<NegotiationEntryResponse> history = listNegotiationHistoryUseCase.listHistory(id).stream()
                .map(NegotiationEntryResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    // Déclenchement manuel des rappels (utile pour la démo — tourne aussi automatiquement chaque jour à 8h)
    @PostMapping("/send-reminders")
    public ResponseEntity<java.util.Map<String, Integer>> sendReminders() {
        int sent = sendVisitRemindersUseCase.sendReminders();
        return ResponseEntity.ok(java.util.Map.of("sent", sent));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VisitResponse> updateStatus(
            Authentication authentication, @PathVariable Long id, @RequestParam VisitStatus status) {
        Long doctorId = resolveDoctorId(authentication);
        Visit v = updateVisitStatusUseCase.updateStatus(
                new UpdateVisitStatusUseCase.UpdateVisitStatusCommand(id, status, doctorId));
        return ResponseEntity.ok(VisitResponse.fullAccess(v));
    }

    @PatchMapping("/{id}/report")
    public ResponseEntity<VisitResponse> submitReport(
            Authentication authentication, @PathVariable Long id, @RequestParam String reportNotes) {
        Long doctorId = resolveDoctorId(authentication);
        Visit v = submitVisitReportUseCase.submitReport(
                new SubmitVisitReportUseCase.SubmitReportCommand(id, reportNotes, doctorId));
        return ResponseEntity.ok(VisitResponse.fullAccess(v));
    }

    // Backs the eye-icon "view details" action on the visits datatable / calendar cards.
    @GetMapping("/{id}")
    public ResponseEntity<VisitResponse> getVisit(Authentication authentication, @PathVariable Long id) {
        Visit visit = getVisitByIdUseCase.getVisit(id);
        assertCanViewVisit(authentication, visit);

        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        boolean canSeeReport = role.equals("DOCTOR") || role.equals("EMPLOYEE");
        return ResponseEntity.ok(canSeeReport ? VisitResponse.fullAccess(visit) : VisitResponse.administrativeView(visit));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<VisitResponse>> listVisits(
            Authentication authentication,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        boolean isEmployee = role.equals("EMPLOYEE");
        boolean isDoctor = role.equals("DOCTOR");
        boolean canSeeReport = isDoctor || isEmployee;

        Long effectiveEmployeeId = isEmployee ? resolveEmployeeId(authentication) : employeeId;
        // A doctor can only ever list their own visits — never another
        // doctor's, and never a patient's full cross-doctor history — so the
        // caller-supplied doctorId is ignored and forced to their own id.
        Long effectiveDoctorId = isDoctor ? resolveDoctorId(authentication) : doctorId;

        PageResult<Visit> result = listVisitsUseCase.listVisits(
                new ListVisitsUseCase.ListVisitsQuery(effectiveEmployeeId, effectiveDoctorId, status, page, size));

        PageResult<VisitResponse> mapped = new PageResult<>(
                result.content().stream()
                        .map(canSeeReport ? VisitResponse::fullAccess : VisitResponse::administrativeView)
                        .toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());

        return ResponseEntity.ok(PagedResponse.from(mapped));
    }

    private void assertCanViewVisit(Authentication authentication, Visit visit) {
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        if (role.equals("ADMIN") || role.equals("HR")) {
            return;
        }
        if (role.equals("EMPLOYEE") && visit.getEmployeeId().equals(resolveEmployeeId(authentication))) {
            return;
        }
        if (role.equals("DOCTOR") && visit.getDoctorId().equals(resolveDoctorId(authentication))) {
            return;
        }
        throw new UnauthorizedVisitAccessException(visit.getId());
    }

    private Long resolveEmployeeId(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).userId();
        return getEmployeeIdByUserIdUseCase.getEmployeeId(userId);
    }

    private Long resolveDoctorId(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).userId();
        return getDoctorIdByUserIdUseCase.getDoctorId(userId);
    }
}
