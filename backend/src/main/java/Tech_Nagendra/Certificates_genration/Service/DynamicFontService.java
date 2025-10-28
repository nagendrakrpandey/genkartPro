package Tech_Nagendra.Certificates_genration.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Service
public class DynamicFontService {

    private final Map<String, Font> loadedFonts = new ConcurrentHashMap<>();
    private final Set<String> availableFontFamilies = ConcurrentHashMap.newKeySet();
    private boolean fontsInitialized = false;

    @PostConstruct
    public void autoLoadFonts() {
        System.out.println("Auto-loading fonts on application startup...");
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                loadFontsDynamically();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public synchronized void loadFontsDynamically() {
        if (fontsInitialized) {
            System.out.println(" Fonts already initialized");
            return;
        }

        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            System.out.println(" Starting dynamic font loading...");

            int resourcesCount = loadFontsFromClasspath("classpath:fonts/**/*.ttf");
            resourcesCount += loadFontsFromClasspath("classpath:fonts/**/*.otf");
            resourcesCount += loadFontsFromClasspath("classpath:fonts/**/*.jar"); // JAR files

            int externalCount = loadFontsFromExternalDirectory();

            registerFontsWithSystem(ge);
            collectAvailableFontFamilies(ge);

            fontsInitialized = true;

            System.out.println(" Dynamic font loading completed!");
            System.out.println(" Loaded " + loadedFonts.size() + " font files");
            System.out.println(" Available font families: " + availableFontFamilies.size());

            printLoadedFonts();

        } catch (Exception e) {
            System.err.println("Error in dynamic font loading: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int loadFontsFromClasspath(String pattern) {
        int count = 0;
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(pattern);

            System.out.println("Found " + resources.length + " resources matching: " + pattern);

            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null) continue;

                    if (filename.toLowerCase().endsWith(".jar")) {
                        count += loadFontsFromJar(resource);
                    } else {
                        count += loadIndividualFontFile(resource, filename);
                    }
                } catch (Exception e) {
                    System.err.println(" Error loading classpath resource " + resource.getFilename() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println(" Error scanning classpath fonts: " + e.getMessage());
        }
        return count;
    }

    private int loadIndividualFontFile(Resource resource, String filename) {
        try {
            String fontKey = generateFontKey(filename);

            if (!loadedFonts.containsKey(fontKey)) {
                try (InputStream is = resource.getInputStream()) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                    loadedFonts.put(fontKey, font);
                    System.out.println("Loaded classpath font: " + filename);
                    return 1;
                }
            }
        } catch (Exception e) {
            System.err.println(" Error loading font file " + filename + ": " + e.getMessage());
        }
        return 0;
    }

    private int loadFontsFromJar(Resource jarResource) {
        int fontCount = 0;
        System.out.println("Processing JAR file: " + jarResource.getFilename());

        try {
            // Create temporary file for the JAR
            java.io.File tempJarFile = java.io.File.createTempFile("fontjar_", ".jar");
            tempJarFile.deleteOnExit();

            // Copy JAR resource to temporary file
            try (InputStream is = jarResource.getInputStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tempJarFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // Process JAR file
            try (JarFile jarFile = new JarFile(tempJarFile)) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName().toLowerCase();

                    // Check if entry is a font file
                    if (!entry.isDirectory() &&
                            (entryName.endsWith(".ttf") || entryName.endsWith(".otf"))) {

                        fontCount += extractAndLoadFontFromJar(jarFile, entry, entryName);
                    }
                }
            }

            System.out.println(" Loaded " + fontCount + " fonts from JAR: " + jarResource.getFilename());

        } catch (Exception e) {
            System.err.println(" Error processing JAR file " + jarResource.getFilename() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return fontCount;
    }

    private int extractAndLoadFontFromJar(JarFile jarFile, JarEntry entry, String entryName) {
        try {
            String fontKey = generateFontKey(entryName);

            if (!loadedFonts.containsKey(fontKey)) {
                // Create temporary file for the font
                java.io.File tempFontFile = java.io.File.createTempFile("font_", getFileExtension(entryName));
                tempFontFile.deleteOnExit();

                // Extract font from JAR to temporary file
                try (InputStream is = jarFile.getInputStream(entry);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFontFile)) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                // Load font from temporary file
                Font font = Font.createFont(Font.TRUETYPE_FONT, tempFontFile);
                loadedFonts.put(fontKey, font);

                System.out.println("    Extracted font from JAR: " + entryName +
                        " (Family: " + font.getFamily() + ")");
                return 1;
            }
        } catch (Exception e) {
            System.err.println(" Error extracting font from JAR entry " + entryName + ": " + e.getMessage());
        }
        return 0;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? ".tmp" : filename.substring(lastDot);
    }

