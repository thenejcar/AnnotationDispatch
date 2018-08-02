import csv
import os
import subprocess
from matplotlib import pyplot as plt

def average_ms(lst):
    return sum(lst) / len(lst) / 1e6


class Tester:
    def __init__(self, repeats, processors):
        self.repeats = repeats
        # processors = ["switch", "reflection", "visitor"]
        self.processors = processors

        self.parameters_range = [1, 2, 3, 5, 10, 15, 20]
        self.results_parameters = {proc: {x: [] for x in self.parameters_range} for proc in self.processors}

        self.classes_range = [1, 2, 3, 4, 5, 8, 10]
        self.results_classes = {proc: {x: [] for x in self.classes_range} for proc in self.processors}

        self.methods_range = [1, 2, 4, 5, 10, 20, 25]
        self.results_methods = {proc: {x: [] for x in self.methods_range} for proc in self.processors}

    def runTest(self):
        for i in range(0, self.repeats):
            print("Test run", i)
            # TODO: when generator is realiable enough, generate new .java files each round

            for processor in self.processors:

                print("\nRecompiling with ", processor, " annotation processor")

                # subprocess.run(["mvn", "-q", "clean"])
                # subprocess.run(["mvn", "-q" "-Ptest-" + processor + "-parameters", "compile"])
                # subprocess.run(["mvn", "-q" "-Ptest-" + processor + "-parameters", "compile"])
                # subprocess.run(["mvn", "-q" "-Ptest-" + processor + "-classes", "compile"])
                subprocess.run(["mvn", "-Ptest-" + processor + "-all", "clean", "compile"])

                # move to target dir and run programs
                os.chdir("target/classes/")

                print("Testing Parameters")
                for num in self.parameters_range:
                    clazz = "Parameters" + str(num)
                    print("%-15s" % clazz, end="")
                    result = subprocess.run(["java", clazz], stdout=subprocess.PIPE)
                    time = int(result.stdout)
                    self.results_parameters[processor][num].append(time)
                    print("%20d" % time)

                print("Testing Classes")
                for num in self.classes_range:
                    clazz = "Classes" + str(num)
                    print("%-15s" % clazz, end="")
                    result = subprocess.run(["java", clazz], stdout=subprocess.PIPE)
                    time = int(result.stdout)
                    self.results_classes[processor][num].append(time)
                    print("%20d" % time)

                print("Testing Methods")
                for num in self.methods_range:
                    clazz = "Methods" + str(num)
                    print("%-15s" % clazz, end="")
                    result = subprocess.run(["java", clazz], stdout=subprocess.PIPE)
                    time = int(result.stdout)
                    self.results_methods[processor][num].append(time)
                    print("%20d" % time)


                os.chdir("../../")


    def write_csv(self):
        print("results:")

        with open('testing_results.csv', 'w') as file:
            header = 'processor,test,num'
            for i in range(0, self.repeats):
                header += ',res%d\n' % i
            header += ",avg"

            file.write(header)
            for proc in self.processors:
                for num in self.parameters_range:
                    row = proc + ',Parameters,' + str(num) + ',' + ','.join([str(i) for i in self.results_parameters[proc][num]]) + ',' + str(average_ms(self.results_parameters[proc][num]))
                    print(row)
                    file.write(row)
                for num in self.classes_range:
                    row = proc + ',Classes,' + str(num) + ',' + ','.join([str(i) for i in self.results_classes[proc][num]]) + ',' + str(average_ms(self.results_classes[proc][num]))
                    print(row)
                    file.write(row)
                for num in self.methods_range:
                    row = proc + ',Methods,' + str(num) + ',' + ','.join([str(i) for i in self.results_methods[proc][num]]) + ',' + str(average_ms(self.results_methods[proc][num]))
                    print(row)
                    file.write(row)

            file.close()

    def read_csv(self):
        # clear the dictionaries
        self.results_parameters = {proc: {x: [] for x in self.parameters_range} for proc in self.processors}
        self.results_classes = {proc: {x: [] for x in self.classes_range} for proc in self.processors}
        self.results_methods = {proc: {x: [] for x in self.methods_range} for proc in self.processors}

        with open('testing_results.csv', 'r') as file:
            reader = csv.reader(file)
            next(reader)
            for row in reader:
                row
                proc = row[0]
                test = row[1]
                num = int(row[2])
                results = row[3:-1]

                if test == "Parameters":
                    self.results_parameters[proc][num] = [int(x) for x in results]
                elif test == "Classes":
                    self.results_classes[proc][num] = [int(x) for x in results]
                elif test == "Methods":
                    self.results_methods[proc][num] = [int(x) for x in results]


    def plot_results(self):
        # for processor in ["switch", "reflection", "visitor"]:
        for proc in self.processors:
            fig = plt.figure(figsize=(7, 7))
            fig.suptitle("Speedtest for " + proc + " annotation processor")
            ax = fig.add_subplot(111)
            ax.set_xlabel('Number of parameters/classes/methods')
            ax.set_ylabel('ms')
            ax.grid(which='major', linestyle='-')
            ax.grid(which='minor', linestyle=':')

            parameters_y = [average_ms(self.results_parameters[proc][x]) for x in self.parameters_range]
            classes_y = [average_ms(self.results_classes[proc][x]) for x in self.classes_range]
            methods_y = [average_ms(self.results_methods[proc][x]) for x in self.methods_range]

            ax.plot(self.parameters_range, parameters_y, '-', color='r')
            ax.plot(self.classes_range, classes_y, '-', color='g')
            ax.plot(self.methods_range, methods_y, '-', color='b')

            for num in self.parameters_range:
                ax.scatter([num] * self.repeats, [(i / 1e6) for i in self.results_parameters[proc][num]], color='r')
            for num in self.classes_range:
                ax.scatter([num] * self.repeats, [(i / 1e6) for i in self.results_classes[proc][num]], color='g')
            for num in self.methods_range:
                ax.scatter([num] * self.repeats, [(i / 1e6) for i in self.results_methods[proc][num]], color='b')

            plt.legend()
            plt.draw()
            fig.savefig('figures/speedtest_' + proc + '.pdf', bbox_inches='tight')



# main
tester = Tester(20, ["switch", "reflection"])

tester.runTest()
tester.write_csv()
tester.plot_results()