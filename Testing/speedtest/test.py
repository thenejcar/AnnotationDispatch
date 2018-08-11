import csv
import os
import subprocess
import time

from matplotlib import pyplot as plt
from matplotlib import patches as mpatches

def average(lst):
    return sum(lst) / len(lst)


class Tester:
    def __init__(self, rounds, compile_rounds, processors):
        self.rounds = rounds
        self.compile_rounds = compile_rounds

        self.processors = processors
        self.test_types = ["Parameters", "ParametersVoid", "Classes", "Methods"]
        self.ranges = {
            'Parameters': [1, 2, 3, 5, 10, 15, 20],
            'ParametersVoid': [1, 2, 3, 5, 10, 15, 20],
            'Classes': [1, 2, 3, 4, 5, 8, 10],
            'Methods': [1, 2, 4, 5, 10, 20, 25]
        }

        # dictionary of all results (running time)
        self.results = {proc: {
            'Parameters': {x: [] for x in self.ranges['Parameters']},
            'ParametersVoid': {x: [] for x in self.ranges['ParametersVoid']},
            'Classes': {x: [] for x in self.ranges['Classes']},
            'Methods': {x: [] for x in self.ranges['Methods']}
        } for proc in processors}

        # compilation times
        self.compile_times = {proc: {
            'Parameters': {x: [] for x in self.ranges['Parameters']},
            'ParametersVoid': {x: [] for x in self.ranges['ParametersVoid']},
            'Classes': {x: [] for x in self.ranges['Classes']},
            'Methods': {x: [] for x in self.ranges['Methods']}
        } for proc in processors}

    def compile(self):
        print("cd ../")
        os.chdir("../")
        subprocess.run(["java", "-classpath", "target/classes/", "si.kisek.annotationdispatchtesting.GenerateTestCases"])

        print("cd speedtest/")
        os.chdir("speedtest/")

        print("mvn -q clean")
        subprocess.run(["mvn", "-q", "clean"])

        for proc in self.processors:
            print("\nRecompiling with ", proc, " annotation processor")

            # clean the target dir
            old_classes = os.listdir("classes-" + proc + "/")
            print("cleaning out", len(old_classes), "old classes")
            for file in old_classes:
                os.remove("classes-" + proc + "/" + file)

            for type in self.test_types:
                for num in self.ranges[type]:
                    t = time.time()
                    str_processor = "-P" + proc
                    str_include = "-Dinclude=" + type + str(num) + ".java"

                    print("mvn", "-q", str_processor, str_include, "compile")
                    subprocess.run(["mvn", "-q", str_processor, str_include, "compile"])

                    t = time.time() - t
                    print(t, "seconds")
                    self.compile_times[proc][type][num].append(t)

                    # move the new classes to target dir
                    files = os.listdir("target/classes/")
                    print("Moving", len(files), "files to " + "classes-" + proc + "/")
                    for file in files:
                        os.rename("target/classes/" + file, "classes-" + proc + "/" + file)

    def runTest(self):
        for i in range(0, self.rounds):
            print("Test run", i)
            for processor in self.processors:
                print("")
                print(processor + ":")
                # move to target dir and run programs
                os.chdir("classes-" + processor + "/")

                for t in self.test_types:

                    print("Testing " + t)
                    for num in self.ranges[t]:
                        clazz = t + str(num)
                        print("%-15s" % clazz, end="")
                        result = subprocess.run(["java", clazz], stdout=subprocess.PIPE)
                        time = int(result.stdout)
                        self.results[processor][t][num].append(time)
                        print("%20d" % time)
                os.chdir("../")


    def write_results(self):
        print("results:")
        with open('testing_results.csv', 'a') as file:
            header = 'processor,test,num'
            for i in range(0, self.rounds):
                header += ',res%d' % i
            header += ",avg\n"

            file.write(header)
            for proc in self.processors:
                for t in self.test_types:
                    for num in self.ranges[t]:
                        row = proc + ',' + t + ',' + str(num) + ',' + ','.join([str(i) for i in self.results[proc][t][num]]) + ',' + str(average(self.results[proc][t][num]) / 1e6) + '\n'
                        print(row)
                        file.write(row)

            file.close()

    def read_csv(self):
        with open('testing_results.csv', 'r') as file:
            reader = csv.reader(file)
            next(reader)
            for row in reader:
                proc = row[0]
                test = row[1]
                num = int(row[2])
                results = row[3:-1]

                # overwrite the lists in the results dict
                self.results[proc][test][num] = [int(x) for x in results]


    def plot_results(self):
        # plot running speed for all tests
        for t in self.test_types:
            fig = plt.figure(figsize=(12, 12))
            fig.suptitle("Speedtest comparison by number of " + t)
            ax = fig.add_subplot(111)
            ax.set_label('Number of ' + t)
            ax.set_ylabel('Time [ms]')
            ax.grid(which='major', linestyle='-')
            ax.grid(which='minor', linestyle=':')

            colors = ['r', 'g', 'b']

            legend = []

            for proc in self.processors:
                c = colors.pop(0)
                for num in self.ranges[t]:
                    ax.scatter([num] * self.rounds * self.compile_rounds, [(i / 1e6) for i in self.results[proc][t][num]], color=c)
                ax.plot(self.ranges[t], [average(self.results[proc][t][x]) / 1e6 for x in self.ranges[t]], '-', color=c)
                legend.append(mpatches.Patch(color=c, label=proc))
                colors.append(c)

            plt.legend(handles=legend)
            plt.draw()
            fig.savefig('figures/speedtest' + t + '.pdf', bbox_inches='tight')

    def plot_compile_times(self):
        # plot compile times of all classes
        for t in self.test_types:
            fig = plt.figure(figsize=(7, 7))
            fig.suptitle("Compile time comparison by number of " + t)
            ax = fig.add_subplot(111)
            ax.set_label('Number of ' + t)
            ax.set_ylabel('Compile time [s]')
            ax.grid(which='major', linestyle='-')
            ax.grid(which='minor', linestyle=':')

            colors = ['r', 'g', 'b']

            legend = []

            for proc in self.processors:
                c = colors.pop(0)
                for num in self.ranges[t]:
                    ax.scatter([num] * self.compile_rounds, self.compile_times[proc][t][num], color=c)
                ax.plot(self.ranges[t], [average(self.compile_times[proc][t][x]) for x in self.ranges[t]], '-', color=c)
                legend.append(mpatches.Patch(color=c, label=proc))
                colors.append(c)

            plt.legend(handles=legend)
            plt.draw()
            fig.savefig('figures/compileTime' + t + '.pdf', bbox_inches='tight')



# main

N = 4 # number of repeats per test case
M = 5 # number of different generated test cases

tester = Tester(N, M, ["visitor", "switch", "reflection"])

# # clean the csv file
# with open('testing_results.csv', 'w') as file:
#     print("cleaning the csv file")
#
# for i in range(0, M):
#     print("Test cases ", i)
#     tester.compile()
#     tester.runTest()
#     tester.write_results()

tester.read_csv()
tester.plot_results()
#tester.plot_compile_times()
