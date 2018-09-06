## Testing

Python script `test.py` (Python 3) performs all the tests on 5 different generated sets of test cases. If will output results in the `compile_time.csv` and `testing_results.csv`, as well as visualisations in the `figures/` directory. The whole testing procedure is done from this script, and will take about 10 hours. Number of rounds, repeats and all the performed test cases can be adjusted within the script and can be used to reduce the test time.

Directory `src/` contains the source for the code generator, that will create all defined test cases when running `GenerateTestCases.main()`. Test cases are placed in the `generated-tests/src/main/java/` directory.

Directory `generated-tests/` contains a maven project that can compile the generated test cases with all three annotation processors. When the `test.py` script is run, the compiled classes are placed in `generated-tests/classes-XXXX` directories.

