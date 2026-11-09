package tileset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Guesses what kind of thing a tile is (tree, path, water, ...) from the name
 * of its model file and the names of its textures, using the naming of the
 * bundled Platinum tilesets. Used to sort tiles into palette folders by type.
 */
public final class TileCategories {

    public static final String TREES = "Trees";
    public static final String PLANTS = "Plants and Flowers";
    public static final String PATHS = "Paths and Roads";
    public static final String GROUND = "Ground and Grass";
    public static final String WATER = "Water and Shore";
    public static final String SAND = "Sand and Beach";
    public static final String CLIFFS = "Cliffs and Mountains";
    public static final String SNOW = "Snow and Ice";
    public static final String BRIDGES = "Bridges and Stairs";
    public static final String OBJECTS = "Fences, Rocks and Objects";
    public static final String SHADOWS = "Shadows";
    public static final String INDOOR = "Indoor";
    public static final String OTHER = "Other";

    /** Display order of the folders. */
    public static final List<String> ORDER = List.of(GROUND, PATHS, PLANTS, TREES, CLIFFS, WATER, SAND,
            SNOW, BRIDGES, OBJECTS, INDOOR, SHADOWS, OTHER);

    private static final class Rule {
        final String category;
        final String[] prefixes;   // the name starts with one of these
        final String[] contains;   // or contains one of these
        final String[] exact;      // or equals one of these

        Rule(String category, String[] prefixes, String[] contains, String[] exact) {
            this.category = category;
            this.prefixes = prefixes;
            this.contains = contains;
            this.exact = exact;
        }

        boolean matches(String name) {
            for (String p : prefixes) {
                if (name.startsWith(p)) {
                    return true;
                }
            }
            for (String c : contains) {
                if (name.contains(c)) {
                    return true;
                }
            }
            for (String e : exact) {
                if (name.equals(e)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final String[] NONE = {};

    private static Rule rule(String category, String[] prefixes, String[] contains, String[] exact) {
        return new Rule(category, prefixes, contains, exact);
    }

    private static String[] a(String... s) {
        return s;
    }

    /** First match wins, so more specific rules come first. */
    private static final List<Rule> MODEL_RULES = List.of(
            rule(SHADOWS, a("bshadow", "shadow", "groundshadow"), NONE, NONE),
            rule(INDOOR, a("carpet", "floor", "housewall", "modern_", "wall_top", "table", "wooden_floor"), NONE, NONE),
            rule(TREES, a("fenter"), a("tree"), NONE),
            rule(CLIFFS, a("mountain", "montain", "jump", "ptrc", "rockclimb", "smoun", "snmoun", "cave"), NONE,
                    a("mt", "cor", "dun", "chole")),
            rule(SNOW, a("snow", "bigice", "longice", "lowice", "mediumice"), NONE, NONE),
            rule(BRIDGES, a("bridge", "bbridge", "stair", "outstair"), NONE, NONE),
            rule(OBJECTS, a("searock", "oceanrock"), NONE, NONE),
            rule(SAND, a("beachsand", "sand", "nsand"), NONE, NONE),
            rule(PATHS, a("road", "groad", "sroad", "graypath", "artroad", "dirtpath", "grasspath", "concrete",
                    "piedras", "relleno", "squareg", "cside", "ccor"), NONE, NONE),
            rule(WATER, a("sea", "lake", "water", "border", "beachext", "puddle", "pudd"), NONE, a("mar")),
            rule(PLANTS, a("tallgrass", "berr", "flower", "bush", "rawbush", "hedge", "grassmark"), a("hana"), NONE),
            rule(OBJECTS, a("fence", "jfence", "frence", "rock", "srock", "brock", "valla", "lamp", "cyclepoint"),
                    NONE, NONE),
            rule(GROUND, a("grass"), NONE, NONE)
    );

    /** Used when the model name says nothing: textures of numbered tiles like "1.obj". */
    private static final List<Rule> TEXTURE_RULES = List.of(
            rule(CLIFFS, a("allpeak", "dun_allpeak", "peak", "criff", "cliff", "wcliff", "a11_"), NONE, NONE),
            rule(TREES, a("tree", "fenter", "conttree"), NONE, NONE),
            rule(OBJECTS, a("c1_lamp", "c1_f_", "imped"), NONE, NONE),
            rule(WATER, a("sea", "lake", "pudd", "hamabe"), NONE, NONE),
            rule(SAND, a("nsand", "beach"), NONE, NONE),
            rule(PLANTS, a("nectgr", "plant", "hana"), NONE, NONE),
            rule(PATHS, a("road", "blueglay", "ckado"), NONE, NONE)
    );

    private TileCategories() {
    }

    /** The folder name for a tile; OTHER if nothing matches. */
    public static String categorize(Tile tile) {
        String model = modelStem(tile.getObjFilename());
        for (Rule r : MODEL_RULES) {
            if (r.matches(model)) {
                return r.category;
            }
        }
        List<String> textures = new ArrayList<>();
        for (int id : tile.getTextureIDs()) {
            textures.add(textureStem(tile.getTileset().getMaterial(id).getImageName()));
        }
        for (Rule r : TEXTURE_RULES) {
            for (String t : textures) {
                if (r.matches(t)) {
                    return r.category;
                }
            }
        }
        return OTHER;
    }

    private static String modelStem(String objFilename) {
        String name = objFilename == null ? "" : objFilename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".obj")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.replaceAll("[\\d_]+$", "");
    }

    private static String textureStem(String imageName) {
        String name = imageName == null ? "" : imageName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.replaceAll("_lm\\d+$", "");
    }

    /** Category -> tile indices, in display order, for tools and tests. */
    public static LinkedHashMap<String, List<Integer>> group(Tileset tileset) {
        LinkedHashMap<String, List<Integer>> groups = new LinkedHashMap<>();
        for (String c : ORDER) {
            groups.put(c, new ArrayList<>());
        }
        for (int i = 0; i < tileset.size(); i++) {
            groups.get(categorize(tileset.get(i))).add(i);
        }
        return groups;
    }
}
