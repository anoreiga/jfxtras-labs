package jfxtras.labs.icalendar.properties;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jfxtras.labs.icalendar.parameters.Parameter;
import jfxtras.labs.icalendar.parameters.ParameterEnum;
import jfxtras.labs.icalendar.parameters.Value.ValueType;
import jfxtras.labs.icalendar.utilities.ICalendarUtilities;

/**
 * Base iCalendar property class
 * Contains other-parameters
 * Also contains methods used by all properties
 * 
 * @author David Bal
 *
 */
public abstract class PropertyBase<T> implements Property
{
    /**
     * VALUE: Value Data Types
     * RFC 5545, 3.2.20, page 28
     * To specify the value for text values in a property or property parameter.
     * This parameter is optional for properties when the default value type is used.
     * 
     * Examples:
     * VALUE=DATE-TIME  (Date-Time is default value, so it isn't necessary to specify)
     * VALUE=DATE
     */
    public ValueType getValueType() { return (valueType == null) ? _valueType : valueType.get(); }
    public ObjectProperty<ValueType> valueParameterProperty()
    {
        if (valueType == null)
        {
            valueType = new SimpleObjectProperty<>(this, ParameterEnum.VALUE_DATA_TYPES.toString(), _valueType);
        }
        return valueType;
    }
    private ValueType _valueType;
    private ObjectProperty<ValueType> valueType;
    public void setValueType(ValueType value)
    {
//        if (value != null)
//        {
//            parameters().add(ParameterEnum.VALUE_DATA_TYPES);
//        } else
//        {
//            parameters().remove(ParameterEnum.VALUE_DATA_TYPES);            
//        }
//        parameterMap().put(ParameterEnum.VALUE_DATA_TYPES, value);
        if (this.valueType == null)
        {
            _valueType = value;
        } else
        {
            this.valueType.set(value);
        }
    }
    public T withValueType(ValueType value) { setValueType(value); return (T) this; } 
    
    /**
     * other-param, 3.2 RFC 5545 page 14
     * the parameter name and value are combined into one object
     */
    @Override
    public ObservableList<Object> otherParameters() { return otherParameters; }
    private ObservableList<Object> otherParameters = FXCollections.observableArrayList();
    public T withOtherParameters(Object... parameter) { otherParameters().addAll(parameter); return (T) this; }
    
    /** The type of the property in the content line, such as DESCRIPTION */
    @Override
    public PropertyEnum propertyType() { return propertyType; }
    private PropertyEnum propertyType;

//    /**
//     * List of all parameter enums in this property
//     */
//    @Override
//    public Collection<ParameterEnum> parameters() { return parameters; }
//    private Collection<ParameterEnum> parameters = new HashSet<>();
    
    /*
     * CONSTRUCTORS
     */
    
    /**
     * construct new property by parsing content line
     * sets parameters by running parseAndSet for each parameter enum
     * 
     * @param propertyString
     */
    public PropertyBase(String propertyString)
    {
        this();
        Map<String, String> map = ICalendarUtilities.propertyLineToParameterMap(propertyString);
        
        // add parameters
        map.entrySet()
            .stream()
            .filter(e -> ! (e.getKey() == ICalendarUtilities.PROPERTY_VALUE_KEY))
            .forEach(e ->
            {
                ParameterEnum p = ParameterEnum.enumFromName(e.getKey());
                System.out.println("parameter:" + e.getKey() + " " + e.getValue() + " " + p);
                if (p != null)
                {
                    p.parse(this, e.getValue());
                } else if ((e.getKey() != null) && (e.getValue() != null))
                { // unknown parameter - store as other parameter
                    otherParameters().add(e.getKey() + "=" + e.getValue());
                } // if parameter doesn't contain both a key and a value it is ignored
            });
        // add property value
        propertyValueString = map.get(ICalendarUtilities.PROPERTY_VALUE_KEY);
    }
    private String propertyValueString;
    protected String getPropertyValueString() { return propertyValueString; }
    
    // construct empty property
    public PropertyBase()
    {
        propertyType = PropertyEnum.enumFromClass(getClass());
    }
    
    // copy constructor
    public PropertyBase(Property source)
    {
        this.propertyType = source.propertyType();
        source.parameters().entrySet().stream().forEach(p -> p.getValue().copyTo(source, this));
//        parameters().stream().forEach(p -> p.copyTo(this));
//        ICalendarParameter.values(getClass())
//                .stream()
//                .forEach(p -> p.copyTo(source, this));
//        PropertyType.enumFromName(propertyName()).copyTo(source, this);
//        setValue = source.getValue();
//        source.copyValueTo(this);
    }
    
    /**
     * Return property content line for iCalendar output files.  See RFC 5545 3.5
     * Contains component property with its value and any populated parameters.
     * Only property name and parameter name/value pairs are added here.
     * Property value is added in subclasses
     * 
     * For example: SUMMARY;LANGUAGE=en-US:Company Holiday Party
     * 
     * @return - the content line
     */
    @Override
    public String toContentLine()
    {
        StringBuilder builder = new StringBuilder(propertyType().toString());
        parameters().entrySet().stream().forEach(p -> builder.append(p.getValue().toContentLine()));
        otherParameters().stream().forEach(p -> builder.append(";" + p));
        return builder.toString();
    }
    
//    @Override
//    public String toContentLine()
//    {
//        StringBuilder builder = new StringBuilder(propertyType().toString());
//        otherParameters().stream().forEach(p -> builder.append(";" + p));
//        return builder.toString();
//    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = (31 * hash) + getValue().hashCode();
        Iterator<Entry<ParameterEnum, Parameter>> i = parameters().entrySet().iterator();
        while (i.hasNext())
        {
            Parameter parameter = i.next().getValue();
            hash = (31 * hash) + parameter.getValue().hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if((obj == null) || (obj.getClass() != getClass())) {
            return false;
        }
        PropertyBase<?> testObj = (PropertyBase<?>) obj;
        boolean valueEquals = getValue().equals(testObj.getValue());
        boolean otherParametersEquals = otherParameters().equals(testObj.otherParameters());
        boolean parametersEquals = parameters()
               .entrySet()
               .stream()
               .map(p -> p.getValue().isEqualTo(this, testObj))
//              .peek(e -> System.out.println(e.toString() + " equals:" + e.isPropertyEqual(this, testObj)))
              .allMatch(b -> b == true);
        return valueEquals && otherParametersEquals && parametersEquals;
    }

    @Override
    public String toString()
    {
        return super.toString() + "," + toContentLine();
    }
}