package formats.imd;

import editor.converter.FileConversionDialog;

import java.awt.Window;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * @author Trifindo, JackHack96
 */
public class ExportImdDialog extends FileConversionDialog {

    private JCheckBox mergeTrianglesCheckBox;

    public ExportImdDialog(Window owner) {
        super(owner);
    }

    @Override
    protected String getSourceFormatName() {
        return "OBJ";
    }

    @Override
    protected String getDestinationFormatName() {
        return "IMD";
    }

    //Called during base construction, before field initializers run
    @Override
    protected JComponent getExtraOptionsComponent() {
        if (mergeTrianglesCheckBox == null) {
            mergeTrianglesCheckBox = new JCheckBox("Merge triangle pairs into quads");
            mergeTrianglesCheckBox.setToolTipText("Draw two triangles that form a flat quad as one quad, "
                    + "like the original maps. Avoids texture shimmer on the DS.");
            mergeTrianglesCheckBox.setSelected(true);
        }
        return mergeTrianglesCheckBox;
    }

    public String getObjFolderPath() {
        return getSourceFolderPath();
    }

    public String getImdFolderPath() {
        return getDestinationFolderPath();
    }

    public ArrayList<String> getSelectedObjNames() {
        return getSelectedFileNames();
    }

    public boolean mergeTriangles() {
        return mergeTrianglesCheckBox == null || mergeTrianglesCheckBox.isSelected();
    }
}
