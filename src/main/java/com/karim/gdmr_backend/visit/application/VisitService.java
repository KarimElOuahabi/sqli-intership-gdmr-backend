package com.karim.gdmr_backend.visit.application;

import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.in.ListUsersUseCase;
import com.karim.gdmr_backend.doctor.domain.port.in.GetDoctorByIdUseCase;
import com.karim.gdmr_backend.employee.domain.port.in.GetEmployeeByIdUseCase;
import com.karim.gdmr_backend.notification.domain.model.NotificationType;
import com.karim.gdmr_backend.notification.domain.port.in.SendNotificationUseCase;
import com.karim.gdmr_backend.timeslot.domain.model.TimeSlot;
import com.karim.gdmr_backend.timeslot.domain.port.in.BookTimeSlotUseCase;
import com.karim.gdmr_backend.timeslot.domain.port.in.ReleaseTimeSlotUseCase;
import com.karim.gdmr_backend.visit.domain.exception.InvalidVisitStatusTransitionException;
import com.karim.gdmr_backend.visit.domain.exception.NotAssignedDoctorException;
import com.karim.gdmr_backend.visit.domain.exception.VisitNotFoundException;
import com.karim.gdmr_backend.visit.domain.model.NegotiationActor;
import com.karim.gdmr_backend.visit.domain.model.NegotiationEntry;
import com.karim.gdmr_backend.visit.domain.model.Visit;
import com.karim.gdmr_backend.visit.domain.model.VisitStatus;
import com.karim.gdmr_backend.visit.domain.model.VisitType;
import com.karim.gdmr_backend.visit.domain.port.in.*;
import com.karim.gdmr_backend.visit.domain.port.out.NegotiationEntryRepositoryPort;
import com.karim.gdmr_backend.visit.domain.port.out.VisitRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class VisitService implements
        RequestSpontaneousVisitUseCase,
        AssignTimeSlotUseCase,
        CreateScheduledVisitUseCase,
        ConfirmVisitUseCase,
        RejectVisitByEmployeeUseCase,
        ConfirmVisitByDoctorUseCase,
        RejectVisitByDoctorUseCase,
        ListNegotiationHistoryUseCase,
        GetVisitByIdUseCase,
        SendVisitRemindersUseCase,
        UpdateVisitStatusUseCase,
        SubmitVisitReportUseCase,
        ListVisitsUseCase,
        GetPatientEmployeeIdsUseCase {

    // Explicit Locale avoids relying on the JVM's default locale for month names — minimal
    // container base images (e.g. Azure's Linux JRE) don't always ship full locale data, which
    // otherwise throws at runtime the first time this formats a real (non-null) date.
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm", Locale.ENGLISH);

    private static final Map<VisitStatus, Set<VisitStatus>> ALLOWED_TRANSITIONS = Map.of(
            VisitStatus.REQUESTED, Set.of(VisitStatus.PROPOSED),
            VisitStatus.PROPOSED, Set.of(VisitStatus.AWAITING_DOCTOR_CONFIRMATION, VisitStatus.REQUESTED),
            VisitStatus.AWAITING_DOCTOR_CONFIRMATION, Set.of(VisitStatus.SCHEDULED, VisitStatus.REQUESTED),
            VisitStatus.SCHEDULED, Set.of(VisitStatus.IN_PROGRESS, VisitStatus.ABSENT),
            VisitStatus.IN_PROGRESS, Set.of(VisitStatus.COMPLETED),
            VisitStatus.COMPLETED, Set.of(),
            VisitStatus.ABSENT, Set.of()
    );

    private final VisitRepositoryPort visitRepository;
    private final NegotiationEntryRepositoryPort negotiationEntryRepository;
    private final BookTimeSlotUseCase bookTimeSlotUseCase;
    private final ReleaseTimeSlotUseCase releaseTimeSlotUseCase;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final GetEmployeeByIdUseCase getEmployeeByIdUseCase;
    private final GetDoctorByIdUseCase getDoctorByIdUseCase;
    private final ListUsersUseCase listUsersUseCase;

    public VisitService(VisitRepositoryPort visitRepository,
                         NegotiationEntryRepositoryPort negotiationEntryRepository,
                         BookTimeSlotUseCase bookTimeSlotUseCase,
                         ReleaseTimeSlotUseCase releaseTimeSlotUseCase,
                         SendNotificationUseCase sendNotificationUseCase,
                         GetEmployeeByIdUseCase getEmployeeByIdUseCase,
                         GetDoctorByIdUseCase getDoctorByIdUseCase,
                         ListUsersUseCase listUsersUseCase) {
        this.visitRepository = visitRepository;
        this.negotiationEntryRepository = negotiationEntryRepository;
        this.bookTimeSlotUseCase = bookTimeSlotUseCase;
        this.releaseTimeSlotUseCase = releaseTimeSlotUseCase;
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.getEmployeeByIdUseCase = getEmployeeByIdUseCase;
        this.getDoctorByIdUseCase = getDoctorByIdUseCase;
        this.listUsersUseCase = listUsersUseCase;
    }

    @Override
    public Visit requestVisit(RequestVisitCommand command) {
        if (command.proposedSlots() == null || command.proposedSlots().isEmpty()
                || command.proposedSlots().size() > 3) {
            throw new IllegalArgumentException("You must propose between 1 and 3 time slots");
        }
        Visit visit = visitRepository.save(
                Visit.createNew(command.employeeId(), command.doctorId(), command.motif(), command.proposedSlots()));

        notifyAllHr(NotificationType.VISIT_REQUESTED, "New visit request",
                "An employee requested a spontaneous visit and is waiting for a slot.", visit.getId());

        return visit;
    }

    @Override
    public Visit assignTimeSlot(AssignTimeSlotCommand command) {
        Visit existing = getOrThrow(command.visitId());

        assertAllowed(existing.getStatus(), VisitStatus.PROPOSED);

        var bookedSlot = bookTimeSlotUseCase.bookTimeSlot(command.timeSlotId(), existing.getDoctorId());

        if (bookedSlot.getVisitType() != existing.getVisitType()) {
            throw new IllegalArgumentException(
                    "TimeSlot type " + bookedSlot.getVisitType() + " does not match visit type " + existing.getVisitType());
        }

        Visit updated = visitRepository.save(existing.withAssignedSlot(bookedSlot.getId(), bookedSlot.getStartTime()));

        notifyEmployee(updated, NotificationType.SLOT_PROPOSED, "A visit was proposed for you",
                "A slot was proposed for " + format(bookedSlot.getStartTime()) + ". Please confirm or suggest another time.");

        return updated;
    }

    @Override
    public Visit createScheduledVisit(CreateScheduledVisitCommand command) {
        TimeSlot slot = bookTimeSlotUseCase.bookTimeSlot(command.timeSlotId(), command.doctorId());

        if (slot.getVisitType() == VisitType.SPONTANEOUS) {
            throw new IllegalArgumentException(
                    "Use the employee request flow for spontaneous visits");
        }

        Visit created = visitRepository.save(Visit.createScheduled(
                command.employeeId(), command.doctorId(), slot.getVisitType(),
                slot.getId(), slot.getStartTime()));

        notifyEmployee(created, NotificationType.SLOT_PROPOSED, "A visit was scheduled for you",
                "HR scheduled a " + slot.getVisitType() + " visit for " + format(slot.getStartTime())
                        + ". Please confirm or suggest another time.");

        return created;
    }

    @Override
    public Visit confirmVisit(Long visitId, Long requestingEmployeeId) {
        Visit existing = getOrThrow(visitId);

        if (!existing.getEmployeeId().equals(requestingEmployeeId)) {
            throw new IllegalStateException("This visit does not belong to you");
        }

        assertAllowed(existing.getStatus(), VisitStatus.AWAITING_DOCTOR_CONFIRMATION);

        Visit updated = visitRepository.save(existing.withStatus(VisitStatus.AWAITING_DOCTOR_CONFIRMATION));

        String message = "The employee confirmed the slot for " + format(updated.getConfirmedDateTime())
                + ". Your confirmation is needed.";
        notifyDoctor(updated, NotificationType.EMPLOYEE_CONFIRMED, "Employee confirmed a visit", message);
        notifyAllHr(NotificationType.EMPLOYEE_CONFIRMED, "Employee confirmed a visit", message, updated.getId());

        return updated;
    }

    @Override
    public Visit rejectByEmployee(RejectByEmployeeCommand command) {
        Visit existing = getOrThrow(command.visitId());

        if (!existing.getEmployeeId().equals(command.requestingEmployeeId())) {
            throw new IllegalStateException("This visit does not belong to you");
        }

        assertAllowed(existing.getStatus(), VisitStatus.REQUESTED);

        Visit updated = reject(existing, NegotiationActor.EMPLOYEE, command.reason(), command.suggestedDateTime());

        String message = "The employee suggested " + format(command.suggestedDateTime())
                + " instead: " + command.reason();
        notifyDoctor(updated, NotificationType.VISIT_REJECTED, "Employee suggested another time", message);
        notifyAllHr(NotificationType.VISIT_REJECTED, "Employee suggested another time", message, updated.getId());

        return updated;
    }

    @Override
    public Visit confirmByDoctor(Long visitId, Long requestingDoctorId) {
        Visit existing = getOrThrow(visitId);

        if (!existing.getDoctorId().equals(requestingDoctorId)) {
            throw new NotAssignedDoctorException(visitId);
        }

        assertAllowed(existing.getStatus(), VisitStatus.SCHEDULED);

        Visit updated = visitRepository.save(existing.withStatus(VisitStatus.SCHEDULED));

        String message = "Your visit is confirmed for " + format(updated.getConfirmedDateTime()) + ".";
        notifyEmployee(updated, NotificationType.VISIT_SCHEDULED, "Visit scheduled", message);
        notifyAllHr(NotificationType.VISIT_SCHEDULED, "Visit scheduled", message, updated.getId());

        return updated;
    }

    @Override
    public Visit rejectByDoctor(Long visitId, Long requestingDoctorId, String reason, LocalDateTime suggestedDateTime) {
        Visit existing = getOrThrow(visitId);

        if (!existing.getDoctorId().equals(requestingDoctorId)) {
            throw new NotAssignedDoctorException(visitId);
        }

        assertAllowed(existing.getStatus(), VisitStatus.REQUESTED);

        Visit updated = reject(existing, NegotiationActor.DOCTOR, reason, suggestedDateTime);

        String message = "The doctor suggested " + format(suggestedDateTime) + " instead: " + reason;
        notifyEmployee(updated, NotificationType.VISIT_REJECTED, "Doctor suggested another time", message);
        notifyAllHr(NotificationType.VISIT_REJECTED, "Doctor suggested another time", message, updated.getId());

        return updated;
    }

    private Visit reject(Visit existing, NegotiationActor actor, String reason, LocalDateTime suggestedDateTime) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to reject a visit");
        }
        if (suggestedDateTime == null) {
            throw new IllegalArgumentException("An alternative date/time is required to reject a visit");
        }

        negotiationEntryRepository.save(
                NegotiationEntry.createNew(existing.getId(), actor, reason.trim(), suggestedDateTime));

        if (existing.getTimeSlotId() != null) {
            releaseTimeSlotUseCase.releaseTimeSlot(existing.getTimeSlotId());
        }

        return visitRepository.save(existing.withReleasedSlot());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NegotiationEntry> listHistory(Long visitId) {
        return negotiationEntryRepository.findAllByVisitId(visitId);
    }

    @Override
    @Transactional(readOnly = true)
    public Visit getVisit(Long visitId) {
        return getOrThrow(visitId);
    }

    @Override
    public Visit updateStatus(UpdateVisitStatusCommand command) {
        Visit existing = getOrThrow(command.visitId());

        if (!existing.getDoctorId().equals(command.requestingDoctorId())) {
            throw new NotAssignedDoctorException(command.visitId());
        }

        assertAllowed(existing.getStatus(), command.newStatus());

        Visit updated = visitRepository.save(existing.withStatus(command.newStatus()));

        if (command.newStatus() == VisitStatus.ABSENT) {
            notifyAllHr(NotificationType.VISIT_ABSENT, "Employee did not show up",
                    "The employee did not attend their visit scheduled for " + format(updated.getConfirmedDateTime()) + ".",
                    updated.getId());
        }

        return updated;
    }

    @Override
    public Visit submitReport(SubmitReportCommand command) {
        Visit existing = getOrThrow(command.visitId());

        if (!existing.getDoctorId().equals(command.requestingDoctorId())) {
            throw new NotAssignedDoctorException(command.visitId());
        }

        assertAllowed(existing.getStatus(), VisitStatus.COMPLETED);

        return visitRepository.save(existing.withReport(command.reportNotes()));
    }

    @Override
    @Transactional(readOnly = true)
    public com.karim.gdmr_backend.auth.domain.model.PageResult<Visit> listVisits(ListVisitsQuery query) {
        return visitRepository.findAllPaged(query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getPatientEmployeeIds(Long doctorId) {
        return visitRepository.findDistinctEmployeeIdsByDoctorId(doctorId);
    }

    @Override
    public int sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Visit> upcoming = visitRepository.findScheduledBetween(now, now.plusHours(24));

        int sent = 0;
        for (Visit visit : upcoming) {
            if (sendNotificationUseCase.alreadySent(visit.getId(), NotificationType.VISIT_REMINDER)) {
                continue;
            }
            String message = "Reminder: your " + visit.getVisitType() + " visit is coming up on "
                    + format(visit.getConfirmedDateTime()) + ".";
            notifyEmployee(visit, NotificationType.VISIT_REMINDER, "Upcoming visit reminder", message);
            notifyDoctor(visit, NotificationType.VISIT_REMINDER, "Upcoming visit reminder", message);
            sent++;
        }
        return sent;
    }

    private void assertAllowed(VisitStatus from, VisitStatus to) {
        Set<VisitStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidVisitStatusTransitionException(from, to);
        }
    }

    private Visit getOrThrow(Long visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? "an unspecified time" : dateTime.format(DISPLAY_FORMAT);
    }

    private void notifyEmployee(Visit visit, NotificationType type, String title, String message) {
        Long userId = getEmployeeByIdUseCase.getEmployeeById(visit.getEmployeeId()).getUserId();
        sendNotificationUseCase.send(new SendNotificationUseCase.SendNotificationCommand(
                userId, type, title, message, visit.getId()));
    }

    private void notifyDoctor(Visit visit, NotificationType type, String title, String message) {
        Long userId = getDoctorByIdUseCase.getDoctorById(visit.getDoctorId()).getUserId();
        sendNotificationUseCase.send(new SendNotificationUseCase.SendNotificationCommand(
                userId, type, title, message, visit.getId()));
    }

    private void notifyAllHr(NotificationType type, String title, String message, Long visitId) {
        var hrUsers = listUsersUseCase.listUsers(
                new ListUsersUseCase.ListUsersQuery(null, Role.HR, true, 0, 200));
        for (User hr : hrUsers.content()) {
            sendNotificationUseCase.send(new SendNotificationUseCase.SendNotificationCommand(
                    hr.getId(), type, title, message, visitId));
        }
    }
}
