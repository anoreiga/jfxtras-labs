package jfxtras.labs.repeatagenda.scene.control.repeatagenda.icalendar;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.SetChangeListener;
import javafx.util.Callback;
import jfxtras.labs.repeatagenda.internal.scene.control.skin.repeatagenda.base24hour.DeleteChoiceDialog;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.ICalendarAgendaUtilities;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.ICalendarAgendaUtilities.ChangeDialogOption;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.ICalendarAgendaUtilities.RRuleType;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.Settings;
import jfxtras.labs.repeatagenda.scene.control.repeatagenda.icalendar.rrule.RRule;

/**
 * Abstract implementation of VComponent with all common methods for VEvent, VTodo, and VJournal
 * 
 * @author David Bal
 * @see VEvent
 *
 * @param <T> - recurrence instance type
 */
public abstract class VComponentBaseAbstract<T> implements VComponent<T>
{
    /**
     * CATEGORIES: RFC 5545 iCalendar 3.8.1.12. page 81
     * This property defines the categories for a calendar component.
     * Example:
     * CATEGORIES:APPOINTMENT,EDUCATION
     * CATEGORIES:MEETING
     */
    // TODO - NEED TO ACCEPT MULTIPLE CATEGORIES - CHANGE TO OBSERVABLE LIST OR SET OR USE COMMA-DELIMATED STRING - NEED TO PUT BOX AROUND APPOINTMENT GROUP FOR THE SELECTED ONE, BUT MULTIPLE CHECKS ARE ALLOWED
    @Override public StringProperty categoriesProperty() { return categoriesProperty; }
    final private StringProperty categoriesProperty = new SimpleStringProperty(this, VComponentProperty.CATEGORIES.toString());
    @Override public String getCategories() { return categoriesProperty.get(); }
    @Override public void setCategories(String value) { categoriesProperty.set(value); }
    
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
    @Override
    public StringProperty commentProperty()
    {
        if (comment == null) comment = new SimpleStringProperty(this, VComponentProperty.COMMENT.toString(), _comment);
        return comment;
    }
    private StringProperty comment;
    private String _comment;
    @Override
    public String getComment() { return (comment == null) ? _comment : comment.get(); }
    @Override
    public void setComment(String comment)
    {
        if (this.comment == null)
        {
            _comment = comment;
        } else
        {
            this.comment.set(comment);            
        }
    }
//    public VEvent withComment(String value) { setComment(value); return this; }

    /**
     * CREATED: Date-Time Created, from RFC 5545 iCalendar 3.8.7.1 page 136
     * This property specifies the date and time that the calendar information was created.
     * This is analogous to the creation date and time for a file in the file system.
     */
    @Override
    public ObjectProperty<ZonedDateTime> dateTimeCreatedProperty() { return dateTimeCreated; }
    final private ObjectProperty<ZonedDateTime> dateTimeCreated = new SimpleObjectProperty<>(this, VComponentProperty.CREATED.toString());
    @Override
    public ZonedDateTime getDateTimeCreated() { return dateTimeCreated.get(); }
    @Override
    public void setDateTimeCreated(ZonedDateTime dtCreated)
    {
        if ((dtCreated != null) && ! (dtCreated.getOffset().equals(ZoneOffset.UTC)))
        {
            throw new DateTimeException("dateTimeStamp (DTSTAMP) must be specified in the UTC time format (Z)");
        }
        this.dateTimeCreated.set(dtCreated);
    }
    
    /**
     * DTSTAMP: Date-Time Stamp, from RFC 5545 iCalendar 3.8.7.2 page 137
     * This property specifies the date and time that the instance of the
     * iCalendar object was created
     */
    @Override
    public ObjectProperty<ZonedDateTime> dateTimeStampProperty() { return dateTimeStamp; }
    final private ObjectProperty<ZonedDateTime> dateTimeStamp = new SimpleObjectProperty<>(this, VComponentProperty.DATE_TIME_STAMP.toString());
    @Override
    public ZonedDateTime getDateTimeStamp() { return dateTimeStamp.get(); }
    @Override
    public void setDateTimeStamp(ZonedDateTime dtStamp)
    {
        if ((dtStamp != null) && ! (dtStamp.getOffset().equals(ZoneOffset.UTC)))
        {
            throw new DateTimeException("dateTimeStamp (DTSTAMP) must be specified in the UTC time format (Z)");
        }
        this.dateTimeStamp.set(dtStamp);
    }
    
    /* DTSTART temporal class and ZoneId
     * 
     * Used to ensure the following date-time properties use the same Temporal class
     * and ZoneId (if using ZonedDateTime, null otherwise)
     * DTEND
     * RECURRENCE-ID
     * EXDATE (underlying collection of Temporals)
     * RDATE (underlying collection of Temporals)
     */
    private DateTimeType dtStartDateTimeType;
    DateTimeType dtStartDateTimeType() { return dtStartDateTimeType; }
    
    /**
     * DTSTART: Date-Time Start, from RFC 5545 iCalendar 3.8.2.4 page 97
     * Start date/time of repeat rule.  Used as a starting point for making the Stream<LocalDateTime> of valid
     * start date/times of the repeating events.
     * Can contain either a LocalDate (DATE) or LocalDateTime (DATE-TIME)
     * @SEE VDateTime
     */
    @Override
    public ObjectProperty<Temporal> dateTimeStartProperty() { return dateTimeStart; }
    final private ObjectProperty<Temporal> dateTimeStart = new SimpleObjectProperty<>(this, VComponentProperty.DATE_TIME_START.toString());
    @Override public Temporal getDateTimeStart() { return dateTimeStart.get(); }
    @Override
    public void setDateTimeStart(Temporal dtStart)
    {
        // check Temporal class is LocalDate, LocalDateTime or ZonedDateTime - others are not supported
        DateTimeType myDateTimeType = DateTimeType.dateTimeTypeFromTemporal(dtStart);
        boolean changed = (dtStartDateTimeType != null) && (myDateTimeType != dtStartDateTimeType);
        dtStartDateTimeType = myDateTimeType;
        dateTimeStart.set(dtStart);
        
        // if type has changed then make all date-time properties the same
        if (changed)
        {
            ensureTemporalTypeConsistency(myDateTimeType);
        }
    }
    
