#!/usr/bin/env python3
import numpy as np
import pandas as pd
# import tmscore.DataBase as db
from operator import itemgetter, attrgetter

from tmscore.som_tsp.io_helper import read_tsp, normalize
from tmscore.som_tsp.neuron import generate_network, get_neighborhood, get_route
from tmscore.som_tsp.distance import select_closest, euclidean_distance, route_distance
#from som_tsp.plot import plot_network, plot_route


class RouteFinder:
    problem = None
    route = None
    distance = None

    def __init__(self):
        pass

    def solve(self, tspFile):
        self.problem = read_tsp(tspFile)
        self.route = self.som(self.problem, 100000)
        self.problem = self.problem.reindex(self.route)
        self.distance = route_distance(self.problem)
        print('Route found for problem in {}, length {}'.format(tspFile, self.distance))
        pass

    def som(self, problem, iterations, learning_rate=0.8):
        """Solve the TSP using a Self-Organizing Map."""

        # Obtain the normalized set of cities (w/ coord in [0,1])
        cities = self.problem.copy()

        cities[['x', 'y']] = normalize(cities[['x', 'y']])

        # The population size is 8 times the number of cities
        n = cities.shape[0] * 8

        # Generate an adequate network of neurons:
        network = generate_network(n)
        #print('Network of {} neurons created. Starting the iterations:'.format(n))

        for i in range(iterations):
            if not i % 100:
                #print('\t> Iteration {}/{}'.format(i, iterations), end="\r")
                pass
            # ChooRoutese a random city
            city = cities.sample(1)[['x', 'y']].values
            winner_idx = select_closest(network, city)
            # Generate a filter that applies changes to the winner's gaussian
            gaussian = get_neighborhood(winner_idx, n//10, network.shape[0])
            # Update the network's weights (closer to the city)
            network += gaussian[:, np.newaxis] * \
                learning_rate * (city - network)
            # Decay the variables
            learning_rate = learning_rate * 0.99997
            n = n * 0.9997

            # Check for plotting interval
            # if not i % 1000:
            #     plot_network(cities, network, name='diagrams/{:05d}.png'.format(i))

            # Check if any parameter has completely decayed.
            if n < 1:
                #print('Radius has completely decayed, finishing execution','at {} iterations'.format(i))
                break
            if learning_rate < 0.001:
                #print('Learning rate has completely decayed, finishing execution','at {} iterations'.format(i))
                break
        else:
            pass
            #print('Completed {} iterations.'.format(iterations))

        # plot_network(cities, network, name='diagrams/final.png')

        self.route = get_route(cities, network)
        # DBobj = db.getTMSDB('tmssample')
        # order = 1
        # for idx, row in cities.sort_values(by='winner').iterrows():
        #     query = {"id": int(row['city'])}
        #     update = {'$set': {'order': order}}
        #     DBobj.update_one(query, update)
        #     order += 1
        # plot_route(cities, route, 'diagrams/route.png')
        return self.route
