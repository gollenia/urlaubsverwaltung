package org.synyx.urlaubsverwaltung.settings;

import de.focus_shift.launchpad.api.HasLaunchpad;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.synyx.urlaubsverwaltung.application.specialleave.SpecialLeaveSettingsItem;
import org.synyx.urlaubsverwaltung.application.specialleave.SpecialLeaveSettingsService;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationType;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeColor;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeService;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.synyx.urlaubsverwaltung.security.SecurityRules.IS_OFFICE;
import static org.synyx.urlaubsverwaltung.settings.AbsenceTypeSettingsDtoMapper.mapToAbsenceTypeItemSettingDto;
import static org.synyx.urlaubsverwaltung.settings.SpecialLeaveSettingsDtoMapper.mapToSpecialLeaveSettingsItems;

@Controller
@RequestMapping("/web/settings/absence-types")
public class SettingsAbsenceTypesViewController implements HasLaunchpad {

    private final SettingsService settingsService;
    private final VacationTypeService vacationTypeService;
    private final SpecialLeaveSettingsService specialLeaveSettingsService;

    @Autowired
    public SettingsAbsenceTypesViewController(SettingsService settingsService,
                                              VacationTypeService vacationTypeService,
                                              SpecialLeaveSettingsService specialLeaveService) {
        this.settingsService = settingsService;
        this.vacationTypeService = vacationTypeService;
        this.specialLeaveSettingsService = specialLeaveService;
    }

    @GetMapping
    @PreAuthorize(IS_OFFICE)
    public String settingsDetails(Model model, Locale locale) {

        final Settings settings = settingsService.getSettings();
        final SettingsAbsenceTypesDto dto = settingsToDto(settings, locale);

        model.addAttribute("settings", dto);

        return "settings/absence-types/settings_absence_types";
    }

    @PostMapping(params = "add-absence-type")
    @PreAuthorize(IS_OFFICE)
    public String addNewAbsenceType(@ModelAttribute("settings") SettingsAbsenceTypesDto settingsDto,
                                    @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                                    Model model) {

        final AbsenceTypeSettingsItemDto newAbsenceType = new AbsenceTypeSettingsItemDto();
        newAbsenceType.setActive(false);
        newAbsenceType.setLabel(null);
        newAbsenceType.setCategory(VacationCategory.OTHER);
        newAbsenceType.setLabels(List.of(
            new AbsenceTypeSettingsItemLabelDto(Locale.GERMAN, ""),
            new AbsenceTypeSettingsItemLabelDto(Locale.forLanguageTag("de-AT"), ""),
            new AbsenceTypeSettingsItemLabelDto(Locale.ENGLISH, ""),
            new AbsenceTypeSettingsItemLabelDto(Locale.forLanguageTag("el"), "")
        ));
        newAbsenceType.setRequiresApprovalToApply(false);
        newAbsenceType.setRequiresApprovalToCancel(false);
        newAbsenceType.setVisibleToEveryone(false);
        newAbsenceType.setColor(VacationTypeColor.YELLOW);

        final List<AbsenceTypeSettingsItemDto> absenceTypes = new ArrayList<>(settingsDto.getAbsenceTypeSettings().getItems());

        if ("frame-absence-type".equals(turboFrame)) {
            model.addAttribute("newAbsenceType", newAbsenceType);
            model.addAttribute("newAbsenceTypeIndex", absenceTypes.size());
            model.addAttribute("frameNewAbsenceTypeRequested", true);
            return "settings/absence-types/absence-types::#frame-absence-type";
        }

        absenceTypes.add(newAbsenceType);
        settingsDto.getAbsenceTypeSettings().setItems(absenceTypes);

        model.addAttribute("settings", settingsDto);

        return "settings/absence-types/settings_absence_types";
    }

    @PostMapping
    @PreAuthorize(IS_OFFICE)
    public String settingsSaved(@Valid @ModelAttribute("settings") SettingsAbsenceTypesDto settingsDto, Errors errors,
                                Model model, RedirectAttributes redirectAttributes) {

        if (errors.hasErrors()) {
            model.addAttribute("settings", settingsDto);
            model.addAttribute("errors", errors);
            return "settings/absence-types/settings_absence_types";
        }

        // TODO save new VacationTypes (dto items without an id)
        final List<VacationTypeUpdate> vacationTypeUpdates = settingsDto.getAbsenceTypeSettings().getItems()
            .stream()
            .map(SettingsAbsenceTypesViewController::absenceTypeDtoToVacationTypeUpdate)
            .toList();
        vacationTypeService.updateVacationTypes(vacationTypeUpdates);

        final SpecialLeaveSettingsDto specialLeaveSettingsDto = settingsDto.getSpecialLeaveSettings();
        final List<SpecialLeaveSettingsItem> specialLeaveSettingsItems = mapToSpecialLeaveSettingsItems(specialLeaveSettingsDto.getSpecialLeaveSettingsItems());
        specialLeaveSettingsService.saveAll(specialLeaveSettingsItems);

        redirectAttributes.addFlashAttribute("success", true);

        return "redirect:/web/settings/absence-types";
    }

    private SettingsAbsenceTypesDto settingsToDto(Settings settings, Locale locale) {
        final SettingsAbsenceTypesDto dto = new SettingsAbsenceTypesDto();
        dto.setId(settings.getId());
        dto.setAbsenceTypeSettings(absenceTypeItemSettingDto(locale));
        dto.setSpecialLeaveSettings(getSpecialLeaveSettingsDto());
        return dto;
    }

    private AbsenceTypeSettingsDto absenceTypeItemSettingDto(Locale locale) {
        final List<VacationType<?>> allVacationTypes = vacationTypeService.getAllVacationTypes();
        return mapToAbsenceTypeItemSettingDto(allVacationTypes, locale);
    }

    private SpecialLeaveSettingsDto getSpecialLeaveSettingsDto() {
        final List<SpecialLeaveSettingsItem> specialLeaveSettingsItems = specialLeaveSettingsService.getSpecialLeaveSettings();
        return SpecialLeaveSettingsDtoMapper.mapToSpecialLeaveSettingsDto(specialLeaveSettingsItems);
    }

    private static VacationTypeUpdate absenceTypeDtoToVacationTypeUpdate(AbsenceTypeSettingsItemDto absenceTypeSettingsItemDto) {
        // editing labels is only possible for CustomVacationTypes yet. Not for ProvidedVacationTypes.
        final List<AbsenceTypeSettingsItemLabelDto> labels = absenceTypeSettingsItemDto.getLabels();
        final Map<Locale, String> labelByLocale = labels == null ? null : labels.stream().collect(toMap(
            AbsenceTypeSettingsItemLabelDto::getLocale,
            AbsenceTypeSettingsItemLabelDto::getLabel
        ));
        return new VacationTypeUpdate(
            absenceTypeSettingsItemDto.getId(),
            absenceTypeSettingsItemDto.isActive(),
            absenceTypeSettingsItemDto.isRequiresApprovalToApply(),
            absenceTypeSettingsItemDto.isRequiresApprovalToCancel(),
            absenceTypeSettingsItemDto.getColor(),
            absenceTypeSettingsItemDto.isVisibleToEveryone(),
            labelByLocale
        );
    }
}