    /**
     * Changes Temporal type of some properties to match the input parameter.  The input
     * parameter should be based on the DTSTART property.
     * 
     * @param dateTimeType
     */
    void ensureTemporalTypeConsistency(DateTimeType dateTimeType)
    {
        // RECURRENCE-ID
        if ((getDateTimeRecurrence() != null) && (dateTimeType != DateTimeType.dateTimeTypeFromTemporal(getDateTimeRecurrence())))
        {
            // convert to new Temporal type
            Temporal newDateTimeRecurrence = DateTimeType.changeDateTimeType(getDateTimeRecurrence(), dateTimeType);
            setDateTimeRecurrence(newDateTimeRecurrence);
        }
        
        // EXDATE
        if (getExDate() != null)
        {
            Temporal firstTemporal = getExDate().getTemporals().iterator().next();
            DateTimeType exDateDateTimeType = DateTimeType.dateTimeTypeFromTemporal(firstTemporal);
            if (dateTimeType != exDateDateTimeType)
            {
                Set<Temporal> newExDateTemporals = getExDate().getTemporals()
                        .stream()
                        .map(t -> DateTimeType.changeDateTimeType(t, dateTimeType))
                        .collect(Collectors.toSet());
                getExDate().getTemporals().clear();
                getExDate().getTemporals().addAll(newExDateTemporals);
            }
        }
        
        // RDATE
        if (getRDate() != null)
        {
            Temporal firstTemporal = getRDate().getTemporals().iterator().next();
            DateTimeType rDateDateTimeType = DateTimeType.dateTimeTypeFromTemporal(firstTemporal);
            if (dateTimeType != rDateDateTimeType)
            {
                Set<Temporal> newRDateTemporals = getRDate().getTemporals()
                        .stream()
                        .map(t -> DateTimeType.changeDateTimeType(t, dateTimeType))
                        .collect(Collectors.toSet());
                getRDate().getTemporals().clear();
                getRDate().getTemporals().addAll(newRDateTemporals);
            }
        }
    }
    
    // Listener for EXDATE and RDATE - checks if added Temporals match DTSTART type
    private final SetChangeListener<? super Temporal> recurrenceListener = (SetChangeListener<? super Temporal>) (SetChangeListener.Change<? extends Temporal> change) ->
    {
        if (change.wasAdded())
        {
            Temporal newTemporal = change.getElementAdded();
            DateTimeType myDateTimeType = DateTimeType.dateTimeTypeFromTemporal(newTemporal);
            if ((dtStartDateTimeType() != null) && (myDateTimeType != dtStartDateTimeType()))
            {
                throw new DateTimeException("Temporal must have the same DateTimeType as DTSTART, (" + myDateTimeType + ", " + dtStartDateTimeType() + ", respectively");
            }
        }
    };
    
    /**
     * EXDATE: Set of date/times exceptions for recurring events, to-dos, journal entries.
     * 3.8.5.1, RFC 5545 iCalendar
     * Is rarely used, so employs lazy initialization.
     */
    @Override
    public ObjectProperty<ExDate> exDateProperty()
    {
        if (exDate == null) exDate = new SimpleObjectProperty<>(this, VComponentProperty.EXCEPTIONS.toString(), _exDate);
        return exDate;
    }
    private ObjectProperty<ExDate> exDate;
    private ExDate _exDate;
    @Override
    public ExDate getExDate() { return (exDate == null) ? _exDate : exDate.getValue(); }
    @Override
    public void setExDate(ExDate exDate)
    {
        if (this.exDate == null)
        {
            _exDate = exDate;
        } else
        {
            this.exDate.set(exDate);
            // ensure Temporals added to ExDate are the same as DTSTART
            exDate.getTemporals().removeListener(recurrenceListener);
            exDate.getTemporals().addListener(recurrenceListener);
        }
    }
    /** true = put all Temporals on one line, false = use one line for each Temporal */
    @Override
    public boolean isExDatesOnOneLine() { return exDatesOnOneLine; }
    private boolean exDatesOnOneLine = false;
    /** true = put all Temporals on one line, false = use one line for each Temporal */
    public void setExDatesOnOneLine(boolean b) { exDatesOnOneLine = b; }
    
    /**
     * LAST-MODIFIED: Date-Time Last Modified, from RFC 5545 iCalendar 3.8.7.3 page 138
     * This property specifies the date and time that the information associated with
     * the calendar component was last revised.
     * 
     * The property value MUST be specified in the UTC time format.
     */
    @Override
    public ObjectProperty<ZonedDateTime> dateTimeLastModifiedProperty() { return dateTimeLastModified; }
    final private ObjectProperty<ZonedDateTime> dateTimeLastModified = new SimpleObjectProperty<ZonedDateTime>(this, VComponentProperty.LAST_MODIFIED.toString());
    @Override
    public ZonedDateTime getDateTimeLastModified() { return dateTimeLastModified.getValue(); }
    @Override
    public void setDateTimeLastModified(ZonedDateTime dtLastModified)
    {
        if (! dtLastModified.getOffset().equals(ZoneOffset.UTC))
        {
            throw new DateTimeException("dateTimeStamp (DTSTAMP) must be specified in the UTC time format (Z)");
        }
        this.dateTimeLastModified.set(dtLastModified);
    }

    /**
     *  ORGANIZER: RFC 5545 iCalendar 3.8.4.3. page 111
     * This property defines the organizer for a calendar component
     * Example:
     * ORGANIZER;CN=John Smith:mailto:jsmith@example.com
     * 
     * The property is stored as a simple string.  The implementation is
     * responsible to extract any contained data elements such as CN, DIR, SENT-BY
     * */
    @Override
    public StringProperty organizerProperty()
    {
        if (organizer == null) organizer = new SimpleStringProperty(this, VComponentProperty.ORGANIZER.toString(), _organizer);
        return organizer;
    }
    private StringProperty organizer;
    private String _organizer;
    @Override
    public String getOrganizer() { return (organizer == null) ? _organizer : organizer.get(); }
    @Override
    public void setOrganizer(String organizer)
    {
        if (this.organizer == null)
        {
            _organizer = organizer;
        } else
        {
            this.organizer.set(organizer);            
        }
    }    

