import h5py as h5
import pandas as pd
import numpy as np


data = np.random.randint(0, 12000, size=(1200, 400))

f = h5.File("data/test.h5", "w")
f.create_dataset("data/expression", data=data, dtype=np.int16, chunks=True, compression="gzip")
f.close()
