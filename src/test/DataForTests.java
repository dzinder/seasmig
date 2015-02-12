package test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.SimpleRootedTree;
import jebl.evolution.trees.Tree;
import jebl.math.Random;
import seasmig.data.Data;
import seasmig.migrationmain.Config;
import seasmig.treelikelihood.LikelihoodTree;
import seasmig.treelikelihood.TransitionModel;
import seasmig.treelikelihood.transitionmodels.ConstantTransitionBaseModel;
import seasmig.treelikelihood.transitionmodels.GeneralSeasonalMigrationBaseModel;
import seasmig.treelikelihood.transitionmodels.SinusoidialSeasonalMigrationBaseModel;
import seasmig.treelikelihood.transitionmodels.TwoSeasonMigrationBaseModel;
import seasmig.treelikelihood.trees.AttributeLoader;
import seasmig.treelikelihood.trees.Sequence;
import seasmig.treelikelihood.trees.SimpleAttributeLoader;
import seasmig.treelikelihood.trees.TreeWithLocations;
import cern.colt.function.DoubleFunction;

@SuppressWarnings("serial")
public class DataForTests implements Data {
	// TODO: This...
	public List<ArrayList<LikelihoodTree>> trees = new ArrayList<ArrayList<LikelihoodTree>>();
	Config config = null;
	long iteration = 0;
	public enum TestType {TEST_USING_GENERATED_TREES, TEST_USING_INPUT_TREES, TEST_MODEL_DEGENERACY};

	// TEST MODELS
	public TransitionModel createModel = null;
	public List<TransitionModel> testModels = new ArrayList<TransitionModel>();
	private int numTestTips=3000;
	private int numLocations;

	// TODO:
	// non static RNG

	protected DataForTests() {};

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {	
		// TODO: Support Serialization of Test Data...
		// TODO: move report of iteration to somewhere else...
		iteration += config.checkpointEvery;
		System.out.print("\riteration: "+iteration);
	}

