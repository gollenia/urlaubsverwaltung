package org.synyx.urlaubsverwaltung.absence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.synyx.urlaubsverwaltung.application.application.Application;
import org.synyx.urlaubsverwaltung.application.application.ApplicationService;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationType;
import org.synyx.urlaubsverwaltung.period.DayLength;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.settings.Settings;
import org.synyx.urlaubsverwaltung.settings.SettingsService;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteService;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteStatus;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendar;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendarService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.time.Month.DECEMBER;
import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.TestDataCreator.createApplication;
import static org.synyx.urlaubsverwaltung.TestDataCreator.createSickNote;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.ALLOWED_CANCELLATION_REQUESTED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.CANCELLED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.REJECTED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.REVOKED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.TEMPORARY_ALLOWED;
import static org.synyx.urlaubsverwaltung.application.application.ApplicationStatus.WAITING;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.HOLIDAY;
import static org.synyx.urlaubsverwaltung.period.DayLength.FULL;
import static org.synyx.urlaubsverwaltung.period.DayLength.ZERO;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteStatus.ACTIVE;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteStatus.CONVERTED_TO_VACATION;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceImplTest {

    private AbsenceServiceImpl sut;

    @Mock
    private ApplicationService applicationService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private SickNoteService sickNoteService;
    @Mock
    private WorkingTimeCalendarService workingTimeCalendarService;

    @BeforeEach
    void setUp() {
        sut = new AbsenceServiceImpl(applicationService, sickNoteService, settingsService, workingTimeCalendarService);
    }

    @Test
    void getOpenAbsencesSinceForPersons() {

        final Settings settings = new Settings();
        final TimeSettings timeSettings = new TimeSettings();
        timeSettings.setTimeZoneId("Etc/UTC");
        settings.setTimeSettings(timeSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        final LocalDate since = LocalDate.of(2020, 10, 13);

        final LocalDate startDate = LocalDate.of(2019, 12, 10);
        final LocalDate endDate = LocalDate.of(2019, 12, 23);
        final Application application = createApplication(person, startDate, endDate, FULL);
        when(applicationService.getForStatesAndPersonSince(List.of(ALLOWED, WAITING, TEMPORARY_ALLOWED, ALLOWED_CANCELLATION_REQUESTED), List.of(person), since)).thenReturn(List.of(application));

        final LocalDate startDateSickNote = LocalDate.of(2019, 10, 10);
        final LocalDate endDateSickNote = LocalDate.of(2019, 10, 23);
        final SickNote sickNote = createSickNote(person, startDateSickNote, endDateSickNote, FULL);
        when(sickNoteService.getForStatesAndPersonSince(List.of(ACTIVE), List.of(person), since)).thenReturn(List.of(sickNote));

        final List<Absence> openAbsences = sut.getOpenAbsencesSince(List.of(person), since);
        assertThat(openAbsences).hasSize(2);
        assertThat(openAbsences.get(0).getPerson()).isEqualTo(person);
        assertThat(openAbsences.get(0).getStartDate()).isEqualTo(ZonedDateTime.parse("2019-12-10T00:00Z[Etc/UTC]"));
        assertThat(openAbsences.get(0).getEndDate()).isEqualTo(ZonedDateTime.parse("2019-12-24T00:00Z[Etc/UTC]"));
        assertThat(openAbsences.get(1).getPerson()).isEqualTo(person);
        assertThat(openAbsences.get(1).getStartDate()).isEqualTo(ZonedDateTime.parse("2019-10-10T00:00Z[Etc/UTC]"));
        assertThat(openAbsences.get(1).getEndDate()).isEqualTo(ZonedDateTime.parse("2019-10-24T00:00Z[Etc/UTC]"));
    }

    @Test
    void getOpenAbsencesSince() {

        final Settings settings = new Settings();
        final TimeSettings timeSettings = new TimeSettings();
        timeSettings.setTimeZoneId("Etc/UTC");
        settings.setTimeSettings(timeSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");

        final LocalDate since = LocalDate.of(2020, 11, 13);

        final LocalDate startDate = LocalDate.of(2019, 11, 10);
        final LocalDate endDate = LocalDate.of(2019, 11, 23);
        final Application application = createApplication(person, startDate, endDate, FULL);
        when(applicationService.getForStatesSince(List.of(ALLOWED, WAITING, TEMPORARY_ALLOWED, ALLOWED_CANCELLATION_REQUESTED), since)).thenReturn(List.of(application));

        final LocalDate startDateSickNote = LocalDate.of(2019, 10, 10);
        final LocalDate endDateSickNote = LocalDate.of(2019, 10, 23);
        final SickNote sickNote = createSickNote(person, startDateSickNote, endDateSickNote, FULL);
        when(sickNoteService.getForStatesSince(List.of(ACTIVE), since)).thenReturn(List.of(sickNote));

        final List<Absence> openAbsences = sut.getOpenAbsencesSince(since);
        assertThat(openAbsences).hasSize(2);
        assertThat(openAbsences.get(0).getPerson()).isEqualTo(person);
        assertThat(openAbsences.get(0).getStartDate()).isEqualTo(ZonedDateTime.parse("2019-11-10T00:00Z[Etc/UTC]"));
        assertThat(openAbsences.get(0).getEndDate()).isEqualTo(ZonedDateTime.parse("2019-11-24T00:00Z[Etc/UTC]"));
        assertThat(openAbsences.get(1).getPerson()).isEqualTo(person);
        assertThat(openAbsences.get(1).getStartDate()).isEqualTo(ZonedDateTime.parse("2019-10-10T00:00Z[Etc/UTC]"));
        assertThat(openAbsences.get(1).getEndDate()).isEqualTo(ZonedDateTime.parse("2019-10-24T00:00Z[Etc/UTC]"));
    }

    @Test
    void ensureOpenAbsencesCallsApplicationServiceForPersonsAndDateInterval() {

        final Person batman = new Person();
        batman.setId(1L);

        final Person superman = new Person();
        superman.setId(2L);

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        sut.getOpenAbsences(List.of(batman, superman), start, end);

        verify(applicationService).getForStatesAndPerson(List.of(ALLOWED, WAITING, TEMPORARY_ALLOWED, ALLOWED_CANCELLATION_REQUESTED), List.of(batman, superman), start, end);
    }

    @Test
    void ensureOpenAbsencesCallsSickNotServiceForPersonsAndDateInterval() {

        final Person batman = new Person();
        batman.setId(1L);

        final Person superman = new Person();
        superman.setId(2L);

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        sut.getOpenAbsences(List.of(batman, superman), start, end);

        verify(sickNoteService).getForStatesAndPerson(List.of(ACTIVE), List.of(batman, superman), start, end);
    }

    @Test
    void ensureVacationMorning() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final VacationType vacationType = VacationType.builder()
            .id(1L)
            .visibleToEveryone(true)
            .build();

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(DayLength.MORNING);
        application.setStatus(ALLOWED);
        application.setVacationType(vacationType);

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.ALLOWED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(true);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isEmpty();
    }

    @Test
    void ensureVacationNoon() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final VacationType vacationType = VacationType.builder()
            .id(1L)
            .visibleToEveryone(false)
            .build();

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(DayLength.NOON);
        application.setStatus(ALLOWED);
        application.setVacationType(vacationType);

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.ALLOWED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(false);
    }

    @Test
    void ensureVacationFull() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final VacationType vacationType = VacationType.builder()
            .id(1L)
            .visibleToEveryone(false)
            .build();

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(FULL);
        application.setStatus(ALLOWED);
        application.setVacationType(vacationType);

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.ALLOWED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(false);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.ALLOWED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(false);
    }

    @Test
    void ensureSickMorning() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> ZERO);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final SickNote sickNote = SickNote.builder()
            .id(42L)
            .person(batman)
            .status(ACTIVE)
            .startDate(start.plusDays(1))
            .endDate(start.plusDays(1))
            .dayLength(DayLength.MORNING)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.ACTIVE);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.SICK);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isEmpty();
    }

    @Test
    void ensureSickNoon() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> ZERO);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final SickNote sickNote = SickNote.builder()
            .id(42L)
            .person(batman)
            .status(ACTIVE)
            .startDate(start.plusDays(1))
            .endDate(start.plusDays(1))
            .dayLength(DayLength.NOON)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.ACTIVE);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.SICK);
    }

    @Test
    void ensureVacationMorningAndSickNoon() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(DayLength.MORNING);
        application.setStatus(ALLOWED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final SickNote sickNote = SickNote.builder()
            .id(1337L)
            .person(batman)
            .status(SickNoteStatus.CANCELLED)
            .startDate(start.plusDays(1))
            .endDate(start.plusDays(1))
            .dayLength(DayLength.NOON)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(2);

        // vacation
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isEmpty();

        // sick
        assertThat(actualAbsences.get(1).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getMorning()).isEmpty();
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(1337L);
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.SICK);
    }

    @Test
    void ensureMultipleVacationDaysWithApplicationsOutsideTheAskedDateRange() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, MAY, 31));
        application.setEndDate(LocalDate.of(2021, JUNE, 10));
        application.setDayLength(FULL);
        application.setStatus(ALLOWED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(1);
        assertThat(absenceRecords.get(0).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(0).getDate()).isEqualTo(LocalDate.of(2021, MAY, 31));
        assertThat(absenceRecords.get(0).getMorning()).isPresent();
        assertThat(absenceRecords.get(0).getNoon()).isPresent();
    }

    @Test
    void ensureMultipleSickDaysWithApplicationsOutsideTheAskedDateRange() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> ZERO);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final SickNote sickNote = SickNote.builder()
            .id(42L)
            .person(batman)
            .status(SickNoteStatus.CANCELLED)
            .startDate(LocalDate.of(2021, MAY, 31))
            .endDate(LocalDate.of(2021, JUNE, 10))
            .dayLength(FULL)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(1);
        assertThat(absenceRecords.get(0).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(0).getDate()).isEqualTo(LocalDate.of(2021, MAY, 31));
        assertThat(absenceRecords.get(0).getMorning()).isPresent();
        assertThat(absenceRecords.get(0).getNoon()).isPresent();
    }

    @Test
    void ensureMultipleVacationDaysWithPublicHolidayFull() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> {
            if (date.equals(LocalDate.of(2021, MAY, 20))) {
                // public holiday -> no work 🎉
                return ZERO;
            } else {
                return FULL;
            }
        });
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, MAY, 1));
        application.setEndDate(LocalDate.of(2021, MAY, 31));
        application.setDayLength(FULL);
        application.setStatus(ALLOWED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(30);

        // 1. May to 19. May -> vacation
        IntStream.range(0, 19).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, MAY, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });

        // 20. May -> public holiday
        // is not existent in absences

        // 21. May to 31. May -> vacation
        IntStream.range(19, 30).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, MAY, index + 2));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });
    }

    @Test
    void ensureMultipleVacationDaysWithPublicHolidayNoon() {
        final LocalDate start = LocalDate.of(2021, DECEMBER, 1);
        final LocalDate end = LocalDate.of(2021, DECEMBER, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> {
            if (date.equals(LocalDate.of(2021, DECEMBER, 24))) {
                // half day "public holiday" at noon -> working in the morning
                return DayLength.MORNING;
            } else {
                return FULL;
            }
        });
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, DECEMBER, 1));
        application.setEndDate(LocalDate.of(2021, DECEMBER, 31));
        application.setDayLength(FULL);
        application.setStatus(ALLOWED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(31);

        // 1. December to 23. December -> vacation
        IntStream.range(0, 23).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });

        // 24. December -> morning: vacation, noon: public holiday
        assertThat(absenceRecords.get(23).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(23).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, 24));
        assertThat(absenceRecords.get(23).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(absenceRecords.get(23).getNoon()).isEmpty();

        // 25. December to 31. December -> vacation
        IntStream.range(24, 30).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });
    }

    @Test
    void ensureMultipleVacationDaysWithPublicHolidayMorning() {
        final LocalDate start = LocalDate.of(2021, DECEMBER, 1);
        final LocalDate end = LocalDate.of(2021, DECEMBER, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> {
            if (date.equals(LocalDate.of(2021, DECEMBER, 24))) {
                // half day "public holiday" in the morning -> working at noon
                return DayLength.NOON;
            } else {
                return DayLength.FULL;
            }
        });
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, DECEMBER, 1));
        application.setEndDate(LocalDate.of(2021, DECEMBER, 31));
        application.setDayLength(FULL);
        application.setStatus(ALLOWED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getOpenAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(31);

        // 1. December to 23. December -> vacation
        IntStream.range(0, 23).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });

        // 24. December -> morning: vacation, noon: public holiday
        assertThat(absenceRecords.get(23).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(23).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, 24));
        assertThat(absenceRecords.get(23).getMorning()).isEmpty();
        assertThat(absenceRecords.get(23).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);

        // 25. December to 31. December -> vacation
        IntStream.range(24, 30).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });
    }

    @Test
    void ensureClosedAbsencesCallsApplicationServiceForPersonsAndDateInterval() {

        final Person batman = new Person();
        batman.setId(1L);

        final Person superman = new Person();
        superman.setId(2L);

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        sut.getClosedAbsences(List.of(batman, superman), start, end);

        verify(applicationService).getForStatesAndPerson(List.of(REJECTED, CANCELLED, REVOKED), List.of(batman, superman), start, end);
    }

    @Test
    void ensureClosedAbsencesCallsSickNotServiceForPersonsAndDateInterval() {

        final Person batman = new Person();
        batman.setId(1L);

        final Person superman = new Person();
        superman.setId(2L);

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        sut.getClosedAbsences(List.of(batman, superman), start, end);

        verify(sickNoteService).getForStatesAndPerson(List.of(CONVERTED_TO_VACATION, SickNoteStatus.CANCELLED), List.of(batman, superman), start, end);
    }

    @Test
    void ensureClosedAbsencesVacationMorning() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final VacationType vacationType = VacationType.builder()
            .id(1L)
            .visibleToEveryone(true)
            .build();

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(DayLength.MORNING);
        application.setStatus(CANCELLED);
        application.setVacationType(vacationType);

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.CANCELLED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(true);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isEmpty();
    }

    @Test
    void ensureClosedAbsencesVacationNoon() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final VacationType vacationType = VacationType.builder()
            .id(1L)
            .visibleToEveryone(false)
            .build();

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(DayLength.NOON);
        application.setStatus(CANCELLED);
        application.setVacationType(vacationType);

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.CANCELLED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(false);
    }

    @Test
    void ensureClosedAbsencesVacationFull() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final VacationType vacationType = VacationType.builder()
            .id(1L)
            .visibleToEveryone(false)
            .build();

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(FULL);
        application.setStatus(CANCELLED);
        application.setVacationType(vacationType);

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.CANCELLED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(false);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.CANCELLED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getVacationTypeId)).hasValue(Optional.of(1L));
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::isVisibleToEveryone)).hasValue(false);
    }

    @Test
    void ensureClosedAbsencesSickMorning() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> ZERO);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final SickNote sickNote = SickNote.builder()
            .id(42L)
            .person(batman)
            .status(SickNoteStatus.CANCELLED)
            .startDate(start.plusDays(1))
            .endDate(start.plusDays(1))
            .dayLength(DayLength.MORNING)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.CANCELLED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.SICK);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isEmpty();
    }

    @Test
    void ensureClosedAbsencesSickNoon() {

        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> ZERO);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final SickNote sickNote = SickNote.builder()
            .id(42L)
            .person(batman)
            .status(SickNoteStatus.CANCELLED)
            .startDate(start.plusDays(1))
            .endDate(start.plusDays(1))
            .dayLength(DayLength.NOON)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getStatus)).hasValue(AbsencePeriod.AbsenceStatus.CANCELLED);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.SICK);
    }

    @Test
    void ensureClosedAbsencesVacationMorningAndSickNoon() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(start.plusDays(1));
        application.setEndDate(start.plusDays(1));
        application.setDayLength(DayLength.MORNING);
        application.setStatus(ALLOWED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final SickNote sickNote = SickNote.builder()
            .id(1337L)
            .person(batman)
            .status(SickNoteStatus.CANCELLED)
            .startDate(start.plusDays(1))
            .endDate(start.plusDays(1))
            .dayLength(DayLength.NOON)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);
        assertThat(actualAbsences).hasSize(2);

        // vacation
        assertThat(actualAbsences.get(0).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning()).isNotEmpty();
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getId)).hasValue(42L);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(actualAbsences.get(0).getAbsenceRecords().get(0).getNoon()).isEmpty();

        // sick
        assertThat(actualAbsences.get(1).getAbsenceRecords()).hasSize(1);
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getPerson()).isSameAs(batman);
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getMorning()).isEmpty();
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getNoon()).isNotEmpty();
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getId)).hasValue(1337L);
        assertThat(actualAbsences.get(1).getAbsenceRecords().get(0).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.SICK);
    }

    @Test
    void ensureClosedAbsencesMultipleVacationDaysWithApplicationsOutsideTheAskedDateRange() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> FULL);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, MAY, 31));
        application.setEndDate(LocalDate.of(2021, JUNE, 10));
        application.setDayLength(FULL);
        application.setStatus(CANCELLED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(1);
        assertThat(absenceRecords.get(0).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(0).getDate()).isEqualTo(LocalDate.of(2021, MAY, 31));
        assertThat(absenceRecords.get(0).getMorning()).isPresent();
        assertThat(absenceRecords.get(0).getNoon()).isPresent();
    }

    @Test
    void ensureClosedAbsencesMultipleSickDaysWithApplicationsOutsideTheAskedDateRange() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> ZERO);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final SickNote sickNote = SickNote.builder()
            .id(42L)
            .person(batman)
            .status(SickNoteStatus.CANCELLED)
            .startDate(LocalDate.of(2021, MAY, 31))
            .endDate(LocalDate.of(2021, JUNE, 10))
            .dayLength(FULL)
            .build();

        when(sickNoteService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(sickNote));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(1);
        assertThat(absenceRecords.get(0).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(0).getDate()).isEqualTo(LocalDate.of(2021, MAY, 31));
        assertThat(absenceRecords.get(0).getMorning()).isPresent();
        assertThat(absenceRecords.get(0).getNoon()).isPresent();
    }

    @Test
    void ensureClosedAbsencesMultipleVacationDaysWithPublicHolidayFull() {
        final LocalDate start = LocalDate.of(2021, MAY, 1);
        final LocalDate end = LocalDate.of(2021, MAY, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> {
            if (date.equals(LocalDate.of(2021, MAY, 20))) {
                // public holiday -> no work 🎉
                return ZERO;
            } else {
                return FULL;
            }
        });
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, MAY, 1));
        application.setEndDate(LocalDate.of(2021, MAY, 31));
        application.setDayLength(FULL);
        application.setStatus(CANCELLED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(30);

        // 1. May to 19. May -> vacation
        IntStream.range(0, 19).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, MAY, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });

        // 20. May -> public holiday
        // is not existent in absences

        // 21. May to 31. May -> vacation
        IntStream.range(19, 30).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, MAY, index + 2));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });
    }

    @Test
    void ensureClosedAbsencesMultipleVacationDaysWithPublicHolidayNoon() {
        final LocalDate start = LocalDate.of(2021, DECEMBER, 1);
        final LocalDate end = LocalDate.of(2021, DECEMBER, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> {
            if (date.equals(LocalDate.of(2021, DECEMBER, 24))) {
                // half day "public holiday" at noon -> working in the morning
                return DayLength.MORNING;
            } else {
                return FULL;
            }
        });
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, DECEMBER, 1));
        application.setEndDate(LocalDate.of(2021, DECEMBER, 31));
        application.setDayLength(FULL);
        application.setStatus(CANCELLED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(31);

        // 1. December to 23. December -> vacation
        IntStream.range(0, 23).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });

        // 24. December -> morning: vacation, noon: public holiday
        assertThat(absenceRecords.get(23).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(23).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, 24));
        assertThat(absenceRecords.get(23).getMorning().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);
        assertThat(absenceRecords.get(23).getNoon()).isEmpty();

        // 25. December to 31. December -> vacation
        IntStream.range(24, 30).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });
    }

    @Test
    void ensureClosedAbsencesMultipleVacationDaysWithPublicHolidayMorning() {
        final LocalDate start = LocalDate.of(2021, DECEMBER, 1);
        final LocalDate end = LocalDate.of(2021, DECEMBER, 31);

        final Person batman = new Person();
        batman.setId(1L);

        final Map<LocalDate, DayLength> personWorkingTimeByDate = buildWorkingTimeByDate(start, end, date -> {
            if (date.equals(LocalDate.of(2021, DECEMBER, 24))) {
                // half day "public holiday" in the morning -> working at noon
                return DayLength.NOON;
            } else {
                return DayLength.FULL;
            }
        });
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(personWorkingTimeByDate);
        when(workingTimeCalendarService.getWorkingTimesByPersons(List.of(batman), new DateRange(start, end))).thenReturn(Map.of(batman, workingTimeCalendar));

        final Application application = new Application();
        application.setId(42L);
        application.setPerson(batman);
        application.setStartDate(LocalDate.of(2021, DECEMBER, 1));
        application.setEndDate(LocalDate.of(2021, DECEMBER, 31));
        application.setDayLength(FULL);
        application.setStatus(CANCELLED);
        application.setVacationType(anyVacationType());

        when(applicationService.getForStatesAndPerson(any(), any(), any(), any())).thenReturn(List.of(application));

        final List<AbsencePeriod> actualAbsences = sut.getClosedAbsences(List.of(batman), start, end);

        assertThat(actualAbsences).hasSize(1);

        final List<AbsencePeriod.Record> absenceRecords = actualAbsences.get(0).getAbsenceRecords();
        assertThat(absenceRecords).hasSize(31);

        // 1. December to 23. December -> vacation
        IntStream.range(0, 23).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });

        // 24. December -> morning: vacation, noon: public holiday
        assertThat(absenceRecords.get(23).getPerson()).isSameAs(batman);
        assertThat(absenceRecords.get(23).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, 24));
        assertThat(absenceRecords.get(23).getMorning()).isEmpty();
        assertThat(absenceRecords.get(23).getNoon().map(AbsencePeriod.RecordInfo::getType)).hasValue(AbsencePeriod.AbsenceType.VACATION);

        // 25. December to 31. December -> vacation
        IntStream.range(24, 30).forEach(index -> {
            assertThat(absenceRecords.get(index).getPerson()).isSameAs(batman);
            assertThat(absenceRecords.get(index).getDate()).isEqualTo(LocalDate.of(2021, DECEMBER, index + 1));
            assertThat(absenceRecords.get(index).getMorning()).isPresent();
            assertThat(absenceRecords.get(index).getNoon()).isPresent();
        });
    }

    private static VacationType anyVacationType() {
        return VacationType.builder().id(1L).build();
    }

    private Map<LocalDate, DayLength> buildWorkingTimeByDate(LocalDate from, LocalDate to, Function<LocalDate, DayLength> dayLengthProvider) {
        Map<LocalDate, DayLength> map = new HashMap<>();
        for (LocalDate date : new DateRange(from, to)) {
            map.put(date, dayLengthProvider.apply(date));
        }
        return map;
    }
}
