#!/bin/bash

SCROUTE=https://sc-routes-labs-dev.apps.r2r.vic.apac.rht-labs.com
DIST=200
NEXT=1800
PAST=0

callLatLong() {
    #http --timeout 240 ${SCROUTE}/api/nearby-departures/${LATLONG}/${DIST}?nextSeconds=${NEXT}\&pastSeconds=${PAST}
    hey -t 120 -c 10 -n 10 ${SCROUTE}/api/nearby-departures/${LATLONG}/${DIST}?nextSeconds=${NEXT}\&pastSeconds=${PAST}
}

LATLONG=-37.7968151,144.9507116
callLatLong
LATLONG=-37.7552514,144.8363011
callLatLong
LATLONG=-37.6905490,144.8687955
callLatLong
LATLONG=-37.8142819,144.9564245
callLatLong
LATLONG=-37.8183886,144.9524854
callLatLong
LATLONG=-37.8974484,145.088703
callLatLong
LATLONG=-37.8143622,144.9675997
callLatLong

callSearch() {
    #http --timeout 240 ${SCROUTE}/api/search-departures/${SEARCH}
    hey -t 120 -c 10 -n 10 ${SCROUTE}/api/search-departures/${SEARCH}
}

SEARCH=central?routeType=2
callSearch
SEARCH=north?routeType=3
callSearch
SEARCH=melbourne%20zoo?routeType=2
callSearch
SEARCH=toorak?routeType=2
callSearch
SEARCH=Lara?routeType=1
callSearch
SEARCH=Lara?routeType=2
callSearch
SEARCH=Lara?routeType=3
callSearch
SEARCH=haines?routeType=1
callSearch
SEARCH=haines?routeType=2
callSearch

