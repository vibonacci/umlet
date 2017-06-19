package com.baselet.standalone;

import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.io.DiagramFileHandler;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import com.itextpdf.text.pdf.PdfReader;  

import javax.imageio.ImageIO;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.util.Scanner;
import static org.junit.Assert.fail;

/**
 * Test several different Batch exports
 * Currently pdf is not tested because it's hard to do a visual comparison of a pdf. svg and eps are cleaned up to make this comparison work
 * To let those tests work on build servers such as Travis CI, only svg uses text in the uxf files (because only svg stores the information system-neutral, eps and png seem to look different on different systems which makes the validation of the output too complex)
 */
public class MainBatchmodeTest {

	static String TEST_FILE_LOCATION; // the testfile is a CustomElement with custom fontsize and fontfamily to simulate a special edge case

	@Rule
	public TemporaryFolder tmpDir = new TemporaryFolder();

	@BeforeClass
	public static void beforeClass() throws URISyntaxException {
		TEST_FILE_LOCATION = MainBatchmodeTest.class.getProtectionDomain().getCodeSource().getLocation().toURI().getSchemeSpecificPart() + MainBatchmodeTest.class.getCanonicalName().replace(".", "/").replace(MainBatchmodeTest.class.getSimpleName(), "");
	}

	@Test
	public void batchConvertToSvg_diagramSpecificFontSizeAndFamily() throws Exception {
		// whitespaces must be trimmed for the test (SVG is an XML based format and whitespaces can be different)
		File expected = changeLines(new File(TEST_FILE_LOCATION + "out_diagramFontSizeAndFamily.svg"), null, true);
		File actual = changeLines(createOutputfile("svg", "in_diagramFontSizeAndFamily.uxf"), null, true);
		assertFilesEqual(expected, actual);
	}

	@Test
	public void batchConvertToSvg_diagramSpecificFontSizeAndFamilyxxx() throws Exception {
		// whitespaces must be trimmed for the test (SVG is an XML based format and whitespaces can be different)
		File expected = changeLines(new File(TEST_FILE_LOCATION + "out_newAllInOne.svg"), null, true);
		File actual = changeLines(createOutputfile("svg", "in_newAllInOne.uxf"), null, true);
		assertFilesEqual(expected, actual);
	}

	@Test
	public void batchConvertToPng_wildcardAndNoOutputParam_newCustomElement() throws Exception {
		File copy = copyInputToTmp("in_newCustomElement.uxf");
		String wildcard = copy.getParent().toString() + "/*";
		MainStandalone.main(new String[] { "-action=convert", "-format=png", "-filename=" + wildcard });
		assertImageEqual(new File(TEST_FILE_LOCATION + "out_newCustomElement.png"), new File(copy + "." + "png"));
	}

	@Test
	public void batchConvertToEps_newCustomElement() throws Exception {
		// eps files contain the CreationDate which must be removed before the comparison
		File output = changeLines(createOutputfile("eps", "in_newCustomElement.uxf"), "%%CreationDate", false);
		File outWithoutCreated = changeLines(copyToTmp("out_newCustomElement.eps"), "%%CreationDate", false);
		assertFilesEqual(outWithoutCreated, output);
	}
        
        @Test
        public void testOnzin() {
            boolean x = true;
            assertTrue(x);
        }
        
        
        //IGOR & MOE
        @Test
	public void assertIfFileIsSaved() throws IOException {
            //LOCATION: "umlet-standalone\src\test\resources\com\baselet\standalone\"
            File testedFile = null;
            testedFile = createOutputfile("svg", "igortest.uxf");
            System.out.println(testedFile.getAbsolutePath());
            assertTrue("The saved file igortest.svg does not exist", testedFile.exists());
	}
        
        //IGOR & MOE
        @Test
	public void assertIfPDFFileIsSaved() throws IOException {
            //LOCATION: "umlet-standalone\src\test\resources\com\baselet\standalone\"
            File testedFile = createOutputfile("pdf", "igortest.uxf"); 
            assertTrue("The saved file igortest.pdf does not exist", testedFile.exists());
	}    
        
