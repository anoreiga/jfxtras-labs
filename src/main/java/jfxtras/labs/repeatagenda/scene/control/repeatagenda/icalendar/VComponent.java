package jfxtras.labs.repeatagenda.scene.control.repeatagenda.icalendar;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.util.Callback;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.ICalendarAgendaUtilities.ChangeDialogOption;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.Settings;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.icalendar.rrule.RRule;

/** Interface for VEVENT, VTODO, VJOURNAL calendar components. 
 * @param <T> - type of recurrence instance, such as an appointment implementation
 * @see VComponentAbstract
 * @see VEvent
 * */
public interface VComponent<T>
{
    final static DateTimeFormatter LOCAL_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)
            .toFormatter();
    final static DateTimeFormatter LOCAL_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2)
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendValue(SECOND_OF_MINUTE, 2)
            .toFormatter();
    final static DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(LOCAL_DATE_FORMATTER)
            .appendLiteral('T')
            .append(LOCAL_TIME_FORMATTER)
            .toFormatter();
    final static DateTimeFormatter ZONED_DATE_TIME_UTC_FORMATTER = new DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME_FORMATTER)
            .appendOffsetId()
            .toFormatter();
    final static DateTimeFormatter ZONED_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .optionalStart()
            .appendLiteral("TZID=")
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(':')
            .optionalEnd()
            .append(LOCAL_DATE_TIME_FORMATTER)
            .toFormatter();
    final static DateTimeFormatter ZONE_FORMATTER = new DateTimeFormatterBuilder()
            .optionalStart()
            .appendLiteral("TZID=")
            .parseCaseSensitive()
            .appendZoneRegionId()
            .optionalEnd()
            .toFormatter();

    /**
     * CATEGORIES: RFC 5545 iCalendar 3.8.1.12. page 81
     * This property defines the categories for a calendar component.
     * Example:
     * CATEGORIES:APPOINTMENT,EDUCATION
     * CATEGORIES:MEETING
     */
    String getCategories();
    void setCategories(String value);
    
    /**
     *  COMMENT: RFC 5545 iCalendar 3.8.1.12. page 83
     * This property specifies non-processing information intended
      to provide a comment to the calendar user.
     * Example:
     * COMMENT:The meeting really needs to include both ourselves
         and the customer. We can't hold this meeting without them.
         As a matter of fact\, the venue for the meeting ought to be at
         their site. - - John
     * */
    String getComment();
    void setComment(String value);
    
    /**
     * CREATED: Date-Time Created, from RFC 5545 iCalendar 3.8.7.1 page 136
     * This property specifies the date and time that the calendar information was created.
     * This is analogous to the creation date and time for a file in the file system.
     * The value MUST be specified in the UTC time format.
     */
    ZonedDateTime getDateTimeCreated();
    void setDateTimeCreated(ZonedDateTime dtCreated);
    
    /**
     * DTSTAMP: Date-Time Stamp, from RFC 5545 iCalendar 3.8.7.2 page 137
     * This property specifies the date and time that the instance of the
     * iCalendar object was created
     * The value MUST be specified in the UTC time format.
     */
    ZonedDateTime getDateTimeStamp();
    void setDateTimeStamp(ZonedDateTime dtStamp);
    
    /**
     * DTSTART: Date-Time Start, from RFC 5545 iCalendar 3.8.2.4 page 97
     * Start date/time of repeat rule.  Used as a starting point for making the Stream<LocalDateTime> of valid
     * start date/times of the repeating events.  Can be either type LocalDate or LocalDateTime
     */
    Temporal getDateTimeStart();
    /**
     * DTSTART: Date-Time Start, from RFC 5545 iCalendar 3.8.2.4 page 97
     * Start date/time of repeat rule.  Used as a starting point for making the Stream<LocalDateTime> of valid
     * start date/times of the repeating events.  Can be either type LocalDate or LocalDateTime
     */
    ObjectProperty<Temporal> dateTimeStartProperty(); // TODO - SHOULD I HAVE PROPERTIES HERE OR JUST IN ABSTRACT?
    void setDateTimeStart(Temporal dtStart);

    /** Component is whole day if dateTimeStart (DTSTART) only contains a date (no time) */
    default boolean isWholeDay() { return ! getDateTimeStart().isSupported(ChronoUnit.NANOS); }
        
    /**
     * EXDATE: Set of date/times exceptions for recurring events, to-dos, journal entries.
     * 3.8.5.1, RFC 5545 iCalendar
     * Is rarely used, so employs lazy initialization.
     */
    ExDate getExDate();
    void setExDate(ExDate exDate);

    /**
     * LAST-MODIFIED: Date-Time Last Modified, from RFC 5545 iCalendar 3.8.7.3 page 138
     * This property specifies the date and time that the information associated with
     * the calendar component was last revised.
     * 
     * The property value MUST be specified in the UTC time format.
     */
    ZonedDateTime getDateTimeLastModified();
    void setDateTimeLastModified(ZonedDateTime dtLastModified);
    
    /**
     * LOCATION: RFC 5545 iCalendar 3.8.1.12. page 87
     * This property defines the intended venue for the activity
     * defined by a calendar component.
     * Example:
     * LOCATION:Conference Room - F123\, Bldg. 002
     */
    String getLocation();
    void setLocation(String value);

    /**
     *  ORGANIZER: RFC 5545 iCalendar 3.8.4.3. page 111
     * This property defines the organizer for a calendar component
     * Example:
     * ORGANIZER;CN=John Smith:mailto:jsmith@example.com
     * 
     * The property is stored as a simple string.  The implementation is
     * responsible to extract any contained data elements such as CN, DIR, SENT-BY
     * */
    String getOrganizer();
    void setOrganizer(String value);
    
    /**
     * RELATED-TO: This property is used to represent a relationship or reference between
     * one calendar component and another.  By default, the property value points to another
     * calendar component's UID that has a PARENT relationship to the referencing object.
     * This field is null unless the object contains as RECURRENCE-ID value.
     * 3.8.4.5, RFC 5545 iCalendar
     */
    String getRelatedTo();
    void setRelatedTo(String uid);
    
    // TODO - NOT IMPLEMENTED YET
    /**
     * Use Google UID extension instead of RELATED-TO to express 
     */
    boolean isGoogleRecurrenceUID();
    void setGoogleRecurrenceUID(boolean b);

    /**
     * RECURRENCE-ID: date or date-time recurrence, from RFC 5545 iCalendar 3.8.4.4 page 112
     * The property value is the original value of the "DTSTART" property of the 
     * recurrence instance.
     */
    Temporal getDateTimeRecurrence();
    void setDateTimeRecurrence(Temporal dtRecurrence);
    
    /** If VComponent has RECURRENCE-ID this returns its parent object */
    VComponent<T> getParent();
    void setParent(VComponent<T> v);
    
    /**
     * RDATE: Set of date/times for recurring events, to-dos, journal entries.
     * 3.8.5.2, RFC 5545 iCalendar
     */
    RDate getRDate();
    void setRDate(RDate rDate);
    
    /**
     * RRULE, Recurrence Rule as defined in RFC 5545 iCalendar 3.8.5.3, page 122.
     * This property defines a rule or repeating pattern for recurring events, 
     * to-dos, journal entries, or time zone definitions
     * If component is not repeating the value is null.
     */
    RRule getRRule();
    void setRRule(RRule rRule);

    /**
     *  SEQUENCE: RFC 5545 iCalendar 3.8.7.4. page 138
     * This property defines the revision sequence number of the calendar component within a sequence of revisions.
     * Example:  The following is an example of this property for a calendar
      component that was just created by the "Organizer":

       SEQUENCE:0

      The following is an example of this property for a calendar
      component that has been revised two different times by the
      "Organizer":

       SEQUENCE:2
     */
    IntegerProperty sequenceProperty();
    int getSequence();
    void setSequence(int value);
    default void incrementSequence() { setSequence(getSequence()+1); }
    
    /**
     *  SUMMARY: RFC 5545 iCalendar 3.8.1.12. page 83
     * This property defines a short summary or subject for the calendar component 
     * Example:
     * SUMMARY:Department Party
     * */
    String getSummary();
    void setSummary(String value);
    
    /**
     * UID, Unique identifier, as defined by RFC 5545, iCalendar 3.8.4.7 page 117
     * A globally unique identifier for the calendar component.
     * Included is an example UID generator.  Other UID generators can be provided by
     * setting the UID callback.
     */
    String getUniqueIdentifier();
    void setUniqueIdentifier(String s);
    
    /** Callback for creating unique uid values  */
    Callback<Void, String> getUidGeneratorCallback();
    void setUidGeneratorCallback(Callback<Void, String> uidCallback);
    
    /**
     * Checks to see if VComponent is has all required properties filled.  Also checks
     * to ensure all properties contain valid values.
     * 
     * @return true for valid VComponent, false for invalid one
     */
    boolean isValid();
    
    /** Stream of dates or date/times that indicate the start of the event(s).
     * For a VEvent without RRULE the stream will contain only one date/time element.
     * A VEvent with a RRULE the stream contains more than one date/time element.  It will be infinite 
     * if COUNT or UNTIL is not present.  The stream has an end when COUNT or UNTIL condition is met.
     * Starts on startDateTime, which must be a valid event date/time, not necessarily the
     * first date/time (DTSTART) in the sequence.
     * 
     * Start date/times are only produced between the ranges set by setDateTimeRanges
     * 
     * @param startTemporal - start dates or date/times produced after this date.  If not on an occurrence,
     * it will be adjusted to be the next occurrence
     * @return - stream of start dates or date/times for the recurrence set
     */
    Stream<Temporal> stream(Temporal startTemporal);

    /**
     * Start of range for which recurrence instances are generated.
     * Should match the start date displayed on the calendar.
     * This is not a part of an iCalendar VComponent.
     */
    Temporal getStartRange();
    /**
     * Start of range for which recurrence instances are generated.
     * Should match the start date displayed on the calendar.
     * This is not a part of an iCalendar VComponent.
     */
    void setStartRange(Temporal start);
    /**
     * End of range for which recurrence instances are generated.
     * Should match the end date displayed on the calendar.
     * This is not a part of an iCalendar VComponent.
     */
    Temporal getEndRange();
    /**
     * End of range for which recurrence instances are generated.
     * Should match the end date displayed on the calendar.
     * This is not a part of an iCalendar VComponent.
     */
    void setEndRange(Temporal end);

    /**
     * Returns the collection of recurrence instances of calendar component of type T that exists
     * between dateTimeRangeStart and dateTimeRangeEnd based on VComponent.
     * Recurrence set is defined in RFC 5545 iCalendar page 121 as follows 
     * "The recurrence set is the complete set of recurrence instances for a calendar component.  
     * The recurrence set is generated by considering the initial "DTSTART" property along with
     * the "RRULE", "RDATE", and "EXDATE" properties contained within the recurring component."
     *  
     * @param start - beginning of time frame to make instances
     * @param end - end of time frame to make instances
     * @return
     */
    Collection<T> makeInstances(Temporal start, Temporal end);
    /**
     * Returns the collection of recurrence instances of calendar component of type T that exists
     * between dateTimeRangeStart and dateTimeRangeEnd based on VComponent.
     * Recurrence set is defined in RFC 5545 iCalendar page 121 as follows 
     * "The recurrence set is the complete set of recurrence instances for a calendar component.  
     * The recurrence set is generated by considering the initial "DTSTART" property along with
     * the "RRULE", "RDATE", and "EXDATE" properties contained within the recurring component."
     * 
     * Uses start and end values from a previous call to makeInstances(Temporal start, Temporal end)
     * If there are no start and end values an exception is thrown.
     *  
     * @return
     */
    Collection<T> makeInstances();
    /**
     * Returns existing instances in the Recurrence Set (defined in RFC 5545 iCalendar page 121)
     * made by the last call of makeRecurrenceSet
     * @param <T> type of recurrence instance, such as an appointment implementation
     * 
     * @return - current instances of the Recurrence Set
     * @see makeRecurrenceSet
     */
    Collection<T> instances();
    
    /**
     * returns string of all line-separated properties for calendar component.
     * 
     * Example:<br>
     * BEGIN:VEVENT<br>
     * DTSTART;TZID=America/Los_Angeles:20160214T080000<br>
     * DTEND;TZID=America/Los_Angeles:20160214T110000<br>
     * RRULE:FREQ=WEEKLY;BYDAY=SU,TU,FR<br>
     * DTSTAMP:20160214T022532Z<br>
     * UID:im8hmpakeigu3d85j3vq9q8bcc@google.com<br>
     * CREATED:20160214T022525Z<br>
     * LAST-MODIFIED:20160214T022525Z<br>
     * SUMMARY:test2<br>
     * END:VEVENT
     * 
     */
    String toComponentText();
    
    /**
     * Handles how an edited VComponent is processed.  For a VComponent with a recurrence rule (RRULE)
     * the user is given a dialog to select ONE, THIS_AND_FUTURE, or ALL instances to edit.
     * For a VComponent without a RRULE there is no dialog.
     * 
     * @param vComponentOriginal - copy of this VComponent before changes
     * @param vComponents - the collection of VComponents that this VComponent belongs to
     * @param startOriginalInstance - date or date/time of selected instance before changes
     * @param startInstance - date or date/time of selected instance after changes
     * @param endInstance - date or date/time of selected instance after changes
     * @param instances - all instances being rendered by all VComponents
     * @param dialogCallback - callback to generate dialog to select ONE, THIS_AND_FUTURE, or ALL.
     *    Can be replaced by a stub for testing (e.g. (m) -> ChangeDialogOption.ALL).
     */
    void handleEdit(
            VComponent<T> vComponentOriginal
          , Collection<VComponent<T>> vComponents
          , Temporal startOriginalInstance
          , Temporal startInstance
          , Temporal endInstance
          , Collection<T> instances
          , Callback<Map<ChangeDialogOption, String>, ChangeDialogOption> dialogCallback);
    
    /**
     * Deletes a VComponent.  For a VComponent with a recurrence rule (RRULE) the user is given a dialog
     * to select ONE, THIS_AND_FUTURE, or ALL instances to delete.
     * 
     * @param vComponents - main collection of all VComponents
     * @param startInstance - start date or date/time of instance
     * @param instance - selected recurrence instance
     * @param instances - collection of all instances across all VComponents
     */
    // TODO - ADD DIALOG CALLBACK HERE
    void handleDelete(
            Collection<VComponent<T>> vComponents
          , Temporal startInstance
          , T instance
          , Collection<T> instances);
    
    /**
     * Copies this object into destination object
     * 
     * @param destination
     */
    void copyTo(VComponent<T> destination);
    
    // DEFAULT METHODS
    
    /**
     * Checks to see if object contains required properties.  Returns empty string if it is
     * valid.  Returns string of errors if not valid.
     */
    default String makeErrorString()
    {
        StringBuilder errorsBuilder = new StringBuilder();
        if (getDateTimeStart() == null) errorsBuilder.append(System.lineSeparator() + "Invalid VComponent.  DTSTART must not be null.");
        if (getDateTimeStamp() == null) errorsBuilder.append(System.lineSeparator() + "Invalid VComponent.  DTSTAMP must not be null.");
        if (getUniqueIdentifier() == null) errorsBuilder.append(System.lineSeparator() + "Invalid VComponent.  UID must not be null.");
        if (getRRule() != null)
        {
            errorsBuilder.append(getRRule().makeErrorString(this));
            /*
             * RFC 5545, page 41
             * If the "DTSTART" property is specified as a date with UTC
             * time or a date with local time and time zone reference, then the
             * UNTIL rule part MUST be specified as a date with UTC time.
             */
            if (getRRule().getUntil() != null)
            {
                if ((getDateTimeStart() instanceof ZonedDateTime))
                {
                    if ((getRRule().getUntil() instanceof ZonedDateTime))
                    {
                        if (! ((ZonedDateTime) getRRule().getUntil()).getOffset().equals(ZoneOffset.UTC))
                        {
                            errorsBuilder.append(System.lineSeparator() + "Invalid RRule: UNTIL must be ZonedDateTime with UTC if DTSTART (dateTimeStart) is ZonedDateTime");
                        }
                    } else
                    {
                        errorsBuilder.append(System.lineSeparator() + "Invalid RRule: UNTIL must be ZonedDateTime if DTSTART (dateTimeStart) is ZonedDateTime");
                    }
                }
            }
        }
        Temporal t1 = stream(getDateTimeStart()).findFirst().get();
        final Temporal first;
        if (getExDate() != null)
        {
            Temporal t2 = Collections.min(getExDate().getTemporals(), VComponent.TEMPORAL_COMPARATOR);
            first = (VComponent.isBefore(t1, t2)) ? t1 : t2;
        } else
        {
            first = t1;
        }           
        if (! first.equals(getDateTimeStart())) errorsBuilder.append(System.lineSeparator() + "Invalid VComponent.  DTSTART (" + getDateTimeStart() + ") must be first occurrence (" + first + ")");
        Class<? extends Temporal> startClass = first.getClass();
        Class<? extends Temporal> untilClass = ((getRRule() != null) && (getRRule().getUntil() != null))
                ? getRRule().getUntil().getClass() : startClass;
        Class<? extends Temporal> eXDateClass = (getExDate() != null) ? getExDate().temporalClass() : startClass;
        Class<? extends Temporal> rDateClass = (getRDate() != null) ? getRDate().temporalClass() : startClass;
        if (startClass != untilClass) errorsBuilder.append(System.lineSeparator() + "Invalid VComponent.  Temporal class type of DTSTART (" + startClass + ") and UNTIL (" + untilClass + ") must be the same.");
        if (startClass != eXDateClass) errorsBuilder.append(System.lineSeparator() + "Invalid VComponent.  Temporal class type of DTSTART (" + startClass + ") and EXDATE (" + eXDateClass + ") must be the same.");
        if (startClass != rDateClass) errorsBuilder.append(System.lineSeparator() + "Invalid VComponent.  Temporal class type of DTSTART (" + startClass + ") and RDATE (" + rDateClass + ") must be the same.");
        
        return errorsBuilder.toString();
    }
    
    /** Returns true if VComponent is an individual (only one instance in recurrence set),
     *  false if has more than 1 instance */
    default boolean isIndividual()
    {
        Iterator<Temporal> i = stream(getDateTimeStart()).iterator();
        if (i.hasNext())
        {
            i.next();
            if (i.hasNext()) return false; // has at least two elements
            else return true; // has only one element
        } else throw new RuntimeException("VComponent stream has no elements");
    }
    
    /** Returns true if VComponent has zero instances in recurrence set */
    default boolean isRecurrenceSetEmpty()
    {
        Iterator<Temporal> i = stream(getDateTimeStart()).iterator();
        return ! i.hasNext();
    }
    
    /**
     * Make easy-to-read date range string
     * 
     * For generating a string representing the whole segment the start parameter should be
     * DTSTART.  For a this-and-future representation start should be the start of the
     * selected instance.
     * 
     * If VComponent is part of a multi-part series only this segment is considered.
     * Example: Dec 5, 2015 - Feb 6, 2016
     *          Nov 12, 2015 - forever
     *          
     * return String representing start and end of VComponent.
     */
    default String rangeToString(Temporal start)
    {
        Temporal lastStart = lastStartTemporal();
        if (start.equals(lastStart)) return temporalToStringPretty(start); // individual            
        else if (lastStart == null) return temporalToStringPretty(start) + " - forever"; // infinite
        else return temporalToStringPretty(start) + " - " + Settings.DATE_FORMAT_AGENDA_EXCEPTION_DATEONLY.format(lastStart); // has finite range (use only date for end)
    }
    /**
     * Make easy-to-read date range string
     * Uses DTSTART as start.
     *      
     * If VComponent is part of a multi-part series only this segment is considered.
     * Example: Dec 5, 2015 - Feb 6, 2016
     *          Nov 12, 2015 - forever
     *          
     * return String representing start and end of VComponent.
     */
 // TODO - PUT THESE METHODS ELSEWHERE - NOT NEEDED BY ICALENDAR - USED BY ICALENDARAGENDA???
    default String rangeToString() { return rangeToString(getDateTimeStart()); }

    /** returns the last date or date/time of the series.  If infinite returns null */
    default Temporal lastStartTemporal()
    {
        if (getRRule() != null)
        {
            if ((getRRule().getCount() == 0) && (getRRule().getUntil() == null))
            {
                return null; // infinite
            }
            else
            { // finite (find end)
                List<Temporal> instances = stream(getDateTimeStart()).collect(Collectors.toList());
                return instances.get(instances.size()-1);
            }
        } else if (getRDate() != null)
        { // has RDATE list finite (find end)
            List<Temporal> instances = stream(getDateTimeStart()).collect(Collectors.toList());
            return instances.get(instances.size()-1);
        } else return getDateTimeStart(); // individual            
    }
    
    /*
     * UTILITY METHODS
     * 
     * Below methods are used to handle dateTimeEnd and dateTimeStart as Temporal objects.  LocalDate
     * objects are compared at start-of-day.
     */
    
    final static Comparator<Temporal> TEMPORAL_COMPARATOR = (t1, t2) -> 
    {
        LocalDateTime ld1 = (t1.isSupported(ChronoUnit.NANOS)) ? LocalDateTime.from(t1) : LocalDate.from(t1).atStartOfDay();
        LocalDateTime ld2 = (t2.isSupported(ChronoUnit.NANOS)) ? LocalDateTime.from(t2) : LocalDate.from(t2).atStartOfDay();
        return ld1.compareTo(ld2);
    };
    
    /**
     * Sorts VComponents by DTSTART date/time
     */
    final static Comparator<? super VComponent<?>> VCOMPONENT_COMPARATOR = (v1, v2) -> 
    {
        Temporal t1 = v1.getDateTimeStart();
        LocalDateTime ld1 = (t1.isSupported(ChronoUnit.NANOS)) ? LocalDateTime.from(t1) : LocalDate.from(t1).atStartOfDay();
        Temporal t2 = v2.getDateTimeStart();
        LocalDateTime ld2 = (t2.isSupported(ChronoUnit.NANOS)) ? LocalDateTime.from(t2) : LocalDate.from(t2).atStartOfDay();
        return ld1.compareTo(ld2);
    };
    
    /** Parse iCalendar date or date/time string into LocalDate, LocalDateTime or ZonedDateTime for following formats:
     * FORM #1: DATE WITH LOCAL TIME e.g. 19980118T230000 (LocalDateTime)
     * FORM #2: DATE WITH UTC TIME e.g. 19980119T070000Z (ZonedDateTime)
     * FORM #3: DATE WITH LOCAL TIME AND TIME ZONE REFERENCE e.g. TZID=America/New_York:19980119T020000 (ZonedDateTime)
     * FORM #4: DATE ONLY e.g. VALUE=DATE:19970304 (LocalDate)
     * 
     * Note: strings can contain optionally contain "VALUE" "=" ("DATE-TIME" / "DATE")) before the date-time portion of the string.
     * e.g. VALUE=DATE:19960401         VALUE=DATE-TIME:19980101T050000Z
     * 
     * Based on ISO.8601.2004
     */
    static Temporal parseTemporal(String temporalString)
    {
        final String form1 = "^[0-9]{8}T([0-9]{6})";
        final String form2 = "^[0-9]{8}T([0-9]{6})Z";
        final String form3 = "^(TZID=.*:)[0-9]{8}T([0-9]{6})";
        final String form4 = "^(VALUE=DATE:)?[0-9]{8}";
        if (temporalString.matches("^VALUE=DATE-TIME:.*")) // remove optional VALUE=DATE-TIME
        {
            temporalString = temporalString.substring(temporalString.indexOf("VALUE=DATE-TIME:")+"VALUE=DATE-TIME:".length()).trim();
        }
        if (temporalString.matches(form1))
        {
            return LocalDateTime.parse(temporalString, LOCAL_DATE_TIME_FORMATTER);
        } else if (temporalString.matches(form2))
        {
            return ZonedDateTime.parse(temporalString, ZONED_DATE_TIME_UTC_FORMATTER);
        } else if (temporalString.matches(form3))
        {
            return ZonedDateTime.parse(temporalString, ZONED_DATE_TIME_FORMATTER);            
        } else if (temporalString.matches(form4))
        {
            if (temporalString.matches("^VALUE=DATE:.*"))
            {
                temporalString = temporalString.substring(temporalString.indexOf("VALUE=DATE:")+"VALUE=DATE:".length()).trim();
            }
            return LocalDate.parse(temporalString, LOCAL_DATE_FORMATTER);
        } else
        {
            throw new IllegalArgumentException("String does not match any DATE or DATE-TIME patterns: " + temporalString);
        }
    }
    
    /**
     * Convert temporal, either LocalDate or LocalDateTime to appropriate iCalendar string
     * Examples:
     * 19980119T020000
     * 19980119
     * 
     * @param temporal LocalDate or LocalDateTime
     * @return iCalendar date or date/time string based on ISO.8601.2004
     */
    static String temporalToString(Temporal temporal)
    {
        if (temporal == null) return null;
        if (temporal instanceof ZonedDateTime)
        {
            ZoneOffset offset = ((ZonedDateTime) temporal).getOffset();
            if (offset == ZoneOffset.UTC)
            {
                return ZONED_DATE_TIME_UTC_FORMATTER.format(temporal);
            } else
            {
                return LOCAL_DATE_TIME_FORMATTER.format(temporal); // don't use ZONED_DATE_TIME_FORMATTER because time zone is added to property tag
            }
        } else if (temporal instanceof LocalDateTime)
        {
            return LOCAL_DATE_TIME_FORMATTER.format(temporal);
        } else if (temporal instanceof LocalDate)
        {
            return LOCAL_DATE_FORMATTER.format(temporal);
        } else
        {
            throw new DateTimeException("Invalid temporal type:" + temporal.getClass().getSimpleName());
        }
    }
    
    /** formats by either LocalDate or LocalDateTime Temporal to an easy-to-read format
     * Example: Dec 5, 2015 - Feb 6, 2016
     *          Nov 12, 2015 - forever
     */
    static String temporalToStringPretty(Temporal temporal)
    {
        if (temporal.isSupported(ChronoUnit.NANOS))
        {
            return Settings.DATE_FORMAT_AGENDA_START.format(temporal);
        } else
        {
            return Settings.DATE_FORMAT_AGENDA_EXCEPTION_DATEONLY.format(temporal);
        }
    };
    
    /**
     * Returns LocalDateTime from Temporal that is an instance of either LocalDate or LocalDateTime
     * If the parameter is type LocalDate the returned LocalDateTime is atStartofDay.
     * If the parameter is type ZonedDateTime the zoneID is changed to ZoneId.systemDefault() before taking the
     * LocalDateTime.
     * 
     * @param temporal - either LocalDate or LocalDateTime type
     * @return LocalDateTime
     */
    static LocalDateTime localDateTimeFromTemporal(Temporal temporal)
    {
        if (temporal == null) return null;
        if (temporal.isSupported(ChronoUnit.NANOS))
        {
            if (temporal instanceof ZonedDateTime)
            {
                ZonedDateTime z = ((ZonedDateTime) temporal).withZoneSameInstant(ZoneId.systemDefault());
                return LocalDateTime.from(z);                
            } else if (temporal instanceof LocalDateTime)
            {
                return LocalDateTime.from(temporal);                
            } else throw new DateTimeException("Invalid temporal type:" + temporal.getClass().getSimpleName());
        } else
        {
            return LocalDate.from(temporal).atStartOfDay();
        }
    }
    
    /** Determines if Temporal is before t2
     * Works for LocalDate or LocalDateTime
     * 
     * @param t1 first Temporal
     * @param t2 second Temporal (to compare with t1)
     * @return true if t1 is before t2, false otherwise
     */
    static boolean isBefore(Temporal t1, Temporal t2)
    {
        if (t1.getClass().equals(t2.getClass()))
        {
            LocalDateTime d1 = localDateTimeFromTemporal(t1);
            LocalDateTime d2 = localDateTimeFromTemporal(t2);
            return d1.isBefore(d2);
        } throw new DateTimeException("For comparision, Temporal classes must be equal (" + t1.getClass().getSimpleName() + ", " + t2.getClass().getSimpleName() + ")");
    }

    /** Determines if Temporal is after t2
     * Works for LocalDate or LocalDateTime
     * 
     * @param t1 first Temporal
     * @param t2 second Temporal (to compare with t1)
     * @return true if t1 is after t2, false otherwise
     */
    static boolean isAfter(Temporal t1, Temporal t2)
    {
        if (t1.getClass().equals(t2.getClass()))
        {
            LocalDateTime d1 = localDateTimeFromTemporal(t1);
            LocalDateTime d2 = localDateTimeFromTemporal(t2);
            return d1.isAfter(d2);
        } throw new DateTimeException("For comparision, Temporal classes must be equal (" + t1.getClass().getSimpleName() + ", " + t2.getClass().getSimpleName() + ")");
    }
    
