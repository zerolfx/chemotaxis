import sys
from functools import partial

from PIL import Image

file = sys.argv[1]

img: Image = Image.open(file).convert('RGB')

with open(file[:-4], 'w') as f:
    print = partial(print, file=f)
    n = img.size[0]
    print(n)

    for i in range(n):
        for j in range(n):
            if img.getpixel((i, j)) == (0, 0, 255):
                print(i + 1, j + 1, end=' ')
    for i in range(n):
        for j in range(n):
            if img.getpixel((i, j)) == (0, 255, 0):
                print(i + 1, j + 1)
    for i in range(n):
        for j in range(n):
            if img.getpixel((i, j)) == (0, 0, 0):
                print(i + 1, j + 1)
