package jfxtras.labs.repeatagenda.scene.control.repeatagenda.icalendar;

import java.time.LocalDateTime;
import java.util.Collection;

import jfxtras.labs.repeatagenda.scene.control.repeatagenda.VEventImpl;
import jfxtras.scene.control.agenda.Agenda.Appointment;

/** Interface for VEVENT, VTODO, VJOURNA calendar components. 
 * @param <T>*/
public interface VComponent
{
    /**
     * Returns the collection of recurrence instances of calendar component of type T that exists
     * between dateTimeRangeStart and dateTimeRangeEnd based on VComponent.
     * Recurrence set is defined in RFC 5545 iCalendar page 121 as follows 
     * "The recurrence set is the complete set of recurrence instances for a calendar component.  
     * The recurrence set is generated by considering the initial "DTSTART" property along with
     * the "RRULE", "RDATE", and "EXDATE" properties contained within the recurring component."
     * 
     * Type T is of the type your appointment your application requires.  For JFxtras Agenda it is
     * Agenda.Appointment.
     * 
     * @param dateTimeRangeStart - Start date/time when appointments will be made
     * @param dateTimeRangeEnd - End date/time when appointments will be made
     * @return - collection of Appointments of type T
     * 
     * @author David Bal
     * @see VEventImpl
     */
    <T> Collection<T> makeInstances(
            LocalDateTime dateTimeRangeStart
          , LocalDateTime dateTimeRangeEnd);
    
    /**
     * Returns existing instances in the Recurrence Set (defined in RFC 5545 iCalendar page 121)
     * made by the last call of makeRecurrenceSet
     * 
     * @return - current instances of the Recurrence Set
     * @see makeRecurrenceSet
     */
    Collection<Appointment> instances();
}
