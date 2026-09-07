package com.karim.gdmr_backend.document.adapter.in.web;

import com.karim.gdmr_backend.document.adapter.in.web.dto.MedicalDocumentResponse;
import com.karim.gdmr_backend.document.domain.model.DocumentType;
import com.karim.gdmr_backend.document.domain.model.MedicalDocument;
import com.karim.gdmr_backend.document.domain.port.in.DeleteDocumentUseCase;
import com.karim.gdmr_backend.document.domain.port.in.DownloadDocumentUseCase;
import com.karim.gdmr_backend.document.domain.port.in.ListEmployeeDocumentsUseCase;
import com.karim.gdmr_backend.document.domain.port.in.UploadDocumentUseCase;
import com.karim.gdmr_backend.employee.domain.port.in.GetEmployeeIdByUserIdUseCase;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Medical document upload, download, listing, deletion")
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final ListEmployeeDocumentsUseCase listEmployeeDocumentsUseCase;
    private final DownloadDocumentUseCase downloadDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final GetEmployeeIdByUserIdUseCase getEmployeeIdByUserIdUseCase;

    public DocumentController(UploadDocumentUseCase uploadDocumentUseCase,
                              ListEmployeeDocumentsUseCase listEmployeeDocumentsUseCase,
                              DownloadDocumentUseCase downloadDocumentUseCase,
                              DeleteDocumentUseCase deleteDocumentUseCase,
                              GetEmployeeIdByUserIdUseCase getEmployeeIdByUserIdUseCase) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.listEmployeeDocumentsUseCase = listEmployeeDocumentsUseCase;
        this.downloadDocumentUseCase = downloadDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.getEmployeeIdByUserIdUseCase = getEmployeeIdByUserIdUseCase;
    }

    // Le formulaire envoie maintenant "employeeUserId" (le userId de l'employé concerné),
    // jamais la clé primaire interne de la table employees — cohérent avec PUT /api/employees/{userId}
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalDocumentResponse> upload(
            Authentication authentication,
            @RequestParam(required = false) Long employeeUserId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long visitId,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) Long previousVersionId,
            @RequestParam MultipartFile file) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String role = extractRole(authentication);

        // Un EMPLOYEE ne peut uploader QUE pour lui-même — on écrase toujours
        // la valeur envoyée par la sienne, résolue depuis son propre token.
        // Un DOCTOR connaît déjà l'employeeId (id domaine) via la visite qu'il traite,
        // pas le userId — on l'accepte directement pour lui éviter une résolution
        // qu'il n'a pas le droit de faire (il n'a pas accès à /api/employees/**).
        Long resolvedEmployeeId;
        if (role.equals("EMPLOYEE")) {
            resolvedEmployeeId = getEmployeeIdByUserIdUseCase.getEmployeeId(principal.userId());
        } else if (role.equals("DOCTOR") && employeeId != null) {
            resolvedEmployeeId = employeeId;
        } else {
            resolvedEmployeeId = getEmployeeIdByUserIdUseCase.getEmployeeId(employeeUserId);
        }

        MedicalDocument saved = uploadDocumentUseCase.upload(
                new UploadDocumentUseCase.UploadDocumentCommand(
                        resolvedEmployeeId, visitId, documentType, file,
                        principal.userId(), role, previousVersionId));

        return ResponseEntity.status(201).body(MedicalDocumentResponse.from(saved));
    }

    @GetMapping("/employee/{employeeUserId}")
    public ResponseEntity<List<MedicalDocumentResponse>> listForEmployee(
            Authentication authentication, @PathVariable Long employeeUserId) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String role = extractRole(authentication);

        Long targetUserId = role.equals("EMPLOYEE") ? principal.userId() : employeeUserId;
        Long resolvedEmployeeId = getEmployeeIdByUserIdUseCase.getEmployeeId(targetUserId);

        List<MedicalDocumentResponse> documents = listEmployeeDocumentsUseCase
                .listForEmployee(resolvedEmployeeId).stream()
                .map(MedicalDocumentResponse::from)
                .toList();

        return ResponseEntity.ok(documents);
    }

    // Variante pour le médecin : il connaît l'employeeId (id domaine) via la visite,
    // pas le userId de l'employé.
    @GetMapping(params = "employeeId")
    public ResponseEntity<List<MedicalDocumentResponse>> listForEmployeeById(
            @RequestParam Long employeeId) {
        List<MedicalDocumentResponse> documents = listEmployeeDocumentsUseCase
                .listForEmployee(employeeId).stream()
                .map(MedicalDocumentResponse::from)
                .toList();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(Authentication authentication, @PathVariable Long id) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String role = extractRole(authentication);

        Long employeeId = role.equals("EMPLOYEE")
                ? getEmployeeIdByUserIdUseCase.getEmployeeId(principal.userId())
                : null;

        var result = downloadDocumentUseCase.download(
                new DownloadDocumentUseCase.DownloadCommand(id, principal.userId(), role, employeeId));

        ByteArrayResource resource = new ByteArrayResource(result.content());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        String role = extractRole(authentication);
        deleteDocumentUseCase.delete(new DeleteDocumentUseCase.DeleteDocumentCommand(id, role));
        return ResponseEntity.noContent().build();
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}