	@SuppressWarnings("unchecked")
	public DataForTests(Config config_, TestType testType, int numTestRepeats, int numLocations_, int numTestTrees, double sparsness, double rateScale, double disturbanceScale) throws IOException, ImportException 	{				

		config = config_;		
		numLocations=numLocations_;
		File treeFile;
		FileReader reader;
		NexusImporter nexusImporter;
		List<Tree> nexsusTrees;
		ArrayList<Tree> nexsusTreeTail;

		switch (testType) {

		case TEST_USING_GENERATED_TREES:

			// TODO: add more tests + test files ....
			// TODO: add tests for states...
			System.out.print("Generating test trees... ");

			// Generate test data and trees

			// For constant model...
			double[][] Q = makeRandomMigrationMatrix(numLocations,rateScale,sparsness); 

			// For two seasonal model...
			double[][] QW = makeRandomMigrationMatrix(numLocations,rateScale,0);
			double[][] QS = makeRandomMigrationMatrix(numLocations,rateScale,0);

			for (int i=0;i<QW.length;i++) {
				for (int j=0;j<QW[0].length;j++) {
					if (i!=j) {
						if (Math.random()<sparsness) {
							QW[i][i]=QW[i][i]+QW[i][j];
							QW[i][j]=0;
							QS[i][i]=QS[i][i]+QS[i][j];
							QS[i][j]=0;
						}
					}
				}
			}

			// For sinusoidal model...
			double[][] rates = makeRandomMigrationMatrix(numLocations,rateScale,sparsness);
			double[][] amps = makeRandomMigrationMatrix(numLocations,1,0.0);
			double[][] phases = makeRandomMigrationMatrix(numLocations,1,0.0);

			switch (config.migrationModelType) {
			case CONSTANT:	
				createModel = new ConstantTransitionBaseModel(Q); 
				break;
			case TWO_CONSTANT_SEASONS: 
				double phase = 0.3; double length = 0.5;
				createModel = new TwoSeasonMigrationBaseModel(QW,QS,phase,phase+length);
				break;
			case SINUSOIDAL:
				createModel = new SinusoidialSeasonalMigrationBaseModel(rates,amps,phases);
				break;
			default: 
				System.err.println("Migration Seasonality: "+config.migrationModelType+" not implemented for this configuration!!!");
				System.exit(-1);
			}

			trees.add(new ArrayList<LikelihoodTree>());
			for (int i=0;i<=numTestTrees;i++) {
				TreeWithLocations testTree = new TreeWithLocations(createModel,numTestTips,config);
				testTree.clearInternalNodes();
				trees.get(0).add(testTree);
			}

			System.out.println(" generated "+trees.get(0).size()+" trees");
			System.out.print("Generating test models... ");

			for (int i=0; i<numTestRepeats; i++) {					
				TransitionModel testModel = null;
				switch (config.migrationModelType) {
				case CONSTANT:	
					testModel = new ConstantTransitionBaseModel(disturbMigrationMatrix(Q,disturbanceScale*i/numTestRepeats)); 
					break;
				case TWO_CONSTANT_SEASONS: 
					double phase = Math.max(0,Math.min(0.5,0.3+i/numTestRepeats*(Random.nextDouble()-0.5))); 
					double length = 0.5;				
					testModel = new TwoSeasonMigrationBaseModel(disturbMigrationMatrix(QW,disturbanceScale*i/numTestRepeats),disturbMigrationMatrix(QS,disturbanceScale*i/numTestRepeats),phase,phase+length);
					break;
				case SINUSOIDAL: 
					testModel = new SinusoidialSeasonalMigrationBaseModel(disturbMigrationMatrix(rates,disturbanceScale*i/numTestRepeats),amps,phases);
					break;
				default: 
					System.err.println("Migration Seasonality: "+config.migrationModelType+" not implemented for this configuration!!!");
					System.exit(-1);
				}

				testModels.add(testModel);			
			}

			DoubleFunction[] rootFreqFunction = new DoubleFunction[Q.length]; 
			DoubleFunction[][] generalMigrationFunction = new DoubleFunction[Q.length][Q[0].length]; 
			for (int i=0;i<Q.length;i++) {
				rootFreqFunction[i]=cern.jet.math.Functions.constant(1.0/Q.length);
				for (int j=0;j<Q[0].length;j++) {
					generalMigrationFunction[i][j]=cern.jet.math.Functions.constant(Q[i][j]);
				}
			}

			break;

		case TEST_USING_INPUT_TREES:{
			// TODO: add more tests + test files ....
			// TODO: add tests for states...
			System.out.print("Generating test trees based on input tree topology ... ");

			// Generate test data and trees

			// For constant model...
			// For constant model...
			Q = makeRandomMigrationMatrix(numLocations,rateScale,sparsness); 

			// For two seasonal model...
			QW = makeRandomMigrationMatrix(numLocations,rateScale,0.0);
			QS = makeRandomMigrationMatrix(numLocations,rateScale,0.0);

			for (int i=0;i<QW.length;i++) {
				for (int j=0;j<QW[0].length;j++) {
					if (i!=j) {	
						if (Math.random()<sparsness) {
							QW[i][i]=QW[i][i]+QW[i][j];
							QW[i][j]=0;
							QS[i][i]=QS[i][i]+QS[i][j];
							QS[i][j]=0;
						}
					}
				}
			}

			// For sinusoidal model...
			rates = makeRandomMigrationMatrix(numLocations,rateScale,sparsness);
			amps = makeRandomMigrationMatrix(numLocations,1,0.0);
			phases = makeRandomMigrationMatrix(numLocations,1,0.0);

			switch (config.migrationModelType) {
			case CONSTANT:	
				createModel = new ConstantTransitionBaseModel(Q); 
				break;
			case TWO_CONSTANT_SEASONS: 
				double phase = 0.3; double length = 0.5;
				createModel = new TwoSeasonMigrationBaseModel(QW,QS,phase,phase+length);
				break;
			case SINUSOIDAL: 
				createModel = new SinusoidialSeasonalMigrationBaseModel(rates,amps,phases);
				break;
			default: 
				System.err.println("Migration Seasonality: "+config.migrationModelType+" not implemented for this configuration!!!");
				System.exit(-1);
			}

			System.out.print("Loading trees... ");

			for (int h=0;h<config.treeFilenames.length;h++) {
				trees.add(new ArrayList<LikelihoodTree>());
				treeFile = new File(config.treeFilenames[h]);
				reader = new FileReader(treeFile);
				nexusImporter = new NexusImporter(reader);

				List<Taxon> taxa = nexusImporter.parseTaxaBlock();		
				HashMap<String,Integer> taxaIndices = new HashMap<String,Integer>();			
				for (int i=0;i<taxa.size();i++) {
					taxaIndices.put(taxa.get(i).getName(), i);
				}

				nexsusTrees = nexusImporter.importTrees();

				System.out.println("loaded "+nexsusTrees.size()+" trees");

				System.out.print("Keeping tail... ");		
				nexsusTreeTail = new ArrayList<jebl.evolution.trees.Tree>();
				for (int i=Math.max(0,nexsusTrees.size()-config.numTreesFromTail);i<nexsusTrees.size();i++) {
					nexsusTreeTail.add(nexsusTrees.get(i));
				}
				System.out.println(" keeping last "+nexsusTreeTail.size()+ " trees");			
				for (int i=0; i<numTestTrees;i++) {				
					TreeWithLocations testTree = new TreeWithLocations(createModel,(SimpleRootedTree) nexsusTreeTail.get(Random.nextInt(nexsusTreeTail.size())),taxaIndices,taxa,config, config.lastTipTime[h]);
					testTree.fillRandomTraits();
					testTree.clearInternalNodes();
					trees.get(h).add(testTree);

					// write tree traits to file
					System.out.print("Writing generated taxa locations to out.testloc"+i+"...");
					File testlocFile = new File("out.testloc"+i);
					testlocFile.delete();
					testlocFile.createNewFile();
					PrintStream testlocStream = new PrintStream(testlocFile);
					testlocStream.print(testTree.printTaxaAndLocation());
					testlocStream.close();
					System.out.println("done");

				}
			}

			System.out.println("Generated "+trees.get(0).size()*trees.size()+" trees with model generated random tip annotations and input tree topology");

			for (int i=0; i<numTestRepeats; i++) {

				TransitionModel testModel = null;
				switch (config.migrationModelType) {
				case CONSTANT:	
					testModel = new ConstantTransitionBaseModel(disturbMigrationMatrix(Q,disturbanceScale*i/numTestRepeats)); 
					break;
				case TWO_CONSTANT_SEASONS: 
					double phase = Math.max(0,Math.min(0.5,0.3+i/numTestRepeats*(Random.nextDouble()-0.5))); 
					double length = 0.5;				
					testModel = new TwoSeasonMigrationBaseModel(disturbMigrationMatrix(QW,disturbanceScale*i/numTestRepeats),disturbMigrationMatrix(QS,disturbanceScale*i/numTestRepeats),phase,phase+length);
					break;
				case SINUSOIDAL: 
					testModel = new SinusoidialSeasonalMigrationBaseModel(disturbMigrationMatrix(rates,disturbanceScale*i/numTestRepeats),amps,phases);
					break;
				default: 
					System.err.println("Migration Seasonality: "+config.migrationModelType+" not implemented for this configuration!!!");
					System.exit(-1);
				}

				testModels.add(testModel);

			}			
		}
		break;

		case TEST_MODEL_DEGENERACY:{

			// TODO: add more tests + test files ....
			// TODO: add tests for states...
			System.out.print("Building degenerate test models... ");

			// Generate test data and trees

			// For constant model...
			Q = makeRandomMigrationMatrix(numLocations,2,0.5); 

			// For two seasonal model...
			QW = myMatrixCopy(Q);
			QS = myMatrixCopy(Q);

			// For sinusoidal model...
			rates = myMatrixCopy(Q);
			amps = makeRandomMigrationMatrix(numLocations,0,0.5);
			phases = makeRandomMigrationMatrix(numLocations,1,0.5);

			// For two constant seasons model...
			double phase = 0.3; double length = 0.5;

			switch (config.migrationModelType) {
			case CONSTANT:	
				createModel = new ConstantTransitionBaseModel(Q); 
				break;
			case TWO_CONSTANT_SEASONS: 
				createModel = new TwoSeasonMigrationBaseModel(QW,QS,phase,phase+length);
				break;
			case SINUSOIDAL:
				createModel = new SinusoidialSeasonalMigrationBaseModel(rates,amps,phases);
				break;
			default: 
				System.err.println("Migration Seasonality: "+config.migrationModelType+" not implemented for this configuration!!!");
				System.exit(-1);
			}

			System.out.print(" done!\nLoading trees... ");	

			trees.add(new ArrayList<LikelihoodTree>());

			treeFile = new File(config.treeFilenames[0]);
			reader = new FileReader(treeFile);
			nexusImporter = new NexusImporter(reader);
			List<Taxon> taxa = nexusImporter.parseTaxaBlock();
			HashMap<String,Integer> taxaIndices = new HashMap<String,Integer>();			
			for (int i=0;i<taxa.size();i++) {
				taxaIndices.put(taxa.get(i).getName(), i);
			}
			nexsusTrees = nexusImporter.importTrees();
			System.out.println("loaded "+nexsusTrees.size()+" trees");

			System.out.print("Keeping tail... ");		
			nexsusTreeTail = new ArrayList<jebl.evolution.trees.Tree>();
			for (int i=Math.max(0,nexsusTrees.size()-config.numTreesFromTail);i<nexsusTrees.size();i++) {
				nexsusTreeTail.add(nexsusTrees.get(i));
			}
			System.out.println(" keeping last "+nexsusTreeTail.size()+ " trees");			

			// Convert trees to internal tree representation
			if (config.locationFilenames[0]!=null) {
				System.out.print("Loading traits... ");
				AttributeLoader attributeLoader= new SimpleAttributeLoader(config,config.locationFilenames[0], null,null, null, null);
				// TODO: think about this...
				HashMap<String,Integer> locationMap = (HashMap<String,Integer>) attributeLoader.getAttributes().get("locations");
				HashMap<String,Double> stateMap = (HashMap<String,Double>) attributeLoader.getAttributes().get("states");
				System.out.println("loaded "+locationMap.size()+" taxon traits");

				System.out.print("Reparsing trees... ");
				if (stateMap==null) {
					for (jebl.evolution.trees.Tree tree : nexsusTreeTail) {
						HashMap<String, Sequence> seqMap = new HashMap<String,Sequence>();
						trees.get(0).add(new TreeWithLocations((SimpleRootedTree) tree,taxaIndices,locationMap,numLocations,config.lastTipTime[0],seqMap,0,config, null, null));
					}
				}
				else {
					// TODO: this...
				}
				System.out.println(" reparsed "+trees.size()+" trees");
			}


			testModels.add(new ConstantTransitionBaseModel(Q));
			testModels.add(new TwoSeasonMigrationBaseModel(QW,QS,phase, phase+length));

			generalMigrationFunction = new DoubleFunction[Q.length][Q[0].length]; 
			for (int i=0;i<Q.length;i++) {
				for (int j=0;j<Q[0].length;j++) {
					generalMigrationFunction[i][j]=cern.jet.math.Functions.constant(Q[i][j]);
				}
			}

			rootFreqFunction = new DoubleFunction[Q.length]; 
			for (int i=0;i<Q.length;i++) {
				rootFreqFunction[i]=cern.jet.math.Functions.constant(1.0/Q.length);
				for (int j=0;j<Q[0].length;j++) {
					generalMigrationFunction[i][j]=cern.jet.math.Functions.constant(Q[i][j]);
				}
			}

			testModels.add(new GeneralSeasonalMigrationBaseModel(generalMigrationFunction, rootFreqFunction,25));

			System.out.println(" generated "+testModels.size()+" test models");

		}
		break;

		}

		// Creating test file 
		System.out.print("Writing test model to out.test ...");
		File testFile = new File("out.test");
		testFile.delete();
		testFile.createNewFile();
		PrintStream testStream = new PrintStream(testFile);
		testStream.print(createModel.parse());
		testStream.println();
		testStream.print(",\""+(new GregorianCalendar()).getTime()+"\"}");
		testStream.close();
		System.out.println("done");



	}

