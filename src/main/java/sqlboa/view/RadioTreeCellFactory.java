package sqlboa.view;

import javafx.beans.property.Property;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class RadioTreeCellFactory implements Callback<TreeView<Object>,TreeCell<Object>> {
    @Override
    public TreeCell call( TreeView param )
    {
        return new TreeCell<Object>()
        {
//            private final RadioButton radio = new RadioButton();
//            private Property<Boolean> prevRadioProp;
//            {
//                setContentDisplay( ContentDisplay.GRAPHIC_ONLY );
//            }
//
//            @Override
//            public void updateItem( Object item, boolean empty )
//            {
//                if ( prevRadioProp != null )
//                {
//                    radio.selectedProperty().unbindBidirectional( prevRadioProp );
//                    prevRadioProp = null;
//                }
//                check.selectedProperty().unbind();
//
//                if ( ! empty && item != null )
//                {
//                    Property<Boolean> selectedProp = ....;
//
//                    if ( getTreeItem().isLeaf() )  // display radio button
//                    {
//                        radio.setText( ... );
//                        radio.selectedProperty().bindBidirectional( selectedProp );
//                        prevRadioProp = selectedProp;
//                        setGraphic( radio );
//                    }
//                    else                          // display checkbox
//                    {
//                        check.setText( ... );
//                        check.selectedProperty().bind( selectedProp );
//                        setGraphic( check );
//                    }
//                }
//                else
//                {
//                    setGraphic( null );
//                    setText( null );
//                }
//            }
        };
    }
}
