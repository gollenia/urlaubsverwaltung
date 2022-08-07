package org.synyx.urlaubsverwaltung.person.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.synyx.urlaubsverwaltung.SearchQuery;
import org.synyx.urlaubsverwaltung.SortComparator;
import org.synyx.urlaubsverwaltung.account.Account;
import org.synyx.urlaubsverwaltung.account.AccountService;
import org.synyx.urlaubsverwaltung.account.VacationDaysLeft;
import org.synyx.urlaubsverwaltung.account.VacationDaysService;
import org.synyx.urlaubsverwaltung.department.Department;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.department.web.UnknownDepartmentException;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.UnknownPersonException;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedata;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedataService;
import org.synyx.urlaubsverwaltung.settings.SettingsService;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTime;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.hasText;
import static org.synyx.urlaubsverwaltung.person.Role.BOSS;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.person.web.PersonDetailsBasedataDtoMapper.mapToPersonDetailsBasedataDto;
import static org.synyx.urlaubsverwaltung.person.web.PersonPermissionsMapper.mapRoleToPermissionsDto;
import static org.synyx.urlaubsverwaltung.security.SecurityRules.IS_PRIVILEGED_USER;

/**
 * Controller for management of {@link Person} entities.
 */
@Controller
@RequestMapping("/web")
public class PersonDetailsViewController {

    private final PersonService personService;
    private final AccountService accountService;
    private final VacationDaysService vacationDaysService;
    private final DepartmentService departmentService;
    private final WorkingTimeService workingTimeService;
    private final SettingsService settingsService;
    private final PersonBasedataService personBasedataService;
    private final Clock clock;

    @Autowired
    public PersonDetailsViewController(PersonService personService, AccountService accountService,
                                       VacationDaysService vacationDaysService, DepartmentService departmentService,
                                       WorkingTimeService workingTimeService, SettingsService settingsService,
                                       PersonBasedataService personBasedataService, Clock clock) {
        this.personService = personService;
        this.accountService = accountService;
        this.vacationDaysService = vacationDaysService;
        this.departmentService = departmentService;
        this.workingTimeService = workingTimeService;
        this.settingsService = settingsService;
        this.personBasedataService = personBasedataService;
        this.clock = clock;
    }

    @GetMapping("/person/{personId}")
    public String showPersonInformation(@PathVariable("personId") Integer personId,
                                        @RequestParam(value = "year", required = false) Optional<Integer> requestedYear,
                                        Model model) throws UnknownPersonException {

        final Person person = personService.getPersonByID(personId).orElseThrow(() -> new UnknownPersonException(personId));
        final Person signedInUser = personService.getSignedInUser();

        if (!departmentService.isSignedInUserAllowedToAccessPersonData(signedInUser, person)) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to access data of user '%s'", signedInUser.getId(), person.getId()));
        }

        final int currentYear = Year.now(clock).getValue();
        final int selectedYear = requestedYear.orElse(currentYear);
        model.addAttribute("currentYear", Year.now(clock).getValue());
        model.addAttribute("selectedYear", selectedYear);

        model.addAttribute("person", person);
        model.addAttribute("permissions", mapRoleToPermissionsDto(List.copyOf(person.getPermissions())));

        final Optional<PersonBasedata> basedataByPersonId = personBasedataService.getBasedataByPersonId(person.getId());
        if (basedataByPersonId.isPresent()) {
            final PersonDetailsBasedataDto personDetailsBasedataDto = mapToPersonDetailsBasedataDto(basedataByPersonId.get());
            model.addAttribute("personBasedata", personDetailsBasedataDto);
        }

        model.addAttribute("departments", departmentService.getAssignedDepartmentsOfMember(person));
        model.addAttribute("departmentHeadOfDepartments", departmentService.getManagedDepartmentsOfDepartmentHead(person));
        model.addAttribute("secondStageAuthorityOfDepartments", departmentService.getManagedDepartmentsOfSecondStageAuthority(person));

        final Optional<WorkingTime> maybeWorkingTime = workingTimeService.getWorkingTime(person, LocalDate.now(clock));
        model.addAttribute("workingTime", maybeWorkingTime.orElse(null));
        model.addAttribute("federalState", maybeWorkingTime.map(WorkingTime::getFederalState)
            .orElseGet(() -> settingsService.getSettings().getWorkingTimeSettings().getFederalState()));

        model.addAttribute("canEditBasedata", signedInUser.hasRole(OFFICE));
        model.addAttribute("canEditPermissions", signedInUser.hasRole(OFFICE));
        model.addAttribute("canEditDepartments", signedInUser.hasRole(OFFICE));
        model.addAttribute("canEditAccounts", signedInUser.hasRole(OFFICE));
        model.addAttribute("canEditWorkingtime", signedInUser.hasRole(OFFICE));

        final Optional<Account> maybeAccount = accountService.getHolidaysAccount(selectedYear, person);
        maybeAccount.ifPresent(account -> model.addAttribute("account", account));

