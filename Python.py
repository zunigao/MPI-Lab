from sklearn.datasets.samples_generator import make_blobs

data, cluster = make_blobs(n_samples=int(1e6), centers=10, n_features=4, random_state=90125)

with open('/tmp/output.txt','w') as f:
    for row in data:
        for col in row:
            f.write(str(col) + ' ')
        f.write('\n')
