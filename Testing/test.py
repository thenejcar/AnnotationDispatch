import csv
import os
import subprocess
import time
import glob

from matplotlib import pyplot as plt
from matplotlib import patches as mpatches
from matplotlib.ticker import MaxNLocator


def average(lst):
    return sum(lst) / len(lst)


ranges = {
    'Parameters': [1, 2, 3, 5, 10, 15, 20],
    # 'Classes_1p': [1, 2, 3, 4, 5, 8, 10],
    # 'ClassesWidth_1p': [1, 2, 3, 4, 5, 8, 10],
    # 'Methods_1p': [1, 2, 4, 5, 10, 20, 25],
    # 'Instances_1p': [1, 2, 4, 5, 10, 20, 25],
    'Classes_2p': [1, 2, 3, 4, 5, 8, 10],
    'ClassesWidth_2p': [1, 2, 3, 4, 5, 8, 10],
    'Methods_2p': [1, 2, 4, 5, 10, 20, 25],
    'Instances_2p': [1, 2, 4, 5, 10, 20, 25],
    # 'Classes_3p': [1, 2, 3, 4, 5, 8, 10],
    # 'ClassesWidth_3p': [1, 2, 3, 4, 5, 8, 10],
    # 'Methods_3p': [1, 2, 4, 5, 10, 20, 25],
    # 'Instances_3p': [1, 2, 4, 5, 10, 20, 25],
    # 'Classes_5p': [1, 2, 3, 4, 5, 8, 10],
    # 'ClassesWidth_5p': [1, 2, 3, 4, 5, 8, 10],
    # 'Methods_5p': [1, 2, 4, 5, 10, 20, 25],
    # 'Instances_5p': [1, 2, 4, 5, 10, 20, 25],
}


