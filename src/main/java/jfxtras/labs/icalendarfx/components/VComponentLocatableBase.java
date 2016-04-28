package jfxtras.labs.icalendarfx.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jfxtras.labs.icalendarfx.properties.PropertyEnum;
import jfxtras.labs.icalendarfx.properties.component.descriptive.Description;
import jfxtras.labs.icalendarfx.properties.component.descriptive.GeographicPosition;
import jfxtras.labs.icalendarfx.properties.component.descriptive.Location;
import jfxtras.labs.icalendarfx.properties.component.descriptive.Priority;
import jfxtras.labs.icalendarfx.properties.component.descriptive.Resources;
import jfxtras.labs.icalendarfx.properties.component.time.DurationProp;

public abstract class VComponentLocatableBase<T> extends VComponentDisplayableBase<T> implements VComponentLocatable<T>, VComponentDescribable2<T>
{
    /**
     * DESCRIPTION
     * RFC 5545 iCalendar 3.8.1.5. page 84
     * 
     * This property provides a more complete description of the
     * calendar component than that provided by the "SUMMARY" property.
     * 
     * Example:
     * DESCRIPTION:Meeting to provide technical review for "Phoenix"
     *  design.\nHappy Face Conference Room. Phoenix design team
     *  MUST attend this meeting.\nRSVP to team leader.
     *
     * Note: Only VJournal allows multiple instances of DESCRIPTION
     */
    @Override public ObjectProperty<Description> descriptionProperty()
    {
        if (description == null)
        {
            description = new SimpleObjectProperty<>(this, PropertyEnum.DESCRIPTION.toString());
        }
        return description;
    }
    private ObjectProperty<Description> description;

    /** 
     * DURATION
     * RFC 5545 iCalendar 3.8.2.5 page 99, 3.3.6 page 34
     * Can't be used if DTEND is used.  Must be one or the other.
     * 
     * Example:
     * DURATION:PT15M
     * */
    @Override public ObjectProperty<DurationProp> durationProperty()
    {
        if (duration == null)
        {
            duration = new SimpleObjectProperty<>(this, PropertyEnum.DURATION.toString());
        }
        return duration;
    }
    private ObjectProperty<DurationProp> duration;
    
    /**
     * GEO: Geographic Position
     * RFC 5545 iCalendar 3.8.1.6 page 85, 3.3.6 page 85
     * This property specifies information related to the global
     * position for the activity specified by a calendar component.
     * 
     * This property value specifies latitude and longitude,
     * in that order (i.e., "LAT LON" ordering).
     * 
     * Example:
     * GEO:37.386013;-122.082932
     */
    @Override public ObjectProperty<GeographicPosition> geographicPositionProperty()
    {
        if (geographicPosition == null)
        {
            geographicPosition = new SimpleObjectProperty<>(this, PropertyEnum.GEOGRAPHIC_POSITION.toString());
        }
        return geographicPosition;
    }
    private ObjectProperty<GeographicPosition> geographicPosition;

    /**
     * LOCATION:
     * RFC 5545 iCalendar 3.8.1.12. page 87
     * This property defines the intended venue for the activity
     * defined by a calendar component.
     * Example:
     * LOCATION:Conference Room - F123\, Bldg. 002
     */
    @Override public ObjectProperty<Location> locationProperty()
    {
        if (location == null)
        {
            location = new SimpleObjectProperty<>(this, PropertyEnum.LOCATION.toString());
        }
        return location;
    }
    private ObjectProperty<Location> location;

    /**
     * PRIORITY
     * RFC 5545 iCalendar 3.8.1.6 page 85, 3.3.6 page 85
     * This property defines the relative priority for a calendar component.
     * This priority is specified as an integer in the range 0 to 9.
     * 
     * Example: The following is an example of a property with the highest priority:
     * PRIORITY:1
     */
    @Override public ObjectProperty<Priority> priorityProperty()
    {
        if (priority == null)
        {
            priority = new SimpleObjectProperty<>(this, PropertyEnum.PRIORITY.toString());
        }
        return priority;
    }
    private ObjectProperty<Priority> priority;
    
    /**
     * RESOURCES:
     * RFC 5545 iCalendar 3.8.1.10. page 91
     * This property defines the equipment or resources
     * anticipated for an activity specified by a calendar component.
     * More than one resource can be specified as a COMMA-separated list
     * Example:
     * RESOURCES:EASEL,PROJECTOR,VCR
     * RESOURCES;LANGUAGE=fr:Nettoyeur haute pression
     */
    @Override
    public ObservableList<Resources> getResources() { return resources; }
    private ObservableList<Resources> resources;
    @Override
    public void setResources(ObservableList<Resources> resources) { this.resources = resources; }

    /** 
     * VALARM
     * Alarm Component
     * RFC 5545 iCalendar 3.6.6. page 71
     * 
     * Provide a grouping of component properties that define an alarm.
     * 
     * The "VALARM" calendar component MUST only appear within either a
     * "VEVENT" or "VTODO" calendar component.
     */
    @Override
    public ObservableList<VAlarm> getVAlarms() { return vAlarms; }
    private ObservableList<VAlarm> vAlarms;
    @Override
    public void setVAlarms(ObservableList<VAlarm> vAlarms) { this.vAlarms = vAlarms; }
    
    /*
     * CONSTRUCTORS
     */
    public VComponentLocatableBase() { }
    
    public VComponentLocatableBase(String contentLines)
    {
        super(contentLines);
    }
    
    /** Include VAlarm sub-components in content lines */
    @Override
    void appendMiddleContentLines(StringBuilder builder)
    {
        super.appendMiddleContentLines(builder);
        if (getVAlarms() != null)
        {
            getVAlarms().stream().forEach(a -> builder.append(a.toContentLines() + System.lineSeparator()));
        }
    }
    
    /** parse VAlarms */
    @Override
    void parseSubComponents(VComponentEnum subcomponentType, String contentLines)
    {
        if (subcomponentType == VComponentEnum.VALARM)
        {
            final ObservableList<VAlarm> list;
            if (getVAlarms() == null)
            {
                list = FXCollections.observableArrayList();
                setVAlarms(list);
            } else
            {
                list = getVAlarms();
            }
            list.add(VAlarm.parse(contentLines));
        } else
        {
            throw new IllegalArgumentException("Unspoorted subcomponent type:" + subcomponentType +
                    " found inside " + componentType() + " component");
        }
    }
}