    /**
     * RDATE: Set of date/times for recurring events, to-dos, journal entries.
     * 3.8.5.2, RFC 5545 iCalendar
    */
    // Is rarely used, so employs lazy initialization.
    @Override
    public ObjectProperty<RDate> rDateProperty()
    {
        if (rDate == null) rDate = new SimpleObjectProperty<RDate>(this, VComponentProperty.RECURRENCES.toString(), _rDate);
        return rDate;
    }
    private ObjectProperty<RDate> rDate;
    private RDate _rDate;
    @Override
    public RDate getRDate() { return (rDate == null) ? _rDate : rDate.getValue(); }
    @Override
    public void setRDate(RDate rDate)
    {
        if (this.rDate == null)
        {
            _rDate = rDate;
        } else
        {
            this.rDate.set(rDate);
            // ensure Temporals added to RDate are the same as DTSTART
            rDate.getTemporals().removeListener(recurrenceListener);
            rDate.getTemporals().addListener(recurrenceListener);
        }
    }

    @Override
    public StringProperty relatedToProperty() { return relatedTo; }
    final private StringProperty relatedTo = new SimpleStringProperty(this, VComponentProperty.RELATED_TO.toString());
    @Override
    public String getRelatedTo() { return relatedTo.getValue(); }
    @Override
    public void setRelatedTo(String s) { relatedTo.set(s); }
    
    // TODO - I may implement Google UID recurrence for segments of recurrence set
    /**
     * Use Google UID extension instead of RELATED-TO to express 
     */
    @Override
    public boolean isGoogleRecurrenceUID() { return googleRecurrenceUID; };
    private boolean googleRecurrenceUID = false; // default to not using Google system
    @Override
    public void setGoogleRecurrenceUID(boolean b) { googleRecurrenceUID = b; };
    
    /**
     * RECURRENCE-ID: Date-Time recurrence, from RFC 5545 iCalendar 3.8.4.4 page 112
     * The property value is the original value of the "DTSTART" property of the 
     * recurrence instance.
     */
    @Override
    public ObjectProperty<Temporal> dateTimeRecurrenceProperty()
    {
        if (dateTimeRecurrence == null) dateTimeRecurrence = new SimpleObjectProperty<>(this, VComponentProperty.RECURRENCE_ID.toString(), _dateTimeRecurrence);
        return dateTimeRecurrence;
    }
    private ObjectProperty<Temporal> dateTimeRecurrence;
    private Temporal _dateTimeRecurrence;
    @Override public Temporal getDateTimeRecurrence()
    {
        return (dateTimeRecurrence == null) ? _dateTimeRecurrence : dateTimeRecurrence.get();
    }
    @Override public void setDateTimeRecurrence(Temporal dtRecurrence)
    {
        DateTimeType myDateTimeType = DateTimeType.dateTimeTypeFromTemporal(dtRecurrence);
        if (myDateTimeType != dtStartDateTimeType())
        {
            throw new DateTimeException("RECURRENCE-ID must have the same DateTimeType as DTSTART, (" + myDateTimeType + ", " + dtStartDateTimeType() + ", respectively");
        }

        if (dateTimeRecurrence == null)
        {
            _dateTimeRecurrence = dtRecurrence;
        } else
        {
            dateTimeRecurrence.set(dtRecurrence);
        }
    }
    
    @Override
    public VComponent<T> getParent() { return parent; }
    private VComponent<T> parent;
    @Override
    public void setParent(VComponent<T> v) { parent = v; }
    
    /**
     * Recurrence Rule, RRULE, as defined in RFC 5545 iCalendar 3.8.5.3, page 122.
     * If event is not repeating value is null
     */
    @Override
    public ObjectProperty<RRule> rRuleProperty()
    {
        if (rRule == null) rRule = new SimpleObjectProperty<RRule>(this, VComponentProperty.RECURRENCE_RULE.toString(), _rRule);
        return rRule;
    }
    private ObjectProperty<RRule> rRule;
    private RRule _rRule;
    @Override
    public RRule getRRule() { return (rRule == null) ? _rRule : rRule.get(); }
    @Override
    public void setRRule(RRule rRule)
    {
        if (this.rRule == null)
        {
            _rRule = rRule;
        } else
        {
            this.rRule.set(rRule);
        }
    }
    
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
    @Override
    public IntegerProperty sequenceProperty() { return sequenceProperty; }
    final private IntegerProperty sequenceProperty = new SimpleIntegerProperty(this, VComponentProperty.SEQUENCE.toString(), 0);
    @Override
    public int getSequence() { return sequenceProperty.get(); }
    @Override
    public void setSequence(int value)
    {
        if (value < 0) throw new IllegalArgumentException("Sequence value must be greater than zero");
        if (value < getSequence()) throw new IllegalArgumentException("New sequence value must be greater than previous value");
        sequenceProperty.set(value);
    }
    
    /**
     *  SUMMARY: RFC 5545 iCalendar 3.8.1.12. page 83
     * This property defines a short summary or subject for the calendar component 
     * Example:
     * SUMMARY:Department Party
     * */
    @Override
    public StringProperty summaryProperty() { return summaryProperty; }
    final private StringProperty summaryProperty = new SimpleStringProperty(this, VComponentProperty.SUMMARY.toString());
    @Override
    public String getSummary() { return summaryProperty.get(); }
    @Override
    public void setSummary(String value) { summaryProperty.set(value); }
    
