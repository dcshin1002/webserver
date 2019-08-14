from json import JSONEncoder

import pandas as pd
from pymongo import MongoClient
import pyrebase

config = {
    "apiKey": "AIzaSyBq9T5DgM3FUes-65pLo_rGlkiXO42EUzA",
    "authDomain": "smart-router-17060.firebaseapp.com",
    "databaseURL": "https://smart-router-17060.firebaseio.com",
    "storageBucket": "smart-router-17060.appspot.com",
    # "serviceAccount": "firebase-adminsdk.json"
}

firebase = pyrebase.initialize_app(config)
firebaseDB = firebase.database()

mongoDBuri = 'mongodb://heroku_h3zn98bh:pcmg8thaanrfo1aqufavp8a3fa@ds359847.mlab.com:59847/heroku_h3zn98bh'
client = MongoClient(mongoDBuri)
mongodb = client.get_default_database()

num_cluster = 0

list_ParcelRaw = []
dict_Parcel = {}
dict_Cluster = {}

df = pd.DataFrame(columns=('lon', 'lat'))
tspFiles = []

def dropDB(name):
    mongodb.drop_collection(name)

def getTMSDB(name):
    return mongodb[name]

class ParcelEncoder(JSONEncoder):
    def default(self, o):
        return o.__dict__

class ParcelRaw:
    def __init__(self, id, addr):
        self.id = id
        self.addr = addr

class Parcel:
    def __init__(self, id, addr, lat, lon):
        self.id = id
        self.addr = addr
        self.lat = lat
        self.lon = lon
