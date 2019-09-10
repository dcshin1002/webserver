import math
import requests

from urllib import parse
from haversine import haversine

import tmscore.DataBase as db


API_HOST = "https://dapi.kakao.com"
API_KEY = "a9a4f76e68df45d99954e267b0337b44"
headers = {'Authorization': 'KakaoAK {}'.format(API_KEY)}


def loadParcelDataFromFirebaseDB(dateForm):
    parcels = db.firebaseDB.child("parcel_list").child(dateForm).get()
    parcelsArr = parcels.val()

    db.num_cluster = -1
    for parcel in parcelsArr:
        if parcel is None:
            continue
        id = parcel['id']
        addr = parcel['consigneeAddr']
        lon = parcel['consigneeLongitude']
        lat = parcel['consigneeLatitude']
        cluster = parcel['sectorId']

        item = db.ParcelRaw(id, addr)
        print(id, addr, lon, lat)
        db.list_ParcelRaw.append(item)
        params = getParamsFromParcelRaw(item)
        db.dict_Parcel[id] = db.Parcel(id, addr, lat, lon, cluster)
        db.df.loc[id] = [lat, lon]
        db.num_cluster = max(db.num_cluster, cluster)

    if db.num_cluster < 1:
        # 60 parcels per a person
        db.num_cluster = math.ceil(len(parcelsArr) / 60)


def loadDataFromCache():
    f = open('tmscore/cache.txt', 'r', encoding='utf-8')
    db.num_cluster = int(f.readline().rstrip())
    lines = f.readlines()
    f.close()
    for line in lines:
        if line:
            info = line.split(",")
            id = int(info[0])
            addr = info[1]
            lon = float(info[2])
            lat = float(info[3].rstrip())
            item = db.ParcelRaw(id, addr)
            db.list_ParcelRaw.append(item)
            params = getParamsFromParcelRaw(item)
            db.dict_Parcel[id] = db.Parcel(id, addr, lat, lon)
            db.df.loc[id] = [lat, lon]


def loadData(fname):
    f = open(fname, 'r', encoding='utf-8')
    db.num_cluster = int(f.readline().rstrip())
    lines = f.readlines()
    f.close()
    for line in lines:
        if line:
            info = line.split(",")
            item = db.ParcelRaw(int(info[0]), info[1].rstrip())
            db.list_ParcelRaw.append(item)
            params = getParamsFromParcelRaw(item)
            resp = req('/v2/local/search/address.json',
                       '', 'GET', params).json()
            addr = resp['documents'][0]['address']['address_name']
            lon = float(resp['documents'][0]['address']['x'])
            lat = float(resp['documents'][0]['address']['y'])
            db.dict_Parcel[item.id] = db.Parcel(item.id, addr, lat, lon)
            db.df.loc[item.id] = [lat, lon]


def saveParcelDataToFirebaseDB(dateForm, cluster, problem, route):
    parcels = db.firebaseDB.child("parcel_list").child(dateForm).get()
    parcelsArr = parcels.val()

    dateKey = "parcel_list/" + dateForm
    data = {dateKey: {}}
    data[dateKey] = parcelsArr
    for i, v in enumerate(route):
        city = problem.iloc[v][0]
        idx = int(city)
        data[dateKey][idx]['sectorId'] = cluster
        data[dateKey][idx]['orderInRoute'] = i+1
    db.firebaseDB.update(data)


def saveJobStateToFirebaseDB(dateForm, status):
    print(status)
    db.firebaseDB.child("backend_status").child(
        dateForm).update({"route_job": status})


def saveTSPFile(fbase):
    for k, items in db.dict_Cluster.items():
        fname = fbase + '_' + str(k+1) + '.tsp'
        f = open(fname, 'w', encoding='utf-8')
        # dummy information
        f.write('NAME : TMS test data_' + str(k+1) + '\n')
        f.write('COMMENT : TMS test data of about 300 spots to deliever\n')
        f.write('TYPE : TSP\n')
        f.write('DIMENSION : ' + str(len(items))+'\n')
        f.write('EDGE_WEIGHT_TYPE : EUC_2D\n')
        f.write('NODE_COORD_SECTION\n')
        for item in items:
            f.write(
                ' '.join([str(item.id), str(item.lat), str(item.lon)])+'\n')
        f.close()

        db.tspFiles.append(fname)


def getTSPFilenames():
    return db.tspFiles


def getParamsFromParcelRaw(rawitem):
    return {"query": rawitem.addr}


def req(path, query, method, data={}):
    url = API_HOST + path + "?"
    if method == 'GET':
        getParams = parse.urlencode(data)
        return requests.get(url + getParams, headers=headers)
    else:
        return requests.post(url, headers=headers, data=data)