    /**
     * Unique identifier, UID as defined by RFC 5545, iCalendar 3.8.4.7 page 117
     * A globally unique identifier for the calendar component.
     * Included is an example UID generator.  Other UID generators can be provided by
     * setting the UID callback.
     */
    @Override
    public StringProperty uniqueIdentifierProperty() { return uniqueIdentifier; }
    final private StringProperty uniqueIdentifier = new SimpleStringProperty(this, VComponentProperty.UNIQUE_IDENTIFIER.toString());
    @Override
    public String getUniqueIdentifier() { return uniqueIdentifier.getValue(); }
    @Override
    public void setUniqueIdentifier(String s)
    {
        if ((s != null) && (s.matches("(.*)google\\.com")) && (s.matches("(.*)_R(.*)")))
        {
            String uid = s.substring(0, s.indexOf("_")) + "@google.com";
            String recurrence = s.substring(s.indexOf("_"), s.indexOf("@")); // not currently used
            setRelatedTo(uid);
        }
        uniqueIdentifier.set(s);
    }
    /** Set uniqueIdentifier by calling uidGeneratorCallback */
    public void setUniqueIdentifier() { setUniqueIdentifier(getUidGeneratorCallback().call(null)); } 
    
    /** Callback for creating unique uid values  */
    @Override
    public Callback<Void, String> getUidGeneratorCallback() { return uidGeneratorCallback; }
    private static Integer nextKey = 0;
    private Callback<Void, String> uidGeneratorCallback = (Void) ->
    { // default UID generator callback
        String dateTime = VComponent.LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.now());
        String domain = "jfxtras.org";
        return dateTime + "-" + nextKey++ + domain;
    };
    @Override
    public void setUidGeneratorCallback(Callback<Void, String> uidCallback) { this.uidGeneratorCallback = uidCallback; }
    
    /**
     * Start of range for which recurrence instances are generated.  Should match the dates displayed on the calendar.
     * This property is not a part of the iCalendar standard
     */
    @Override
    public Temporal getStartRange() { return startRange; }
    private Temporal startRange;
    @Override
    public void setStartRange(Temporal start)
    {
        if (getDateTimeStart().isSupported(ChronoUnit.NANOS))
        {
            LocalDateTime dt = LocalDateTime.from(start);
            if (getDateTimeStart() instanceof ZonedDateTime)
            {
                ZoneId zone = (start instanceof ZonedDateTime) ? ZonedDateTime.from(start).getZone() : ZonedDateTime.from(getDateTimeStart()).getZone();
                startRange = ZonedDateTime.of(dt, zone);
                return;
            } else if (getDateTimeStart() instanceof LocalDateTime)
            {
                startRange = dt;
                return;
            } else
            {
                throw new RuntimeException("Invalid startRange:" + start);
            }
        } else
        {
            startRange = LocalDate.from(start);
        }
    }
    
    /**
     * End of range for which recurrence instances are generated.  Should match the dates displayed on the calendar.
     */
    @Override
    public Temporal getEndRange() { return endRange; }
    private Temporal endRange;
    @Override
    public void setEndRange(Temporal end)
    {
        if (getDateTimeStart().isSupported(ChronoUnit.NANOS))
        {
            LocalDateTime dt = LocalDateTime.from(end);
            if (getDateTimeStart() instanceof ZonedDateTime)
            {
                ZoneId zone = (end instanceof ZonedDateTime) ? ZonedDateTime.from(end).getZone() : ZonedDateTime.from(getDateTimeStart()).getZone();
                endRange = ZonedDateTime.of(dt, zone);
                return;
            } else if (getDateTimeStart() instanceof LocalDateTime)
            {
                endRange = dt;
                return;
            } else
            {
                throw new RuntimeException("Invalid endRange:" + end);
            }
        } else
        {
            endRange = LocalDate.from(end);
        }
    }

    // CONSTRUCTORS
    /** Copy constructor */
    public VComponentBaseAbstract(VComponentBaseAbstract<T> vcomponent)
    {
        copy(vcomponent, this);
    }
    
    public VComponentBaseAbstract() { }
    
    @Override
    public void handleEdit(
            VComponent<T> vComponentOriginal
          , Collection<VComponent<T>> vComponents
          , Temporal startOriginalInstance
          , Temporal startInstance
          , Temporal endInstance
          , Collection<T> instances
          , Callback<Map<ChangeDialogOption, String>, ChangeDialogOption> dialogCallback)
    {
        final RRuleType rruleType = ICalendarAgendaUtilities.getRRuleType(getRRule(), vComponentOriginal.getRRule());
//        System.out.println("rruleType" + rruleType);
        boolean incrementSequence = true;
        switch (rruleType)
        {
        case HAD_REPEAT_BECOMING_INDIVIDUAL:
            becomingIndividual(vComponentOriginal, startInstance, endInstance);
            // fall through
        case WITH_NEW_REPEAT: // no dialog
        case INDIVIDUAL:
            if (! equals(vComponentOriginal)) updateInstances(instances);
            break;
        case WITH_EXISTING_REPEAT:
            if (! equals(vComponentOriginal)) // if changes occurred
            {
                List<VComponent<T>> relatedVComponents = Arrays.asList(this);
                Map<ChangeDialogOption, String> choices = new LinkedHashMap<>();
                String one = VComponent.temporalToStringPretty(startInstance);
                choices.put(ChangeDialogOption.ONE, one);
                if (! isIndividual())
                {
                    {
                        String future = VComponent.rangeToString(relatedVComponents, startInstance);
                        choices.put(ChangeDialogOption.THIS_AND_FUTURE, future);
                    }
                    String all = VComponent.rangeToString(this);
                    choices.put(ChangeDialogOption.ALL, all);
                }
                ChangeDialogOption changeResponse = dialogCallback.call(choices);
                switch (changeResponse)
                {
                case ALL:
                    if (relatedVComponents.size() == 1)
                    {
                        updateInstances(instances);
                    } else
                    {
                        throw new RuntimeException("Only 1 relatedVComponents currently supported");
                    }
                    break;
                case CANCEL:
                    vComponentOriginal.copyTo(this); // return to original this
                    incrementSequence = false;
                    break;
                case THIS_AND_FUTURE:
                    editThisAndFuture(vComponentOriginal, vComponents, startOriginalInstance, startInstance, instances);
                    break;
                case ONE:
                    editOne(vComponentOriginal, vComponents, startOriginalInstance, startInstance, endInstance, instances);
                    break;
                default:
                    break;
                }
            }
        }
        if (! isValid()) throw new RuntimeException(errorString());
        if (incrementSequence) incrementSequence();
    }
    
    private void updateInstances(Collection<T> instances)
    {
        Collection<T> instancesTemp = new ArrayList<>(); // use temp array to avoid unnecessary firing of Agenda change listener attached to appointments
        instancesTemp.addAll(instances);
        instancesTemp.removeIf(a -> instances().stream().anyMatch(a2 -> a2 == a));
        instances().clear(); // clear VEvent's outdated collection of appointments
        instancesTemp.addAll(makeInstances()); // make new appointments and add to main collection (added to VEvent's collection in makeAppointments)
        instances.clear();
        instances.addAll(instancesTemp);
    }
    
    /**
     * Part of handleEdit.
     * Changes a VComponent with a RRULE to be an individual,
     * 
     * @param vComponentOriginal
     * @param startInstance
     * @param endInstance
     * @see #handleEdit(VComponent, Collection, Temporal, Temporal, Temporal, Collection, Callback)
     */
    protected void becomingIndividual(VComponent<T> vComponentOriginal, Temporal startInstance, Temporal endInstance)
    {
        setRRule(null);
        setRDate(null);
        setExDate(null);
        if (vComponentOriginal.getRRule() != null)
        { // RRULE was removed, update DTSTART
            setDateTimeStart(startInstance);
        }
    }
    
    /**
     * Edit one instance of a VEvent with a RRule.  The instance becomes a new VEvent without a RRule
     * as with the same UID as the parent and a recurrence-id for the replaced date or date/time.
     * 
     * @see #handleEdit(VComponent, Collection, Temporal, Temporal, Temporal, Collection)
     */
    protected void editOne(
            VComponent<T> vEventOriginal
          , Collection<VComponent<T>> vComponents
          , Temporal startOriginalInstance
          , Temporal startInstance
          , Temporal endInstance
          , Collection<T> instances)
    {
        // Apply dayShift, if any
        Period dayShift = Period.between(LocalDate.from(getDateTimeStart()), LocalDate.from(startInstance));
//        System.out.println("dayShift:" + dayShift + " " + getDateTimeStart() + " " + startInstance);
        Temporal newStart = getDateTimeStart().plus(dayShift);
        setDateTimeStart(newStart);
//        if (isWholeDay())
//        {
//            LocalDate start = LocalDate.from(startInstance);
//            setDateTimeStart(start);
//        } else
//        {
////            vEventOriginal.getDateTimeStart()
//            setDateTimeStart(startInstance);
//        }
        setRRule(null);
        
//        // TODO - USE ZONEDDATETIME FOR INSTANCES?
//        if ((getDateTimeStart() instanceof ZonedDateTime) && (startOriginalInstance instanceof LocalDateTime))
//        {
//            ZoneId id = ((ZonedDateTime) getDateTimeStart()).getZone();
//            ZonedDateTime z = ((LocalDateTime) startOriginalInstance).atZone(id);
//            setDateTimeRecurrence(z);
//        } else
//        {
//            setDateTimeRecurrence(startOriginalInstance);
//        }
        
        System.out.println("startOriginalInstance:" + startOriginalInstance);
        setDateTimeRecurrence(startOriginalInstance);
        setDateTimeStamp(ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z")));
        setParent(vEventOriginal);
   
        // Add recurrence to original vEvent
        vEventOriginal.getRRule().recurrences().add(this);

        // Check for validity
        if (! isValid()) throw new RuntimeException(errorString());
        if (! vEventOriginal.isValid()) throw new RuntimeException(vEventOriginal.errorString());
        
        // Remove old appointments, add back ones
        Collection<T> instancesTemp = new ArrayList<>(); // use temp array to avoid unnecessary firing of Agenda change listener attached to appointments
        instancesTemp.addAll(instances);
        instancesTemp.removeIf(a -> vEventOriginal.instances().stream().anyMatch(a2 -> a2 == a));
        vEventOriginal.instances().clear(); // clear vEventOriginal outdated collection of appointments
        instancesTemp.addAll(vEventOriginal.makeInstances()); // make new appointments and add to main collection (added to vEventNew's collection in makeAppointments)
        instances().clear(); // clear vEvent outdated collection of appointments
//        System.out.println("editone:" + this);
        instancesTemp.addAll(makeInstances()); // add vEventOld part of new appointments
        instances.clear();
        instances.addAll(instancesTemp);
        vComponents.add(vEventOriginal);
    }
    
    /**
     * Changing this and future instances in VComponent is done by ending the previous
     * VComponent with a UNTIL date or date/time and starting a new VComponent from 
     * the selected instance.  EXDATE, RDATE and RECURRENCES are split between both
     * VComponents.  vEventNew has new settings, vEvent has former settings.
     * 
     * @see VComponent#handleEdit(VComponent, Collection, Temporal, Temporal, Temporal, Collection)
     */
    protected void editThisAndFuture(
            VComponent<T> vComponentOriginal
          , Collection<VComponent<T>> vComponents
          , Temporal startOriginalInstance
          , Temporal startInstance
          , Collection<T> instances)
    {
        // adjust original VEvent
        if (vComponentOriginal.getRRule().getCount() != null) vComponentOriginal.getRRule().setCount(0);
        Temporal previousDay = startOriginalInstance.minus(1, ChronoUnit.DAYS);
        Temporal untilNew = (isWholeDay()) ? LocalDate.from(previousDay).atTime(23, 59, 59) : previousDay; // use last second of previous day, like Yahoo
        vComponentOriginal.getRRule().setUntil(untilNew);
        
        setDateTimeStart(startInstance);
        setUniqueIdentifier();
        String relatedUID = (vComponentOriginal.getRelatedTo() == null) ? vComponentOriginal.getUniqueIdentifier() : vComponentOriginal.getRelatedTo();
        setRelatedTo(relatedUID);
        setDateTimeStamp(ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z")));
        
        // Split EXDates dates between this and newVEvent
        if (getExDate() != null)
        {
            getExDate().getTemporals().clear();
            final Iterator<Temporal> exceptionIterator = getExDate().getTemporals().iterator();
            while (exceptionIterator.hasNext())
            {
                Temporal d = exceptionIterator.next();
                int result = VComponent.TEMPORAL_COMPARATOR.compare(d, startInstance);
                if (result < 0)
                {
                    exceptionIterator.remove();
                } else {
                    getExDate().getTemporals().add(d);
                }
            }
            if (getExDate().getTemporals().isEmpty()) setExDate(null);
            if (getExDate().getTemporals().isEmpty()) setExDate(null);
        }

        // Split recurrence date/times between this and newVEvent
        if (getRDate() != null)
        {
            getRDate().getTemporals().clear();
            final Iterator<Temporal> recurrenceIterator = getRDate().getTemporals().iterator();
            while (recurrenceIterator.hasNext())
            {
                Temporal d = recurrenceIterator.next();
                int result = VComponent.TEMPORAL_COMPARATOR.compare(d, startInstance);
                if (result < 0)
                {
                    recurrenceIterator.remove();
                } else {
                    getRDate().getTemporals().add(d);
                }
            }
            if (getRDate().getTemporals().isEmpty()) setRDate(null);
            if (getRDate().getTemporals().isEmpty()) setRDate(null);
        }

        // Split instance dates between this and newVEvent
        if (getRRule().recurrences() != null)
        {
            getRRule().recurrences().clear();
            final Iterator<VComponent<?>> recurrenceIterator = getRRule().recurrences().iterator();
            while (recurrenceIterator.hasNext())
            {
                VComponent<?> d = recurrenceIterator.next();
                if (VComponent.isBefore(d.getDateTimeRecurrence(), startInstance))
                {
                    recurrenceIterator.remove();
                } else {
                    getRRule().recurrences().add(d);
                }
            }
        }
        
        // Modify COUNT for the edited vEvent
        if (getRRule().getCount() > 0)
        {
            int countInOrginal = vComponentOriginal.makeInstances().size();
            int countInNew = getRRule().getCount() - countInOrginal;
            getRRule().setCount(countInNew);
        }
        
        if (! vComponentOriginal.isValid()) throw new RuntimeException(vComponentOriginal.errorString());
        vComponents.add(vComponentOriginal);

        // Remove old appointments, add back ones
        Collection<T> instancesTemp = new ArrayList<>(); // use temp array to avoid unnecessary firing of Agenda change listener attached to appointments
        instancesTemp.addAll(instances);
        instancesTemp.removeIf(a -> vComponentOriginal.instances().stream().anyMatch(a2 -> a2 == a));
        vComponentOriginal.instances().clear(); // clear vEvent outdated collection of appointments
        instancesTemp.addAll(vComponentOriginal.makeInstances()); // make new appointments and add to main collection (added to vEvent's collection in makeAppointments)
        instances().clear(); // clear vEvent's outdated collection of appointments
        instancesTemp.addAll(makeInstances()); // add vEventOld part of new appointments
        instances.clear();
        instances.addAll(instancesTemp);
    }
     
    
    @Override
    public void handleDelete(
            Collection<VComponent<T>> vComponents
          , Temporal startInstance
          , T instance
          , Collection<T> instances)
    {
        int count = this.instances().size();
        if (count == 1)
        {
            vComponents.remove(this);
            instances.remove(instance);
        } else // more than one instance
        {
            Map<ChangeDialogOption, String> choices = new LinkedHashMap<>();
            String one = VComponent.temporalToStringPretty(startInstance);
            choices.put(ChangeDialogOption.ONE, one);
            if (! this.isIndividual())
            {
                {
                    String future = VComponent.rangeToString(this, startInstance);
                    choices.put(ChangeDialogOption.THIS_AND_FUTURE, future);
                }
                String all = VComponent.rangeToString(this);
                choices.put(ChangeDialogOption.ALL, all);
            }
            DeleteChoiceDialog dialog = new DeleteChoiceDialog(choices, Settings.resources);        
            Optional<ChangeDialogOption> result = dialog.showAndWait();
            ChangeDialogOption changeResponse = (result.isPresent()) ? result.get() : ChangeDialogOption.CANCEL;
            switch (changeResponse)
            {
            case ALL:
                List<VComponent<?>> relatedVComponents = new ArrayList<>();
                if (this.getDateTimeRecurrence() == null)
                { // is parent
                    relatedVComponents.addAll(this.getRRule().recurrences());
                    relatedVComponents.add(this);
                } else
                { // is child (recurrence).  Find parent delete all children
                    relatedVComponents.addAll(this.getParent().getRRule().recurrences());
                    relatedVComponents.add(this.getParent());
                }
                relatedVComponents.stream().forEach(v -> vComponents.remove(v));
                vComponents.removeAll(relatedVComponents);
                List<?> appointmentsToRemove = relatedVComponents.stream()
                        .flatMap(v -> v.instances().stream())
                        .collect(Collectors.toList());
                instances.removeAll(appointmentsToRemove);
                break;
            case CANCEL:
                break;
            case ONE:
                if (getExDate() == null) { setExDate(new ExDate(startInstance)); }
                else { getExDate().getTemporals().add(startInstance); }
                instances.removeIf(a -> a.equals(instance));
                break;
            case THIS_AND_FUTURE:
                if (getRRule().getCount() == 0) getRRule().setCount(0);
                Temporal previousDay = startInstance.minus(1, ChronoUnit.DAYS);
                Temporal untilNew = (this.isWholeDay()) ? LocalDate.from(previousDay).atTime(23, 59, 59) : previousDay; // use last second of previous day, like Yahoo
                this.getRRule().setUntil(untilNew);

                // Remove old appointments, add back ones
                Collection<T> instancesTemp = new ArrayList<>(); // use temp array to avoid unnecessary firing of Agenda change listener attached to appointments
                instancesTemp.addAll(instances);
                instancesTemp.removeIf(a -> instances().stream().anyMatch(a2 -> a2 == a));
                instances().clear(); // clear this's outdated collection of appointments
                instancesTemp.addAll(this.makeInstances()); // add vEventOld part of new appointments
                instances.clear();
                instances.addAll(instancesTemp);
                break;
            default:
                break;
            }
        }
    }

    /** Deep copy all fields from source to destination */
    private static void copy(VComponentBaseAbstract<?> source, VComponentBaseAbstract<?> destination)
    {
        destination.setCategories(source.getCategories());
        destination.setComment(source.getComment());
        destination.setDateTimeStamp(source.getDateTimeStamp());
        destination.setDateTimeStart(source.getDateTimeStart());
        destination.setRelatedTo(source.getRelatedTo());
        destination.setSequence(source.getSequence());
        destination.setSummary(source.getSummary());
        destination.setUniqueIdentifier(source.getUniqueIdentifier());
        if (source.getRRule() != null)
        {
            if (destination.getRRule() == null)
            { // make new RRule object for destination if necessary
                try {
                    RRule newRRule = source.getRRule().getClass().newInstance();
                    destination.setRRule(newRRule);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            source.getRRule().copyTo(destination.getRRule());
        }
        if (source.getExDate() != null)
        {
            if (destination.getExDate() == null)
            { // make new EXDate object for destination if necessary
                try {
                    ExDate newEXDate = source.getExDate().getClass().newInstance();
                    destination.setExDate(newEXDate);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            source.getExDate().copyTo(destination.getExDate());
        }
        if (source.getRDate() != null)
        {
            if (destination.getRDate() == null)
            { // make new RDate object for destination if necessary
                try {
                    RDate newRDate = source.getRDate().getClass().newInstance();
                    destination.setRDate(newRDate);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            source.getRDate().copyTo(destination.getRDate());
        }
    }
    
    /** Deep copy all fields from source to destination */
    @Override
    public void copyTo(VComponent<T> destination)
    {
        copy(this, (VComponentBaseAbstract<?>) destination);
    }

    /**
     * Needed by toString in subclasses.
     *  
     * Make list of properties and string values for toString method in subclasses (like VEvent)
     * Used by toString method in subclasses */
    List<String> makePropertiesList()
    {
        List<String> properties = new ArrayList<>();
        Arrays.stream(VComponentProperty.values())
                .forEach(p ->
                {
                    String newLine = p.makeContentLine(this);
                    if (newLine != null)
                    {
                        properties.add(newLine);
                    }
                });
        return properties;
    }
    
    /**
     * Needed by parse methods in subclasses 
     * 
     * Convert a list of strings containing properties of a iCalendar component and
     * populate its properties.  Used to make a new object from a List<String>.
     * 
     * @param vComponent - VComponent input parameter
     * @param strings - list of properties
     * @return VComponent with parsed properties added
     */
    protected static VComponentBaseAbstract<?> parseVComponent(VComponentBaseAbstract<?> vComponent, List<String> strings)
    {
        Iterator<String> lineIterator = strings.iterator();
        while (lineIterator.hasNext())
        {
            String line = lineIterator.next();
            // identify iCalendar property ending index (property name must start at the beginning of the line)
            int propertyValueSeparatorIndex = 0;
            for (int i=0; i<line.length(); i++)
            {
                if ((line.charAt(i) == ';') || (line.charAt(i) == ':'))
                {
                    propertyValueSeparatorIndex = i;
                    break;
                }
            }
            if (propertyValueSeparatorIndex == 0)
            {
                continue; // line doesn't contain a property, get next one
            }
            String propertyName = line.substring(0, propertyValueSeparatorIndex);
            String value = line.substring(propertyValueSeparatorIndex + 1).trim();
            if (value.isEmpty())
            { // skip empty properties
                continue;
            }
            VComponentProperty property = VComponentProperty.propertyFromString(propertyName);
            boolean propertyFound = property.setVComponent(vComponent, value); // runs method in enum to set property
            if (propertyFound)
            {
                lineIterator.remove();                
            }
        }
        return vComponent;
    }
    
    // Variables for start date or date/time cache used as starting Temporal for stream
    private static final int CACHE_RANGE = 51; // number of values in cache
    private static final int CACHE_SKIP = 21; // store every nth value in the cache
    private int skipCounter = 0; // counter that increments up to CACHE_SKIP, indicates time to record a value, then resets to 0
    private Temporal[] temporalCache; // the start date or date/time cache
    private Temporal dateTimeStartLast; // last dateTimeStart, when changes indicates clearing the cache is necessary
    private RRule rRuleLast; // last rRule, when changes indicates clearing the cache is necessary
    private int cacheStart = 0; // start index where cache values are stored (starts in middle)
    private int cacheEnd = 0; // end index where cache values are stored

    /**
     * finds previous stream Temporal before input parameter value
     * 
     * @param value
     * @return
     */
    @Deprecated // not using anymore - may delete in future
    public Temporal previousStreamValue(Temporal value)
    {
        final Temporal start; 
        if (cacheEnd == 0)
        {
            start = getDateTimeStart();
        } else
        { // try to get start from cache
            Temporal m  = null;
            for (int i=cacheEnd; i>cacheStart; i--)
            {
                if (VComponent.isBefore(temporalCache[i], value))
                {
                    m = temporalCache[i];
                    break;
                }
            }
            start = (m != null) ? m : getDateTimeStart();
        }
        Iterator<Temporal> i = streamNoCache(start).iterator();
        Temporal lastT = null;
        while (i.hasNext())
        {
            Temporal t = i.next();
            if (! VComponent.isBefore(t, value)) break;
            lastT = t;
        }
        return lastT;
    }
    
    @Override
    public Stream<Temporal> stream(Temporal start)
    {
        // adjust start to ensure its not before dateTimeStart
        final Temporal start2 = (VComponent.isBefore(start, getDateTimeStart())) ? getDateTimeStart() : start;
        final Stream<Temporal> stream1; // individual or rrule stream
        final Temporal earliestCacheValue;
        final Temporal latestCacheValue;
        
        if (getRRule() == null)
        { // if individual event
            stream1 = Arrays.asList(getDateTimeStart())
                    .stream()
                    .filter(d -> ! VComponent.isBefore(d, start2));
            earliestCacheValue = null;
            latestCacheValue = null;
        } else
        {
            // check if cache needs to be cleared (changes to RRULE or DTSTART)
            if ((dateTimeStartLast != null) && (rRuleLast != null))
            {
                boolean startChanged = ! getDateTimeStart().equals(dateTimeStartLast);
                boolean rRuleChanged = ! getRRule().equals(rRuleLast);
                if (startChanged || rRuleChanged)
                {
                    temporalCache = null;
                    cacheStart = 0;
                    cacheEnd = 0;
                    skipCounter = 0;
                    dateTimeStartLast = getDateTimeStart();
                    rRuleLast = getRRule();
                }
            } else
            { // save current DTSTART and RRULE for next time
                dateTimeStartLast = getDateTimeStart();
                rRuleLast = getRRule();
            }
            
            final Temporal match;
            
            // use cache if available to find matching start date or date/time
            if (temporalCache != null)
            {
                // Reorder cache to maintain centered and sorted
                final int len = temporalCache.length;
                final Temporal[] p1;
                final Temporal[] p2;
                if (cacheEnd < cacheStart) // high values have wrapped from end to beginning
                {
                    p1 = Arrays.copyOfRange(temporalCache, cacheStart, len);
                    p2 = Arrays.copyOfRange(temporalCache, 0, Math.min(cacheEnd+1,len));
                } else if (cacheEnd > cacheStart) // low values have wrapped from beginning to end
                {
                    p2 = Arrays.copyOfRange(temporalCache, cacheStart, len);
                    p1 = Arrays.copyOfRange(temporalCache, 0, Math.min(cacheEnd+1,len));
                } else
                {
                    p1 = null;
                    p2 = null;
                }
                if (p1 != null)
                { // copy elements to accommodate wrap and restore sort order
                    int p1Index = 0;
                    int p2Index = 0;
                    for (int i=0; i<len; i++)
                    {
                        if (p1Index < p1.length)
                        {
                            temporalCache[i] = p1[p1Index];
                            p1Index++;
                        } else if (p2Index < p2.length)
                        {
                            temporalCache[i] = p2[p2Index];
                            p2Index++;
                        } else
                        {
                            cacheEnd = i;
                            break;
                        }
                    }
                }
    
                // Find match in cache
                latestCacheValue = temporalCache[cacheEnd];
                if ((! VComponent.isBefore(start2, temporalCache[cacheStart])))
                {
                    Temporal m = latestCacheValue;
                    for (int i=cacheStart; i<cacheEnd+1; i++)
                    {
                        if (VComponent.isAfter(temporalCache[i], start2))
                        {
                            m = temporalCache[i-1];
                            break;
                        }
                    }
                    match = m;
                } else
                { // all cached values too late - start over
                    cacheStart = 0;
                    cacheEnd = 0;
                    temporalCache[cacheStart] = getDateTimeStart();
                    match = getDateTimeStart();
                }
                earliestCacheValue = temporalCache[cacheStart];
            } else
            { // no previous cache.  initialize new array with dateTimeStart as first value.
                temporalCache = new Temporal[CACHE_RANGE];
                temporalCache[cacheStart] = getDateTimeStart();
                match = getDateTimeStart();
                earliestCacheValue = getDateTimeStart();
                latestCacheValue = getDateTimeStart();
            }
            stream1 = getRRule().stream(match);
        }
        
        Stream<Temporal> stream2 = (getRDate() == null) ? stream1 : getRDate().stream(stream1, start2); // add recurrence list
        Stream<Temporal> stream3 = (getExDate() == null) ? stream2 : getExDate().stream(stream2, start2); // remove exceptions
        Stream<Temporal> stream4 = stream3
                .peek(t ->
                { // save new values in cache
                    if (getRRule() != null)
                    {
                        if (VComponent.isBefore(t, earliestCacheValue))
                        {
                            if (skipCounter == CACHE_SKIP)
                            {
                                cacheStart--;
                                if (cacheStart < 0) cacheStart = CACHE_RANGE - 1;
                                if (cacheStart == cacheEnd) cacheEnd--; // just overwrote oldest value - push cacheEnd down
                                temporalCache[cacheStart] = t;
                                skipCounter = 0;
                            } else skipCounter++;
                        }
                        if (VComponent.isAfter(t, latestCacheValue))
                        {
                            if (skipCounter == CACHE_SKIP)
                            {
                                cacheEnd++;
                                if (cacheEnd == CACHE_RANGE) cacheEnd = 0;
                                if (cacheStart == cacheEnd) cacheStart++; // just overwrote oldest value - push cacheStart up
                                temporalCache[cacheEnd] = t;
                                skipCounter = 0;
                            } else skipCounter++;
                        }
                        // check if start or end needs to wrap
                        if (cacheEnd < 0) cacheEnd = CACHE_RANGE - 1;
                        if (cacheStart == CACHE_RANGE) cacheStart = 0;
                    }
                })
                .filter(t -> ! VComponent.isBefore(t, start2)); // remove too early events;

        return stream4;
    }

    /** Stream of date/times that indicate the start of the event(s).
     * For a VEvent without RRULE the stream will contain only one date/time element.
     * A VEvent with a RRULE the stream contains more than one date/time element.  It will be infinite 
     * if COUNT or UNTIL is not present.  The stream has an end when COUNT or UNTIL condition is met.
     * Starts on startDateTime, which must be a valid event date/time, not necessarily the
     * first date/time (DTSTART) in the sequence. 
     * 
     * @param start - starting date or date/time for which occurrence start date or date/time
     * are generated by the returned stream
     * @return stream of starting dates or date/times for occurrences after rangeStart
     */
    private Stream<Temporal> streamNoCache(Temporal start)
    {
        final Stream<Temporal> stream1;
        if (getRRule() == null)
        { // if individual event
            stream1 = Arrays.asList(getDateTimeStart())
                    .stream()
                    .filter(d -> ! VComponent.isBefore(d, start));
        } else
        { // if has recurrence rule
            stream1 = getRRule().stream(getDateTimeStart());
        }
        Stream<Temporal> stream2 = (getRDate() == null) ? stream1 : getRDate().stream(stream1, start); // add recurrence list
        Stream<Temporal> stream3 = (getExDate() == null) ? stream2 : getExDate().stream(stream2, start); // remove exceptions
        return stream3.filter(t -> ! VComponent.isBefore(t, start));
    }

}