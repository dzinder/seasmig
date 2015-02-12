package test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.GregorianCalendar;

import jebl.evolution.io.ImportException;

import org.junit.Test;

import seasmig.data.Data;
import seasmig.migrationmain.Config;
import seasmig.treelikelihood.LikelihoodTree;
import test.DataForTests.TestType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestLikelihood {
	// TODO: Organize this....
	
	@Test
	public void testLikelihood() throws IOException, ImportException {
		final int numTestRepeats = 10;
		final double sparsness=0.0;
		final double scale=1;
		final int nLocations=4;
		final int numTestTrees=20;
		final double disturbanceScale=1;
		
		// Load config
		System.out.print("Loading config file... ");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Config config = null;
		try {
			config = gson.fromJson(new FileReader("config.json"), Config.class);
			System.out.println(" done");
		}
		catch(Throwable e)	{
			config=new Config();
			System.out.println("config.json file not found, using default config. See out.config.json for details");			
		}			

		System.out.print("Writing full config options to out.config...");
		config.outputToFile("out.config",gson);
		System.out.println(" done");

		// Load data files and prepare data....			
		Data data = new DataForTests(config,TestType.TEST_USING_INPUT_TREES,numTestRepeats,nLocations, numTestTrees, sparsness,scale, disturbanceScale);

		// Creating test file 
		File testFile = new File("out.test");
		testFile.delete();
		testFile.createNewFile();
		PrintStream testStream = new PrintStream(testFile);
		System.out.println("Calculating tree likelihood using the same model used to create the tree: SEASONALITY "+config.migrationModelType+",");				
		testStream.print("{\""+config.migrationModelType+"\",");
		testStream.print(((DataForTests)data).createModel.parse());
		System.out.println(((DataForTests)data).createModel.print());
		double createLikelihood = 0;
		for (LikelihoodTree tree : data.getTrees().get(0)) {
			System.out.print(".");
			LikelihoodTree workingCopy = tree.copy();
			workingCopy.clearInternalNodes();
			workingCopy.setMigrationModel(((DataForTests)data).createModel);
			createLikelihood+=workingCopy.logLikelihood();
		}
		createLikelihood=createLikelihood/data.getTrees().get(0).size();
		System.out.println(createLikelihood);
		
		boolean testPass=true;
		
		System.out.println("\nCalculating tree likelihood using test models with increasing noise:");
		for (int i=0;i<((DataForTests)data).testModels.size();i++) {
			if (i%numTestRepeats ==0) {						
				System.out.println("SEASONALITY "+((DataForTests)data).testModels.get(i).getModelName());						
			}

			double testLikelihood = 0;
			for (LikelihoodTree tree : data.getTrees().get(0)) {
				System.out.print(".");
				LikelihoodTree workingCopy = tree.copy();
				workingCopy.setMigrationModel(((DataForTests)data).testModels.get(i));
				workingCopy.clearInternalNodes();
				testLikelihood+=workingCopy.logLikelihood();
			}
			testLikelihood=testLikelihood/data.getTrees().get(0).size();
			System.out.println(testLikelihood);
			if (createLikelihood<testLikelihood) {
				testPass=false;
				System.out.println(((DataForTests)data).testModels.get(i).print());
			}
		}
		testStream.print(",\""+(new GregorianCalendar()).getTime()+"\"}");
		testStream.close();
		
		assertEquals(testPass,true);

	}


}
