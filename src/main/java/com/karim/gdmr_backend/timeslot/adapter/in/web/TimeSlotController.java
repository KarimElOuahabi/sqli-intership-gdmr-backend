package com.karim.gdmr_backend.timeslot.adapter.in.web;

import com.karim.gdmr_backend.auth.adapter.in.web.dto.PagedResponse;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.timeslot.adapter.in.web.dto.CreateTimeSlotRequest;
import com.karim.gdmr_backend.timeslot.adapter.in.web.dto.TimeSlotResponse;
import com.karim.gdmr_backend.timeslot.domain.model.TimeSlot;
import com.karim.gdmr_backend.timeslot.domain.port.in.CreateTimeSlotUseCase;
import com.karim.gdmr_backend.timeslot.domain.port.in.ListTimeSlotsUseCase;
import com.karim.gdmr_backend.visit.domain.model.VisitType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Time Slots", description = "Doctor availability slot creation and listing")
public class TimeSlotController {

    private final CreateTimeSlotUseCase createTimeSlotUseCase;
    private final ListTimeSlotsUseCase listTimeSlotsUseCase;

    public TimeSlotController(CreateTimeSlotUseCase createTimeSlotUseCase,
                             ListTimeSlotsUseCase listTimeSlotsUseCase) {
        this.createTimeSlotUseCase = createTimeSlotUseCase;
        this.listTimeSlotsUseCase = listTimeSlotsUseCase;
    }

    @PostMapping("/api/timeslot")
    public ResponseEntity<TimeSlotResponse> createTimeSlot(@Valid @RequestBody CreateTimeSlotRequest request) {
        TimeSlot saved = createTimeSlotUseCase.createTimeSlot(
                new CreateTimeSlotUseCase.CreateTimeSlotCommand(
                        request.doctorId(), request.startTime(), request.endTime(), request.visitType()));
        return ResponseEntity.status(201).body(TimeSlotResponse.from(saved));
    }

    @GetMapping("/api/timeslots")
    public ResponseEntity<PagedResponse<TimeSlotResponse>> listTimeSlots(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) VisitType visitType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<TimeSlot> result = listTimeSlotsUseCase.listTimeSlots(
                new ListTimeSlotsUseCase.ListTimeSlotsQuery(doctorId, availableOnly, visitType, page, size));

        PageResult<TimeSlotResponse> mapped = new PageResult<>(
                result.content().stream().map(TimeSlotResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());

        return ResponseEntity.ok(PagedResponse.from(mapped));
    }
}