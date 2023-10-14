package org.synyx.urlaubsverwaltung.application.vacationtype;

import java.util.Objects;

public final class ProvidedVacationType implements VacationType {

    private final Long id;
    private final boolean active;
    private final VacationCategory category;
    private final String messageKey;
    private final boolean requiresApprovalToApply;
    private final boolean requiresApprovalToCancel;
    private final VacationTypeColor color;
    private final boolean visibleToEveryone;

    private ProvidedVacationType(Builder builder) {
        this.id = builder.getId();
        this.active = builder.isActive();
        this.category = builder.getCategory();
        this.messageKey = builder.getMessageKey();
        this.requiresApprovalToApply = builder.isRequiresApprovalToApply();
        this.requiresApprovalToCancel = builder.isRequiresApprovalToCancel();
        this.color = builder.getColor();
        this.visibleToEveryone = builder.isVisibleToEveryone();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public VacationCategory getCategory() {
        return category;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public boolean isRequiresApprovalToApply() {
        return requiresApprovalToApply;
    }

    @Override
    public boolean isRequiresApprovalToCancel() {
        return requiresApprovalToCancel;
    }

    @Override
    public VacationTypeColor getColor() {
        return this.color;
    }

    @Override
    public boolean isVisibleToEveryone() {
        return visibleToEveryone;
    }

    @Override
    public String toString() {
        return "ProvidedVacationType{" +
            "id=" + id +
            ", active=" + active +
            ", category=" + category +
            ", messageKey='" + messageKey + '\'' +
            ", requiresApprovalToApply='" + requiresApprovalToApply + '\'' +
            ", requiresApprovalToCancel='" + requiresApprovalToCancel + '\'' +
            ", color='" + color + '\'' +
            ", visibleToEveryone=" + visibleToEveryone +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProvidedVacationType that = (ProvidedVacationType) o;
        return active == that.active
            && requiresApprovalToApply == that.requiresApprovalToApply
            && requiresApprovalToCancel == that.requiresApprovalToCancel
            && visibleToEveryone == that.visibleToEveryone
            && category == that.category
            && Objects.equals(messageKey, that.messageKey)
            && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, category, messageKey, requiresApprovalToApply, requiresApprovalToCancel, color, visibleToEveryone);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ProvidedVacationType vacationType) {
        return new Builder()
            .id(vacationType.id)
            .active(vacationType.active)
            .category(vacationType.category)
            .messageKey(vacationType.messageKey)
            .requiresApprovalToApply(vacationType.requiresApprovalToApply)
            .requiresApprovalToCancel(vacationType.requiresApprovalToCancel)
            .color(vacationType.color)
            .visibleToEveryone(vacationType.visibleToEveryone);
    }

    public static final class Builder {

        private Long id;
        private boolean active;
        private VacationCategory category;
        private String messageKey;
        private boolean requiresApprovalToApply;
        private boolean requiresApprovalToCancel;
        private VacationTypeColor color;
        private boolean visibleToEveryone;

        private Long getId() {
            return id;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        private boolean isActive() {
            return active;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        private VacationCategory getCategory() {
            return category;
        }

        public Builder category(VacationCategory category) {
            this.category = category;
            return this;
        }

        private String getMessageKey() {
            return messageKey;
        }

        public Builder messageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }

        private boolean isRequiresApprovalToApply() {
            return requiresApprovalToApply;
        }

        public Builder requiresApprovalToApply(boolean requiresApprovalToApply) {
            this.requiresApprovalToApply = requiresApprovalToApply;
            return this;
        }

        private boolean isRequiresApprovalToCancel() {
            return requiresApprovalToCancel;
        }

        public Builder requiresApprovalToCancel(boolean requiresApprovalToCancel) {
            this.requiresApprovalToCancel = requiresApprovalToCancel;
            return this;
        }

        private VacationTypeColor getColor() {
            return color;
        }

        public Builder color(VacationTypeColor color) {
            this.color = color;
            return this;
        }

        private boolean isVisibleToEveryone() {
            return visibleToEveryone;
        }

        public Builder visibleToEveryone(boolean visibleToEveryone) {
            this.visibleToEveryone = visibleToEveryone;
            return this;
        }

        public ProvidedVacationType build() {
            return new ProvidedVacationType(this);
        }
    }
}
