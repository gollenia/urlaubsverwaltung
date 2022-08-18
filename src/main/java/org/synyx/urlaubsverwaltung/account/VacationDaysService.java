package org.synyx.urlaubsverwaltung.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.synyx.urlaubsverwaltung.application.application.Application;
import org.synyx.urlaubsverwaltung.application.application.ApplicationService;
import org.synyx.urlaubsverwaltung.application.application.ApplicationStatus;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.settings.SettingsService;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysCountService;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendar;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeSettings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED_CANCELLATION_REQUESTED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.TEMPORARY_ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.WAITING;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.HOLIDAY;


/**
 * Provides calculation of used / left vacation days.
 */
@Service
public class VacationDaysService {

    private final WorkDaysCountService workDaysCountService;
    private final ApplicationService applicationService;
    private final SettingsService settingsService;
    private final Clock clock;

    @Autowired
    public VacationDaysService(WorkDaysCountService workDaysCountService, ApplicationService applicationService,
                               SettingsService settingsService, Clock clock) {
        this.workDaysCountService = workDaysCountService;
        this.applicationService = applicationService;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    /**
     * Calculates the total number of days that are left to be used for applying for leave.
     *
     * <p>NOTE: The calculation depends on the current date. If it's before the expiry date, the left remaining vacation days are
     * relevant for calculation and if it's after the expiry date, only the not expiring remaining vacation days are relevant for
     * calculation.</p>
     *
     * @param account {@link Account}
     * @return total number of left vacation days
     */
    BigDecimal calculateTotalLeftVacationDays(Account account) {
        final LocalDate today = LocalDate.now(clock);
        final LocalDate firstDayOfYear = Year.of(account.getYear()).atDay(1);
        final LocalDate lastDayOfYear = firstDayOfYear.with(lastDayOfYear());
        return calculateTotalLeftVacationDays(firstDayOfYear, lastDayOfYear, today, account);
    }

    private BigDecimal calculateTotalLeftVacationDays(LocalDate start, LocalDate end, LocalDate today, Account account) {
        final WorkingTimeSettings workingTimeSettings = settingsService.getSettings().getWorkingTimeSettings();
        return getVacationDaysLeft(start, end, account, workingTimeSettings)
            .getLeftVacationDays(today, account.doRemainigVacationDaysExpire(), account.getExpiryDate());
    }

    /**
     * This version of the method also considers the account for next year,
     * so that it can adjust for vacation days carried over from this year to the next and then used there
     * (reducing the amount available in this year accordingly)
     *
     * @param account  the account for the year to calculate the vacation days for
     * @param nextYear the account for following year, if available
     * @return information about the vacation days left for that year
     */
    public VacationDaysLeft getVacationDaysLeft(Account account, Optional<Account> nextYear) {
        final LocalDate firstDayOfYear = Year.of(account.getYear()).atDay(1);
        final LocalDate lastDayOfYear = firstDayOfYear.with(lastDayOfYear());
        final WorkingTimeSettings workingTimeSettings = settingsService.getSettings().getWorkingTimeSettings();
        return getVacationDaysLeft(firstDayOfYear, lastDayOfYear, account, nextYear, workingTimeSettings);
    }

    private VacationDaysLeft getVacationDaysLeft(LocalDate start, LocalDate end, Account account, WorkingTimeSettings workingTimeSettings) {
        return getVacationDaysLeft(start, end, account, Optional.empty(), workingTimeSettings);
    }

    public Map<Account, HolidayAccountVacationDays> getVacationDaysLeft(List<Account> holidayAccounts, LocalDate from, LocalDate to, Map<Person, WorkingTimeCalendar> workingTimeCalendarsByPerson) {
        if (from.isAfter(to) || to.getYear() != from.getYear()) {
            throw new IllegalStateException("'from' must be before 'after' and they both must have the same year.");
        }

        final List<Account> holidayAccountsForYear = holidayAccounts.stream().filter(account -> account.getYear() == from.getYear()).collect(toList());
        final Map<Account, UsedVacationDaysTuple> usedVacationDaysByAccount = getUsedVacationDays(holidayAccountsForYear, from, to, workingTimeCalendarsByPerson);

        return usedVacationDaysByAccount.entrySet().stream()
            .map(entry -> {
                final Account account = entry.getKey();
                final UsedVacationDaysTuple usedVacationDaysTuple = entry.getValue();

                final BigDecimal vacationDays = account.getActualVacationDays();
                final BigDecimal remainingVacationDays = account.getRemainingVacationDays();
                final BigDecimal remainingVacationDaysNotExpiring = account.getRemainingVacationDaysNotExpiring();

                final UsedVacationDaysYear usedVacationDaysYear = usedVacationDaysTuple.getUsedVacationDaysYear();
                final VacationDaysLeft vacationDaysLeftYear = VacationDaysLeft.builder()
                    .withAnnualVacation(vacationDays)
                    .withRemainingVacation(remainingVacationDays)
                    .notExpiring(remainingVacationDaysNotExpiring)
                    .forUsedVacationDaysBeforeExpiry(usedVacationDaysYear.getUsedVacationDaysBeforeExpiryDate())
                    .forUsedVacationDaysAfterExpiry(usedVacationDaysYear.getUsedVacationDaysAfterExpiryDate())
                    .withVacationDaysUsedNextYear(ZERO)
                    .build();

                final UsedVacationDaysDateRange usedVacationDaysDateRange = usedVacationDaysTuple.getUsedVacationDaysDateRange();
                final VacationDaysLeft vacationDaysLeftDateRange = VacationDaysLeft.builder()
                    .withAnnualVacation(vacationDays)
                    .withRemainingVacation(remainingVacationDays)
                    .notExpiring(remainingVacationDaysNotExpiring)
                    .forUsedVacationDaysBeforeExpiry(usedVacationDaysDateRange.getUsedVacationDaysBeforeExpiryDate())
                    .forUsedVacationDaysAfterExpiry(usedVacationDaysDateRange.getUsedVacationDaysAfterExpiryDate())
                    .withVacationDaysUsedNextYear(ZERO)
                    .build();

                return new HolidayAccountVacationDays(account, vacationDaysLeftYear, vacationDaysLeftDateRange);
            })
            .collect(toMap(HolidayAccountVacationDays::getAccount, identity()));
    }

    private Map<Account, UsedVacationDaysTuple> getUsedVacationDays(List<Account> holidayAccounts, LocalDate from, LocalDate to, Map<Person, WorkingTimeCalendar> workingTimeCalendarsByPerson) {

        final LocalDate firstDayOfYear = from.with(firstDayOfYear());
        final LocalDate lastDayOfYear = to.with(lastDayOfYear());

        final List<Person> persons = holidayAccounts.stream().map(Account::getPerson).distinct().collect(toList());
        final List<ApplicationStatus> status = List.of(WAITING, TEMPORARY_ALLOWED, ALLOWED, ALLOWED_CANCELLATION_REQUESTED);
        final List<Application> applicationsTouchingDateRange = applicationService.getForStatesAndPerson(status, persons, firstDayOfYear, lastDayOfYear);

        return getUsedVacationDaysBetweenTwoMilestones(holidayAccounts, applicationsTouchingDateRange, from, to, workingTimeCalendarsByPerson);
    }

    private Map<Account, UsedVacationDaysTuple> getUsedVacationDaysBetweenTwoMilestones(List<Account> holidayAccounts, List<Application> applications, LocalDate from, LocalDate to, Map<Person, WorkingTimeCalendar> workingTimeCalendarsByPerson) {

        final Map<Person, List<Application>> applicationsByPerson = applications.stream()
            .filter(application -> application.getVacationType().getCategory().equals(HOLIDAY))
            .collect(groupingBy(Application::getPerson));

        // check persons actual working time for the applicationForLeave.
        // the returned working time is the duration of usedVacationDays.
        // e.g. working time: MONDAY=FULL TUESDAY=FULL WEDNESDAY=ZERO THURSDAY=FULL FRIDAY=FULL
        //      application for a full week -> 4 used vacation days
        return holidayAccounts.stream().flatMap(holidayAccount -> {
            final Person person = holidayAccount.getPerson();

            if (applicationsByPerson.containsKey(person)) {
                final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendarsByPerson.get(person);
                return applicationsByPerson.get(person).stream()
                    .map(application -> usedVacationDays(holidayAccount, application, workingTimeCalendar))
                    .map(usedVacationDays -> Map.entry(holidayAccount, usedVacationDays));
            }

            // person has no applied applicationForLeaves -> zero used vacation days
            return Stream.of(Map.entry(holidayAccount, UsedVacationDaysTuple.identity()));
        }).collect(groupingBy(
            // group by account
            Entry::getKey,
            // and summarize used vacation days of the account's applications
            reducing(UsedVacationDaysTuple.identity(), Entry::getValue, Addable::add)
        ));
    }

    private UsedVacationDaysTuple usedVacationDays(Account holidayAccount, Application application, WorkingTimeCalendar workingTimeCalendar) {

        final LocalDate holidayAccountExpiryDate = holidayAccount.getExpiryDate();
        final LocalDate lastDayBeforeExpiryDate = holidayAccountExpiryDate.minusDays(1);

        // assumed that applicationForLeave does not start in previous year and ends in next year :x
        // which would be an applicationForLeave for over a year. TODO could this be a valid case?
        final LocalDate applicationStartOrFirstDayOfYear = max(application.getStartDate(), application.getEndDate().with(firstDayOfYear()));
        final LocalDate applicationEndOrLastDayOfYear = min(application.getEndDate(), application.getStartDate().with(lastDayOfYear()));

        // use vacation days scoped to from/to date range
        final BigDecimal dateRangeWorkDaysCountBeforeExpiryDate;
        final BigDecimal dateRangeWorkDaysCountAfterExpiryDate;
        if (applicationStartOrFirstDayOfYear.isBefore(holidayAccountExpiryDate)) {
            // TODO consider from/to. currently the full year is considered here.
            final LocalDate dateRangeStartAfterExpiryDate = max(applicationStartOrFirstDayOfYear, holidayAccountExpiryDate);
            final LocalDate dateRangeEndBeforeExpiryDate = min(applicationEndOrLastDayOfYear, lastDayBeforeExpiryDate);

            dateRangeWorkDaysCountBeforeExpiryDate = workingTimeCalendar.workingTime(applicationStartOrFirstDayOfYear, dateRangeEndBeforeExpiryDate);
            dateRangeWorkDaysCountAfterExpiryDate = workingTimeCalendar.workingTime(dateRangeStartAfterExpiryDate, applicationEndOrLastDayOfYear);
        } else {
            dateRangeWorkDaysCountBeforeExpiryDate = ZERO;
            dateRangeWorkDaysCountAfterExpiryDate = workingTimeCalendar.workingTime(applicationStartOrFirstDayOfYear, applicationEndOrLastDayOfYear);
        }

        final UsedVacationDaysDateRange dateRangeUsedVacationDays;
        if (application.getDayLength().isHalfDay()) {
            // halfDay application is only possible for one localDate.
            // so we can safely divide the calculated workDays by 2.
            dateRangeUsedVacationDays = new UsedVacationDaysDateRange(divideBy2(dateRangeWorkDaysCountBeforeExpiryDate), divideBy2(dateRangeWorkDaysCountAfterExpiryDate));
        } else {
            dateRangeUsedVacationDays = new UsedVacationDaysDateRange(dateRangeWorkDaysCountBeforeExpiryDate, dateRangeWorkDaysCountAfterExpiryDate);
        }

        // use vacation days considering full year
        final BigDecimal yearWorkDaysCountBeforeExpiry;
        final BigDecimal yearWorkDaysCountAfterExpiry;
        if (applicationStartOrFirstDayOfYear.isBefore(lastDayBeforeExpiryDate)) {
            final LocalDate yearEndBeforeExpiryDate = min(applicationEndOrLastDayOfYear, lastDayBeforeExpiryDate);
            final LocalDate yearStartAfterExpiryDate = max(applicationStartOrFirstDayOfYear, holidayAccountExpiryDate);
            yearWorkDaysCountBeforeExpiry = workingTimeCalendar.workingTime(applicationStartOrFirstDayOfYear, yearStartAfterExpiryDate);
            yearWorkDaysCountAfterExpiry = workingTimeCalendar.workingTime(yearEndBeforeExpiryDate, applicationEndOrLastDayOfYear);
        } else {
            yearWorkDaysCountBeforeExpiry = ZERO;
            yearWorkDaysCountAfterExpiry = workingTimeCalendar.workingTime(applicationStartOrFirstDayOfYear, applicationEndOrLastDayOfYear);
        }

        final UsedVacationDaysYear yearUsedVacationDays;
        if (application.getDayLength().isHalfDay()) {
            // halfDay application is only possible for one localDate.
            // so we can safely divide the calculated workDays by 2.
            yearUsedVacationDays = new UsedVacationDaysYear(divideBy2(yearWorkDaysCountBeforeExpiry), divideBy2(yearWorkDaysCountAfterExpiry));
        } else {
            yearUsedVacationDays = new UsedVacationDaysYear(yearWorkDaysCountBeforeExpiry, yearWorkDaysCountAfterExpiry);
        }

        return new UsedVacationDaysTuple(dateRangeUsedVacationDays, yearUsedVacationDays);
    }

    private BigDecimal divideBy2(BigDecimal value) {
        return value.divide(BigDecimal.valueOf(2), 2, RoundingMode.CEILING);
    }

    private static LocalDate max(LocalDate localDate, LocalDate localDate2) {
        return localDate.isBefore(localDate2) ? localDate2 : localDate;
    }

    private static LocalDate min(LocalDate localDate, LocalDate localDate2) {
        return localDate.isBefore(localDate2) ? localDate : localDate2;
    }

    private interface Addable<T> {
        T add(T toAdd);
    }

    private static class UsedVacationDaysTuple implements Addable<UsedVacationDaysTuple> {
        private final UsedVacationDaysDateRange usedVacationDaysDateRange;
        private final UsedVacationDaysYear usedVacationDaysYear;

        UsedVacationDaysTuple(UsedVacationDaysDateRange usedVacationDaysDateRange, UsedVacationDaysYear usedVacationDaysYear) {
            this.usedVacationDaysDateRange = usedVacationDaysDateRange;
            this.usedVacationDaysYear = usedVacationDaysYear;
        }

        UsedVacationDaysDateRange getUsedVacationDaysDateRange() {
            return usedVacationDaysDateRange;
        }

        UsedVacationDaysYear getUsedVacationDaysYear() {
            return usedVacationDaysYear;
        }

        @Override
        public UsedVacationDaysTuple add(UsedVacationDaysTuple toAdd) {
            return new UsedVacationDaysTuple(
                usedVacationDaysDateRange.add(toAdd.usedVacationDaysDateRange),
                usedVacationDaysYear.add(toAdd.usedVacationDaysYear)
            );
        }

        static UsedVacationDaysTuple identity() {
            return new UsedVacationDaysTuple(new UsedVacationDaysDateRange(ZERO, ZERO), new UsedVacationDaysYear(ZERO, ZERO));
        }
    }

    private static class UsedVacationDaysYear implements Addable<UsedVacationDaysYear> {

        private final BigDecimal usedVacationDaysBeforeExpiryDate;
        private final BigDecimal usedVacationDaysAfterExpiryDate;

        UsedVacationDaysYear(BigDecimal usedVacationDaysBeforeExpiryDate, BigDecimal usedVacationDaysAfterExpiryDate) {
            this.usedVacationDaysBeforeExpiryDate = usedVacationDaysBeforeExpiryDate;
            this.usedVacationDaysAfterExpiryDate = usedVacationDaysAfterExpiryDate;
        }

        BigDecimal getUsedVacationDaysBeforeExpiryDate() {
            return usedVacationDaysBeforeExpiryDate;
        }

        BigDecimal getUsedVacationDaysAfterExpiryDate() {
            return usedVacationDaysAfterExpiryDate;
        }

        @Override
        public UsedVacationDaysYear add(UsedVacationDaysYear toAdd) {
            return new UsedVacationDaysYear(
                usedVacationDaysBeforeExpiryDate.add(toAdd.usedVacationDaysBeforeExpiryDate),
                usedVacationDaysAfterExpiryDate.add(toAdd.usedVacationDaysAfterExpiryDate)
            );
        }
    }

    private static class UsedVacationDaysDateRange implements Addable<UsedVacationDaysDateRange> {

        private final BigDecimal usedVacationDaysBeforeExpiryDate;
        private final BigDecimal usedVacationDaysAfterExpiryDate;

        UsedVacationDaysDateRange(BigDecimal usedVacationDaysBeforeExpiryDate, BigDecimal usedVacationDaysAfterExpiryDate) {
            this.usedVacationDaysBeforeExpiryDate = usedVacationDaysBeforeExpiryDate;
            this.usedVacationDaysAfterExpiryDate = usedVacationDaysAfterExpiryDate;
        }

        BigDecimal getUsedVacationDaysBeforeExpiryDate() {
            return usedVacationDaysBeforeExpiryDate;
        }

        BigDecimal getUsedVacationDaysAfterExpiryDate() {
            return usedVacationDaysAfterExpiryDate;
        }

        @Override
        public UsedVacationDaysDateRange add(UsedVacationDaysDateRange toAdd) {
            return new UsedVacationDaysDateRange(
                usedVacationDaysBeforeExpiryDate.add(toAdd.usedVacationDaysBeforeExpiryDate),
                usedVacationDaysAfterExpiryDate.add(toAdd.usedVacationDaysAfterExpiryDate)
            );
        }
    }

    private VacationDaysLeft getVacationDaysLeft(LocalDate start, LocalDate end, Account account, Optional<Account> nextYear, WorkingTimeSettings workingTimeSettings) {

        final BigDecimal vacationDays = account.getActualVacationDays();
        final BigDecimal remainingVacationDays = account.getRemainingVacationDays();
        final BigDecimal remainingVacationDaysNotExpiring = account.getRemainingVacationDaysNotExpiring();

        final BigDecimal usedVacationDaysBeforeExpiryDate;
        final BigDecimal usedVacationDaysAfterExpiryDate;

        if (account.doRemainigVacationDaysExpire()) {
            final LocalDate lastDayBeforeExpiryDate = account.getExpiryDate().minusDays(1);
            final LocalDate endBeforeExpiryDate = end.isAfter(lastDayBeforeExpiryDate) ? lastDayBeforeExpiryDate : end;

            final LocalDate expiryDate = account.getExpiryDate();
            final LocalDate startAfterExpiryDate = start.isBefore(expiryDate) ? expiryDate : start;

            usedVacationDaysBeforeExpiryDate = getUsedVacationDaysBetweenTwoMilestones(account.getPerson(), start, endBeforeExpiryDate, workingTimeSettings);
            usedVacationDaysAfterExpiryDate = getUsedVacationDaysBetweenTwoMilestones(account.getPerson(), startAfterExpiryDate, end, workingTimeSettings);
        } else {
            usedVacationDaysBeforeExpiryDate = getUsedVacationDaysBetweenTwoMilestones(account.getPerson(), start, end, workingTimeSettings);
            usedVacationDaysAfterExpiryDate = ZERO;
        }

        final BigDecimal usedVacationDaysNextYear = nextYear
            .map(nextYearAccount -> getUsedRemainingVacationDays(nextYearAccount, workingTimeSettings))
            .orElse(ZERO);

        return VacationDaysLeft.builder()
            .withAnnualVacation(vacationDays)
            .withRemainingVacation(remainingVacationDays)
            .notExpiring(remainingVacationDaysNotExpiring)
            .forUsedVacationDaysBeforeExpiry(usedVacationDaysBeforeExpiryDate)
            .forUsedVacationDaysAfterExpiry(usedVacationDaysAfterExpiryDate)
            .withVacationDaysUsedNextYear(usedVacationDaysNextYear)
            .build();
    }

    public BigDecimal getUsedRemainingVacationDays(Account account) {
        final WorkingTimeSettings workingTimeSettings = settingsService.getSettings().getWorkingTimeSettings();
        return getUsedRemainingVacationDays(account, workingTimeSettings);
    }

    private BigDecimal getUsedRemainingVacationDays(Account account, WorkingTimeSettings workingTimeSettings) {
        final LocalDate firstDayOfYear = Year.of(account.getYear()).atDay(1);
        final LocalDate lastDayOfYear = firstDayOfYear.with(lastDayOfYear());
        return getUsedRemainingVacationDays(firstDayOfYear, lastDayOfYear, account, workingTimeSettings);
    }

    private BigDecimal getUsedRemainingVacationDays(LocalDate start, LocalDate end, Account account, WorkingTimeSettings workingTimeSettings) {

        if (start.isAfter(end)) {
            return ZERO;
        }

        if (account.getRemainingVacationDays().signum() > 0) {

            final VacationDaysLeft left = getVacationDaysLeft(start, end, account, workingTimeSettings);

            final BigDecimal totalUsed = account.getActualVacationDays()
                .add(account.getRemainingVacationDays())
                .subtract(left.getVacationDays())
                .subtract(left.getRemainingVacationDays());

            final BigDecimal remainingUsed = totalUsed.subtract(account.getActualVacationDays());

            if (remainingUsed.signum() > 0) {
                return remainingUsed;
            }
        }

        return ZERO;
    }

    BigDecimal getUsedVacationDaysBetweenTwoMilestones(Person person, LocalDate firstMilestone, LocalDate lastMilestone, WorkingTimeSettings workingTimeSettings) {

        if (firstMilestone.isAfter(lastMilestone)) {
            return ZERO;
        }

        final List<ApplicationStatus> statuses = List.of(WAITING, TEMPORARY_ALLOWED, ALLOWED, ALLOWED_CANCELLATION_REQUESTED);
        return applicationService.getApplicationsForACertainPeriodAndPersonAndVacationCategory(firstMilestone, lastMilestone, person, statuses, HOLIDAY).stream()
            .map(application -> getUsedVacationDays(application, person, firstMilestone, lastMilestone, workingTimeSettings))
            .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal getUsedVacationDays(Application application, Person person, LocalDate firstMilestone, LocalDate lastMilestone, WorkingTimeSettings workingTimeSettings) {
        final LocalDate startDate = application.getStartDate().isBefore(firstMilestone) ? firstMilestone : application.getStartDate();
        final LocalDate endDate = application.getEndDate().isAfter(lastMilestone) ? lastMilestone : application.getEndDate();
        return workDaysCountService.getWorkDaysCount(application.getDayLength(), startDate, endDate, person, workingTimeSettings);
    }
}
