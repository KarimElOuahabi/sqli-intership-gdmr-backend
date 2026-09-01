package com.karim.gdmr_backend.shared.infrastructure.adapter.in.web;

import com.karim.gdmr_backend.auth.domain.exception.InvalidProfilePictureException;
import com.karim.gdmr_backend.auth.domain.exception.UserNotFoundException;
import com.karim.gdmr_backend.document.domain.exception.UnauthorizedDocumentAccessException;
import com.karim.gdmr_backend.document.domain.exception.UnauthorizedDocumentTypeException;
import com.karim.gdmr_backend.document.domain.exception.UnsupportedFileTypeException;
import com.karim.gdmr_backend.timeslot.domain.exception.TimeSlotNotAvailableException;
import com.karim.gdmr_backend.timeslot.domain.exception.TimeSlotNotFoundException;
import com.karim.gdmr_backend.doctor.domain.exception.DoctorNotAvailableException;
import com.karim.gdmr_backend.doctor.domain.exception.DoctorNotFoundException;
import com.karim.gdmr_backend.doctor.domain.exception.InvalidRoleForDoctorException;
import com.karim.gdmr_backend.employee.domain.exception.EmployeeNotFoundException;
import com.karim.gdmr_backend.employee.domain.exception.InvalidRoleForEmployeeException;
import com.karim.gdmr_backend.notification.domain.exception.NotificationNotFoundException;
import com.karim.gdmr_backend.notification.domain.exception.UnauthorizedNotificationAccessException;
import com.karim.gdmr_backend.profileissue.domain.exception.ProfileIssueNotFoundException;
import com.karim.gdmr_backend.visit.domain.exception.InvalidVisitStatusTransitionException;
import com.karim.gdmr_backend.visit.domain.exception.NotAssignedDoctorException;
import com.karim.gdmr_backend.visit.domain.exception.UnauthorizedVisitAccessException;
import com.karim.gdmr_backend.visit.domain.exception.VisitNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Ressource introuvable → 404 ----
    @ExceptionHandler({
            UserNotFoundException.class,
            EmployeeNotFoundException.class,
            DoctorNotFoundException.class,
            TimeSlotNotFoundException.class,
            VisitNotFoundException.class,
            NotificationNotFoundException.class,
            ProfileIssueNotFoundException.class,
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex, request);
    }

    // ---- Conflit d'état métier (créneau déjà pris, médecin indisponible) → 409 ----
    @ExceptionHandler({
            TimeSlotNotAvailableException.class,
            DoctorNotAvailableException.class,
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex, request);
    }

    // ---- Suppression bloquée par une dépendance en base (ex: visites/documents liés) → 409 ----
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                       HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                new RuntimeException("This record can't be deleted because other data still references it "
                        + "(e.g. visits or documents). Deactivate it instead."),
                request);
    }

    // ---- Requête invalide au regard des règles métier → 400 ----
    @ExceptionHandler({
            InvalidVisitStatusTransitionException.class,
            InvalidRoleForEmployeeException.class,
            InvalidRoleForDoctorException.class,
            UnauthorizedDocumentTypeException.class,
            UnsupportedFileTypeException.class,
            InvalidProfilePictureException.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex, request);
    }

    // ---- Accès refusé (pas le bon médecin, pas le bon rôle) → 403 ----
    @ExceptionHandler({
            NotAssignedDoctorException.class,
            UnauthorizedDocumentAccessException.class,   // ← ajouté
            UnauthorizedVisitAccessException.class,
            UnauthorizedNotificationAccessException.class,
            AccessDeniedException.class,
    })
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex, request);
    }

    // ---- Fichier trop volumineux → 400 ----
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                new RuntimeException("The uploaded file is too large."), request);
    }

    // ---- Erreurs de validation Bean Validation (@Valid) → 400, avec détail champ par champ ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "Validation Failed",
                "One or more fields are invalid", request.getRequestURI(), fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ---- Filet de sécurité : toute exception non prévue → 500, sans fuiter les détails internes ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                new RuntimeException("An unexpected error occurred"), request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, RuntimeException ex,
                                                HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(status).body(response);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    public record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path,
            List<String> fieldErrors
    ) {}
}