    private int loadFontsFromExternalDirectory() {
        int count = 0;
        try {
            String[] possiblePaths = {
                    "C:/fonts",
                    "D:/fonts",
                    System.getProperty("user.home") + "/fonts",
                    "/usr/share/fonts",
                    "/Library/Fonts",
                    "fonts/" // Current directory fonts folder
            };

            for (String path : possiblePaths) {
                count += loadFontsFromPath(path);
            }
        } catch (Exception e) {
            System.err.println(" Error loading external fonts: " + e.getMessage());
        }
        return count;
    }

    private int loadFontsFromPath(String directoryPath) {
        int count = 0;
        try {
            java.io.File fontDir = new java.io.File(directoryPath);
            if (!fontDir.exists() || !fontDir.isDirectory()) {
                return 0;
            }

            System.out.println(" Scanning external directory: " + directoryPath);

            java.io.File[] files = fontDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".ttf") ||
                            name.toLowerCase().endsWith(".otf") ||
                            name.toLowerCase().endsWith(".jar")
            );

            if (files != null) {
                for (java.io.File file : files) {
                    if (file.getName().toLowerCase().endsWith(".jar")) {
                        count += loadFontsFromExternalJar(file);
                    } else {
                        count += loadIndividualExternalFont(file);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(" Error scanning directory " + directoryPath + ": " + e.getMessage());
        }
        return count;
    }

    private int loadFontsFromExternalJar(java.io.File jarFile) {
        int fontCount = 0;
        System.out.println(" Processing external JAR: " + jarFile.getName());

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName().toLowerCase();

                if (!entry.isDirectory() &&
                        (entryName.endsWith(".ttf") || entryName.endsWith(".otf"))) {

                    fontCount += extractAndLoadFontFromJar(jar, entry, entryName);
                }
            }

            System.out.println(" Loaded " + fontCount + " fonts from external JAR: " + jarFile.getName());

        } catch (Exception e) {
            System.err.println(" Error processing external JAR " + jarFile.getName() + ": " + e.getMessage());
        }
        return fontCount;
    }

    private int loadIndividualExternalFont(java.io.File fontFile) {
        try {
            String fontKey = generateFontKey(fontFile.getName());

            if (!loadedFonts.containsKey(fontKey)) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                loadedFonts.put(fontKey, font);
                System.out.println(" Loaded external font: " + fontFile.getName());
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Error loading external font " + fontFile.getName() + ": " + e.getMessage());
        }
        return 0;
    }

    private void registerFontsWithSystem(GraphicsEnvironment ge) {
        int registeredCount = 0;
        for (Font font : loadedFonts.values()) {
            try {
                ge.registerFont(font);
                registeredCount++;
            } catch (Exception e) {
                System.err.println(" Error registering font: " + e.getMessage());
            }
        }
        System.out.println(" Registered " + registeredCount + " fonts with system");
    }

    private void collectAvailableFontFamilies(GraphicsEnvironment ge) {
        String[] fontFamilies = ge.getAvailableFontFamilyNames();
        availableFontFamilies.clear();
        availableFontFamilies.addAll(Arrays.asList(fontFamilies));
    }

    private String generateFontKey(String filename) {
        return filename.toLowerCase().replaceAll("[^a-z0-9.]", "_");
    }

    private void printLoadedFonts() {
        System.out.println("\n LOADED FONT FILES:");
        loadedFonts.keySet().stream()
                .sorted()
                .forEach(key -> {
                    Font font = loadedFonts.get(key);
                    System.out.println("   • " + key + " (Family: " + font.getFamily() + ")");
                });

        System.out.println("\n AVAILABLE FONT FAMILIES:");
    }

    public Font getFont(String fontName, int style, float size) {
        for (Map.Entry<String, Font> entry : loadedFonts.entrySet()) {
            if (entry.getKey().toLowerCase().contains(fontName.toLowerCase())) {
                return entry.getValue().deriveFont(style, size);
            }
        }

        return new Font(fontName, style, (int) size);
    }

    public Set<String> getAvailableFontFamilies() {
        return new HashSet<>(availableFontFamilies);
    }

    public Map<String, Font> getLoadedFonts() {
        return new HashMap<>(loadedFonts);
    }

    public boolean isFontFamilyAvailable(String fontFamily) {
        return availableFontFamilies.stream()
                .anyMatch(family -> family.equalsIgnoreCase(fontFamily));
    }

    public synchronized void reloadFonts() {
        fontsInitialized = false;
        loadedFonts.clear();
        availableFontFamilies.clear();
        loadFontsDynamically();
    }

    public Map<String, Object> getFontInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("loadedFontFiles", loadedFonts.size());
        info.put("availableFontFamilies", availableFontFamilies.size());
        info.put("fontsInitialized", fontsInitialized);
        info.put("loadedFontKeys", new ArrayList<>(loadedFonts.keySet()));

        List<String> sampleFamilies = availableFontFamilies.stream()
                .sorted()
                .limit(10)
                .toList();
        info.put("sampleFontFamilies", sampleFamilies);

        return info;
    }
}