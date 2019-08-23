import sys
import os
import tmscore.DataController
import tmscore.Distributer
from tmscore.RouteFinder import RouteFinder
import tmscore.Adapter as adap

def testMain():
    if len(sys.argv)<5:
        print("arguments are not matched")
        return
    if sys.argv[1] == "set":
        year = sys.argv[2]
        month = sys.argv[3]
        day = sys.argv[4]
        adap.setClustersWork(int(year),int(month),int(day))
    # elif sys.argv[1] == "setRoute":
    #    adap.setRoute()

if __name__ == "__main__":
    testMain()

# Takes first name and last name via command
# line arguments and then display them
print("Output from Python")
for i,arg in enumerate(sys.argv):
    print(str(i)+"-th name: " + arg)
print("script running path :", os.path.dirname(os.path.abspath(__file__)))
print("working path :", os.getcwd())