	private double[][] myMatrixCopy(double[][] q) {
		double [][] returnValue = new double[q.length][];
		for(int i = 0; i < q.length; i++) {
			returnValue[i] = q[i].clone();
		}				
		return returnValue;
	}

	public static double[][] makeRandomMigrationMatrix(int size, double scale, double sparseness) {
		// sparsness==1 ==> all zeros
		// sparsness=0 ==> all with value
		// For test purposes...
		double[][] returnValue = new double[size][size];
		for (int i=0;i<size;i++) {
			double rowSum=0;
			for (int j=0;j<size;j++) {
				if (i!=j) {
					returnValue[i][j]=Math.random()*scale*(Math.random()<sparseness?0.0:1.0);
					rowSum+=returnValue[i][j];
				}
			}
			returnValue[i][i]=-rowSum;
		}
		return returnValue;
	}

	private double[][] disturbMigrationMatrix(double[][] migrationMatrix, double disturbanceMagnitude) {
		// For test purposes...
		double[][] returnValue = myMatrixCopy(migrationMatrix);
		for (int i=0;i<migrationMatrix.length;i++) {
			double rowSum=0;
			for (int j=0;j<migrationMatrix.length;j++) {
				if (i!=j) {
					double disturbedValue = returnValue[i][j] + (Math.random()-0.5)*disturbanceMagnitude;
					if (disturbedValue<0) // replace negative value with half orignial value 
						disturbedValue=returnValue[i][j]/2.0;
					returnValue[i][j]=disturbedValue;
					rowSum+=returnValue[i][j];
				}
			}
			returnValue[i][i]=-rowSum;
		}
		return returnValue;
	}

	public List<ArrayList<LikelihoodTree>> getTrees() {
		return trees;
	}

	public int getNumLocations() {
		return numLocations;
	}






}
