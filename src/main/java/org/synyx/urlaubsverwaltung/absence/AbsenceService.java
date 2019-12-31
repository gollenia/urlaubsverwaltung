package org.synyx.urlaubsverwaltung.absence;


import org.synyx.urlaubsverwaltung.calendarintegration.absence.Absence;
import org.synyx.urlaubsverwaltung.person.Person;

import java.util.List;


public interface AbsenceService {

    /**
     * Get absences from a list of persons
     *
     * @param persons
     * @return
     */
    List<Absence> getOpenAbsences(List<Person> persons);
}
