package appeng.compiler;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import appeng.client.guidebook.Guide;
import appeng.client.guidebook.GuidePage;
import appeng.client.guidebook.compiler.PageCompiler;
import appeng.client.guidebook.extensions.ExtensionCollection;
import appeng.core.AppEng;
import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class PageCompilerTest {
    private Path guidebookFolder;

    @BeforeEach
    void setUp() throws Exception {
        guidebookFolder = findGuidebookFolder();
    }

    @Test
    void testCompileIndexPage() throws Exception {
        compilePage("index");
    }

    private GuidePage compilePage(String id) throws Exception {
        var path = guidebookFolder.resolve(id + ".md");
        try (var in = Files.newInputStream(path)) {
            var parsed = PageCompiler.parse("ae2", AppEng.makeId(id), in);
            var testPages = Guide.builder(AppEng.MOD_ID, "ae2guide")
                    .developmentSources(guidebookFolder)
                    .watchDevelopmentSources(false)
                    .registerReloadListener(false)
                    .build();
            return PageCompiler.compile(testPages, ExtensionCollection.empty(), parsed);
        }
    }

    private static Path findGuidebookFolder() throws Exception {
        var sources = System.getProperty("guideDev.ae2guide.sources");
        if (sources != null) {
            return Paths.get(sources);
        }

        // Search up for the guidebook folder
        var url = PageCompilerTest.class.getProtectionDomain().getCodeSource().getLocation();
        var jarPath = Paths.get(url.toURI());
        var current = jarPath.getParent();
        while (current != null) {
            var guidebookFolder = current.resolve("guidebook");
            if (Files.isDirectory(guidebookFolder) && Files.exists(guidebookFolder.resolve("index.md"))) {
                return guidebookFolder;
            }

            current = current.getParent();
        }

        throw new FileNotFoundException("Couldn't find guidebook folder. Started looking at " + jarPath);
    }
}
