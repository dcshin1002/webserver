#!/usr/bin/env python3
import json

from django.shortcuts import render
from django.http import HttpResponse, JsonResponse
from rq import Queue
from worker import conn

import tmscore.DataBase as db
import tmscore.DataController as dcon
import tmscore.Distributer as distributer
from tmscore.RouteFinder import RouteFinder

q = Queue(connection=conn)


def index(req):
    dcon.loadDataFromFirebaseDB('2019-07-11')
    return JsonResponse({
        'msg': '<pre>Firebase activated</pre>',
        'jobid': None,
    })


def setClusters(req, year, month, day):
    if year == None or month == None or day == None:
        return JsonResponse({
            'msg': '<pre>Invalid URL</pre>',
            'jobid': None
        })

    result = q.enqueue(setClustersWork, args=(
        year, month, day), job_timeout=600)

    return JsonResponse({
        'msg': '<pre>setClusters() Processing...</pre>',
        'jobid': result.get_id(),
    })


def setClustersWork(year, month, day, data=None):
    dateForm = '-'.join([str(year), str("%02d" % month), str("%02d" % day)])
    print(dateForm)
    dcon.loadDataFromFirebaseDB(dateForm)
    # if data is None:
    #     dcon.loadDataFromCache()
    # else:
    #     dcon.loadData(data)

    distributer.clustering()
    dcon.saveTSPFile('data')
    finder = RouteFinder()
    for c, fname in enumerate(dcon.getTSPFilenames()):
        finder.route(fname)
        dcon.saveDataToFirebaseDB(dateForm, c, finder.problem, finder.route)
        print('firebaseDB updated for cluster', c)
    print('success setClusters')


def getWorkProgress(req, jobid):
    job = None
    if jobid:
        job = q.fetch_job(jobid)

    if job is None:
        return JsonResponse({
            'msg': '<pre>jobid is wrong !!</pre>',
            'jobid': None,
            'status': None
        })

    return JsonResponse({
        'msg': '<pre>Getting status for job</pre>',
        'jobid': jobid,
        'status': job.get_status()
    })


def getClusters(req, date=None):
    DBobj = db.getTMSDB('tmssample')
    cursor = DBobj.distinct('clusterNum')  # , {'date': date})

    jsonStr = json.dumps(cursor)
    print(jsonStr)
    return HttpResponse('<pre>' + jsonStr + '</pre>')


def getEachCluster(clusterID, date=None):
    # print(date, cluseterID)
    jsonStr = json.dumps(db.ParcelEncoder().encode(db.dict_Cluster[clusterID]))
    return jsonStr

    # - res:
    # sorted List of Parcels
    # {ParcelID, address, lat, lon, deliveryState}
    pass


def getParcelState(parcelID):
    # - res:
    # deliveryState
    # PictureFile
    pass


def setParcelState(parcelID, pictureFile, updateState):
    # - res:
    # deliveryState
    pass
