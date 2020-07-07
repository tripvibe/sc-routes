#!/bin/bash

SCROUTE=https://sc-routes-labs-dev.apps.r2r.vic.apac.rht-labs.com
DIST=200
NEXT=1800
PAST=0

callLatLong() {
    http --timeout 240 ${SCROUTE}/api/nearby-departures/${LATLONG}/${DIST}?nextSeconds=${NEXT}\&pastSeconds=${PAST}
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


callSearch() {
    http --timeout 240 ${SCROUTE}/api/search-departures/${SEARCH}
}

SEARCH=central
callSearch
SEARCH=north
callSearch
SEARCH=melbourne%20zoo
callSearch
SEARCH=toorak
callSearch