//    /**
//     * Add value in chronoUnit to temporal
//     * Automatically converts nanos to days if temporal is LocalDate.
//     * 
//     * @param dateTimeStart
//     * @param startShiftInNanos
//     * @param days
//     */
//    @Deprecated
//    static Temporal addNanos(Temporal temporal, long nanos)
//    {
//        if (temporal.isSupported(ChronoUnit.NANOS))
//        {
//            return temporal.plus(nanos, ChronoUnit.NANOS);            
//        } else
//        {
//            return temporal.plus(nanos/NANOS_IN_DAY, ChronoUnit.DAYS);            
//        }
//    }

    /**
     * Return list of all related VComponents that make up entire recurrence set.
     * List also contains all VComponents that share the same UID.
     * 
     * Used to edit or delete entire recurrence set.
     * 
     * @param vComponents : collection of all VComponents
     * @param vComponent : VComponent to match to parent, children and branches
     * @return
     */
    static <U> List<VComponent<U>> findRelatedVComponents(Collection<VComponent<U>> vComponents, VComponent<U> vComponent)
    {
        String uid = vComponent.getUniqueIdentifier();
        return vComponents.stream()
                .filter(v -> v.getUniqueIdentifier().equals(uid))
                .collect(Collectors.toList());
    }

    
    /**
     * Return list of all related VComponents that make up entire recurrence set.
     * List also contains input parameter vComponent, parent, children,
     * or branches.
     * 
     * Used to edit or delete entire recurrence set.
     * 
     * @param vComponents : collection of all VComponents
     * @param vComponent : VComponent to match to parent, children and branches
     * @return
     */
    @Deprecated // matched by relatedTo - too inclusive.  Will be probably deleted in future.
    static <U> List<VComponent<U>> findRelatedVComponents2(Collection<VComponent<U>> vComponents, VComponent<U> vComponent)
    {
        final String uid = (vComponent.getRelatedTo() != null) ? vComponent.getRelatedTo() : vComponent.getUniqueIdentifier();
        System.out.println("uid:" + uid + " " + vComponents.size());

        return vComponents.stream()
//                .forEach(System.out::println);
                .filter( v ->
                {
                    boolean isChild = (v.getUniqueIdentifier() != null) ? v.getUniqueIdentifier().equals(uid) : false;
                    boolean isBranch2 = (v.getRelatedTo() != null) ? v.getRelatedTo().equals(uid) : false;
                    return isChild || isBranch2;
                })
                .sorted(VCOMPONENT_COMPARATOR)
                .collect(Collectors.toList());
    }

    // TODO - PROBABLY THESE METHODS SHOULD GO TO ICALENDARUTILITIES
    /**
     * Makes easy-to-read string of date range for the VComponents
     * For ALL edit option (one VComponent)
     * 
     * @param vComponent
     * @return - easy-to-read string of date range for the VComponents
     */
    static <U> String rangeToString(VComponent<U> vComponent)
    {
        return rangeToString(Arrays.asList(vComponent));
    }
    /**
     * Makes easy-to-read string of date range for the VComponents
     * Beginning of range is parameter start
     * For ALL edit option (one VComponent)
     * 
     * @param vComponent
     * @param start - Temporal start date or date/time
     * @return - easy-to-read string of date range for the VComponents
     */
    static <U> String rangeToString(VComponent<U> vComponent, Temporal start)
    {
        return rangeToString(Arrays.asList(vComponent), start);
    }
    /**
     * For ALL edit option (list of VComponents)
     * 
     * @param relatives - list of all related VComponents
     * @return - easy-to-read string of date range for the VComponents
     */
    static <U> String rangeToString(Collection<VComponent<U>> relatives)
    {
        return rangeToString(relatives, null);
    }
    /**
     * For THIS_AND_FUTURE_ALL edit option
     * 
     * @param relatives - list of all related VComponents
     * @param start - Temporal start date or date/time
     * @return - easy-to-read string of date range for the VComponents
     */
    static <U> String rangeToString(Collection<VComponent<U>> relatives, Temporal start)
    {
        if (relatives.size() == 0) return null;
        Iterator<VComponent<U>> i = relatives.iterator();
        VComponent<U> v1 = i.next();
        Temporal start2 = (start == null) ? v1.getDateTimeStart() : start; // set initial start
        Temporal end = v1.lastStartTemporal();
        if (i.hasNext())
        {
            VComponent<U> v = i.next();
            if (start != null) start2 = (isBefore(v.getDateTimeStart(), start2)) ? v.getDateTimeStart() : start2;
            if (end != null) // null means infinite
            {
                Temporal myEnd = v.lastStartTemporal();
                if (myEnd == null) end = null;
                else end = (isAfter(myEnd, end)) ? v.lastStartTemporal() : end;
            }
        }
        if (start2.equals(end)) return temporalToStringPretty(start2); // individual            
        else if (end == null) return temporalToStringPretty(start2) + " - forever"; // infinite
        else return temporalToStringPretty(start2) + " - " + Settings.DATE_FORMAT_AGENDA_EXCEPTION_DATEONLY.format(end); // has finite range (only returns date for end of range)
    }
    
    /**
     * Counts number of instances in recurrence set.  returns -1 for infinite.
     * Recurrence set of collection of VComponents making up a series.
     * 
     * @param relatives
     * @return
     */
    static <U> int countVComponents(Collection<VComponent<U>> relatives)
    {        
        int count=0;
        Iterator<VComponent<U>> i = relatives.iterator();
        while (i.hasNext())
        {
            VComponent<U> v = i.next();
            if (v.getRRule() == null && v.getRDate() == null) count++; // individual
            else if ((v.getRRule().getUntil() == null) && (v.getRRule().getCount() == 0)) count = -1; // infinite
            else count += v.getRRule().stream(v.getDateTimeStart()).count();
            if (count == -1) break;
        }
        if (count == 0) throw new RuntimeException("Invalid VComponent: no instances in recurrence set");
        return count;
    }

}