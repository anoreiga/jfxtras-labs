package jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour;

import jfxtras.labs.icalendarfx.components.VEvent;

public class EditVEventTabPane extends EditDisplayableTabPane<VEvent>
{
    public EditVEventTabPane( )
    {
        setEditDescriptive(new DescriptiveVEventVBox());
        getDescriptiveAnchorPane().getChildren().add(getEditDescriptive());
    }
}