package editor.converter;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

/** Locates the external Nintendo g3dcvtr tool independently of launch location. */
public final class ConverterLocator {

    private static final String EXECUTABLE = "g3dcvtr.exe";

    private ConverterLocator() {
    }

    public static String getConverterPath() {
        for (Path candidate : getCandidates()) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toString();
            }
        }
        return Paths.get("converter", EXECUTABLE).toAbsolutePath().normalize().toString();
    }

    /**
     * Path of a file as the converter must receive it. On Windows the plain path; under Wine the
     * absolute path on Wine's Z: drive (the whole Unix file system), so it works from any working
     * directory. (A relative path only works when the file is below the working directory.)
     */
    public static String toConverterPath(String path) {
        if (isWindows()) {
            return path;
        }
        return "Z:" + Paths.get(path).toAbsolutePath().normalize().toString().replace('/', '\\');
    }

    /**
     * Why the converter couldn't be started, for error dialogs: the file is missing, or (on macOS and
     * Linux) Wine, which runs it, isn't installed.
     */
    public static String converterNotFoundMessage() {
        if (!Files.isRegularFile(Paths.get(getConverterPath()))) {
            return "The program \"g3dcvtr.exe\" is not found in the \"converter\" folder.\n"
                    + "Put the program and its *.dll files in the folder and try again.";
        }
        if (!isWindows() && !isWineInstalled()) {
            return "Wine is needed to run \"g3dcvtr.exe\" on macOS and Linux, but it isn't installed.\n"
                    + "Install Wine (macOS: brew install --cask wine-stable, or the Wine app from winehq.org;\n"
                    + "Linux: your distribution's wine package), then start the editor again.";
        }
        return "The program \"g3dcvtr.exe\" could not be started.\n"
                + "Check that it and its *.dll files are in the \"converter\" folder"
                + (isWindows() ? "." : " and that Wine works (\"wine --version\" in a terminal).");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /** Whether a "wine" executable is on the PATH (that is how the export runs the converter). */
    static boolean isWineInstalled() {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (!dir.isEmpty() && Files.isExecutable(Paths.get(dir, "wine"))) {
                return true;
            }
        }
        return false;
    }

    private static LinkedHashSet<Path> getCandidates() {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        addCandidateAndAncestors(candidates, Paths.get(System.getProperty("user.dir", ".")));
        try {
            URI location = ConverterLocator.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path appPath = Paths.get(location).toAbsolutePath().normalize();
            addCandidateAndAncestors(candidates,
                    Files.isDirectory(appPath) ? appPath : appPath.getParent());
        } catch (Exception ignored) {
        }
        return candidates;
    }

    private static void addCandidateAndAncestors(LinkedHashSet<Path> candidates, Path start) {
        Path current = start == null ? null : start.toAbsolutePath().normalize();
        for (int depth = 0; current != null && depth < 6; depth++) {
            candidates.add(current.resolve("converter").resolve(EXECUTABLE));
            current = current.getParent();
        }
    }
}
