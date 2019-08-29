import tmscore.DataBase as db
from sklearn.cluster import KMeans
import json
from pymongo import MongoClient
import datetime


def clusteringPredefined():
    for i in range(db.num_cluster):
        db.dict_Cluster[i+1] = []

    for idx, row in db.df.iterrows():
        parcel = db.dict_Parcel[idx]
        if parcel.cluster and parcel.cluster > 0:
            db.dict_Cluster[parcel.cluster].append(parcel)


def clustering():
    kmeans = KMeans(n_clusters=db.num_cluster)
    kmeans.fit_predict(db.df)
    for i in range(db.num_cluster):
        db.dict_Cluster[i] = []

    i = 0
    for idx, row in db.df.iterrows():
        cluster_idx = kmeans.labels_[i]
        id = idx
        db.dict_Cluster[cluster_idx].append(db.dict_Parcel[id])
        i += 1

    # DBObj = db.getTMSDB('tmssample')
    # db.dropDB('tmssample')
    # now = datetime.datetime.now()
    # todayDate = now.strftime('%Y-%m-%d')

    # for v in db.dict_Cluster:
    #     cluster_num = str(v)
    #     for c in db.dict_Cluster[v]:
    #         DATA = {'id': c.id,
    #                 'date': todayDate,
    #                 'lat': c.lat,
    #                 'lon': c.lon,
    #                 'addr': c.addr,
    #                 'clusterNum':cluster_num,
    #                 'order':0,
    #                 'state':0,
    #                 'picName':''}
    #         DBObj.insert_one(DATA)
