package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.PreventiveFlag;
import com.hostel.hostel_backend.repositories.ComplaintRepository;
import com.hostel.hostel_backend.repositories.PreventiveFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PreventiveMaintenanceServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private PreventiveFlagRepository preventiveFlagRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PreventiveMaintenanceService preventiveMaintenanceService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCheckRecurrenceUnderThreshold() {
        when(complaintRepository.countByAssetIdAndCategoryAndRaisedAtAfter(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(2L);

        preventiveMaintenanceService.checkRecurrence("A-214", "PLUMBING");

        // Verify no flag saved or audit logged since count is 2 (< 3)
        verify(preventiveFlagRepository, never()).save(any(PreventiveFlag.class));
        verify(auditService, never()).log(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    public void testCheckRecurrenceOverThresholdNewFlag() {
        when(complaintRepository.countByAssetIdAndCategoryAndRaisedAtAfter(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(3L);

        when(preventiveFlagRepository.findByAssetIdAndCategoryAndResolved(anyString(), anyString(), anyBoolean()))
                .thenReturn(Optional.empty());

        when(preventiveFlagRepository.save(any(PreventiveFlag.class))).thenAnswer(i -> {
            PreventiveFlag flag = i.getArgument(0);
            flag.setId("flag-123");
            return flag;
        });

        preventiveMaintenanceService.checkRecurrence("A-214", "ELECTRICAL");

        // Verify new flag saved and audit logged
        verify(preventiveFlagRepository, times(1)).save(any(PreventiveFlag.class));
        verify(auditService, times(1)).log(
                eq("SYSTEM"),
                eq("SYSTEM"),
                eq("PREVENTIVE_MAINTENANCE_FLAG"),
                eq("PREVENTIVE_FLAG"),
                eq("flag-123"),
                anyMap()
        );
    }

    @Test
    public void testCheckRecurrenceOverThresholdUpdateFlag() {
        when(complaintRepository.countByAssetIdAndCategoryAndRaisedAtAfter(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(4L);

        PreventiveFlag existingFlag = new PreventiveFlag();
        existingFlag.setId("flag-existing");
        existingFlag.setAssetId("A-214");
        existingFlag.setCategory("CLEANLINESS");
        existingFlag.setComplaintCount(3);

        when(preventiveFlagRepository.findByAssetIdAndCategoryAndResolved("A-214", "CLEANLINESS", false))
                .thenReturn(Optional.of(existingFlag));

        when(preventiveFlagRepository.save(any(PreventiveFlag.class))).thenAnswer(i -> i.getArgument(0));

        preventiveMaintenanceService.checkRecurrence("A-214", "CLEANLINESS");

        // Verify existing flag is saved with count updated to 4 and audit logged
        verify(preventiveFlagRepository, times(1)).save(existingFlag);
        verify(auditService, times(1)).log(
                eq("SYSTEM"),
                eq("SYSTEM"),
                eq("PREVENTIVE_MAINTENANCE_FLAG"),
                eq("PREVENTIVE_FLAG"),
                eq("flag-existing"),
                anyMap()
        );
    }
}
