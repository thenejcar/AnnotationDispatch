import csv
import os
import subprocess
import time

from matplotlib import pyplot as plt
from matplotlib import patches as mpatches

def average_ms(lst):
    return sum(lst) / len(lst) / 1e6


class Tester:
    def __init__(self, repeats, processors):
        self.repeats = repeats
        # processors = ["switch", "reflection", "visitor"]
        self.processors = processors
        self.test_types = ["Parameters", "Classes", "Methods"]
        self.ranges = {
            'Parameters': [1, 2, 3, 5, 10, 15, 20],
            'Classes': [1, 2, 3, 4, 5, 8, 10],
            'Methods': [1, 2, 4, 5, 10, 20, 25]
        }

        # dictionary of all results (running time)
        self.results = {proc: {
            'Parameters': {x: [] for x in self.ranges['Parameters']},
            'Classes': {x: [] for x in self.ranges['Classes']},
            'Methods': {x: [] for x in self.ranges['Methods']}
        } for proc in processors}

        # compilation times
        self.compile_times = {proc: {
            'Parameters': {},
            'Classes': {},
            'Methods': {}
        } for proc in processors}

    def compile(self):
        print("mvn clean")
        subprocess.run(["mvn", "clean"])

        for proc in self.processors:
            print("\nRecompiling with ", proc, " annotation processor")

            for type in self.test_types:
                for num in self.ranges[type]:
                    t = time.time()
                    str_processor = "-P" + proc
                    str_include = "-Dinclude=" + type + str(num) + ".java"

                    print("mvn", str_processor, str_include, "compile")
                    subprocess.run(["mvn", str_processor, str_include, "compile"])

                    t = time.time() - t
                    print(t, "seconds")
                    self.compile_times[proc][type][num] = t

                    # move the classes to correct dir
                    files = os.listdir("target/classes/")
                    print("Moving", len(files), "files to " + "classes-" + proc + "/")
                    for file in files:
                        os.rename("target/classes/" + file, "classes-" + proc + "/" + file)

    def runTest(self):
        for i in range(0, self.repeats):
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
        with open('testing_results.csv', 'w') as file:
            header = 'processor,test,num'
            for i in range(0, self.repeats):
                header += ',res%d' % i
            header += ",avg\n"

            file.write(header)
            for proc in self.processors:
                for t in self.test_types:
                    for num in self.ranges[t]:
                        row = proc + ',' + t + ',' + str(num) + ',' + ','.join([str(i) for i in self.results[proc][t][num]]) + ',' + str(average_ms(self.results[proc][t][num])) + '\n'
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
                    ax.scatter([num] * self.repeats, [(i / 1e6) for i in self.results[proc][t][num]], color=c)
                ax.plot(self.ranges[t], [average_ms(self.results[proc][t][x]) for x in self.ranges[t]], '-', color=c)
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
                ax.plot(self.ranges[t], [self.compile_times[proc][t][x] for x in self.ranges[t]], '-', color=c)
                legend.append(mpatches.Patch(color=c, label=proc))
                colors.append(c)

            plt.legend(handles=legend)
            plt.draw()
            fig.savefig('figures/compile-time-' + t + '.pdf', bbox_inches='tight')



# main
tester = Tester(20, ["visitor", "switch", "reflection"])

# tester.compile()
# tester.plot_compile_times()
#
# tester.runTest()
# tester.write_results()

tester.read_csv()
tester.plot_results()