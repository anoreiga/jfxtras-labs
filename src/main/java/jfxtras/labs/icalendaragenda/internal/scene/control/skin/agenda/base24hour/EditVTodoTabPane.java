package jfxtras.labs.icalendaragenda.internal.scene.control.skin.agenda.base24hour;

import jfxtras.labs.icalendarfx.components.VTodo;

public class EditVTodoTabPane extends EditDisplayableTabPane<VTodo>
{
    public EditVTodoTabPane( )
    {
        setEditDescriptive(new DescriptiveVTodoVBox());
        getDescriptiveAnchorPane().getChildren().add(getEditDescriptive());
    }
}