class Tester:
    def __init__(self, rounds, compile_rounds, processors, test_types):
        self.rounds = rounds
        self.compile_rounds = compile_rounds

        self.processors = processors
        self.test_types = test_types

        # dictionaries to hold running time, compilation times and file sizes
        self.results = {proc: {key : {x: [] for x in ranges[key]} for key in ranges.keys()} for proc in processors}
        self.compile_times = {proc: {key : {x: [] for x in ranges[key]} for key in ranges.keys()} for proc in processors}
        self.file_sizes = {proc: {key : {x: [] for x in ranges[key]} for key in ranges.keys()} for proc in processors}

        # slovenian translations for the pdf charts
        self.naslov = {}
        for key in ranges.keys():
            if (key.startswith("Parameters")):
                self.naslov[key] = "Število parametrov"
            elif (key.startswith("Methods")):
                self.naslov[key] = "Število modelov"
            elif (key.startswith("Instances")):
                self.naslov[key] = "Število primerkov"
            elif (key.startswith("ClassesWidth")):
                self.naslov[key] = "Širina razredne hierarhije"
            elif (key.startswith("Classes")):
                self.naslov[key] = "Globina razredne hierarhije"


        self.marker = '.'

    def compile(self):
        subprocess.run(["java", "-classpath", "target/classes/", "si.kisek.annotationdispatchtesting.GenerateTestCases"])

        print("cd generated-tests/")
        os.chdir("generated-tests/")

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
                for num in ranges[type]:
                    t = time.time()
                    str_processor = "-P" + proc
                    str_include = "-Dinclude=" + type + "_" +str(num) + ".java"

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

                    # remember the filesize
                    du = filesize(proc, type, num)
                    print(du, "MB")
                    self.file_sizes[proc][type][num].append(du)
        print("cd ../")
        os.chdir("../")

    def write_compile_times(self):
        print("compile times:")
        with open('compile_times.csv', 'a') as file:
            header = 'processor,test,num'
            for i in range(0, self.compile_rounds):
                header += ',res%d' % i
            header += ",avg\n"

            file.write(header)
            for proc in self.processors:
                for t in self.test_types:
                    for num in ranges[t]:
                        row = proc + ',' + t + ',' + str(num) + ',' + ','.join(
                            [str(i) for i in self.compile_times[proc][t][num]]) + ',' + str(
                            average(self.compile_times[proc][t][num])) + '\n'
                        print(row)
                        file.write(row)

            file.close()

    def read_compile_times(self):
        with open('compile_times.csv', 'r') as file:
            reader = csv.reader(file)
            next(reader)
            for row in reader:
                proc = row[0]
                test = row[1]
                num = int(row[2])
                times = row[3:-1]

                # overwrite the lists in the results dict
                self.compile_times[proc][test][num] = [float(x) for x in times]

    def write_file_sizes(self):
        print("compile times:")
        with open('file_sizes.csv', 'a') as file:
            header = 'processor,test,num'
            for i in range(0, self.compile_rounds):
                header += ',res%d' % i
            header += ",avg\n"

            file.write(header)
            for proc in self.processors:
                for t in self.test_types:
                    for num in ranges[t]:
                        row = proc + ',' + t + ',' + str(num) + ',' + ','.join(
                            [str(i) for i in self.file_sizes[proc][t][num]]) + ',' + str(
                            average(self.file_sizes[proc][t][num])) + '\n'
                        print(row)
                        file.write(row)

            file.close()

    def read_file_sizes(self):
        with open('file_sizes.csv', 'r') as file:
            reader = csv.reader(file)
            next(reader)
            for row in reader:
                proc = row[0]
                test = row[1]
                num = int(row[2])
                times = row[3:-1]

                # overwrite the lists in the results dict
                self.file_sizes[proc][test][num] = [float(x) for x in times]

    def runTest(self):
        print("cd generated-tests/")
        os.chdir("generated-tests/")
        for i in range(0, self.rounds):
            print("Test run", i)
            for processor in self.processors:
                if (processor == 'unmodified'):
                    continue  # skip the default
                print("")
                print(processor + ":")
                # move to target dir and run programs
                os.chdir("classes-" + processor + "/")

                for t in self.test_types:

                    print("Testing " + t)
                    for num in ranges[t]:
                        clazz = t + "_" + str(num)
                        print("%-15s" % clazz, end="")
                        result = subprocess.run(["java", clazz], stdout=subprocess.PIPE)
                        time = int(result.stdout)
                        self.results[processor][t][num].append(time)
                        print("%20d" % time)
                os.chdir("../")

        print("cd ../")
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
                if (proc == 'unmodified'):
                    continue  # skip the default
                for t in self.test_types:
                    for num in ranges[t]:
                        row = proc + ',' + t + ',' + str(num) + ',' + ','.join(
                            [str(i) for i in self.results[proc][t][num]]) + ',' + str(
                            average(self.results[proc][t][num]) / 1e6) + '\n'
                        print(row)
                        file.write(row)

            file.close()

    def read_results(self):
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

    def plot_results(self, includeVisitor=True):
        # plot running speed for all tests
        for t in self.test_types:
            fig = plt.figure(figsize=(7, 7))
            # fig.suptitle("Speedtest comparison by number of " + t)
            ax = fig.add_subplot(111)
            ax.set_xlabel(self.naslov[t])
            ax.xaxis.set_major_locator(MaxNLocator(integer=True))
            ax.set_ylabel('Čas izvajanja [s]')
            ax.grid(which='major', linestyle='-')
            ax.grid(which='minor', linestyle=':')

            legend = []

            for proc in self.processors:
                if (proc == 'unmodified'):
                    continue  # skip the default
                elif (proc == 'visitor'):
                    if (not includeVisitor):
                        continue

                    if t.endswith("Void"):
                        labela = "Obiskovalec (void)"
                        c = '#800000'
                    else:
                        labela = "Obiskovalec"
                        c = 'r'
                elif (proc == 'tree'):
                    labela = "Odločitevno drevo"
                    c = 'g'
                elif (proc == 'reflection'):
                    labela = "Odsevnost"
                    c = 'b'
                else:
                    labela = proc
                    c = 'k'

                for num in ranges[t]:
                    ax.scatter([num] * self.rounds * self.compile_rounds,
                               [(i / 1e9) for i in self.results[proc][t][num]], marker=self.marker, color=c)
                ax.plot(ranges[t], [average(self.results[proc][t][x]) / 1e9 for x in ranges[t]], '-', color=c)
                legend.append(mpatches.Patch(color=c, label=labela))

            plt.legend(handles=legend)
            plt.draw()
            if (includeVisitor):
                fig.savefig('figures/speedtest' + t + '.pdf', bbox_inches='tight')
            else:
                fig.savefig('figures/speedtest' + t + 'NoVisitor.pdf', bbox_inches='tight')

    def plot_compile_times(self, includeVisitor=True):

        # plot compile times of all classes
        for t in self.test_types:
            fig = plt.figure(figsize=(7, 7))
            # fig.suptitle("Compile time comparison by number of " + t)
            ax = fig.add_subplot(111)
            ax.set_xlabel(self.naslov[t])
            ax.xaxis.set_major_locator(MaxNLocator(integer=True))
            ax.set_ylabel('Čas prevajanja [s]')
            ax.grid(which='major', linestyle='-')
            ax.grid(which='minor', linestyle=':')

            legend = []

            for proc in self.processors:
                if (proc == 'unmodified'):
                    labela = "Brez obdelave anotacij"
                    c = 'k'
                elif (proc == 'visitor'):
                    if (not includeVisitor):
                        continue

                    if t.endswith("Void"):
                        labela = "Obiskovalec (void)"
                        c = '#800000'
                    else:
                        labela = "Obiskovalec"
                        c = 'r'
                elif (proc == 'tree'):
                    labela = "Odločitevno drevo"
                    c = 'g'
                elif (proc == 'reflection'):
                    labela = "Odsevnost"
                    c = 'b'
                else:
                    labela = proc
                    c = 'k'

                for num in ranges[t]:
                    ax.scatter([num] * self.compile_rounds, self.compile_times[proc][t][num], marker=self.marker,
                               color=c)
                ax.plot(ranges[t], [average(self.compile_times[proc][t][x]) for x in ranges[t]], '-', color=c)
                legend.append(mpatches.Patch(color=c, label=labela))

            plt.legend(handles=legend)
            plt.draw()
            if (includeVisitor):
                fig.savefig('figures/compileTime' + t + '.pdf', bbox_inches='tight')
            else:
                fig.savefig('figures/compileTime' + t + 'NoVisitor.pdf', bbox_inches='tight')

    def plot_file_sizes(self, includeVisitor=True):

        # plot compile times of all classes
        for t in self.test_types:
            fig = plt.figure(figsize=(7, 7))
            # fig.suptitle("Compile time comparison by number of " + t)
            ax = fig.add_subplot(111)
            ax.set_xlabel(self.naslov[t])
            ax.xaxis.set_major_locator(MaxNLocator(integer=True))
            ax.set_ylabel('Velikost prevedenih datotek [MB]')
            ax.grid(which='major', linestyle='-')
            ax.grid(which='minor', linestyle=':')

            legend = []

            for proc in self.processors:
                if (proc == 'unmodified'):
                    labela = "Brez obdelave anotacij"
                    c = 'k'
                elif (proc == 'visitor'):
                    if (not includeVisitor):
                        continue

                    if t.endswith("Void"):
                        labela = "Obiskovalec (void)"
                        c = '#800000'
                    else:
                        labela = "Obiskovalec"
                        c = 'r'
                elif (proc == 'tree'):
                    labela = "Odločitevno drevo"
                    c = 'g'
                elif (proc == 'reflection'):
                    labela = "Odsevnost"
                    c = 'b'
                else:
                    labela = proc
                    c = 'k'

                for num in ranges[t]:
                    ax.scatter([num] * self.compile_rounds, self.file_sizes[proc][t][num], marker=self.marker, color=c)
                ax.plot(ranges[t], [average(self.file_sizes[proc][t][x]) for x in ranges[t]], '-', color=c)
                legend.append(mpatches.Patch(color=c, label=labela))

            plt.legend(handles=legend)
            plt.draw()
            if (includeVisitor):
                fig.savefig('figures/fileSize' + t + '.pdf', bbox_inches='tight')
            else:
                fig.savefig('figures/fileSize' + t + 'NoVisitor.pdf', bbox_inches='tight')

    def combined_parameters_plot(self):
        for t in ["Parameters", "Classes", "Methods", "Instances"]:

            fig = plt.figure(figsize=(7, 7))
            # fig.suptitle("Visitor comparison with void methods")
            ax = fig.add_subplot(111)
            # ax.set_xlabel('Number of Parameters')
            ax.set_xlabel(self.naslov[t])
            ax.xaxis.set_major_locator(MaxNLocator(integer=True))
            # ax.set_ylabel('Compile time [s]')
            ax.set_ylabel('Čas izvajanja [s]')
            ax.grid(which='major', linestyle='-')
            ax.grid(which='minor', linestyle=':')

            legend = []

            proc = "visitor"
            c1 = 'r'
            c2 = '#800000'

            for num in ranges[t]:
                ax.scatter([num] * self.rounds * self.compile_rounds, [(i / 1e9) for i in self.results[proc][t][num]],
                           marker=self.marker, color=c1)
            ax.plot(ranges[t], [average(self.results[proc][t][x]) / 1e9 for x in ranges[t]], '-', color=c1)
            legend.append(mpatches.Patch(color=c1, label="Obiskovalec"))

            for num in ranges[t + "Void"]:
                ax.scatter([num] * self.rounds * self.compile_rounds,
                           [(i / 1e9) for i in self.results[proc][t + "Void"][num]], marker=self.marker, color=c2)
            ax.plot(ranges[t + "Void"],
                    [average(self.results[proc][t + "Void"][x]) / 1e9 for x in ranges[t + "Void"]], '-', color=c2)
            legend.append(mpatches.Patch(color=c2, label="Obiskovalec (void)"))

            plt.legend(handles=legend)
            plt.draw()
            fig.savefig('figures/speedtest' + t + 'Both.pdf', bbox_inches='tight')


