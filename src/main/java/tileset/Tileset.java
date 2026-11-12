
package tileset;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import editor.MainFrame;
import editor.smartdrawing.SmartGrid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.swing.*;

import utils.Utils;

/**
 * @author Trifindo
 */
public class Tileset {

    //File extension
    public static final String fileExtension = "pdsts";

    //Tileset path
    public String tilesetFolderPath = "";

    //Tiles
    private ArrayList<Tile> tiles = new ArrayList();

    //Textures
    private ArrayList<Texture> textures = new ArrayList<>();
    private ArrayList<TilesetMaterial> materials = new ArrayList();

    public static final BufferedImage defaultTexture = Utils.loadTexImageAsResource("/imgs/defaultTexture.png");

    //Smart grid
    private ArrayList<SmartGrid> sgridArray = new ArrayList<>();

    //Palette folders (tile organization, saved in the .meta sidecar file)
    private ArrayList<PaletteFolder> paletteFolders = new ArrayList<>();

    public Tileset() {
        tiles = new ArrayList();
        textures = new ArrayList<>();
        materials = new ArrayList<>();

        sgridArray = new ArrayList<>();
        //sgridArray.add(new SmartGrid());
    }

    @Override
    public Tileset clone() {
        Tileset tileset = new Tileset();

        tileset.tiles = new ArrayList<>();
        for (int i = 0; i < tiles.size(); i++) {
            tileset.tiles.add(tiles.get(i));
        }

        tileset.textures = new ArrayList<>();
        for (int i = 0; i < textures.size(); i++) {
            tileset.textures.add(textures.get(i));
        }

        tileset.materials = new ArrayList<>();
        for (TilesetMaterial material : materials) {
            tileset.materials.add(material.clone()); //TODO: need clone here?
        }

        tileset.sgridArray = new ArrayList<>();
        for (int i = 0; i < sgridArray.size(); i++) {
            tileset.sgridArray.add(sgridArray.get(i));
        }

        tileset.paletteFolders = new ArrayList<>(paletteFolders);

        return tileset;
    }

    public ArrayList<PaletteFolder> getPaletteFolders() {
        return paletteFolders;
    }

    /** The folder with the given path, or null if it does not exist. */
    public PaletteFolder getPaletteFolder(String path) {
        for (PaletteFolder folder : paletteFolders) {
            if (folder.getPath().equals(path)) {
                return folder;
            }
        }
        return null;
    }

    public PaletteFolder getOrCreatePaletteFolder(String path) {
        PaletteFolder folder = getPaletteFolder(path);
        if (folder == null) {
            folder = new PaletteFolder(path);
            paletteFolders.add(folder);
        }
        return folder;
    }

    /**
     * Creates the folder and any missing parent folders of its path
     * ("Terrain/Grass/HGSS" nests HGSS inside Grass inside Terrain).
     */
    public PaletteFolder getOrCreatePaletteFolderWithParents(String path) {
        if (path.isEmpty()) {
            return getOrCreatePaletteFolder(path);
        }
        PaletteFolder folder = null;
        StringBuilder prefix = new StringBuilder();
        for (String segment : path.split("/")) {
            if (prefix.length() > 0) {
                prefix.append('/');
            }
            prefix.append(segment);
            folder = getOrCreatePaletteFolder(prefix.toString());
        }
        return folder;
    }

    /** The parent path of a nested folder path, or null for root folders. */
    public static String getParentFolderPath(String path) {
        int idx = path.lastIndexOf('/');
        return idx < 0 ? null : path.substring(0, idx);
    }

    /**
     * Removes the folder and all its subfolders; their tiles keep existing
     * only in the All Tiles section.
     */
    public void removePaletteFolder(PaletteFolder folder) {
        String prefix = folder.getPath() + "/";
        paletteFolders.removeIf(f -> f == folder || f.getPath().startsWith(prefix));
        for (Tile tile : tiles) {
            ArrayList<String> memberships = new ArrayList<>(tile.getPaletteFolderSlots().keySet());
            for (String path : memberships) {
                if (path.equals(folder.getPath()) || path.startsWith(prefix)) {
                    tile.removePaletteFolder(path);
                }
            }
        }
        for (SmartGrid grid : sgridArray) {
            String path = grid.getPaletteFolder();
            if (path.equals(folder.getPath()) || path.startsWith(prefix)) {
                grid.setPaletteFolder("");
            }
        }
    }

