import sys

from PIL import Image

file = sys.argv[1]

with open(file, 'r') as f:
    n = int(f.readline())
    img = Image.new('RGB', (n, n), (255, 255, 255))
    pixels = img.load()
    sx, sy, tx, ty = map(int, f.readline().split())
    pixels[sy - 1, sx - 1] = (0, 0, 255)
    pixels[ty - 1, tx - 1] = (0, 255, 0)
    try:
        while True:
            x, y = map(int, f.readline().split())
            pixels[y - 1, x - 1] = (0, 0, 0)
    except Exception:
        pass
    img.save(file + '.png')
