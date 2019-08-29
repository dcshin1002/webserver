#!/usr/bin/env python3
import json

from django.shortcuts import render
from django.http import HttpResponse, JsonResponse
from rq import Queue
from rq.job import Job, get_current_job
from worker import conn

import tmscore.DataBase as db
import tmscore.DataController as dcon
import tmscore.Distributer as distributer
from tmscore.RouteFinder import RouteFinder

q = Queue(connection=conn)


def index(req):
    return HttpResponse("<pre>Welcome to Beyond TMS system !</pre>")


def setClusters(req, year, month, day):
    if year == None or month == None or day == None:
        return JsonResponse({
            'msg': '<pre>Invalid URL</pre>',
            'jobid': None
        })

    result = q.enqueue(setClustersWork, args=(
        year, month, day), job_timeout=600)

    # dateForm = '-'.join([str(year), str("%02d" % month), str("%02d" % day)])
    # dcon.saveJobStateToFirebaseDB(
    #     dateForm, Job.fetch(result.get_id()).get_status())

    return JsonResponse({
        'msg': '<pre>setClusters() Processing...</pre>',
        'jobid': result.get_id(),
    })


def setClustersWork(year, month, day, data=None):
    dateForm = '-'.join([str(year), str("%02d" % month), str("%02d" % day)])
    print(dateForm)
    dcon.saveJobStateToFirebaseDB(dateForm, get_current_job().get_status())
    dcon.loadParcelDataFromFirebaseDB(dateForm)

    distributer.clustering()
    dcon.saveTSPFile('data')
    finder = RouteFinder()
    for c, fname in enumerate(dcon.getTSPFilenames()):
        finder.solve(dateForm, fname)
        dcon.saveParcelDataToFirebaseDB(
            dateForm, c+1, finder.problem, finder.route)
        print('firebaseDB updated for cluster', c+1)
    dcon.saveJobStateToFirebaseDB(dateForm, "finished")
    print('setClusters Done')


def setRoute(req, year, month, day):
    if year == None or month == None or day == None:
        return JsonResponse({
            'msg': '<pre>Invalid URL</pre>',
            'jobid': None
        })

    result = q.enqueue(setRouteWork, args=(
        year, month, day), job_timeout=600)

    # dateForm = '-'.join([str(year), str("%02d" % month), str("%02d" % day)])
    # dcon.saveJobStateToFirebaseDB(
    #     dateForm, Job.fetch(result.get_id()).get_status())

    return JsonResponse({
        'msg': '<pre>setRoute() Processing...</pre>',
        'jobid': result.get_id(),
    })


def setRouteWork(year, month, day, cluster=None):
    dateForm = '-'.join([str(year), str("%02d" % month), str("%02d" % day)])
    print(dateForm)
    dcon.saveJobStateToFirebaseDB(dateForm, get_current_job().get_status())
    dcon.loadParcelDataFromFirebaseDB(dateForm)

    distributer.clusteringPredefined()
    dcon.saveTSPFile('data')
    finder = RouteFinder()
    for c, fname in enumerate(dcon.getTSPFilenames()):
        finder.solve(dateForm, fname)
        dcon.saveParcelDataToFirebaseDB(
            dateForm, c+1, finder.problem, finder.route)
        print('firebaseDB updated for cluster', c+1)
    dcon.saveJobStateToFirebaseDB(dateForm, "finished")
    print('setRouter Done')


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
