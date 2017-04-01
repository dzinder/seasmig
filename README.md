# Bayesian Inference and Stochastic Mapping of Seasonal Migration Processes from Phylogenetic Tree Distributions (SeasMig)
### github.com/dzinder/seasmig

### This release is compatible with cited publications.

-----------------------------------------------------------------------------------------------------------
### For details and citation please refer to:
* [Seasonality in the migration and establishment of H3N2 Influenza lineages with epidemic growth and decline](https://bmcevolbiol.biomedcentral.com/articles/10.1186/s12862-014-0272-2) Daniel Zinder, Trevor Bedford, Edward B Baskerville, Robert J Woods, Manojit Roy and Mercedes Pascual, BMC Evolutionary Biology 2014

* [Supplementary Methods for Bayesian Inference and Stochastic Mapping of Seasonal Migration Processes from Phylogenetic Tree Distributions (SeasMig)](https://static-content.springer.com/esm/art%3A10.1186%2Fs12862-014-0272-2/MediaObjects/12862_2014_272_MOESM2_ESM.pdf) Daniel Zinder, Trevor Bedford, Edward B Baskerville, Robert J Woods, Manojit Roy and Mercedes Pascual, BMC Evolutionary Biology 2014

**Requires:**
Java JRE 1.6 and newer

**For running type:**
java -jar seasmig.jar

### Parameter file (config.json):
-----------------------
```
{
  "sampleFilename": "samples.jsons",
  "swapStatsFilename": "swap_stats.txt",
  "logLevel": {
    "name": "INFO",
    "value": 800,
    "resourceBundleName": "sun.util.logging.resources.logging"
  },
  "checkpointFilename": "checkpoint.bin",
  "priorLikelihoodFilename": "prior_likelihood.txt",
  "mlFilename": "ml.txt",
  "thin": 20,
  "burnIn": 200,
  "iterationCount": 100000000,
  "tuneEvery": 50,
  "tuneFor": 200,
  "mlthin": 5,
  "initialHistoryCount": 5,
  "chainCount": 1,
  "heatPower": 1.0,
  "swapInterval": 1,
  "targetAcceptanceRate": 0.25,
  "checkpointEvery": 1000,
  "restoreFromDatabase": false,
  "migrationModelType": "CONSTANT_AS_INPUT",
  "twoSeasonParameterization": "VARIABLE_SELECTION_DIFF",
  "twoSeasonPhase": "FREE_PHASE_FIXED_LENGTH",
  "fixedPhase": 0.3,
  "minSeasonLength": 0.3333333,
  "fixRate": false,
  "nSeasonalParts": 4,
  "nConstantSeasonsParameterization": "ALL",
  "noSeasonalityParameterization": "ALL",
  "epochParameterization": "EPOCHAL",
  "nEpochs": 2,
  "epochTimes": [
    2006.0
  ],
  "minEpochTime": 1985.0,
  "maxEpochTime": 2011.0,
  "veryLongTime": 1000.0,
  "asrTrees": true,
  "smTrees": true,
  "smAlternativeTreeOutput": true,
  "smTransitions": true,
  "smTipDwellings": true,
  "smLineages": false,
  "smDescendants": false,
  "smTrunkStats": false,
  "smMigrationNodeNumTipAndSequenceData": true,
  "seqMutationStats": true,
  "seqMutationsStatsCodonOutput": true,
  "seqMutationsStatsSeqOutput": true,
  "seqStochasticMappingStartTime": 1970.0,
  "locationFilenames": [
    "accession_locid.txt"
  ],
  "treeFilenames": [
    "test.trees"
  ],
  "treeWeights": [
    1.0
  ],
  "numTreesFromTail": 50,
  "numLocations": 7,
  "sampleTreesSequentially": true,
  "stateReconstructionAndTreeOutput": "SEQ_STOCHASTIC_MAPPING",
  "lastTipTime": [
    2012.0
  ],
  "presentDayTipInterval": 2.25,
  "timeToDesignateTrunk": 4.0,
  "maxSMBranchRetries": 200000,
  "alignmentFilenames": [
    "all_epitopes.txt"
  ],
  "seqModelType": "HKY_3CP_AS_INPUT",
  "verificationTolerance": 3.0,
  "migrationModelFilenames": [
    "migration_models.txt"
  ],
  "codonModelFilenames": [
    "codon_models.txt"
  ]
}
```

## Output Files:
-------------
* out.config.json - parameters used
* as specified in config.json
