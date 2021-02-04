# Author: Alexander Lachmann
# Test API for data ingestion
# /create
# /append
# /removesamples
# /removerepository
# /persist

import urllib.request
import json

def apiRequest(data, baseurl, endpoint):
    params = json.dumps(data).encode('utf8')
    req = urllib.request.Request(baseurl+"/"+endpoint, data=params, headers={'content-type': 'application/json', 'Authorization': 'Token XOXO'})
    response = urllib.request.urlopen(req)
    print(response.read().decode('utf8'))

# functionality in the datamanagement/FileUploadManager.java
# can do things like create new libraries
baseurl = "http://localhost:8080/enrichmentapi/origin/api/v1"
token = "XOXO"  #password token from system environment
repository_uuid = "dhXdse-uuid-1234"    # uuid for repository, the binary file generated will have the same name as the id

data_type = "rank_matrix"  #geneset_library
data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'entities': ['a', 'b', 'c', 'd', 'ae', 'f', 'g', 'aa', 'a1', 'a2', 'a3', 'a4', 'a5', 'a6', 'a7'],
    'data_type': data_type
}
apiRequest(data, baseurl, "create")

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'signatures': [
        {
            "uuid" : "s1xx",
            "entity_values" : [1.23, 1.2342, 3.2332424, 3, 7, 4, 4, -10.5678, -9.0, -8.0, -7.0, -6.0, -5.0, -4.0, -3.0]
        },
        {
            "uuid" : "s2xx",
            "entity_values" : [9.23, 5.2342, -37.2332424, 3, 7, -4, 4, -10.5678, -9.0, -48.0, 70.0, -6.0, 5.0, -4.0, 3.0]
        },
        {
            "uuid" : "s3xx",
            "entity_values" : [0.23, 51000.2342, 3.2332424, 3, 7, 1, 4, -12.5678, -9.0, -8.0, -7.0, -26.0, 5.0, 4.0, 13.0]
        }
    ]
}
apiRequest(data, baseurl, "append")
apiRequest(data, baseurl, "listrepositories")


repository_uuid = "dhXdse-uuid-set12"    # uuid for repository, the binary file generated will have the same name as the id
data_type = "geneset_library"       #"rank_matrix"

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'entities': ['a', 'b', 'c', 'd', 'ae', 'f', 'g', 'aa', 'a1', 'a2', 'a3', 'a4', 'a5', 'a6', 'a7'],
    'data_type': data_type
}

apiRequest(data, baseurl, "create")

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'signatures': [
        {
            "uuid" : "s1xx",
            "entities" : ["a", "b", "aa"]
        },
        {
            "uuid" : "s2xx",
            "entities" : ["b", "f", "c", "unknown", "ae"]
        },
        {
            "uuid" : "s3xx",
            "entities" : ['a4', 'a5', 'a6', 'a7']
        }
    ]
}

apiRequest(data, baseurl, "append")
apiRequest(data, baseurl, "listrepositories")
apiRequest(data, baseurl, "persist")

query_url = "http://localhost:8080/enrichmentapi/api/v1"

file = {
    'datasetname': repository_uuid,
    'bucket': "mssm-sigcomm",
    'file': repository_uuid+".so"
}
apiRequest(file, query_url, "load")
apiRequest(data, query_url, "listdata")

data = {
    'database': repository_uuid,
    "entity_ids" : ['a', 'b', 'aa', 'd', 'ae', 'f', 'g', 'c', 'a1', 'a2', 'a3', 'a4', 'a5', 'a6', 'a7'],
    "entity_values" : [1000.23, 10000.2342, 3000.2332424, -3, -7, -4, -4, -10.5678, -9.0, -8.0, -7.0, -60.0, -50.0, -40.0, -30.0]
}

apiRequest(data, query_url, "enrich/rankset")




