package org.synyx.urlaubsverwaltung.application.vacationtype;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * Describes a type of vacation.
 *
 * @since 2.15.0
 */
@Entity(name = "vacation_type")
public class VacationTypeEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @GeneratedValue(strategy = SEQUENCE, generator = "vacation_type_generator")
    @SequenceGenerator(name = "vacation_type_generator", sequenceName = "vacation_type_id_seq")
    private Long id;

    @NotNull
    private boolean active;

    @NotNull
    @Enumerated(STRING)
    private VacationCategory category;

    @NotNull
    private boolean requiresApprovalToApply;

    @NotNull
    private boolean requiresApprovalToCancel;

    @NotNull
    @Enumerated(STRING)
    private VacationTypeColor color;

    @NotNull
    private boolean visibleToEveryone;


    /**
     * defines whether the vacationType is a provided one (custom=false) or the user has created it (custom=true)
     */
    private boolean custom;

    /**
     * messageKey can be {@code null} and is only defined for ProvidedVacationType.
     */
    private String messageKey;

    /**
     * labelByLocale can be {@code null} and is only defined for CustomVacationType.
     */
    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "vacation_type_label", joinColumns = @JoinColumn(name = "vacation_type_id"))
    @MapKeyColumn(name = "locale")
    @Column(name = "label")
    private Map<Locale, String> labelByLocale;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean enabled) {
        this.active = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VacationCategory getCategory() {
        return category;
    }

    public void setCategory(VacationCategory category) {
        this.category = category;
    }

    public boolean isOfCategory(VacationCategory category) {
        return getCategory().equals(category);
    }

    public boolean isRequiresApprovalToApply() {
        return requiresApprovalToApply;
    }

    public void setRequiresApprovalToApply(boolean requiresApprovalToApply) {
        this.requiresApprovalToApply = requiresApprovalToApply;
    }

    public boolean isRequiresApprovalToCancel() {
        return requiresApprovalToCancel;
    }

    public void setRequiresApprovalToCancel(boolean requiresApprovalToCancel) {
        this.requiresApprovalToCancel = requiresApprovalToCancel;
    }

    public void setColor(VacationTypeColor color) {
        this.color = color;
    }

    public VacationTypeColor getColor() {
        return this.color;
    }

    public boolean isVisibleToEveryone() {
        return visibleToEveryone;
    }

    public void setVisibleToEveryone(boolean visibleToEveryone) {
        this.visibleToEveryone = visibleToEveryone;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public Map<Locale, String> getLabelByLocale() {
        return labelByLocale;
    }

    public VacationTypeEntity setLabelByLocale(Map<Locale, String> labelByLocale) {
        this.labelByLocale = labelByLocale;
        return this;
    }

    @Override
    public String toString() {
        return "VacationTypeEntity{" +
            "id=" + id +
            ", active=" + active +
            ", category=" + category +
            ", requiresApprovalToApply=" + requiresApprovalToApply +
            ", requiresApprovalToCancel=" + requiresApprovalToCancel +
            ", color=" + color +
            ", visibleToEveryone=" + visibleToEveryone +
            ", custom=" + custom +
            ", messageKey=" + messageKey +
            ", labelByLocale=" + labelByLocale +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final VacationTypeEntity that = (VacationTypeEntity) o;
        return null != this.getId() && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