def filesize(proc, type, num, prefix=""):
    s = 0
    s += os.path.getsize("classes-" + proc + "/" + type + "_" + str(num) + ".class")
    for file in glob.glob("classes-" + proc + "/" + type + "_" + str(num) + "$*"):
        s += os.path.getsize(file)
    return s / 1e6


# main

total_time = time.time()

print("rebuilding the test generator...")
print("mvn -q clean compile")
subprocess.run(["mvn", "-q", "clean", "compile"])

N = 5 # number of repeats per test case
M = 5 # number of different generated test cases

tester = Tester(N, M, ["visitor", "tree", "reflection", "unmodified"], list(ranges.keys()))

## clean the csv files
with open('testing_results.csv', 'w') as file:
    print("cleaning the csv file: " + file.name)

with open('compile_times.csv', 'w') as file:
    print("cleaning the csv file: " + file.name)

with open('file_sizes.csv', 'w') as file:
    print("cleaning the csv file: " + file.name)

for i in range(0, M):
     print("Test cases ", i)
     tester.compile()
     tester.runTest()

tester.write_results()
tester.write_compile_times()
tester.write_file_sizes()

#tester.read_results()
tester.plot_results()
tester.plot_results(False)
# tester.combined_parameters_plot()

#tester.read_compile_times()
tester.plot_compile_times()
tester.plot_compile_times(False)

#tester.read_file_sizes()
tester.plot_file_sizes()
tester.plot_file_sizes(False)

total_time = time.time() - total_time
print("Testing finished in", total_time / 60.0, "minutes.")
