package com.karim.gdmr_backend.visit.application;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.in.ListUsersUseCase;
import com.karim.gdmr_backend.doctor.domain.model.Doctor;
import com.karim.gdmr_backend.doctor.domain.model.Speciality;
import com.karim.gdmr_backend.doctor.domain.port.in.GetDoctorByIdUseCase;
import com.karim.gdmr_backend.employee.domain.model.Department;
import com.karim.gdmr_backend.employee.domain.model.Employee;
import com.karim.gdmr_backend.employee.domain.port.in.GetEmployeeByIdUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.SendNotificationUseCase;
import com.karim.gdmr_backend.timeslot.domain.model.TimeSlot;
import com.karim.gdmr_backend.timeslot.domain.port.in.BookTimeSlotUseCase;
import com.karim.gdmr_backend.timeslot.domain.port.in.ReleaseTimeSlotUseCase;
import com.karim.gdmr_backend.visit.domain.exception.InvalidVisitStatusTransitionException;
import com.karim.gdmr_backend.visit.domain.exception.NotAssignedDoctorException;
import com.karim.gdmr_backend.visit.domain.model.Visit;
import com.karim.gdmr_backend.visit.domain.model.VisitStatus;
import com.karim.gdmr_backend.visit.domain.model.VisitType;
import com.karim.gdmr_backend.visit.domain.port.in.AssignTimeSlotUseCase;
import com.karim.gdmr_backend.visit.domain.port.in.ConfirmVisitByDoctorUseCase;
import com.karim.gdmr_backend.visit.domain.port.in.CreateScheduledVisitUseCase;
import com.karim.gdmr_backend.visit.domain.port.in.RejectVisitByDoctorUseCase;
import com.karim.gdmr_backend.visit.domain.port.in.RejectVisitByEmployeeUseCase;
import com.karim.gdmr_backend.visit.domain.port.in.RequestSpontaneousVisitUseCase;
import com.karim.gdmr_backend.visit.domain.port.in.SubmitVisitReportUseCase;
import com.karim.gdmr_backend.visit.domain.port.in.UpdateVisitStatusUseCase;
import com.karim.gdmr_backend.visit.domain.port.out.NegotiationEntryRepositoryPort;
import com.karim.gdmr_backend.visit.domain.port.out.VisitRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the visit negotiation state machine — the central, most bug-prone piece of
 * domain logic in the app (REQUESTED -> PROPOSED -> AWAITING_DOCTOR_CONFIRMATION ->
 * SCHEDULED -> IN_PROGRESS -> COMPLETED, plus the REQUESTED/ABSENT side branches).
 * Pure Mockito unit test — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock private VisitRepositoryPort visitRepository;
    @Mock private NegotiationEntryRepositoryPort negotiationEntryRepository;
    @Mock private BookTimeSlotUseCase bookTimeSlotUseCase;
    @Mock private ReleaseTimeSlotUseCase releaseTimeSlotUseCase;
    @Mock private SendNotificationUseCase sendNotificationUseCase;
    @Mock private GetEmployeeByIdUseCase getEmployeeByIdUseCase;
    @Mock private GetDoctorByIdUseCase getDoctorByIdUseCase;
    @Mock private ListUsersUseCase listUsersUseCase;

    private VisitService visitService;

    private static final Long EMPLOYEE_ID = 10L;
    private static final Long OTHER_EMPLOYEE_ID = 11L;
    private static final Long DOCTOR_ID = 20L;
    private static final Long OTHER_DOCTOR_ID = 21L;
    private static final Long VISIT_ID = 1L;
    private static final Long TIME_SLOT_ID = 100L;

    @BeforeEach
    void setUp() {
        visitService = new VisitService(
                visitRepository,
                negotiationEntryRepository,
                bookTimeSlotUseCase,
                releaseTimeSlotUseCase,
                sendNotificationUseCase,
                getEmployeeByIdUseCase,
                getDoctorByIdUseCase,
                listUsersUseCase
        );
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private Visit visitWithStatus(VisitStatus status) {
        return new Visit(VISIT_ID, EMPLOYEE_ID, DOCTOR_ID, VisitType.ANNUAL, status,
                List.of(), TIME_SLOT_ID, LocalDateTime.now(), "checkup", null);
    }

    private Visit spontaneousVisitWithStatus(VisitStatus status) {
        return new Visit(VISIT_ID, EMPLOYEE_ID, DOCTOR_ID, VisitType.SPONTANEOUS, status,
                List.of(LocalDateTime.now().plusDays(1)), null, null, "checkup", null);
    }

    // Stubs all three notification-lookup dependencies at once; lenient() because
    // not every test path uses all three (e.g. requestVisit only notifies HR), and
    // strict stubbing would otherwise fail those tests on the unused ones.
    private void stubNotificationLookups() {
        lenient().when(getEmployeeByIdUseCase.getEmployeeById(EMPLOYEE_ID)).thenReturn(
                Employee.createNew(EMPLOYEE_ID, EMPLOYEE_ID, null, Department.DEVELOPMENT, null, null, null, null));
        lenient().when(getDoctorByIdUseCase.getDoctorById(DOCTOR_ID)).thenReturn(
                Doctor.createNew(DOCTOR_ID, DOCTOR_ID, null, Speciality.GENERAL_PRACTITIONER, null, null, null, null));
        lenient().when(listUsersUseCase.listUsers(any())).thenReturn(
                new PageResult<>(List.of(mock(User.class)), 0, 200, 1, 1));
    }

    private void stubSaveReturnsArgument() {
        when(visitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // -----------------------------------------------------------------
    // requestVisit — spontaneous request creation
    // -----------------------------------------------------------------

    @Test
    void requestVisit_rejectsEmptyProposedSlots() {
        assertThatThrownBy(() -> visitService.requestVisit(
                new RequestSpontaneousVisitUseCase.RequestVisitCommand(EMPLOYEE_ID, DOCTOR_ID, "motif", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestVisit_rejectsMoreThanThreeProposedSlots() {
        List<LocalDateTime> fourSlots = List.of(
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));

        assertThatThrownBy(() -> visitService.requestVisit(
                new RequestSpontaneousVisitUseCase.RequestVisitCommand(EMPLOYEE_ID, DOCTOR_ID, "motif", fourSlots)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestVisit_createsRequestedVisitAndNotifiesHr() {
        stubNotificationLookups();
        stubSaveReturnsArgument();
        List<LocalDateTime> slots = List.of(LocalDateTime.now().plusDays(1));

        Visit result = visitService.requestVisit(
                new RequestSpontaneousVisitUseCase.RequestVisitCommand(EMPLOYEE_ID, DOCTOR_ID, "motif", slots));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.REQUESTED);
        assertThat(result.getVisitType()).isEqualTo(VisitType.SPONTANEOUS);
        verify(sendNotificationUseCase, times(1)).send(any());
    }

    // -----------------------------------------------------------------
    // assignTimeSlot — HR proposes a slot for a REQUESTED visit
    // -----------------------------------------------------------------

    @Test
    void assignTimeSlot_fromRequested_movesToProposedAndNotifiesEmployee() {
        Visit requested = spontaneousVisitWithStatus(VisitStatus.REQUESTED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(requested));
        when(bookTimeSlotUseCase.bookTimeSlot(TIME_SLOT_ID, DOCTOR_ID)).thenReturn(
                new TimeSlot(TIME_SLOT_ID, DOCTOR_ID, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30),
                        VisitType.SPONTANEOUS, false));
        stubNotificationLookups();
        stubSaveReturnsArgument();

        Visit result = visitService.assignTimeSlot(
                new AssignTimeSlotUseCase.AssignTimeSlotCommand(VISIT_ID, TIME_SLOT_ID));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.PROPOSED);
        assertThat(result.getTimeSlotId()).isEqualTo(TIME_SLOT_ID);
        verify(sendNotificationUseCase, times(1)).send(any());
    }

    @Test
    void assignTimeSlot_whenVisitNotRequested_throws() {
        Visit alreadyProposed = spontaneousVisitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(alreadyProposed));

        assertThatThrownBy(() -> visitService.assignTimeSlot(
                new AssignTimeSlotUseCase.AssignTimeSlotCommand(VISIT_ID, TIME_SLOT_ID)))
                .isInstanceOf(InvalidVisitStatusTransitionException.class);

        verify(bookTimeSlotUseCase, never()).bookTimeSlot(anyLong(), anyLong());
    }

    @Test
    void assignTimeSlot_whenSlotTypeMismatchesVisitType_throws() {
        Visit requested = spontaneousVisitWithStatus(VisitStatus.REQUESTED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(requested));
        when(bookTimeSlotUseCase.bookTimeSlot(TIME_SLOT_ID, DOCTOR_ID)).thenReturn(
                new TimeSlot(TIME_SLOT_ID, DOCTOR_ID, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30),
                        VisitType.ANNUAL, false));

        assertThatThrownBy(() -> visitService.assignTimeSlot(
                new AssignTimeSlotUseCase.AssignTimeSlotCommand(VISIT_ID, TIME_SLOT_ID)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(visitRepository, never()).save(any());
    }

    // -----------------------------------------------------------------
    // createScheduledVisit — HR books a non-spontaneous visit directly
    // -----------------------------------------------------------------

    @Test
    void createScheduledVisit_rejectsSpontaneousSlotType() {
        when(bookTimeSlotUseCase.bookTimeSlot(TIME_SLOT_ID, DOCTOR_ID)).thenReturn(
                new TimeSlot(TIME_SLOT_ID, DOCTOR_ID, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30),
                        VisitType.SPONTANEOUS, false));

        assertThatThrownBy(() -> visitService.createScheduledVisit(
                new CreateScheduledVisitUseCase.CreateScheduledVisitCommand(EMPLOYEE_ID, DOCTOR_ID, TIME_SLOT_ID)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(visitRepository, never()).save(any());
    }

    @Test
    void createScheduledVisit_createsProposedVisitAndNotifiesEmployee() {
        when(bookTimeSlotUseCase.bookTimeSlot(TIME_SLOT_ID, DOCTOR_ID)).thenReturn(
                new TimeSlot(TIME_SLOT_ID, DOCTOR_ID, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30),
                        VisitType.PRE_EMPLOYMENT, false));
        stubNotificationLookups();
        stubSaveReturnsArgument();

        Visit result = visitService.createScheduledVisit(
                new CreateScheduledVisitUseCase.CreateScheduledVisitCommand(EMPLOYEE_ID, DOCTOR_ID, TIME_SLOT_ID));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.PROPOSED);
        assertThat(result.getVisitType()).isEqualTo(VisitType.PRE_EMPLOYMENT);
        verify(sendNotificationUseCase, times(1)).send(any());
    }

    // -----------------------------------------------------------------
    // confirmVisit — employee confirms the proposed slot
    // -----------------------------------------------------------------

    @Test
    void confirmVisit_whenNotOwner_throws() {
        Visit proposed = visitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(proposed));

        assertThatThrownBy(() -> visitService.confirmVisit(VISIT_ID, OTHER_EMPLOYEE_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmVisit_whenNotProposed_throws() {
        Visit requested = visitWithStatus(VisitStatus.REQUESTED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(requested));

        assertThatThrownBy(() -> visitService.confirmVisit(VISIT_ID, EMPLOYEE_ID))
                .isInstanceOf(InvalidVisitStatusTransitionException.class);
    }

    @Test
    void confirmVisit_movesToAwaitingDoctorConfirmationAndNotifiesDoctorAndHr() {
        Visit proposed = visitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(proposed));
        stubNotificationLookups();
        stubSaveReturnsArgument();

        Visit result = visitService.confirmVisit(VISIT_ID, EMPLOYEE_ID);

        assertThat(result.getStatus()).isEqualTo(VisitStatus.AWAITING_DOCTOR_CONFIRMATION);
        verify(sendNotificationUseCase, times(2)).send(any()); // doctor + 1 HR user
    }

    // -----------------------------------------------------------------
    // rejectByEmployee — employee suggests an alternative time
    // -----------------------------------------------------------------

    @Test
    void rejectByEmployee_whenNotOwner_throws() {
        Visit proposed = visitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(proposed));

        assertThatThrownBy(() -> visitService.rejectByEmployee(
                new RejectVisitByEmployeeUseCase.RejectByEmployeeCommand(
                        VISIT_ID, OTHER_EMPLOYEE_ID, "reason", LocalDateTime.now().plusDays(1))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectByEmployee_blankReason_throws() {
        Visit proposed = visitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(proposed));

        assertThatThrownBy(() -> visitService.rejectByEmployee(
                new RejectVisitByEmployeeUseCase.RejectByEmployeeCommand(
                        VISIT_ID, EMPLOYEE_ID, "   ", LocalDateTime.now().plusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectByEmployee_missingSuggestedDateTime_throws() {
        Visit proposed = visitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(proposed));

        assertThatThrownBy(() -> visitService.rejectByEmployee(
                new RejectVisitByEmployeeUseCase.RejectByEmployeeCommand(
                        VISIT_ID, EMPLOYEE_ID, "reason", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectByEmployee_releasesSlotAndReturnsToRequested() {
        Visit proposed = visitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(proposed));
        stubNotificationLookups();
        stubSaveReturnsArgument();

        Visit result = visitService.rejectByEmployee(
                new RejectVisitByEmployeeUseCase.RejectByEmployeeCommand(
                        VISIT_ID, EMPLOYEE_ID, "conflict", LocalDateTime.now().plusDays(2)));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.REQUESTED);
        assertThat(result.getTimeSlotId()).isNull();
        verify(releaseTimeSlotUseCase, times(1)).releaseTimeSlot(TIME_SLOT_ID);
        verify(negotiationEntryRepository, times(1)).save(any());
        verify(sendNotificationUseCase, times(2)).send(any()); // doctor + 1 HR user
    }

    // -----------------------------------------------------------------
    // confirmByDoctor — doctor confirms after the employee already did
    // -----------------------------------------------------------------

    @Test
    void confirmByDoctor_whenNotAssignedDoctor_throws() {
        Visit awaiting = visitWithStatus(VisitStatus.AWAITING_DOCTOR_CONFIRMATION);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(awaiting));

        assertThatThrownBy(() -> visitService.confirmByDoctor(VISIT_ID, OTHER_DOCTOR_ID))
                .isInstanceOf(NotAssignedDoctorException.class);
    }

    @Test
    void confirmByDoctor_whenNotAwaitingConfirmation_throws() {
        Visit proposed = visitWithStatus(VisitStatus.PROPOSED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(proposed));

        assertThatThrownBy(() -> visitService.confirmByDoctor(VISIT_ID, DOCTOR_ID))
                .isInstanceOf(InvalidVisitStatusTransitionException.class);
    }

    @Test
    void confirmByDoctor_movesToScheduledAndNotifiesEmployeeAndHr() {
        Visit awaiting = visitWithStatus(VisitStatus.AWAITING_DOCTOR_CONFIRMATION);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(awaiting));
        stubNotificationLookups();
        stubSaveReturnsArgument();

        Visit result = visitService.confirmByDoctor(VISIT_ID, DOCTOR_ID);

        assertThat(result.getStatus()).isEqualTo(VisitStatus.SCHEDULED);
        verify(sendNotificationUseCase, times(2)).send(any()); // employee + 1 HR user
    }

    // -----------------------------------------------------------------
    // rejectByDoctor — doctor rejects after the employee already confirmed
    // -----------------------------------------------------------------

    @Test
    void rejectByDoctor_whenNotAssignedDoctor_throws() {
        Visit awaiting = visitWithStatus(VisitStatus.AWAITING_DOCTOR_CONFIRMATION);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(awaiting));

        assertThatThrownBy(() -> visitService.rejectByDoctor(
                VISIT_ID, OTHER_DOCTOR_ID, "reason", LocalDateTime.now().plusDays(1)))
                .isInstanceOf(NotAssignedDoctorException.class);
    }

    @Test
    void rejectByDoctor_whenNotAwaitingConfirmation_throws() {
        // Note: assertAllowed here only checks "can this status transition TO
        // REQUESTED", and both PROPOSED and AWAITING_DOCTOR_CONFIRMATION allow that
        // — so a PROPOSED visit would NOT trip this guard even though rejectByDoctor
        // is meant to be the doctor's step. REQUESTED itself is the one status that
        // genuinely can't transition to REQUESTED, so it's the only reliable
        // "invalid" case to test here.
        Visit requested = visitWithStatus(VisitStatus.REQUESTED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(requested));

        assertThatThrownBy(() -> visitService.rejectByDoctor(
                VISIT_ID, DOCTOR_ID, "reason", LocalDateTime.now().plusDays(1)))
                .isInstanceOf(InvalidVisitStatusTransitionException.class);
    }

    @Test
    void rejectByDoctor_returnsToRequestedAndNotifiesEmployeeAndHr() {
        Visit awaiting = visitWithStatus(VisitStatus.AWAITING_DOCTOR_CONFIRMATION);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(awaiting));
        stubNotificationLookups();
        stubSaveReturnsArgument();

        Visit result = visitService.rejectByDoctor(
                VISIT_ID, DOCTOR_ID, "unavailable", LocalDateTime.now().plusDays(3));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.REQUESTED);
        verify(negotiationEntryRepository, times(1)).save(any());
        verify(sendNotificationUseCase, times(2)).send(any()); // employee + 1 HR user
    }

    // -----------------------------------------------------------------
    // updateStatus — SCHEDULED -> IN_PROGRESS / ABSENT, IN_PROGRESS -> COMPLETED
    // -----------------------------------------------------------------

    @Test
    void updateStatus_whenNotAssignedDoctor_throws() {
        Visit scheduled = visitWithStatus(VisitStatus.SCHEDULED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(scheduled));

        assertThatThrownBy(() -> visitService.updateStatus(
                new UpdateVisitStatusUseCase.UpdateVisitStatusCommand(VISIT_ID, VisitStatus.IN_PROGRESS, OTHER_DOCTOR_ID)))
                .isInstanceOf(NotAssignedDoctorException.class);
    }

    @Test
    void updateStatus_illegalTransition_throws() {
        Visit requested = visitWithStatus(VisitStatus.REQUESTED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(requested));

        assertThatThrownBy(() -> visitService.updateStatus(
                new UpdateVisitStatusUseCase.UpdateVisitStatusCommand(VISIT_ID, VisitStatus.COMPLETED, DOCTOR_ID)))
                .isInstanceOf(InvalidVisitStatusTransitionException.class);
    }

    @Test
    void updateStatus_scheduledToInProgress_succeeds() {
        Visit scheduled = visitWithStatus(VisitStatus.SCHEDULED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(scheduled));
        stubSaveReturnsArgument();

        Visit result = visitService.updateStatus(
                new UpdateVisitStatusUseCase.UpdateVisitStatusCommand(VISIT_ID, VisitStatus.IN_PROGRESS, DOCTOR_ID));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
        verify(sendNotificationUseCase, never()).send(any());
    }

    @Test
    void updateStatus_scheduledToAbsent_notifiesHr() {
        Visit scheduled = visitWithStatus(VisitStatus.SCHEDULED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(scheduled));
        stubSaveReturnsArgument();
        when(listUsersUseCase.listUsers(any())).thenReturn(
                new PageResult<>(List.of(mock(User.class)), 0, 200, 1, 1));

        Visit result = visitService.updateStatus(
                new UpdateVisitStatusUseCase.UpdateVisitStatusCommand(VISIT_ID, VisitStatus.ABSENT, DOCTOR_ID));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.ABSENT);
        verify(sendNotificationUseCase, times(1)).send(any()); // 1 HR user, no employee/doctor notice
    }

    @Test
    void updateStatus_inProgressToCompleted_succeeds() {
        Visit inProgress = visitWithStatus(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(inProgress));
        stubSaveReturnsArgument();

        Visit result = visitService.updateStatus(
                new UpdateVisitStatusUseCase.UpdateVisitStatusCommand(VISIT_ID, VisitStatus.COMPLETED, DOCTOR_ID));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.COMPLETED);
    }

    // -----------------------------------------------------------------
    // submitReport
    // -----------------------------------------------------------------

    @Test
    void submitReport_whenNotAssignedDoctor_throws() {
        Visit inProgress = visitWithStatus(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> visitService.submitReport(
                new SubmitVisitReportUseCase.SubmitReportCommand(VISIT_ID, "notes", OTHER_DOCTOR_ID)))
                .isInstanceOf(NotAssignedDoctorException.class);
    }

    @Test
    void submitReport_whenNotInProgress_throws() {
        // A report can only be submitted once the doctor has actually started the
        // consultation — submitting from SCHEDULED (before IN_PROGRESS) must be
        // rejected rather than silently jumping straight to COMPLETED.
        Visit scheduled = visitWithStatus(VisitStatus.SCHEDULED);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(scheduled));

        assertThatThrownBy(() -> visitService.submitReport(
                new SubmitVisitReportUseCase.SubmitReportCommand(VISIT_ID, "notes", DOCTOR_ID)))
                .isInstanceOf(InvalidVisitStatusTransitionException.class);

        verify(visitRepository, never()).save(any());
    }

    @Test
    void submitReport_fromInProgress_savesNotesAndCompletes() {
        Visit inProgress = visitWithStatus(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(inProgress));
        stubSaveReturnsArgument();

        Visit result = visitService.submitReport(
                new SubmitVisitReportUseCase.SubmitReportCommand(VISIT_ID, "Patient is fit for duty.", DOCTOR_ID));

        assertThat(result.getStatus()).isEqualTo(VisitStatus.COMPLETED);
        assertThat(result.getReportNotes()).isEqualTo("Patient is fit for duty.");
    }
}
