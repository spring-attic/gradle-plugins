package org.springframework.build.docbook;

import com.icl.saxon.TransformerFactoryImpl;

import java.util.zip.*;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.*;

import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.*;

import org.slf4j.LoggerFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class DocbookPlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.task('docbookHtml', type: DocbookHtml) << {
            description = 'Generates chunked docbook html output.'
            xdir = 'html'
            //classpath = buildscript.configurations.classpath
        }
    }

}

class DocbookHtml extends Docbook {

    @Override
    protected void preTransform(Transformer transformer, File sourceFile, File outputFile) {
        String rootFilename = outputFile.getName();
        rootFilename = rootFilename.substring(0, rootFilename.lastIndexOf('.'));
        transformer.setParameter("root.filename", rootFilename);
        transformer.setParameter("base.dir", outputFile.getParent() + File.separator);
    }
}

public class Docbook extends DefaultTask {
    @Input
    String extension = 'html';

    @Input
    boolean XIncludeAware = true;

    @Input
    boolean highlightingEnabled = true;

    String admonGraphicsPath;

    @InputDirectory
    File sourceDirectory = new File(project.getProjectDir(), "build/reference-work");

    @Input
    String sourceFileName;

    @InputFile
    File stylesheet;

    @OutputDirectory
    File docsDir = new File(project.getBuildDir(), "reference");

    @InputFiles
    Configuration classpath

    @TaskAction
    public final void transform() {
        // the docbook tasks issue spurious content to the console. redirect to INFO level
        // so it doesn't show up in the default log level of LIFECYCLE unless the user has
        // run gradle with '-d' or '-i' switches -- in that case show them everything
        switch (project.gradle.startParameter.logLevel) {
            case LogLevel.DEBUG:
            case LogLevel.INFO:
                break;
            default:
                logging.captureStandardOutput(LogLevel.INFO)
                logging.captureStandardError(LogLevel.INFO)
        }

        SAXParserFactory factory = new org.apache.xerces.jaxp.SAXParserFactoryImpl();
        factory.setXIncludeAware(XIncludeAware);
        docsDir.mkdirs();

        File srcFile = new File(sourceDirectory, sourceFileName);
        String outputFilename = srcFile.getName().substring(0, srcFile.getName().length() - 4) + '.' + extension;

        File oDir = new File(getDocsDir(), xdir)
        File outputFile = new File(oDir, outputFilename);

        Result result = new StreamResult(outputFile.getAbsolutePath());
        CatalogResolver resolver = new CatalogResolver(createCatalogManager());
        InputSource inputSource = new InputSource(srcFile.getAbsolutePath());

        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(resolver);
        TransformerFactory transformerFactory = new TransformerFactoryImpl();
        transformerFactory.setURIResolver(resolver);
        URL url = stylesheet.toURL();
        Source source = new StreamSource(url.openStream(), url.toExternalForm());
        Transformer transformer = transformerFactory.newTransformer(source);

        if (highlightingEnabled) {
            File highlightingDir = new File(getProject().getBuildDir(), "highlighting");
            if (!highlightingDir.exists()) {
                highlightingDir.mkdirs();
                extractHighlightFiles(highlightingDir);
            }

            transformer.setParameter("highlight.source", "1");
            transformer.setParameter("highlight.xslthl.config", new File(highlightingDir, "xslthl-config.xml").toURI().toURL());

            if (admonGraphicsPath != null) {
                transformer.setParameter("admon.graphics", "1");
                transformer.setParameter("admon.graphics.path", admonGraphicsPath);
            }
        }

        preTransform(transformer, srcFile, outputFile);

        transformer.transform(new SAXSource(reader, inputSource), result);

        postTransform(outputFile);
    }

    private void extractHighlightFiles(File toDir) {
        File docbookZip = classpath.files.find { file -> file.name.contains('docbook-xsl-')};

        if (docbookZip == null) {
            throw new GradleException("Docbook zip file not found");
        }

        ZipFile zipFile = new ZipFile(docbookZip);

        Enumeration e = zipFile.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            if (ze.getName().matches(".*/highlighting/.*\\.xml")) {
                String filename = ze.getName().substring(ze.getName().lastIndexOf("/highlighting/") + 14);
                copyFile(zipFile.getInputStream(ze), new File(toDir, filename));
            }
        }
    }

    private void copyFile(InputStream source, File destFile) {
        destFile.createNewFile();
        FileOutputStream to = null;
        try {
            to = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = source.read(buffer)) > 0) {
                to.write(buffer, 0, bytesRead);
            }
        } finally {
            if (source != null) {
                source.close();
            }
            if (to != null) {
                to.close();
            }
        }
    }

    protected void preTransform(Transformer transformer, File sourceFile, File outputFile) {
    }

    protected void postTransform(File outputFile) {
    }

    private CatalogManager createCatalogManager() {
        CatalogManager manager = new CatalogManager();
        manager.setIgnoreMissingProperties(true);
        ClassLoader classLoader = this.getClass().getClassLoader();
        StringBuilder builder = new StringBuilder();
        String docbookCatalogName = "docbook/catalog.xml";
        URL docbookCatalog = classLoader.getResource(docbookCatalogName);

        if (docbookCatalog == null) {
            throw new IllegalStateException("Docbook catalog " + docbookCatalogName + " could not be found in " + classLoader);
        }

        builder.append(docbookCatalog.toExternalForm());

        Enumeration enumeration = classLoader.getResources("/catalog.xml");
        while (enumeration.hasMoreElements()) {
            builder.append(';');
            URL resource = (URL) enumeration.nextElement();
            builder.append(resource.toExternalForm());
        }
        String catalogFiles = builder.toString();
        manager.setCatalogFiles(catalogFiles);
        return manager;
    }
}
