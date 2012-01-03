package org.springframework.build.docbook;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Accept a source file.
 * transform it to chunked HTML
 * with images
 * with syntax highlighting
 * with ${...} replacement
 *
 * @author Chris Beams
 */
public class DocbookTransformer {

	private boolean xIncludeAware = true;
	private String sourceFilePath;
	private String stylesheetPath;

	public void setXIncludeAware(boolean xIncludeAware) {
		this.xIncludeAware = xIncludeAware;
	}

	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}

	public void setStylesheetPath(String stylesheetPath) {
		this.stylesheetPath = stylesheetPath;
	}

	public void transform() throws Exception {

		if (sourceFilePath == null)
			throw new IllegalArgumentException("sourceFilePath must not be null");
		if (stylesheetPath == null)
			throw new IllegalArgumentException("stylesheetPath must not be null");

		SAXParserFactory factory = new SAXParserFactoryImpl();
		factory.setXIncludeAware(xIncludeAware);

		File srcFile = new File(sourceFilePath);
		String outputFilename = srcFile.getName().substring(0, srcFile.getName().length() - 4) + '.' + "html";

		File oDir = new File("/tmp");
		File outputFile = new File(oDir, outputFilename);

		Result result = new StreamResult(outputFile.getAbsolutePath());
		CatalogResolver resolver = new CatalogResolver(createCatalogManager());
		InputSource inputSource = new InputSource(srcFile.getAbsolutePath());

		XMLReader reader = factory.newSAXParser().getXMLReader();
		reader.setEntityResolver(resolver);
		TransformerFactory transformerFactory = new TransformerFactoryImpl();
		transformerFactory.setURIResolver(resolver);
		File stylesheet = new File(stylesheetPath);
		URL url = stylesheet.toURL();
		Source source = new StreamSource(url.openStream(), url.toExternalForm());
		Transformer transformer = transformerFactory.newTransformer(source);

		transformer.transform(new SAXSource(reader, inputSource), result);

		/*
		docsDir.mkdirs();

		File srcFile = new File(sourceDirectory, sourceFileName);
		String outputFilename = srcFile.getName().substring(0, srcFile.getName().length() - 4) + '.' + extension;

		File oDir = new File(getDocsDir(), xdir)
		File outputFile = new File(oDir, outputFilename);

		Result result = new StreamResult(outputFile.getAbsolutePath());
		CatalogResolver resolver = new CatalogResolver(createCatalogManager());
		InputSource inputSource = new InputSource(srcFile.getAbsolutePath());

		XMLReader reader = factory.newSAXParser().getXMLReader();
		reader.setEntityResolver(resolver);
		TransformerFactory transformerFactory = new TransformerFactoryImpl();
		transformerFactory.setURIResolver(resolver);
		URL url = stylesheet.toURL();
		Source source = new StreamSource(url.openStream(), url.toExternalForm());
		Transformer transformer = transformerFactory.newTransformer(source);

		if (highlightingEnabled) {
			File highlightingDir = new File(getProject().getBuildDir(), "highlighting");
			if (!highlightingDir.exists()) {
				highlightingDir.mkdirs();
				extractHighlightFiles(highlightingDir);
			}

			transformer.setParameter("highlight.source", "1");
			transformer.setParameter("highlight.xslthl.config", new File(highlightingDir, "xslthl-config.xml").toURI().toURL());

			if (admonGraphicsPath != null) {
				transformer.setParameter("admon.graphics", "1");
				transformer.setParameter("admon.graphics.path", admonGraphicsPath);
			}
		}

		preTransform(transformer, srcFile, outputFile);

		transformer.transform(new SAXSource(reader, inputSource), result);

		postTransform(outputFile);
		*/
	}
	
    private CatalogManager createCatalogManager() throws IOException {
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
