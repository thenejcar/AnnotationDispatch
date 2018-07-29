import sys
import os
from collections import defaultdict
import subprocess

results_parameters = defaultdict(list)
results_classes = defaultdict(list)
results_methods = defaultdict(list)

for i in range(0, 20):
    print("Test run", i)
    for processor in ["switch", "reflection", "visitor"]:

        print("Recompiling with ", processor, " annotation processor")

        # subprocess.run(["mvn", "-q", "clean"])
        # subprocess.run(["mvn", "-q" "-Ptest-" + processor + "-parameters", "compile"])
        # subprocess.run(["mvn", "-q" "-Ptest-" + processor + "-parameters", "compile"])
        # subprocess.run(["mvn", "-q" "-Ptest-" + processor + "-classes", "compile"])
        subprocess.run(["mvn", "-Ptest-" + processor + "-all", "-q", "clean", "compile"])

        # move to target dir and run programs
        os.chdir("target/classes/")

        for test_type in ["Parameters", "Classes", "Methods"]:
            print("Testing for", test_type)
            for file in os.listdir("./"):
                clazz = file.split(".")[0]
                print(clazz, end=" .........  ")
                result = subprocess.run(["java", clazz], stdout=subprocess.PIPE)
                time = int(result.stdout)
                results_parameters[processor].append(time)
                print(time)

        os.chdir("../../")


for (k, v) in results_parameters.items():
    print(k + ":", (sum(v) / 20) / 1e6)

for (k, v) in results_classes.items():
    print(k + ":", (sum(v) / 20) / 1e6)

for (k, v) in results_methods.items():
    print(k + ":", (sum(v) / 20) / 1e6)
