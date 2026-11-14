package com.leadnest.crm.dto;

import com.leadnest.crm.domain.Activity;
import com.leadnest.crm.domain.ActivityType;
import com.leadnest.crm.domain.Deal;
import com.leadnest.crm.domain.DealStage;
import com.leadnest.crm.domain.FollowUp;
import com.leadnest.crm.domain.FollowUpView;
import com.leadnest.crm.domain.HistoryEventType;
import com.leadnest.crm.domain.Lead;
import com.leadnest.crm.domain.LeadHistory;
import com.leadnest.crm.domain.LeadSource;
import com.leadnest.crm.domain.LeadStatus;
import com.leadnest.crm.domain.Role;
import com.leadnest.crm.domain.UserAccount;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record MessageResponse(String message) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank String password
    ) {
    }

    public record PasswordChangeRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {
    }

    public record UserResponse(Long id, String name, String email, Role role, boolean active, Instant createdAt) {
        public static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(), user.getCreatedAt());
        }
    }

    public record CreateAgentRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record ActiveStatusRequest(@NotNull Boolean active) {
    }

    public record LeadWriteRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @Size(max = 160) String company,
            @NotNull LeadSource source,
            @Size(max = 4000) String notes,
            Long agentId
    ) {
    }

    public record StatusChangeRequest(
            @NotNull LeadStatus status,
            @Size(max = 500) String lostReason
    ) {
    }

    public record AssignLeadRequest(@NotNull Long agentId) {
    }

    public record LeadResponse(
            Long id,
            String name,
            String email,
            String phone,
            String company,
            LeadSource source,
            LeadStatus status,
            Long assignedAgentId,
            String assignedAgentName,
            String notes,
            String lostReason,
            boolean archived,
            Instant archivedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static LeadResponse from(Lead lead) {
            UserAccount agent = lead.getAssignedAgent();
            return new LeadResponse(
                    lead.getId(),
                    lead.getName(),
                    lead.getEmail(),
                    lead.getPhone(),
                    lead.getCompany(),
                    lead.getSource(),
                    lead.getStatus(),
                    agent.getId(),
                    agent.getName(),
                    lead.getNotes(),
                    lead.getLostReason(),
                    lead.isArchived(),
                    lead.getArchivedAt(),
                    lead.getCreatedAt(),
                    lead.getUpdatedAt());
        }
    }

    public record ActivityRequest(
            @NotNull ActivityType type,
            @NotBlank @Size(max = 2000) String description
    ) {
    }

    public record ActivityResponse(
            Long id,
            Long leadId,
            ActivityType type,
            String description,
            Long authorId,
            String authorName,
            Instant createdAt
    ) {
        public static ActivityResponse from(Activity activity) {
            return new ActivityResponse(
                    activity.getId(),
                    activity.getLead().getId(),
                    activity.getType(),
                    activity.getDescription(),
                    activity.getAuthor().getId(),
                    activity.getAuthor().getName(),
                    activity.getCreatedAt());
        }
    }

    public record FollowUpRequest(
            @NotBlank @Size(max = 2000) String description,
            @NotNull Instant dueAt
    ) {
    }

    public record FollowUpResponse(
            Long id,
            Long leadId,
            String leadName,
            String description,
            Instant dueAt,
            boolean completed,
            Instant completedAt,
            Long createdById,
            String createdByName,
            Instant createdAt,
            FollowUpView state
    ) {
        public static FollowUpResponse from(FollowUp followUp) {
            FollowUpView state = FollowUpView.PENDING;
            if (followUp.isCompleted()) {
                state = FollowUpView.COMPLETED;
            } else if (followUp.getDueAt().isBefore(Instant.now())) {
                state = FollowUpView.OVERDUE;
            }
            return new FollowUpResponse(
                    followUp.getId(),
                    followUp.getLead().getId(),
                    followUp.getLead().getName(),
                    followUp.getDescription(),
                    followUp.getDueAt(),
                    followUp.isCompleted(),
                    followUp.getCompletedAt(),
                    followUp.getCreatedBy().getId(),
                    followUp.getCreatedBy().getName(),
                    followUp.getCreatedAt(),
                    state);
        }
    }

    public record HistoryResponse(
            Long id,
            HistoryEventType eventType,
            String detail,
            Long actorId,
            String actorName,
            Instant createdAt
    ) {
        public static HistoryResponse from(LeadHistory history) {
            return new HistoryResponse(
                    history.getId(),
                    history.getEventType(),
                    history.getDetail(),
                    history.getActor().getId(),
                    history.getActor().getName(),
                    history.getCreatedAt());
        }
    }

    public record DealWriteRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull LocalDate expectedCloseDate
    ) {
    }

    public record DealUpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull LocalDate expectedCloseDate
    ) {
    }

    public record StageChangeRequest(
            @NotNull DealStage stage,
            @Size(max = 500) String closingReason
    ) {
    }

    public record DealResponse(
            Long id,
            String title,
            Long leadId,
            String leadName,
            BigDecimal amount,
            DealStage stage,
            LocalDate expectedCloseDate,
            String closingReason,
            Long assignedAgentId,
            String assignedAgentName,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static DealResponse from(Deal deal) {
            Lead lead = deal.getLead();
            UserAccount agent = lead.getAssignedAgent();
            return new DealResponse(
                    deal.getId(),
                    deal.getTitle(),
                    lead.getId(),
                    lead.getName(),
                    deal.getAmount().setScale(2, RoundingMode.HALF_UP),
                    deal.getStage(),
                    deal.getExpectedCloseDate(),
                    deal.getClosingReason(),
                    agent.getId(),
                    agent.getName(),
                    deal.getCreatedAt(),
                    deal.getUpdatedAt());
        }
    }

    public record LeadDetailResponse(
            LeadResponse lead,
            List<ActivityResponse> activities,
            List<FollowUpResponse> followUps,
            DealResponse deal,
            List<HistoryResponse> history
    ) {
    }

    public record StatusCount(LeadStatus status, long count) {
    }

    public record DashboardResponse(
            long activeLeadCount,
            long qualifiedLeadCount,
            BigDecimal openDealValue,
            BigDecimal wonDealValue,
            long overdueFollowUpCount,
            List<StatusCount> leadsByStatus,
            List<FollowUpResponse> upcomingFollowUps
    ) {
    }
}
