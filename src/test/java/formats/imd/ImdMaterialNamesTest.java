package formats.imd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tileset.TilesetMaterial;

class ImdMaterialNamesTest {

    private static List<TilesetMaterial> materials(String... names) {
        List<TilesetMaterial> out = new ArrayList<>();
        for (String name : names) {
            TilesetMaterial m = new TilesetMaterial();
            m.setMaterialName(name);
            out.add(m);
        }
        return out;
    }

    @Test
    void repeatedNamesGetASuffixSoG3dcvtrAcceptsTheModel() {
        assertArrayEquals(new String[]{"beach", "ngrass", "beach_2", "beach_3"},
                ImdModel.uniqueMaterialNames(materials("beach", "ngrass", "beach", "beach")));
    }

    @Test
    void aSuffixNeverCollidesWithAnExistingName() {
        assertArrayEquals(new String[]{"beach_2", "beach", "beach_3"},
                ImdModel.uniqueMaterialNames(materials("beach_2", "beach", "beach")));
    }

    @Test
    void uniqueNamesStayUnchanged() {
        assertArrayEquals(new String[]{"tree01", "tshadow"},
                ImdModel.uniqueMaterialNames(materials("tree01", "tshadow")));
    }
}
