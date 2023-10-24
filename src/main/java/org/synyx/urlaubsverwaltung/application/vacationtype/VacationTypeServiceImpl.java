package org.synyx.urlaubsverwaltung.application.vacationtype;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.HOLIDAY;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.OTHER;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.OVERTIME;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.SPECIALLEAVE;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.UNPAIDLEAVE;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeColor.YELLOW;

@Service
@Transactional
public class VacationTypeServiceImpl implements VacationTypeService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final VacationTypeRepository vacationTypeRepository;
    private final MessageSource messageSource;

    @Autowired
    VacationTypeServiceImpl(VacationTypeRepository vacationTypeRepository, MessageSource messageSource) {
        this.vacationTypeRepository = vacationTypeRepository;
        this.messageSource = messageSource;
    }

    @Override
    public Optional<VacationType<?>> getById(Long id) {
        return Optional.of(convert(vacationTypeRepository.getReferenceById(id), messageSource));
    }

    @Override
    public List<VacationType<?>> getAllVacationTypes() {
        return vacationTypeRepository.findAll(Sort.by("id")).stream()
            .map(vacationTypeEntity -> convert(vacationTypeEntity, messageSource))
            .collect(toList());
    }

    @Override
    public List<VacationType<?>> getActiveVacationTypes() {
        return vacationTypeRepository.findByActiveIsTrueOrderById().stream()
            .map(vacationTypeEntity -> convert(vacationTypeEntity, messageSource))
            .collect(toList());
    }

    @Override
    public List<VacationType<?>> getActiveVacationTypesWithoutCategory(VacationCategory vacationCategory) {
        return getActiveVacationTypes().stream()
            .filter(vacationType -> vacationType.getCategory() != vacationCategory)
            .collect(toList());
    }

    @Override
    public void updateVacationTypes(List<VacationTypeUpdate> vacationTypeUpdates) {

        final Map<Long, VacationTypeUpdate> byId = vacationTypeUpdates.stream()
            .collect(toMap(VacationTypeUpdate::getId, vacationTypeUpdate -> vacationTypeUpdate));

        final List<VacationTypeEntity> updatedEntities = vacationTypeRepository.findAllById(byId.keySet())
            .stream()
            .map(vacationTypeEntity -> convert(vacationTypeEntity, messageSource))
            .map(vacationType -> {
                final VacationTypeUpdate vacationTypeUpdate = byId.get(vacationType.getId());
                if (vacationType instanceof ProvidedVacationType providedVacationType) {
                    return Optional.of(updateProvidedVacationType(providedVacationType, vacationTypeUpdate));
                } else if (vacationType instanceof CustomVacationType customVacationType) {
                    return Optional.of(updateCustomVacationType(customVacationType, vacationTypeUpdate));
                } else {
                    LOG.error("cannot handle vacationTypeUpdate={} for unknown vacationType implementation.", vacationTypeUpdate);
                    return Optional.empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(VacationType.class::cast)
            .map(VacationTypeServiceImpl::convert)
            .collect(toList());

        vacationTypeRepository.saveAll(updatedEntities);
    }

    @Override
    public void createVacationTypes(Collection<VacationType<?>> vacationTypes) {

        final List<VacationTypeEntity> newEntities = vacationTypes.stream()
            .map(VacationTypeServiceImpl::convert)
            .filter(entity -> {
                final boolean isNew = entity.getId() == null;
                if (!isNew) {
                    LOG.info("skipping vacationType={} from list of newly created vacation types due to existing id.", entity);
                }
                return isNew;
            })
            .toList();

        vacationTypeRepository.saveAll(newEntities);
    }

    public static VacationTypeEntity convert(VacationType<?> vacationType) {
        if (vacationType instanceof ProvidedVacationType providedVacationType) {
            return convertProvidedVacationType(providedVacationType);
        } else if (vacationType instanceof CustomVacationType customVacationType) {
            return convertCustomVacationType(customVacationType);
        } else {
            throw new IllegalStateException("could not convert unknown vacationType.");
        }
    }

    public static VacationType<? extends VacationType<?>> convert(VacationTypeEntity vacationTypeEntity, MessageSource messageSource) {
        if (vacationTypeEntity.isCustom()) {
            return convertCustomVacationType(vacationTypeEntity);
        } else {
            return convertProvidedVacationType(vacationTypeEntity, messageSource);
        }
    }

    @EventListener(ApplicationStartedEvent.class)
    void insertDefaultVacationTypes() {
        final long count = vacationTypeRepository.count();
        if (count == 0) {

            final VacationTypeEntity holiday = createVacationTypeEntity(1000L, true, HOLIDAY, "application.data.vacationType.holiday", true, true, YELLOW, false);
            final VacationTypeEntity specialleave = createVacationTypeEntity(2000L, true, SPECIALLEAVE, "application.data.vacationType.specialleave", true, true, YELLOW, false);
            final VacationTypeEntity unpaidleave = createVacationTypeEntity(3000L, true, UNPAIDLEAVE, "application.data.vacationType.unpaidleave", true, true, YELLOW, false);
            final VacationTypeEntity overtime = createVacationTypeEntity(4000L, true, OVERTIME, "application.data.vacationType.overtime", true, true, YELLOW, false);
            final VacationTypeEntity parentalLeave = createVacationTypeEntity(5000L, false, OTHER, "application.data.vacationType.parentalLeave", true, true, YELLOW, false);
            final VacationTypeEntity maternityProtection = createVacationTypeEntity(5001L, false, OTHER, "application.data.vacationType.maternityProtection", true, true, YELLOW, false);
            final VacationTypeEntity sabbatical = createVacationTypeEntity(5002L, false, OTHER, "application.data.vacationType.sabbatical", true, true, YELLOW, false);
            final VacationTypeEntity paidLeave = createVacationTypeEntity(5003L, false, OTHER, "application.data.vacationType.paidLeave", true, true, YELLOW, false);
            final VacationTypeEntity cure = createVacationTypeEntity(5004L, false, OTHER, "application.data.vacationType.cure", true, true, YELLOW, false);
            final VacationTypeEntity education = createVacationTypeEntity(5005L, false, OTHER, "application.data.vacationType.education", true, true, YELLOW, false);
            final VacationTypeEntity homeOffice = createVacationTypeEntity(5006L, false, OTHER, "application.data.vacationType.homeOffice", true, true, YELLOW, false);
            final VacationTypeEntity outOfOffice = createVacationTypeEntity(5007L, false, OTHER, "application.data.vacationType.outOfOffice", true, true, YELLOW, false);
            final VacationTypeEntity training = createVacationTypeEntity(5008L, false, OTHER, "application.data.vacationType.training", true, true, YELLOW, false);
            final VacationTypeEntity employmentBan = createVacationTypeEntity(5009L, false, OTHER, "application.data.vacationType.employmentBan", true, true, YELLOW, false);
            final VacationTypeEntity educationalLeave = createVacationTypeEntity(5010L, false, OTHER, "application.data.vacationType.educationalLeave", true, true, YELLOW, false);

            final List<VacationTypeEntity> vacationTypes = List.of(holiday, holiday, specialleave, unpaidleave, overtime, parentalLeave, maternityProtection, sabbatical, paidLeave, cure, education, homeOffice, outOfOffice, training, employmentBan, educationalLeave);
            final List<VacationTypeEntity> savesVacationTypes = vacationTypeRepository.saveAll(vacationTypes);
            LOG.info("Saved initial vacation types {}", savesVacationTypes);
        }
    }

    private static VacationTypeEntity createVacationTypeEntity(Long id, boolean active, VacationCategory category, String messageKey, boolean requiresApprovalToApply, boolean requiresApprovalToCancel, VacationTypeColor color, boolean visibleToEveryone) {
        final VacationTypeEntity vacationTypeEntity = new VacationTypeEntity();
        vacationTypeEntity.setCustom(false);
        vacationTypeEntity.setId(id);
        vacationTypeEntity.setActive(active);
        vacationTypeEntity.setCategory(category);
        vacationTypeEntity.setMessageKey(messageKey);
        vacationTypeEntity.setRequiresApprovalToApply(requiresApprovalToApply);
        vacationTypeEntity.setRequiresApprovalToCancel(requiresApprovalToCancel);
        vacationTypeEntity.setColor(color);
        vacationTypeEntity.setVisibleToEveryone(visibleToEveryone);
        return vacationTypeEntity;
    }

    private static CustomVacationType updateCustomVacationType(CustomVacationType customVacationType, VacationTypeUpdate vacationTypeUpdate) {

        final Map<Locale, String> labelByLocale = vacationTypeUpdate.getLabelByLocale()
            .orElseThrow(() -> new IllegalStateException("expected locales to be defined. cannot update %s".formatted(customVacationType)));

        return CustomVacationType.builder(customVacationType)
            .active(vacationTypeUpdate.isActive())
            .requiresApprovalToApply(vacationTypeUpdate.isRequiresApprovalToApply())
            .requiresApprovalToCancel(vacationTypeUpdate.isRequiresApprovalToCancel())
            .color(vacationTypeUpdate.getColor())
            .visibleToEveryone(vacationTypeUpdate.isVisibleToEveryone())
            .labelByLocale(labelByLocale)
            .build();
    }

    private static ProvidedVacationType updateProvidedVacationType(ProvidedVacationType providedVacationType,
                                                                   VacationTypeUpdate vacationTypeUpdate) {
        // updating label of ProvidedVacationType is not yet implemented.
        // therefore no messageSource required. we can just use the given providedVacationType instance.
        // as soon as the label of a ProvidedVacationType can be updated, we get the new label from the VacationTypeUpdate.
        return ProvidedVacationType.builder(providedVacationType)
            .active(vacationTypeUpdate.isActive())
            .requiresApprovalToApply(vacationTypeUpdate.isRequiresApprovalToApply())
            .requiresApprovalToCancel(vacationTypeUpdate.isRequiresApprovalToCancel())
            .color(vacationTypeUpdate.getColor())
            .visibleToEveryone(vacationTypeUpdate.isVisibleToEveryone())
            .messageKey(providedVacationType.getMessageKey())
            .build();
    }

    private static CustomVacationType convertCustomVacationType(VacationTypeEntity customVacationTypeEntity) {
        return CustomVacationType.builder()
            .id(customVacationTypeEntity.getId())
            .active(customVacationTypeEntity.isActive())
            .category(customVacationTypeEntity.getCategory())
            .requiresApprovalToApply(customVacationTypeEntity.isRequiresApprovalToApply())
            .requiresApprovalToCancel(customVacationTypeEntity.isRequiresApprovalToCancel())
            .color(customVacationTypeEntity.getColor())
            .visibleToEveryone(customVacationTypeEntity.isVisibleToEveryone())
            .labelByLocale(customVacationTypeEntity.getLabelByLocale())
            .build();
    }

    private static ProvidedVacationType convertProvidedVacationType(VacationTypeEntity providedVacationType,
                                                                    MessageSource messageSource) {
        return ProvidedVacationType.builder(messageSource)
            .id(providedVacationType.getId())
            .active(providedVacationType.isActive())
            .category(providedVacationType.getCategory())
            .requiresApprovalToApply(providedVacationType.isRequiresApprovalToApply())
            .requiresApprovalToCancel(providedVacationType.isRequiresApprovalToCancel())
            .color(providedVacationType.getColor())
            .visibleToEveryone(providedVacationType.isVisibleToEveryone())
            .messageKey(providedVacationType.getMessageKey())
            .build();
    }

    private static VacationTypeEntity convertProvidedVacationType(ProvidedVacationType providedVacationType) {
        final VacationTypeEntity vacationTypeEntity = toEntityBase(providedVacationType);
        vacationTypeEntity.setCustom(false);
        vacationTypeEntity.setMessageKey(providedVacationType.getMessageKey());
        return vacationTypeEntity;
    }

    private static VacationTypeEntity convertCustomVacationType(CustomVacationType customVacationType) {
        final VacationTypeEntity vacationTypeEntity = toEntityBase(customVacationType);
        vacationTypeEntity.setCustom(true);
        vacationTypeEntity.setLabelByLocale(customVacationType.getLabelByLocale());
        return vacationTypeEntity;
    }

    private static VacationTypeEntity toEntityBase(VacationType<?> vacationType) {
        final VacationTypeEntity vacationTypeEntity = new VacationTypeEntity();
        vacationTypeEntity.setId(vacationType.getId());
        vacationTypeEntity.setActive(vacationType.isActive());
        vacationTypeEntity.setCategory(vacationType.getCategory());
        vacationTypeEntity.setRequiresApprovalToApply(vacationType.isRequiresApprovalToApply());
        vacationTypeEntity.setRequiresApprovalToCancel(vacationType.isRequiresApprovalToCancel());
        vacationTypeEntity.setColor(vacationType.getColor());
        vacationTypeEntity.setVisibleToEveryone(vacationType.isVisibleToEveryone());
        return vacationTypeEntity;
    }
}

