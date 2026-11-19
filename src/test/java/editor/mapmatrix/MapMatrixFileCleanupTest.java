package editor.mapmatrix;

import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import formats.collisions.Collisions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Point;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapMatrixFileCleanupTest {

    @TempDir
    Path tempDir;

    @Test
    void saveDeletesOnlyStaleFilesOfItsOwnMap() throws Exception {
        MapEditorHandler handler = new MapEditorHandler(null);
        MapMatrix mapMatrix = handler.getMapMatrix();
        mapMatrix.getMatrix().clear();
        mapMatrix.getMatrix().put(new Point(0, 0), new MapData(handler));

        File dir = tempDir.resolve("project").toFile();
        assertTrue(dir.mkdirs());
        String path = new File(dir, "route_1_2.pdsmap").getPath();
        mapMatrix.filePath = path;

        File stale = file(dir, "route_1_2_05_05." + Collisions.fileExtension);
        File used = file(dir, "route_1_2_00_00." + Collisions.fileExtension);
        File ownExport = file(dir, "route_1_2.imd");
        File otherMap = file(dir, "other_03_04.imd");

        mapMatrix.saveGridsToFile(path, mapMatrix.getMatrix().entrySet());

        assertFalse(stale.exists());
        assertTrue(used.exists());
        assertTrue(ownExport.exists(), "the map's own export must be kept");
        assertTrue(otherMap.exists(), "files of other maps must be kept");
    }

    @Test
    void saveAsCleansTheNewFolderAndLeavesTheOldOne() throws Exception {
        MapEditorHandler handler = new MapEditorHandler(null);
        MapMatrix mapMatrix = handler.getMapMatrix();
        mapMatrix.getMatrix().clear();
        mapMatrix.getMatrix().put(new Point(0, 0), new MapData(handler));

        File oldDir = tempDir.resolve("old").toFile();
        File newDir = tempDir.resolve("new").toFile();
        assertTrue(oldDir.mkdirs());
        assertTrue(newDir.mkdirs());
        mapMatrix.filePath = new File(oldDir, "park.pdsmap").getPath();

        File oldStale = file(oldDir, "park_05_05.imd");
        File newStale = file(newDir, "copy_05_05.imd");

        String newPath = new File(newDir, "copy.pdsmap").getPath();
        mapMatrix.saveGridsToFile(newPath, mapMatrix.getMatrix().entrySet());

        assertTrue(new File(newPath).isFile());
        assertTrue(oldStale.exists(), "Save As must not touch the old folder");
        assertFalse(newStale.exists());
    }

    private static File file(File dir, String name) throws Exception {
        File f = new File(dir, name);
        assertTrue(f.createNewFile());
        return f;
    }
}
