import json
import requests
import sys

def sorting(item):
    separated = item.split("-")
    date = separated[-1]
    if(len(date) == 1):
        date = separated[-2]
    return date

def getLatest(d, r):
    url = 'http://repo.lab.pl.alcatel-lucent.com/api/docker/' + r + '/v2/' + d + '/tags/list'
    response = requests.get(url, auth=('restapi', 'restapi'))
    jData = json.loads(response.text)
    tags = sorted(jData['tags'], key=sorting)
    tags = list(filter(lambda s: "centos7" in s, tags))
    tag = tags[-1]
    if (tag == 'latest'):
        print(tags[-2])
    else:
        print(tag)

getLatest(sys.argv[1], sys.argv[2])
