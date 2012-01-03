package org.springframework.build.docbook;

import org.junit.Test;

public class DocbookTransformerTests {
	@Test
	public void test() throws Exception {
		DocbookTransformer dt = new DocbookTransformer();
		dt.setSourceFilePath("/tmp/index.xml");
		dt.setStylesheetPath("/tmp/stylesheet.xsl");
		dt.transform();
	}
}
