import os
import shutil
import subprocess
from collections import defaultdict

# r, b, time
CONFIG = [
    (20, 50, 100),
    (20, 30, 100),
    (10, 50, 100),
    (10, 30, 100),

    (20, 100, 1000),
]

TEAM = 'g8'

COMMAND = "java -cp .:chemotaxis/org.json.jar chemotaxis.sim.Simulator --team {team_name} --spawnFreq {r} --budget {b} -m {map_name} --turns {time} -s 10 -l log.txt "

result = defaultdict(lambda: defaultdict(dict))

CUR_DIR = os.path.abspath(os.path.dirname(__file__))
BASEDIR = os.path.dirname(os.path.dirname(CUR_DIR))
MAP_DIR = os.path.join(BASEDIR, 'maps', TEAM)
print("BASEDIR: ", BASEDIR)

print(subprocess.check_output("make compile".split(), cwd=BASEDIR))

for folder in os.listdir(CUR_DIR):
    path = os.path.join(CUR_DIR, folder)
    if not os.path.isdir(path):
        continue
    for file in ['Agent.java', 'Controller.java']:
        shutil.copy(os.path.join(path, file), os.path.join(CUR_DIR, file))

    temp_dir = os.path.join(MAP_DIR, folder)
    os.makedirs(temp_dir, exist_ok=True)

    for map_file in os.listdir(MAP_DIR):
        if map_file.endswith('.map'):
            shutil.copyfile(os.path.join(MAP_DIR, map_file), os.path.join(temp_dir, map_file))
            for r, b, time in CONFIG:
                try:
                    output = subprocess.check_output(
                        COMMAND.format(team_name=TEAM + os.path.sep + folder, map_name=map_file, r=r, b=b,
                                       time=time).split(), cwd=BASEDIR)
                except subprocess.CalledProcessError as e:
                    output = e.output
#                 print(output)
                info = output.decode().split('\n')[-5].split('] ')[1]
                print("Algorithm: %s, Map: %s, Config: %s => %s" % (folder, map_file, (r, b, time), info))
                result[folder][map_file][(r, b, time)] = info

    shutil.rmtree(temp_dir)

for file in ['Agent.java', 'Controller.java']:
    os.remove(os.path.join(CUR_DIR, file))

print(subprocess.check_output("make clean".split(), cwd=BASEDIR))

print(result)