        return "thymeleaf/person/person_detail";
    }

    @PreAuthorize(IS_PRIVILEGED_USER)
    @GetMapping(value = "/person")
    public String showPerson(@RequestParam(value = "active", required = false, defaultValue = "true") boolean active,
                             @RequestParam(value = "department", required = false) Optional<Integer> requestedDepartmentId,
                             @RequestParam(value = "year", required = false) Optional<Integer> requestedYear,
                             @RequestParam(value = "query", required = false, defaultValue = "") String query,
                             @SortDefault.SortDefaults({
                                 @SortDefault(sort = "person.firstName", direction = Sort.Direction.ASC)
                             })
                             Pageable pageable,
                             @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                             Model model) throws UnknownDepartmentException {

        final int currentYear = Year.now(clock).getValue();
        final Integer selectedYear = requestedYear.orElse(currentYear);

        final Person signedInUser = personService.getSignedInUser();

        Sort personSort = Sort.unsorted();
        Sort accountSort = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            final String propertyWithPrefix = order.getProperty();
            if (propertyWithPrefix.startsWith("person.")) {
                final String property = propertyWithPrefix.replace("person.", "");
                personSort = personSort.and(Sort.by(order.getDirection(), property));
            } else if (propertyWithPrefix.startsWith("account.")) {
                final String property = propertyWithPrefix.replace("account.", "");
                accountSort = accountSort.and(Sort.by(order.getDirection(), property));
            }
        }
        final Pageable personPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), personSort);

        final SearchQuery<Person> personSearchQuery = new SearchQuery<>(Person.class, personPageable, query);
        final Page<Person> personPage;

        if (requestedDepartmentId.isPresent()) {
            final Integer departmentId = requestedDepartmentId.get();
            final Department department = departmentService.getDepartmentById(departmentId)
                .orElseThrow(() -> new UnknownDepartmentException(departmentId));

            model.addAttribute("department", department);

            personPage = active
                ? departmentService.getManagedMembersOfPersonAndDepartment(signedInUser, departmentId, personSearchQuery)
                : departmentService.getManagedInactiveMembersOfPersonAndDepartment(signedInUser, departmentId, personSearchQuery);


        } else {
            personPage = active
                ? getRelevantActivePersons(signedInUser, personSearchQuery)
                : getRelevantInactivePersons(signedInUser, personSearchQuery);
        }

        preparePersonView(signedInUser, personPage, personSort, accountSort, selectedYear, model);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("active", active);
        model.addAttribute("query", query);
        model.addAttribute("turboFrameRequested", StringUtils.hasText(turboFrame));

        if (turboFrame == null || turboFrame.isBlank()) {
            return "thymeleaf/person/persons";
        } else {
            return "thymeleaf/person/persons::#" + turboFrame;
        }
    }

    private Page<Person> getRelevantActivePersons(Person signedInUser, SearchQuery<Person> personSearchQuery) {
        if (signedInUser.hasRole(BOSS) || signedInUser.hasRole(OFFICE)) {
            return personService.getActivePersons(personSearchQuery);
        } else {
            return departmentService.getManagedMembersOfPerson(signedInUser, personSearchQuery);
        }
    }

    private Page<Person> getRelevantInactivePersons(Person signedInUser, SearchQuery<Person> personSearchQuery) {
        if (signedInUser.hasRole(BOSS) || signedInUser.hasRole(OFFICE)) {
            return personService.getInactivePersons(personSearchQuery);
        }
        return departmentService.getManagedInactiveMembersOfPerson(signedInUser, personSearchQuery);
    }

    private List<Department> getRelevantDepartmentsSortedByName(Person signedInUser) {

        final Set<Department> relevantDepartments = new HashSet<>();

        if (signedInUser.hasRole(BOSS) || signedInUser.hasRole(OFFICE)) {
            departmentService.getAllDepartments().stream()
                .collect(toCollection(() -> relevantDepartments));
        }

        if (signedInUser.hasRole(DEPARTMENT_HEAD)) {
            departmentService.getManagedDepartmentsOfDepartmentHead(signedInUser).stream()
                .collect(toCollection(() -> relevantDepartments));
        }

        if (signedInUser.hasRole(SECOND_STAGE_AUTHORITY)) {
            departmentService.getManagedDepartmentsOfSecondStageAuthority(signedInUser).stream()
                .collect(toCollection(() -> relevantDepartments));
        }

        if (!signedInUser.isPrivileged()) {
            departmentService.getAssignedDepartmentsOfMember(signedInUser).stream()
                .collect(toCollection(() -> relevantDepartments));
        }

        return Stream.of(relevantDepartments).flatMap(Set::stream)
            .distinct()
            .sorted(comparing(Department::getName))
            .collect(toList());
    }

    private void preparePersonView(Person signedInUser, Page<Person> personPage, Sort originalPersonSort, Sort originalAccountSort, int year, Model model) {

        final LocalDate now = LocalDate.now(clock);

        final List<PersonDto> personDtos = new ArrayList<>(personPage.getContent().size());
        for (Person person : personPage) {
            final PersonDto.Builder personDtoBuilder = PersonDto.builder();

            final Optional<Account> account = accountService.getHolidaysAccount(year, person);
            if (account.isPresent()) {
                final Account holidaysAccount = account.get();
                final Optional<Account> accountNextYear = accountService.getHolidaysAccount(year + 1, person);
                final VacationDaysLeft vacationDaysLeft = vacationDaysService.getVacationDaysLeft(holidaysAccount, accountNextYear);

                final boolean beforeExpiryDate = now.isBefore(holidaysAccount.getExpiryDate());
                final double remainingVacationDays = beforeExpiryDate
                    ? vacationDaysLeft.getRemainingVacationDays().doubleValue()
                    : vacationDaysLeft.getRemainingVacationDaysNotExpiring().doubleValue();

                personDtoBuilder
                    .entitlementYear(holidaysAccount.getAnnualVacationDays().doubleValue())
                    .entitlementActual(holidaysAccount.getActualVacationDays().doubleValue())
                    .entitlementRemaining(holidaysAccount.getRemainingVacationDays().doubleValue())
                    .vacationDaysLeft(vacationDaysLeft.getVacationDays().doubleValue())
                    .vacationDaysLeftRemaining(remainingVacationDays);
            }

            final String lastName = person.getFirstName() == null && person.getLastName() == null
                ? person.getUsername()
                : person.getLastName();

            personDtoBuilder
                .id(person.getId())
                .gravatarUrl(person.getGravatarURL())
                .firstName(person.getFirstName())
                .niceName(person.getNiceName())
                .lastName(lastName);

            personBasedataService.getBasedataByPersonId(person.getId())
                .ifPresent(personBasedata -> personDtoBuilder.personnelNumber(personBasedata.getPersonnelNumber()));

            final PersonDto personDto = personDtoBuilder.build();

            personDtos.add(personDto);
        }

        final Comparator<PersonDto> accountComparator = new SortComparator<>(PersonDto.class, originalAccountSort);
        personDtos.sort(accountComparator.thenComparing(PersonDto::getNiceName));

        final boolean showPersonnelNumberColumn = personDtos.stream()
            .anyMatch(personDto -> hasText(personDto.getPersonnelNumber()));

        final PageImpl<PersonDto> personDtoPage = new PageImpl<>(personDtos, personPage.getPageable(), personPage.getTotalElements());
        model.addAttribute("personPage", personDtoPage);

        final PersonPageSortDto personPageSortDto = personPageSortDto(originalPersonSort, originalAccountSort);
        model.addAttribute("personPageSort", personPageSortDto);

        final List<Integer> pageNumbers = IntStream.rangeClosed(1, personDtoPage.getTotalPages()).boxed().collect(toList());
        model.addAttribute("personPageNumbers", pageNumbers);

        model.addAttribute("showPersonnelNumberColumn", showPersonnelNumberColumn);
        model.addAttribute("now", now);
        model.addAttribute("departments", getRelevantDepartmentsSortedByName(signedInUser));
        model.addAttribute("sortQuery", originalPersonSort.stream().map(order -> order.getProperty() + "," + order.getDirection()).collect(toList()).stream().reduce((s, s2) -> s + "&" + s2).orElse(""));
    }

    private static PersonPageSortDto personPageSortDto(Sort originalPersonSort, Sort originalAccountSort) {
        final Sort.Order orderFirstName = originalPersonSort.getOrderFor("firstName");
        final Sort.Order orderLastName = originalPersonSort.getOrderFor("lastName");
        final Sort.Order orderEntitlementYear = originalAccountSort.getOrderFor("entitlementYear");
        final Sort.Order orderEntitlementActual = originalAccountSort.getOrderFor("entitlementActual");
        final Sort.Order orderVacationDaysLeft = originalAccountSort.getOrderFor("vacationDaysLeft");
        final Sort.Order orderEntitlementRemaining = originalAccountSort.getOrderFor("entitlementRemaining");
        final Sort.Order orderVacationDaysLeftRemaining = originalAccountSort.getOrderFor("vacationDaysLeftRemaining");

        if (orderFirstName != null) {
            return PersonPageSortDto.firstName(orderFirstName.isAscending());
        } else if (orderLastName != null) {
            return PersonPageSortDto.lastName(orderLastName.isAscending());
        } else if (orderEntitlementYear != null) {
            return PersonPageSortDto.entitlementYear(orderEntitlementYear.isAscending());
        } else if (orderEntitlementActual != null) {
            return PersonPageSortDto.entitlementActual(orderEntitlementActual.isAscending());
        } else if (orderVacationDaysLeft != null) {
            return PersonPageSortDto.vacationDaysLeft(orderVacationDaysLeft.isAscending());
        } else if (orderEntitlementRemaining != null) {
            return PersonPageSortDto.entitlementRemaining(orderEntitlementRemaining.isAscending());
        } else if (orderVacationDaysLeftRemaining != null) {
            return PersonPageSortDto.vacationDaysLeftRemaining(orderVacationDaysLeftRemaining.isAscending());
        } else {
            return PersonPageSortDto.firstName(true);
        }
    }
}
