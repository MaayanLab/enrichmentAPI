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


#----------------------------------------------------------------
#------------------ test API for rank data ----------------------
#----------------------------------------------------------------

data_type = "rank_matrix"  #geneset_library


#----------------------------------------------------------------
# create a repository
# if repository exists all signatures will be deleted and an empty repository is created

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'entities': ['a', 'b', 'c', 'd', 'ae', 'f', 'g', 'aa'],
    'data_type': data_type
}

apiRequest(data, baseurl, "listrepositories")
apiRequest(data, baseurl, "create")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
# append a list of signatures to a previously created repository
# when uploading entity values they have to be in the same order of the initialization step.
# if a vector has a number of values mismatching the number of entities the signature will be ignored

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'signatures': [
        {
            "uuid" : "s1xx",
            "entity_values" : [1.23, 1.2342, 3.2332424, 3, 7, 4, 4, -10.5678]
        },
        {
            "uuid" : "s2xx",
            "entity_values" : [9.23, 5.2342, -37.2332424, 3, 7, -4, 4, -10.5678]
        },
        {
            "uuid" : "s3xx",
            "entity_values" : [0.23, 51000.2342, 3.2332424, 3, 7, 1, 4, -12.5678]
        }
    ]
}

apiRequest(data, baseurl, "append")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
# persist created repository with the appended signatures
# this is independend of the data type
# persisting the data will write a binary object and push it to an aws bucket specified in the environment
# the dataset json will be updated in the aws bucket
# after completion the repository is automatically removed from memory
# the repositories will have to be reinitialized to take effect

data = {
    'repository_uuid': repository_uuid,
    'token': token
}

apiRequest(data, baseurl, "persist")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
# remove a list of samples from a given repository
# this is independend of the data type

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'signatures': ["s1xx", "s3xx"]
}

apiRequest(data, baseurl, "removesamples")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
# remove a given repository
# this is independend of the data type

data = {
    'repository_uuid': repository_uuid,
    'token': token
}

apiRequest(data, baseurl, "removerepository")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
#------------------- test API geneset data ----------------------
#----------------------------------------------------------------

data_type = "geneset_library"       #"rank_matrix"  #geneset_library


#----------------------------------------------------------------
# create a repository
# if repository exists all signatures will be deleted and an empty repository is created
# the enties have to be declared at repository creation
# if an entity was not initialized it will be ignored

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'entities': ['a', 'b', 'c', 'd', 'ae', 'f', 'g', 'aa'],
    'data_type': data_type
}

apiRequest(data, baseurl, "listrepositories")
apiRequest(data, baseurl, "create")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
# append a list of signatures to a previously created repository
# when uploading entity sets only uuids of entities are saved when they where provided in the creation step of the repository

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
            "entities" : ["d"]
        }
    ]
}

apiRequest(data, baseurl, "append")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
# persist created repository with the appended signatures
# this is independend of the data type
# persisting the data will write a binary object and push it to an aws bucket specified in the environment
# the dataset json will be updated in the aws bucket
# after completion the repository is automatically removed from memory
# the repositories will have to be reinitialized to take effect

data = {
    'repository_uuid': repository_uuid,
    'token': token
}

apiRequest(data, baseurl, "persist")
apiRequest(data, baseurl, "listrepositories")

data = {
    'repository_uuid': repository_uuid,
    'token': token,
    'signatures': ["s1xx", "s3xx"]
}


#----------------------------------------------------------------
# remove a list of samples from a given repository
# this is independend of the data type

apiRequest(data, baseurl, "removesamples")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
# remove a given repository
# this is independend of the data type

data = {
    'repository_uuid': repository_uuid,
    'token': token
}

apiRequest(data, baseurl, "removerepository")
apiRequest(data, baseurl, "listrepositories")


#----------------------------------------------------------------
#------------------- reload data repositories -------------------
#----------------------------------------------------------------


baseurl = "http://localhost:8080/EnrichmentAPI/api/v1"
apiRequest(data, baseurl, "loadrepositories")