        //IGOR & MOE
        @Test
	public void assertIfPDFFileIsNotCorrupt() throws IOException {
            //LOCATION: "umlet-standalone\src\test\resources\com\baselet\standalone\"
            File testedFile = createOutputfile("pdf", "igortest.uxf"); 
            //File testedFile = createOutputfile("svg", "igortest.uxf"); //test the test
            
            try {  
                PdfReader pdfReader = new PdfReader(testedFile.getAbsolutePath());  
                String textFromPdfFilePageOne = PdfTextExtractor.getTextFromPage( pdfReader, 1 ); 
                assertTrue(true); 
                
            }  
            catch ( Exception e ) {  
                fail("PDF FILE IS CORRUPT");
            }       
	}            
        
        //IGOR & MOE
        @Test
	public void assertIfNotSavedFileExists() throws IOException {
            File testedFile = null;
            try {
                testedFile = createOutputfile("svg", "fileDoesNotExist.uxf");
            }
            catch(IOException e) {}
            assertTrue("An unexisting file seems to exist. Test Failed.", testedFile == null);
	}     
        
        //IGOR & MOE
        @Test
        public void assertIfSavedFileContainsProperElements() throws IOException { 
            File testedFile = createOutputfile("svg", "igortest.uxf");
            Scanner scanner = new Scanner(testedFile);

            boolean found = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(">igor</text")) { //check if file contains UML class object "igor"
                    found = true;
                    break;
                }
            }    
            assertTrue("The file does not contain the expected element igor", found);
        }   
        
        //IGOR & MOE
        @Test
        //file ooadtest.uxf is a generated UML diagram by UMLET with classes Klant, Pakket, Ingredient, Abonnement
        public void testGenerateClassElements() throws IOException {
            File testedFile = null;  
            //testedFile = createOutputfileCustom("svg", "ooadtest.uxf");   
            testedFile = createOutputfile("svg", "ooadtest.uxf"); 
            
            Scanner scanner = new Scanner(testedFile);
            int foundCounter = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                 //check if file contains the four expected classes
                if (line.contains(">ooad::Abonnement</text") || line.contains(">ooad::Klant</text")
                        || line.contains(">ooad::Ingredient</text") || line.contains(">ooad::Pakket</text")) {
                    foundCounter ++;
                }
                if (foundCounter == 4) {
                    break;
                }
            }               
            assertTrue("Not all four expected elements Klant, Pakket, Ingredient, Abonnement are found in the saved file.",foundCounter == 4);
        }  
        
        @Test
        //file ooadtest.uxf is a generated UML diagram by UMLET with attributes pakketNaam: String, voorraad: boolean, klantNaam: String and prijs: Double
        public void testGenerateClassDiagramAttributes() throws IOException {
            File testedFile = null;  
            //testedFile = createOutputfileCustom("svg", "ooadtest.uxf");   
            testedFile = createOutputfile("svg", "ooadtest.uxf"); 
            
            Scanner scanner = new Scanner(testedFile);
            int foundCounter = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                
                //check if file contains the four expected attributes of classes
                if (line.contains(">~pakketNaam: String</text") || line.contains(">~voorraad: boolean</text")
                        || line.contains(">~klantNaam: String</text") || line.contains(">~prijs: double</text")) {
                    foundCounter ++;
                }
                if (foundCounter == 4) {
                    break;
                }
            }               
            assertTrue("Not all four expected attributes pakketNaam: String, voorraad: boolean, klantNaam: String and prijs: Double"
                    + " are found in the saved file.",foundCounter == 4);
        }    
        
        @Test
        //file ooadtest.uxf is a generated UML diagram by UMLET with methods updateProfile(), Klant(), updateVoorraad(), Ingredient()
        public void testGenerateClassDiagramMethods() throws IOException {
            File testedFile = null;  
            //testedFile = createOutputfileCustom("pdf", "ooadtest.uxf");   
            testedFile = createOutputfile("svg", "ooadtest.uxf"); 
            
            Scanner scanner = new Scanner(testedFile);
            int foundCounter = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                
                //check if file contains the four expected methods of classes
                if (line.contains(">+updateProfile(int klantId, String klantnaam, String adres, String email, String betalingsgegevens, String afleveradres, String password): void</text")
                        || line.contains(">+Klant(int klantId, String klantNaam, String adres, String email, String betalingsgegevens, String afleveradres, String password): ctor</text")
                        || line.contains(">~updateVoorraad(int hoeveelheid): void</text")
                        || line.contains(">+Ingredient(int ingredientID, String toevoegDatum, int voorraad, String allergenen, String ingredientNaam): ctor</text")) {
                    foundCounter ++;
                }
                if (foundCounter == 4) {
                    break;
                }
            }               
            assertTrue("Not all four expected methods updateProfile(), Klant(), updateVoorraad(), Ingredient()"
                    + " are found in the saved file." ,foundCounter == 4);
        }         
        
        //igor moe manual create file
	private File createOutputfileCustom(String format, String inputFilename) throws IOException {
		String outputFileLoc = "c:/bla." + format;
		MainStandalone.main(new String[] { "-action=convert", "-format=" + format, "-filename=" + copyInputToTmp(inputFilename), "-output=" + outputFileLoc });
		File outputFile = new File(outputFileLoc);
		return outputFile;
	}        
        
        
        public void assertContentSavedFile() throws IOException {
        }

	private void assertImageEqual(File expected, File actual) throws IOException {
		BufferedImage expectedPicture = ImageIO.read(expected);
		BufferedImage actualPicture = ImageIO.read(actual);

		int expectedHeight = expectedPicture.getHeight();
		int expectedWidth = expectedPicture.getWidth();
		String expSize = Integer.toString(expectedWidth) + "x" + Integer.toString(expectedHeight);
		String actSize = Integer.toString(actualPicture.getWidth()) + "x" + Integer.toString(actualPicture.getHeight());
		assertTrue("The size of the images " + expected + " and " + actual + " must match. Expected: " + expSize + ", Actual: " + actSize, expSize.equals(actSize));

		for (int y = 0; y < expectedHeight; y++) {
			for (int x = 0; x < expectedWidth; x++) {
				assertTrue("The images " + expected + " and " + actual + " don't match in the pixel (" + x + "/" + y + ")", expectedPicture.getRGB(x, y) == actualPicture.getRGB(x, y));
			}
		}
	}

	private void assertFilesEqual(File expected, File actual) throws IOException {
		assertTrue("The content of both files must match. Expected: " + expected + ", Actual: " + actual, Files.equal(expected, actual));
	}

	private File changeLines(File output, String excludedPrefix, boolean streamlineWhitespaces) throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(output);
		String s;
		String totalStr = "";
		BufferedReader br = null;
		try {
			br = new BufferedReader(fr);
			while ((s = br.readLine()) != null) {
				if (excludedPrefix != null && !s.startsWith(excludedPrefix)) {
					totalStr += s + System.getProperty("line.separator");
				}
			}
			FileWriter fw = new FileWriter(output);
			if (streamlineWhitespaces) {
				totalStr = totalStr.replaceAll("\\s+", " ");
			}
			fw.write(totalStr);
			fw.close();
			return output;
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	private File createOutputfile(String format, String inputFilename) throws IOException {
		String outputFileLoc = tmpDirString() + "bla." + format;
		MainStandalone.main(new String[] { "-action=convert", "-format=" + format, "-filename=" + copyInputToTmp(inputFilename), "-output=" + outputFileLoc });
		File outputFile = new File(outputFileLoc);
		return outputFile;
	}

	private File copyInputToTmp(String inputFilename) throws IOException {
		return copyToTmp(inputFilename);
	}

	private File copyToTmp(String file) throws IOException {
		File newFile = tmpDir.newFile();
		Files.copy(new File(TEST_FILE_LOCATION + file), newFile);
		return newFile;
	}

	private String tmpDirString() {
		return tmpDir.getRoot().toURI().getSchemeSpecificPart();
	}

}
