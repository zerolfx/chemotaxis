import sys

from PIL import Image

file = sys.argv[1]

with open(file, 'r') as f:
    n = int(f.readline())
    img = Image.new('RGB', (n, n), (255, 255, 255))
    pixels = img.load()
    sx, sy, tx, ty = map(int, f.readline().split())
    pixels[sx - 1, sy - 1] = (0, 0, 255)
    pixels[tx - 1, ty - 1] = (0, 255, 0)
    try:
        while True:
            x, y = map(int, f.readline().split())
            pixels[x - 1, y - 1] = (0, 0, 0)
    except Exception:
        pass
    img.save(file + '.png')