    /**
     * Renames or moves the folder (a new path may have a different parent).
     * Subfolders and the folder references of all affected tiles follow.
     */
    public void renamePaletteFolder(PaletteFolder folder, String newPath) {
        String oldPath = folder.getPath();
        String oldPrefix = oldPath + "/";
        for (PaletteFolder f : paletteFolders) {
            if (f == folder) {
                f.setPath(newPath);
            } else if (f.getPath().startsWith(oldPrefix)) {
                f.setPath(newPath + "/" + f.getPath().substring(oldPrefix.length()));
            }
        }
        for (Tile tile : tiles) {
            tile.renamePaletteFolder(oldPath, newPath);
        }
        for (SmartGrid grid : sgridArray) {
            String path = grid.getPaletteFolder();
            if (path.equals(oldPath)) {
                grid.setPaletteFolder(newPath);
            } else if (path.startsWith(oldPrefix)) {
                grid.setPaletteFolder(newPath + "/" + path.substring(oldPrefix.length()));
            }
        }
    }

    public void saveImagesToFile(String path) {
        for (int i = 0; i < materials.size(); i++) {
            TilesetMaterial material = materials.get(i);
            String outPath = path + File.separator + material.getImageName();
            try {
                File outputfile = new File(outPath);
                ImageIO.write(material.getTextureImg(), "png", outputfile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public ArrayList<Integer> replaceMaterial(int oldIndex, int newIndex) {
        ArrayList<Integer> indicesTilesReplaced;
        indicesTilesReplaced = replaceTextureIDs(oldIndex, newIndex);
        removeUnusedTextures();
        return indicesTilesReplaced;
    }

    public ArrayList<Integer> replaceTextureIDs(int oldIndex, int newIndex) {
        ArrayList<Integer> indicesTilesReplaced = new ArrayList<>();
        for (int i = 0; i < tiles.size(); i++) {
            if (Collections.replaceAll(tiles.get(i).getTextureIDs(), oldIndex, newIndex)) {
                indicesTilesReplaced.add(i);
            }
        }
        return indicesTilesReplaced;
    }

    public void removeUnusedTextures() {
        ArrayList<Integer> textureUsage = countTextureUsage();

        // TODO: could this be improved by using pointers instead of IDs?
        for (int i = 0; i < textureUsage.size(); i++) {
            if (textureUsage.get(i) == 0) {
                materials.remove(i);
                textureUsage.remove(i);
                shiftTextureIDsFrom(i);
                i--;
            }
        }
    }

    public void swapMaterials(int indexMat1, int indexMat2) {
        Collections.swap(textures, indexMat1, indexMat2);
        Collections.swap(materials, indexMat1, indexMat2);
        for (Tile tile : tiles) {
            tile.swapMaterials(indexMat1, indexMat2);
        }

    }

    public void removeTile(int index) {
        tiles.remove(index);

        removeUnusedTextures();
    }

    public void removeTiles(ArrayList<Integer> indices) {
        for (int i = 0; i < indices.size(); i++) {

        }
    }

    private ArrayList<Integer> countTextureUsage() {
        ArrayList<Integer> count = new ArrayList<>();
        /*
        for (Texture texture : textures) {
            count.add(0);
        }*/
        for (int i = 0; i < materials.size(); i++) {
            count.add(0);
        }

        for (Tile tile : tiles) {
            for (Integer i : tile.getTextureIDs()) {
                count.set(i, count.get(i) + 1);
            }
        }
        return count;
    }

    public boolean addTexture(String path) throws IOException {
        String filename = new File(path).getName();

        int textIndex = getIndexOfMaterialByImgName(filename);
        if (textIndex == -1) {
            TilesetMaterial material = new TilesetMaterial();
            material.setTextureImg(Tile.loadTextureImgWithDefault(path));
            material.setImageName(filename);
            String textureNameImd = Utils.removeExtensionFromPath(filename);
            material.setMaterialName(textureNameImd);
            material.setTextureNameImd(textureNameImd);
            material.setPaletteNameImd(textureNameImd + "_pl");
            materials.add(material);
            return true;
        } else {
            return false;
        }
    }

    public boolean replaceTexture(int index, String path) throws IOException {
        String filename = new File(path).getName();

        int textIndex = getIndexOfMaterialByImgName(filename);
        if (textIndex == -1 || textIndex == index) {
            TilesetMaterial material = materials.get(index);
            BufferedImage img = Tile.loadTextureImg(path);
            material.setTextureImg(img);
            material.setImageName(filename);
            /*
            String textureNameImd = Utils.removeExtensionFromPath(filename);
            material.setMaterialName(textureNameImd);
            material.setTextureNameImd(textureNameImd);
            material.setPaletteNameImd(textureNameImd + "_pl");*/
            return true;
        } else {
            return false;
        }
    }

    private void shiftTextureIDsFrom(int index) {
        for (Tile tile : tiles) {
            for (int i = 0; i < tile.getTextureIDs().size(); i++) {
                int id = tile.getTextureIDs().get(i);
                if (id > index) {
                    tile.getTextureIDs().set(i, id - 1);
                }
            }
        }
    }

    public void updateTextures(GL2 gl) {
        for (int i = 0; i < textures.size(); i++) {
            textures.get(i).destroy(gl);
        }
        loadTexturesGL();
    }

    public void swapTiles(int e1, int e2) {
        Collections.swap(tiles, e1, e2);
    }

    public void moveTiles(ArrayList<Integer> indices) {
        ArrayList<Tile> newTiles = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            newTiles.add(tiles.get(indices.get(i)));
        }
        tiles = newTiles;
    }

    public void loadTexturesGL() {
        textures = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            textures.add(loadTextureGL(i));
        }
    }

    private Texture loadTextureGL(int index) {
        Texture tex = null;
        try {
            BufferedImage img = Utils.cloneImg(materials.get(index).getTextureImg());
            ImageUtil.flipImageVertically(img);
            tex = AWTTextureIO.newTexture(GLProfile.getDefault(), img, false);
        } catch (Exception e) {
            tex = AWTTextureIO.newTexture(GLProfile.getDefault(), Tileset.defaultTexture, false);
            //e.printStackTrace();
        }
        return tex;
    }

    public void loadTextureImgs() throws IOException {
        ArrayList<Integer> textureUsage = countTextureUsage();
        for (int i = 0; i < textureUsage.size(); i++) {
            if (textureUsage.get(i) == 0) {
                materials.remove(i);
                textureUsage.remove(i);
                shiftTextureIDsFrom(i);
                i--;
            }
        }
        for (int i = 0; i < materials.size(); i++) {
            String textureName = materials.get(i).getImageName();
            materials.get(i).setTextureImg(Tile.loadTextureImgWithDefault(tilesetFolderPath + "/" + textureName));
        }
    }

    public void loadTextureImgsAsResource() throws IOException {
        ArrayList<Integer> textureUsage = countTextureUsage();
        for (int i = 0; i < textureUsage.size(); i++) {
            if (textureUsage.get(i) == 0) {
                materials.remove(i);
                textureUsage.remove(i);
                shiftTextureIDsFrom(i);
                i--;
            }
        }
        for (int i = 0; i < materials.size(); i++) {
            String textureName = materials.get(i).getImageName();
            //System.out.println(tilesetFolderPath);
            BufferedImage img = Utils.loadTexImageAsResource(tilesetFolderPath + "/" + textureName);
            materials.get(i).setTextureImg(img);
        }
    }

    public void loadTextureImgFromPath(int index, String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        materials.get(index).setTextureImg(img);
    }

    public ArrayList<Texture> getTextures() {
        return textures;
    }

    public void setTiles(ArrayList<Tile> tiles) {
        this.tiles = tiles;
    }

    public void addTile(Tile tile) {
        tile.setTileset(this);
        this.tiles.add(tile);
    }

    public void importTile(Tile tile) {
        ArrayList<Integer> texIDs = tile.getTextureIDs();
        for (int i = 0; i < texIDs.size(); i++) {
            TilesetMaterial material = tile.getTileset().getMaterial(texIDs.get(i));
            texIDs.set(i, importMaterial(material));
        }
        tile.setTileset(this);
        tiles.add(tile);
    }

    /**
     * Index of the material an imported tile should use for {@code material}.
     * An existing material with the same image name is reused only if it shows
     * the same pixels. Different tilesets use the same file name (for example
     * "file1Material.png") for different images, so a clash gets a renamed copy
     * ("file1Material_2.png"), which later imports of that image reuse.
     */
    private int importMaterial(TilesetMaterial material) {
        String name = material.getImageName();
        int index = getIndexOfMaterialByImgName(name);
        if (index == -1) {
            materials.add(material);
            return materials.size() - 1;
        }
        if (materials.get(index).samePixels(material)) {
            return index;
        }
        String base = Utils.removeExtensionFromPath(name);
        String ext = name.substring(base.length());
        for (int n = 2; ; n++) {
            String candidate = base + "_" + n + ext;
            int existing = getIndexOfMaterialByImgName(candidate);
            if (existing != -1) {
                if (materials.get(existing).samePixels(material)) {
                    return existing;
                }
                continue;
            }
            String suffix = "_" + n;
            String textureNameImd = uniqueImdName(material.getTextureNameImd(), suffix, true);
            String paletteNameImd = uniqueImdName(material.getPaletteNameImd(), suffix, false);
            TilesetMaterial renamed = material.clone();
            renamed.setImageName(candidate);
            renamed.setMaterialName(material.getMaterialName() == null ? null : material.getMaterialName() + suffix);
            renamed.setTextureNameImd(textureNameImd);
            renamed.setPaletteNameImd(paletteNameImd);
            materials.add(renamed);
            return materials.size() - 1;
        }
    }

    /** Max length of texture and palette names in NSBTX files. */
    private static final int IMD_NAME_MAX = 16;

    /**
     * {@code name + suffix}, shortened to fit {@link #IMD_NAME_MAX} and made
     * unique among this tileset's texture (or palette) names, so the IMD export
     * does not merge the renamed texture with the original.
     */
    private String uniqueImdName(String name, String suffix, boolean texture) {
        if (name == null) {
            return null;
        }
        java.util.Set<String> used = new java.util.HashSet<>();
        for (TilesetMaterial m : materials) {
            used.add(texture ? m.getTextureNameImd() : m.getPaletteNameImd());
        }
        // palette names are usually "<texture>_pl": keep the "_pl" at the end
        String head = name;
        String tail = "";
        if (!texture && name.endsWith("_pl")) {
            head = name.substring(0, name.length() - 3);
            tail = "_pl";
        }
        for (int n = 0; ; n++) {
            String s = n == 0 ? suffix : suffix + "_" + n;
            String end = s + tail;
            String start = head.length() + end.length() > IMD_NAME_MAX
                    ? head.substring(0, Math.max(0, IMD_NAME_MAX - end.length())) : head;
            String candidate = start + end;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
    }

    /** Folders made by importing tiles are named like this; the sort by type empties them. */
    public static final String IMPORT_FOLDER_PREFIX = "Imported from ";

    /**
     * Imports tiles and files them under a palette folder, so tiles taken from
     * different source tilesets stay apart in the tile selector. Tile indices
     * and everything else about the tileset are unaffected.
     */
    public void importTiles(ArrayList<Tile> tiles, String folderName) {
        String path = folderPathFromName(folderName);
        if (!path.isEmpty()) {
            path = IMPORT_FOLDER_PREFIX + path;
        }
        importTiles(tiles);
        if (path.isEmpty()) {
            return;
        }
        PaletteFolder folder = getPaletteFolder(path);
        if (folder == null) {
            folder = getOrCreatePaletteFolder(path);
            folder.setRows(1);
        }
        for (Tile tile : tiles) {
            tile.setPaletteSlot(-1);
            tile.setPaletteFolder(path);
        }
    }

    /**
     * Like {@link #importTiles(ArrayList, String)}, but skips tiles that already
     * exist in this tileset (same model, see {@link Tile#modelKey()}), including
     * repeats inside the imported batch. The existing tile keeps its folder and
     * its collision defaults.
     *
     * @return the number of tiles skipped
     */
    public int importTilesSkippingDuplicates(ArrayList<Tile> tiles, String folderName) {
        java.util.HashSet<String> known = new java.util.HashSet<>();
        for (Tile tile : this.tiles) {
            known.add(tile.modelKey());
        }
        ArrayList<Tile> fresh = new ArrayList<>();
        for (Tile tile : tiles) {
            if (known.add(tile.modelKey())) { // keys are taken before importTile re-points the textures
                fresh.add(tile);
            }
        }
        importTiles(fresh, folderName);
        return tiles.size() - fresh.size();
    }

    /**
     * Files tiles into palette folders by what they are (see {@link TileCategories}).
     * Only tiles that are in "All Tiles" or in a folder made by an import are
     * moved, so tiles you filed by hand stay where they are. Import folders this
     * empties are removed.
     *
     * @return the number of tiles moved
     */
    public int sortIntoCategoryFolders() {
        return sortIntoCategoryFolders(folder -> folder.isEmpty() || folder.startsWith(IMPORT_FOLDER_PREFIX));
    }

    /** Same, for tiles whose current folder path is accepted by {@code movable}. */
    public int sortIntoCategoryFolders(java.util.function.Predicate<String> movable) {
        java.util.Map<Tile, String> target = new java.util.LinkedHashMap<>();
        java.util.LinkedHashSet<String> emptied = new java.util.LinkedHashSet<>();
        java.util.Set<String> needed = new java.util.HashSet<>();
        for (Tile tile : tiles) {
            String current = tile.getPaletteFolder();
            if (TileCategories.ORDER.contains(current) || !movable.test(current)) {
                continue;
            }
            String category = TileCategories.categorize(tile);
            target.put(tile, category);
            needed.add(category);
            if (!current.isEmpty()) {
                emptied.add(current);
            }
        }
        for (String category : TileCategories.ORDER) { // creation order is display order
            if (needed.contains(category) && getPaletteFolder(category) == null) {
                getOrCreatePaletteFolder(category).setRows(1);
            }
        }
        for (java.util.Map.Entry<Tile, String> e : target.entrySet()) {
            e.getKey().setPaletteSlot(-1);
            e.getKey().setPaletteFolder(e.getValue());
        }
        for (String path : emptied) {
            PaletteFolder folder = getPaletteFolder(path);
            if (folder != null && !hasTilesInFolder(path)) {
                removePaletteFolder(folder);
            }
        }
        return target.size();
    }

    private boolean hasTilesInFolder(String path) {
        String prefix = path + "/";
        for (Tile tile : tiles) {
            for (String folder : tile.getPaletteFolderSlots().keySet()) {
                if (folder.equals(path) || folder.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Folder path for a display name: no "/" (it separates nested folders). */
    public static String folderPathFromName(String name) {
        if (name == null) {
            return "";
        }
        return name.replace('/', '-').replace('_', ' ').trim();
    }

    public void importTiles(ArrayList<Tile> tiles) {
        for (Tile tile : tiles) {
            importTile(tile);
        }

        System.out.println("Tiles imported");
    }


    public Tile get(int index) {
        return tiles.get(index);
    }

    public int size() {
        return tiles.size();
    }

    public ArrayList<Tile> getTiles() {
        return tiles;
    }

    public Texture getTexture(int index) {
        return textures.get(index);
    }

    public BufferedImage getTextureImg(int index) {
        return materials.get(index).getTextureImg();
    }

    public int getIndexOfTile(Tile tile) {
        return tiles.indexOf(tile);
    }

    public void duplicateTile(int index) {
        tiles.add(index, tiles.get(index).clone());
    }

    public void duplicateTiles(ArrayList<Integer> indices) {
        int startIndex = indices.get(indices.size() - 1) + 1;
        for (int i = 0; i < indices.size(); i++) {
            tiles.add(startIndex + i, tiles.get(indices.get(i)).clone());
        }
    }

    public int getIndexOfTileByObjFilename(String filename) {
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i).getObjFilename().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    public int getIndexOfMaterialByImgName(String textureName) {
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).getImageName().equals(textureName)) {
                return i;
            }
        }
        return -1;
    }

    public String getMaterialName(int index) {
        return materials.get(index).getMaterialName();
    }

    public String getImageName(int index) {
        return materials.get(index).getImageName();
    }

    public String getPaletteNameImd(int index) {
        return materials.get(index).getPaletteNameImd();
    }

    public void setMaterialName(int index, String name) {
        materials.get(index).setMaterialName(name);
    }

    public void setPaletteNameImd(int index, String name) {
        materials.get(index).setPaletteNameImd(name);
    }

    public String getTextureNameImd(int index) {
        return materials.get(index).getTextureNameImd();
    }

    public void setTextureNameImd(int index, String name) {
        materials.get(index).setTextureNameImd(name);
    }

    public ArrayList<SmartGrid> getSmartGridArray() {
        return sgridArray;
    }

    public void setSgridArray(ArrayList<SmartGrid> sgridArray) {
        this.sgridArray = sgridArray;
    }


    public ArrayList<TilesetMaterial> getMaterials() {
        return materials;
    }

    public TilesetMaterial getMaterial(int index) {
        return materials.get(index);
    }

    public int indexOfTileVisualData(Tile tile) {
        if (tile == null) {
            for (int i = 0; i < tiles.size(); i++)
                if (tiles.get(i) == null)
                    return i;
        } else {
            for (int i = 0; i < tiles.size(); i++)
                if (tile.equalsVisualData(tiles.get(i)))
                    return i;
        }
        return -1;
    }

}
