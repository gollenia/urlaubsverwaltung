package org.synyx.urlaubsverwaltung.overtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.synyx.urlaubsverwaltung.application.application.Application;
import org.synyx.urlaubsverwaltung.application.application.ApplicationService;
import org.synyx.urlaubsverwaltung.application.application.ApplicationStatus;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationType;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonDeletedEvent;
import org.synyx.urlaubsverwaltung.person.Role;
import org.synyx.urlaubsverwaltung.settings.Settings;
import org.synyx.urlaubsverwaltung.settings.SettingsService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.Month.AUGUST;
import static java.time.Month.JANUARY;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.USER;

@ExtendWith(MockitoExtension.class)
class OvertimeServiceImplTest {

    private OvertimeServiceImpl sut;

    @Mock
    private OvertimeRepository overtimeRepository;
    @Mock
    private OvertimeCommentRepository overtimeCommentRepository;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private OvertimeMailService overtimeMailService;
    @Mock
    private SettingsService settingsService;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new OvertimeServiceImpl(overtimeRepository, overtimeCommentRepository, applicationService, overtimeMailService, settingsService, clock);
    }

    // Record overtime -------------------------------------------------------------------------------------------------
    @Test
    void ensurePersistsOvertimeAndComment() {

        final Overtime overtime = new Overtime();
        final Person author = new Person();

        sut.record(overtime, Optional.of("Foo Bar"), author);

        verify(overtimeRepository).save(overtime);
        verify(overtimeCommentRepository).save(any(OvertimeComment.class));
    }

    @Test
    void ensureRecordingUpdatesLastModificationDate() {

        final Person author = new Person();
        final Overtime overtime = new Overtime();
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        final Overtime savedOvertime = sut.record(overtime, Optional.empty(), author);
        assertThat(savedOvertime.getLastModificationDate()).isEqualTo(LocalDate.now(clock));
    }

    @Test
    void ensureRecordingOvertimeSendsNotification() {

        final Person author = new Person();

        final Overtime overtime = new Overtime();
        overtime.setPerson(author);
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        final OvertimeComment overtimeComment = new OvertimeComment();
        when(overtimeCommentRepository.save(any())).thenReturn(overtimeComment);

        sut.record(overtime, Optional.of("Foo Bar"), author);

        verify(overtimeMailService, never()).sendOvertimeNotificationToApplicantFromManagement(overtime, overtimeComment, author);
        verify(overtimeMailService).sendOvertimeNotificationToApplicantFromApplicant(overtime, overtimeComment);
        verify(overtimeMailService).sendOvertimeNotificationToManagement(overtime, overtimeComment);
    }

    @Test
    void ensureRecordingOvertimeSendsNotificationFromManagement() {

        final Person author = new Person();
        author.setId(1L);

        final Person person = new Person();
        person.setId(2L);

        final Overtime overtime = new Overtime();
        overtime.setPerson(person);
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        final OvertimeComment overtimeComment = new OvertimeComment();
        when(overtimeCommentRepository.save(any())).thenReturn(overtimeComment);

        sut.record(overtime, Optional.of("Foo Bar"), author);

        verify(overtimeMailService, never()).sendOvertimeNotificationToApplicantFromApplicant(overtime, overtimeComment);
        verify(overtimeMailService).sendOvertimeNotificationToApplicantFromManagement(overtime, overtimeComment, author);
        verify(overtimeMailService).sendOvertimeNotificationToManagement(overtime, overtimeComment);
    }

    @Test
    void ensureCreatesCommentWithCorrectActionForNewOvertime() {

        final Overtime overtime = new Overtime();
        final Person author = new Person();

        sut.record(overtime, Optional.empty(), author);

        final ArgumentCaptor<OvertimeComment> commentCaptor = ArgumentCaptor.forClass(OvertimeComment.class);
        verify(overtimeCommentRepository).save(commentCaptor.capture());

        final OvertimeComment comment = commentCaptor.getValue();
        assertThat(comment).isNotNull();
        assertThat(comment.getAction()).isEqualTo(OvertimeCommentAction.CREATED);
    }

    @Test
    void ensureCreatesCommentWithCorrectActionForExistentOvertime() {

        final Overtime overtime = new Overtime();
        overtime.setId(1L);
        final Person author = new Person();

        sut.record(overtime, Optional.empty(), author);

        final ArgumentCaptor<OvertimeComment> commentCaptor = ArgumentCaptor.forClass(OvertimeComment.class);
        verify(overtimeCommentRepository).save(commentCaptor.capture());
        final OvertimeComment comment = commentCaptor.getValue();
        assertThat(comment).isNotNull();
        assertThat(comment.getAction()).isEqualTo(OvertimeCommentAction.EDITED);
    }

    @Test
    void ensureCreatedCommentWithoutTextHasCorrectProperties() {

        final Person author = new Person();

        final Overtime overtime = new Overtime();
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        sut.record(overtime, Optional.empty(), author);

        final ArgumentCaptor<OvertimeComment> commentCaptor = ArgumentCaptor.forClass(OvertimeComment.class);
        verify(overtimeCommentRepository).save(commentCaptor.capture());
        final OvertimeComment comment = commentCaptor.getValue();
        assertThat(comment).isNotNull();
        assertThat(comment.getPerson()).isEqualTo(author);
        assertThat(comment.getOvertime()).isEqualTo(overtime);
        assertThat(comment.getText()).isNull();
    }

    @Test
    void ensureCreatedCommentWithTextHasCorrectProperties() {

        final Person author = new Person();

        final Overtime overtime = new Overtime();
        when(overtimeRepository.save(overtime)).thenReturn(overtime);

        sut.record(overtime, Optional.of("Foo"), author);

        final ArgumentCaptor<OvertimeComment> commentCaptor = ArgumentCaptor.forClass(OvertimeComment.class);
        verify(overtimeCommentRepository).save(commentCaptor.capture());
        final OvertimeComment comment = commentCaptor.getValue();
        assertThat(comment).isNotNull();
        assertThat(comment.getPerson()).isEqualTo(author);
        assertThat(comment.getOvertime()).isEqualTo(overtime);
        assertThat(comment.getText()).isEqualTo("Foo");
    }

    // Get overtime record by ID ---------------------------------------------------------------------------------------
    @Test
    void ensureGetByIDCallsCorrectDAOMethod() {

        sut.getOvertimeById(42L);

        verify(overtimeRepository).findById(42L);
    }

    @Test
    void ensureReturnsEmptyOptionalIfNoOvertimeFoundForID() {

        when(overtimeRepository.findById(anyLong())).thenReturn(Optional.empty());

        final Optional<Overtime> maybeOvertime = sut.getOvertimeById(42L);
        assertThat(maybeOvertime).isEmpty();
    }

    // Get overtime comments -------------------------------------------------------------------------------------------
    @Test
    void ensureGetCommentsCorrectDAOMethod() {

        final Overtime overtime = new Overtime();
        sut.getCommentsForOvertime(overtime);

        verify(overtimeCommentRepository).findByOvertime(overtime);
    }

    // Get total overtime for year -------------------------------------------------------------------------------------
    @Test
    void ensureReturnsZeroIfPersonHasNoOvertimeRecordsYetForTheGivenYear() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        final LocalDate firstDayOfYear = LocalDate.of(2016, 1, 1);
        final LocalDate lastDayOfYear = LocalDate.of(2016, 12, 31);
        when(overtimeRepository.findByPersonAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(person, firstDayOfYear, lastDayOfYear)).thenReturn(List.of());

        final Duration totalHours = sut.getTotalOvertimeForPersonAndYear(person, 2016);
        assertThat(totalHours).isEqualTo(Duration.ZERO);
    }

    @Test
    void ensureReturnsCorrectYearOvertimeForPerson() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        final Overtime overtimeRecord = new Overtime(person, LocalDate.of(2016, 1, 5), LocalDate.of(2016, 1, 5), Duration.ofHours(1));
        final Overtime otherOvertimeRecord = new Overtime(person, LocalDate.of(2016, 2, 5), LocalDate.of(2016, 2, 5), Duration.ofHours(10));

        final LocalDate firstDayOfYear = LocalDate.of(2016, 1, 1);
        final LocalDate lastDayOfYear = LocalDate.of(2016, 12, 31);
        when(overtimeRepository.findByPersonAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(person, firstDayOfYear, lastDayOfYear)).thenReturn(List.of(overtimeRecord, otherOvertimeRecord));

        final Duration totalHours = sut.getTotalOvertimeForPersonAndYear(person, 2016);
        assertThat(totalHours).isEqualTo(Duration.ofHours(11));
    }

    @Test
    void ensureGetTotalOvertimeForPersonBeforeYear() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        final Overtime overtime = new Overtime(person, LocalDate.of(2016, 1, 5), LocalDate.of(2016, 1, 5), Duration.ofHours(10));
        final Overtime overtime2 = new Overtime(person, LocalDate.of(2016, 2, 5), LocalDate.of(2016, 2, 5), Duration.ofHours(4));

        final LocalDate firstDayOfYear = LocalDate.of(2017, 1, 1);
        final LocalDate lastDayOfBeforeYear = firstDayOfYear.minusYears(1).with(lastDayOfYear());
        when(overtimeRepository.findByPersonAndStartDateIsBefore(person, firstDayOfYear)).thenReturn(List.of(overtime, overtime2));
        when(applicationService.getTotalOvertimeReductionOfPersonUntil(person, lastDayOfBeforeYear)).thenReturn(Duration.ofHours(1));

        final Duration totalHours = sut.getTotalOvertimeForPersonBeforeYear(person, 2017);
        assertThat(totalHours).isEqualTo(Duration.ofHours(13));
    }

    // Get left overtime -----------------------------------------------------------------------------------------------
    @Test
    void ensureReturnsZeroAsLeftOvertimeIfPersonHasNoOvertimeRecordsYet() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        when(overtimeRepository.calculateTotalHoursForPerson(person)).thenReturn(Optional.empty());
        when(applicationService.getTotalOvertimeReductionOfPerson(person)).thenReturn(Duration.ZERO);

        final Duration totalHours = sut.getLeftOvertimeForPerson(person);
        assertThat(totalHours).isEqualTo(Duration.ZERO);

        verify(overtimeRepository).calculateTotalHoursForPerson(person);
        verify(applicationService).getTotalOvertimeReductionOfPerson(person);
    }

    @Test
    void ensureTheLeftOvertimeIsTheDifferenceBetweenTotalOvertimeAndOvertimeReduction() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        when(overtimeRepository.calculateTotalHoursForPerson(person)).thenReturn(Optional.of((double) Duration.ofHours(10L).toMinutes() / 60));
        when(applicationService.getTotalOvertimeReductionOfPerson(person)).thenReturn(Duration.ofHours(1));

        final Duration leftOvertime = sut.getLeftOvertimeForPerson(person);
        assertThat(leftOvertime).isEqualTo(Duration.ofHours(9));

        verify(overtimeRepository).calculateTotalHoursForPerson(person);
        verify(applicationService).getTotalOvertimeReductionOfPerson(person);
    }

    @Test
    void ensureTheLeftOvertimeIsZeroIfPersonHasNeitherOvertimeRecordsNorOvertimeReduction() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        when(overtimeRepository.calculateTotalHoursForPerson(person)).thenReturn(Optional.empty());
        when(applicationService.getTotalOvertimeReductionOfPerson(person)).thenReturn(Duration.ZERO);

        final Duration leftOvertime = sut.getLeftOvertimeForPerson(person);
        assertThat(leftOvertime).isEqualTo(Duration.ZERO);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureOfficeIsAllowedToWriteOthersOvertime(boolean overtimeWritePrivilegedOnly) {

        final Person signedInUser = new Person();
        signedInUser.setPermissions(List.of(OFFICE));
        final Person personOfOvertime = new Person();
        when(settingsService.getSettings()).thenReturn(overtimeSettings(overtimeWritePrivilegedOnly));

        assertThat(sut.isUserIsAllowedToWriteOvertime(signedInUser, personOfOvertime)).isTrue();
    }

    @Test
    void ensureUserIsNotAllowedToWriteOwnOvertimeWithPrivilegedRestriction() {

        final Person person = new Person();
        person.setPermissions(List.of(USER));

        when(settingsService.getSettings()).thenReturn(overtimeSettings(true));

        assertThat(sut.isUserIsAllowedToWriteOvertime(person, person)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OFFICE", "DEPARTMENT_HEAD", "SECOND_STAGE_AUTHORITY", "BOSS"})
    void ensurePrivilegedPersonIsAllowedToWriteOwnOvertimeWithPrivilegedRestriction(Role role) {

        final Person person = new Person();
        person.setPermissions(List.of(role));

        when(settingsService.getSettings()).thenReturn(overtimeSettings(true));

        assertThat(sut.isUserIsAllowedToWriteOvertime(person, person)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OFFICE", "DEPARTMENT_HEAD", "SECOND_STAGE_AUTHORITY", "BOSS", "USER"})
    void ensurePersonIsAllowedToWriteOwnOvertimeWithoutPrivilegedRestriction(Role role) {

        final Person person = new Person();
        person.setPermissions(List.of(role));

        when(settingsService.getSettings()).thenReturn(overtimeSettings(false));

        assertThat(sut.isUserIsAllowedToWriteOvertime(person, person)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"DEPARTMENT_HEAD", "SECOND_STAGE_AUTHORITY", "BOSS", "USER"})
    void ensurePersonIsNotAllowedToWriteOthersOvertimeWithNoPrivilegedRestriction(Role role) {

        final Person person = new Person();
        person.setPermissions(List.of(role));
        final Person other = new Person();

        when(settingsService.getSettings()).thenReturn(overtimeSettings(false));

        assertThat(sut.isUserIsAllowedToWriteOvertime(person, other)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"DEPARTMENT_HEAD", "SECOND_STAGE_AUTHORITY", "BOSS", "OFFICE"})
    void ensurePersonIsAllowedToWriteOthersOvertimeWithPrivilegedRestriction(Role role) {

        final Person signedInUser = new Person();
        signedInUser.setPermissions(List.of(role));
        final Person personOfOvertime = new Person();

        when(settingsService.getSettings()).thenReturn(overtimeSettings(true));

        assertThat(sut.isUserIsAllowedToWriteOvertime(signedInUser, personOfOvertime)).isTrue();
    }

    @Test
    void ensureGetLeftOvertimeTotalAndDateRangeForPersons() {
        final LocalDate from = LocalDate.now(clock).withMonth(AUGUST.getValue()).with(firstDayOfMonth());
        final LocalDate to = LocalDate.now(clock).withMonth(AUGUST.getValue()).with(lastDayOfMonth());

        final Person person = new Person();
        person.setId(1L);

        final Person person2 = new Person();
        person2.setId(2L);

        final List<Person> persons = List.of(person, person2);

        final Overtime overtimeOne = new Overtime();
        overtimeOne.setPerson(person);
        overtimeOne.setStartDate(from);
        overtimeOne.setEndDate(from.plusDays(1));
        overtimeOne.setDuration(Duration.ofHours(1));

        final Overtime overtimeOneOne = new Overtime();
        overtimeOneOne.setPerson(person);
        overtimeOneOne.setStartDate(to.plusDays(1));
        overtimeOneOne.setEndDate(to.plusDays(1));
        overtimeOneOne.setDuration(Duration.ofHours(1));

        final Overtime overtimeTwo = new Overtime();
        overtimeTwo.setPerson(person2);
        overtimeTwo.setStartDate(from.plusDays(4));
        overtimeTwo.setEndDate(from.plusDays(4));
        overtimeTwo.setDuration(Duration.ofHours(10));

        final Overtime overtimeThree = new Overtime();
        overtimeThree.setPerson(person2);
        overtimeThree.setStartDate(to.plusDays(4));
        overtimeThree.setEndDate(to.plusDays(4));
        overtimeThree.setDuration(Duration.ofHours(10));

        when(overtimeRepository.findByPersonIsInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(persons, from.with(firstDayOfYear()), to.with(lastDayOfYear())))
            .thenReturn(List.of(overtimeOne, overtimeOneOne, overtimeTwo, overtimeThree));
        when(overtimeRepository.findByPersonIsInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(persons, from, to))
            .thenReturn(List.of(overtimeOne, overtimeTwo));

        final List<Application> applications = List.of();

        final Map<Person, LeftOvertime> actual = sut.getLeftOvertimeTotalAndDateRangeForPersons(persons, applications, from, to);
        assertThat(actual)
            .hasSize(2)
            .containsKey(person)
            .containsKey(person2);

        final LeftOvertime leftOvertime = actual.get(person);
        assertThat(leftOvertime.getLeftOvertimeOverall()).isEqualTo(Duration.ofHours(2));
        assertThat(leftOvertime.getLeftOvertimeDateRange()).isEqualTo(Duration.ofHours(1));

        final LeftOvertime leftOvertime2 = actual.get(person2);
        assertThat(leftOvertime2.getLeftOvertimeOverall()).isEqualTo(Duration.ofHours(20));
        assertThat(leftOvertime2.getLeftOvertimeDateRange()).isEqualTo(Duration.ofHours(10));
    }

    @Test
    void ensureGetLeftOvertimeTotalAndDateRangeForPersonsWithOvertimeReduction() {
        final LocalDate from = LocalDate.now(clock).withMonth(AUGUST.getValue()).with(firstDayOfMonth());
        final LocalDate to = LocalDate.now(clock).withMonth(AUGUST.getValue()).with(lastDayOfMonth());

        final Person person = new Person();
        person.setId(1L);

        final Person person2 = new Person();
        person2.setId(2L);

        final List<Person> persons = List.of(person, person2);

        final Overtime overtimeOne = new Overtime();
        overtimeOne.setPerson(person);
        overtimeOne.setStartDate(from);
        overtimeOne.setEndDate(from.plusDays(1));
        overtimeOne.setDuration(Duration.ofHours(1));

        final Overtime overtimeOneOne = new Overtime();
        overtimeOneOne.setPerson(person);
        overtimeOneOne.setStartDate(to.plusDays(1));
        overtimeOneOne.setEndDate(to.plusDays(1));
        overtimeOneOne.setDuration(Duration.ofHours(1));

        final Overtime overtimeTwo = new Overtime();
        overtimeTwo.setPerson(person2);
        overtimeTwo.setStartDate(from.plusDays(4));
        overtimeTwo.setEndDate(from.plusDays(4));
        overtimeTwo.setDuration(Duration.ofHours(10));

        final Overtime overtimeTwoTwo = new Overtime();
        overtimeTwoTwo.setPerson(person2);
        overtimeTwoTwo.setStartDate(to.plusDays(4));
        overtimeTwoTwo.setEndDate(to.plusDays(4));
        overtimeTwoTwo.setDuration(Duration.ofHours(10));

        when(overtimeRepository.findByPersonIsInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(persons, from.with(firstDayOfYear()), to.with(lastDayOfYear())))
            .thenReturn(List.of(overtimeOne, overtimeOneOne, overtimeTwo, overtimeTwoTwo));
        when(overtimeRepository.findByPersonIsInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(persons, from, to))
            .thenReturn(List.of(overtimeOne, overtimeTwo));

        final VacationType overtimeVacationType = VacationType.builder()
            .id(1L)
            .category(VacationCategory.OVERTIME)
            .build();

        final Application personOvertimeReduction = new Application();
        personOvertimeReduction.setId(1L);
        personOvertimeReduction.setPerson(person);
        personOvertimeReduction.setStatus(ApplicationStatus.ALLOWED);
        personOvertimeReduction.setVacationType(overtimeVacationType);
        personOvertimeReduction.setHours(Duration.ofMinutes(90));
        // overtime reduction should result in `overall`. NOT in `date range`.
        personOvertimeReduction.setStartDate(LocalDate.now(clock).withMonth(JANUARY.getValue()));
        personOvertimeReduction.setEndDate(LocalDate.now(clock).withMonth(JANUARY.getValue()));

        final List<Application> applications = List.of(personOvertimeReduction);

        final Map<Person, LeftOvertime> actual = sut.getLeftOvertimeTotalAndDateRangeForPersons(persons, applications, from, to);
        assertThat(actual)
            .hasSize(2)
            .containsKey(person)
            .containsKey(person2);

        final LeftOvertime leftOvertime = actual.get(person);
        assertThat(leftOvertime.getLeftOvertimeOverall()).isEqualTo(Duration.ofMinutes(30));
        assertThat(leftOvertime.getLeftOvertimeDateRange()).isEqualTo(Duration.ofHours(1));

        final LeftOvertime leftOvertime2 = actual.get(person2);
        assertThat(leftOvertime2.getLeftOvertimeOverall()).isEqualTo(Duration.ofHours(20));
        assertThat(leftOvertime2.getLeftOvertimeDateRange()).isEqualTo(Duration.ofHours(10));
    }

    @Test
    void ensureGetLeftOvertimeTotalAndDateRangeForPersonsIncludesEntriesForPersonsWithoutOvertimeReduction() {
        final LocalDate from = LocalDate.now(clock).withMonth(AUGUST.getValue()).with(firstDayOfMonth());
        final LocalDate to = LocalDate.now(clock).withMonth(AUGUST.getValue()).with(lastDayOfMonth());

        final Person personWithoutOvertime = new Person();
        personWithoutOvertime.setId(1L);

        final List<Person> persons = List.of(personWithoutOvertime);

        when(overtimeRepository.findByPersonIsInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(persons, from.with(firstDayOfYear()), to.with(lastDayOfYear()))).thenReturn(List.of());
        when(overtimeRepository.findByPersonIsInAndEndDateIsGreaterThanEqualAndStartDateIsLessThanEqual(persons, from, to)).thenReturn(List.of());

        final List<Application> applications = List.of();

        final Map<Person, LeftOvertime> actual = sut.getLeftOvertimeTotalAndDateRangeForPersons(persons, applications, from, to);
        assertThat(actual)
            .hasSize(1)
            .containsKey(personWithoutOvertime);

        final LeftOvertime leftOvertime = actual.get(personWithoutOvertime);
        assertThat(leftOvertime).isNotNull();
        assertThat(leftOvertime.getLeftOvertimeOverall()).isEqualTo(Duration.ZERO);
        assertThat(leftOvertime.getLeftOvertimeDateRange()).isEqualTo(Duration.ZERO);
    }

    @Test
    void ensureDeletionOnPersonDeletionEvent() {
        final Person person = new Person();

        sut.deleteAll(new PersonDeletedEvent(person));

        final InOrder inOrder = inOrder(overtimeCommentRepository, overtimeRepository);
        inOrder.verify(overtimeCommentRepository).deleteByOvertimePerson(person);
        inOrder.verify(overtimeRepository).deleteByPerson(person);
    }

    private static OvertimeDurationSum overtimeDurationSum(Person person, Double duration) {
        return new OvertimeDurationSum() {
            @Override
            public Person getPerson() {
                return person;
            }

            @Override
            public Double getDurationDouble() {
                return duration;
            }
        };
    }

    private Settings overtimeSettings(boolean overtimeWritePrivilegedOnly) {

        final Settings settings = new Settings();
        settings.getOvertimeSettings().setOvertimeWritePrivilegedOnly(overtimeWritePrivilegedOnly);

        return settings;
    }
}
