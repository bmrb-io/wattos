"""
Author: Jurgen F. Doreleijers, BMRB, Jan 2007
"""
# Standard
from Wattos.Utils.localConstants import urlDB
import urllib
# BMRB specific
#from localConstants import *

def getEntryListFromCsvFile(urlLocation):
  result = []
##108d
##149d
  r1 = urllib.urlopen(urlLocation)
  data = r1.read()
  r1.close()
  dataLines = data.split("\n")
  for dataLine in dataLines:
    if dataLine:
        (pdbCode,) = dataLine.split()
        result.append( pdbCode )
  return result


def getBmrbNmrGridEntries():
  result = []
  urlLocation = urlDB+"/entry.txt"
##4583    \N    108d    \N    \N
##4584    \N    149d    \N    \N
  r1 = urllib.urlopen(urlLocation)
  data = r1.read()
  r1.close()
  dataLines = data.split("\n")
  for dataLine in dataLines:
    if dataLine:
        # b is for bogus/unused
#        print "DEBUG: read dataLine: [%s]" % dataLine
        (b1,b2,pdbCode,b3,b4) = dataLine.split() #@UnusedVariable
        result.append( pdbCode )
  return result

def getBmrbNmrGridEntriesDOCRfREDDone():
  result = []
  urlLocation = urlDB+"/mrfile.txt"
##61458    7567    4-filtered-FRED    2gov    2006-05-11
##61459    7567    4-filtered-FRED    2gov    2006-05-11
  r1 = urllib.urlopen(urlLocation)
  data = r1.read()
  r1.close()
  dataLines = data.split("\n")
  for dataLine in dataLines:
    if dataLine:
        # b is for bogus/unused
        (b1,b2,stage,pdbCode,b3) = dataLine.split() #@UnusedVariable
        if stage=="4-filtered-FRED":
            if pdbCode not in result:
                result.append( pdbCode )
  return result

#### 1
##listDOCRfREDiteration1 = [ '1a24','1a57','1a5j','1a6x','1aa3','1ab7','1af8','1afi','1agg','1ah9','1ahl','1aj3','1ajw','1ak7','1alg','1aoy','1apf','1auz','1azj','1azk','1b1v','1b22','1b2t','1b3c','1b4r','1b64','1b75','1b9p','1ba4','1bal','1bcn','1bct','1beg','1bf0','1bf9','1bfi','1bgk','1bh4','1bip','1bjx','1bla','1blj','1blq','1blr','1bm4','1bnb','1bno','1bo0','1bqv','1bqz','1brv','1bsh','1bu9','1bvm','1by0','1bym','1bzg','1c05','1c06','1c3t','1c49','1c54','1c7v','1c8p','1ce3','1cej','1cey','1cfc','1cfe','1cfg','1chl','1ck2','1ck9','1ckv','1cky','1ckz','1cmf','1cmg','1cmr','1cmz','1cn7','1cok','1cq0','1cw5','1cw6','1cwx','1cx1','1cxr','1cz5','1d0r','1d1n','1d1r','1d2b','1d3z','1d5v','1d8b','1d8v','1d8z','1d9a','1d9s','1dby','1dc2','1dc7','1dcj','1dd2','1de3','1dec','1df6','1dkc','1dmo','1doq','1dp3','1ds9','1dsk','1du1','1du2','1du9','1dv5','1dx7','1e0g','1e0h','1e17','1e41','1e5c','1e8l','1e8q','1e8r','1e9k','1e9t','1ed7','1ef5','1efe','1egx','1ehx','1eih','1eiw','1eo1','1eot','1eph','1epj','1eq0','1eq3','1eww','1eza','1ezo','1ezp','1ezy','1f16','1f2h','1f3y','1f43','1f53','1fad','1faf','1fcl','1fd6','1fdm','1fex','1fgp','1fho','1fht','1fjd','1fjn','1fjp','1fme','1fmm','1fo7','1fow','1fqq','1fr0','1fry','1fsb','1fuw','1fvl','1fwo','1fwp','1fyj','1g03','1g11','1g26','1g2t','1g4f','1g6j','1g6m','1g91','1g9l','1g9p','1ga3','1gb1','1gb4','1gd3','1gd4','1gd5','1ge9','1ggw','1gh5','1gh8','1gh9','1ghh','1ghk','1ghu','1gjs','1gw3','1gxe','1gxg','1gyz','1h0z','1h2o','1h3z','1h40','1h7d','1h7j','1h95','1ha6','1ha8','1hd6','1hdl','1hej','1hfg','1hn6','1ho0','1ho2','1ho7','1ho9','1hof','1hp2','1hp3','1hp9','1hpw','1hqi','1hs7','1hvw','1hx2','1hx7','1hy8','1hzk','1i11','1i25','1i2u','1i2v','1i42','1i5j','1i6c','1i6g','1ib9','1ich','1idl','1ie6','1ieh','1iez','1ifw','1ig6','1igl','1il6','1imq','1iox','1ip9','1iqo','1irh','1irz','1itf','1itl','1ivm','1iw4','1ix5','1iy5','1iy6','1iyc','1iyg','1iyu','1iyy','1j0t','1j6y','1j7m','1j7q','1j7r','1j8c','1j8i','1j8k','1jas','1jc6','1jcu','1jdq','1je3','1je4','1je9','1jfj','1jfn','1jgk','1jh3','1jhb','1ji8','1jjg','1jjz','1jkz','1jlz','1jnj','1jns','1jo5','1jo6','1jor','1jqr','1jr6','1jrj','1jrm','1jt8','1ju8','1jv8','1jw2','1jw3','1jwe','1jyt','1jzu','1k0p','1k0s','1k0x','1k18','1k19','1k1c','1k1v','1k1z','1k3j','1k5o','1k8b','1k8h','1k8o','1k9c','1kal','1kbs','1kdf','1kdl','1kft','1kgm','1khm','1kik','1kio','1kj0','1kjs','1kkd','1kkg','1kma','1kmd','1kn7','1kot','1koy','1koz','1kq8','1kqh','1kqi','1krw','1ks0','1ktm','1kul','1kun','1kv4','1kx6','1l1i','1l1p','1l3g','1l3h','1l3y','1l7b','1l7y','1ld5','1ld6','1ldl','1ldr','1lfc','1lg4','1lgl','1lkj','1lm0','1lqh','1ls4','1lsi','1lyp','1m12','1m2f','1m30','1m39','1m3a','1m3b','1m3c','1m5z','1m7t','1m8l','1m94','1m9g','1maj','1mb6','1mg8','1mjd','1mk3','1mke','1mm4','1mm5','1mmc','1mnl','1mp1','1mpz','1mut','1mzk','1n1u','1n3g','1n4i','1n4t','1n4y','1n6u','1n6z','1n88','1n8m','1n91','1n9d','1nb1','1nbj','1nbl','1nct','1nd9','1ne5','1ner','1nfa','1ni7','1nj7','1nm7','1nmr','1nmv','1nor','1nr3','1nrb','1nso','1nwb','1nwv','1nxi','1ny4','1ny9','1nyn','1nzp','1o1w','1o6x','1o78','1o8r','1o8t','1oca','1odp','1odq','1odr','1oef','1ojg','1omt','1omu','1onb','1op1','1oq3','1or5','1orx','1p1t','1p9j','1pav','1pbu','1pfl','1plo','1pmc','1pms','1pn5','1pux','1q27','1q2k','1q2n','1q59','1qbf','1qfd','1qfr','1qhk','1qk6','1qk7','1qkh','1qly','1qnd','1qqv','1qu5','1qxf','1r63','1roo','1rot','1sdf','1spy','1ssn','1sso','1suh','1sut','1tbd','1tcp','1tfb','1tfs','1tih','1tit','1tle','1tnn','1tnp','1tnw','1tof','1txa','1u2f','1ud7','1ugl','1uxc','1vhp','1vih','1vii','1vtp','1wkt','1xna','1zac','1zaq','2bbg','2cjn','2cpb','2cps','2eza','2ezh','2ezl','2ezm','2hfh','2hid','2hp8','2hqi','2igg','2igh','2ktx','2lfb','2mob','2pta','2r63','2sob','2sxl','2u1a','2u2f','2vik','3bdo','3ci2','3gb1','3hsf','3nla','3pdz','3trx' ]
##print "Number entries listDOCRfREDiteration1        : %s" % len(listDOCRfREDiteration1)
##
#### 2
##listCurrentEntries = getBmrbNmrGridEntries()
##listCurrentEntries.sort()
##print "Number entries listCurrentEntries            : %s" % len(listCurrentEntries)
##
#### 3
##listDOCRfREDiteration2 = []
##for x in listCurrentEntries:
##    if x not in listDOCRfREDiteration1:
##        listDOCRfREDiteration2.append(x)
##listDOCRfREDiteration2.sort()
##print "Number entries listDOCRfREDiteration2        : %s" % len(listDOCRfREDiteration2)
##
#### 4
##listDOCRfREDDone = getBmrbNmrGridEntriesDOCRfREDDone()
##listDOCRfREDDone.sort()
##print "Number entries listDOCRfREDDone              : %s" % len(listDOCRfREDDone)
##
#### 5
##listDOCRfREDiteration2Todo = []
##for x in listDOCRfREDiteration2:
##    if x not in listDOCRfREDDone:
##        listDOCRfREDiteration2Todo.append(x)
##print "Number entries listDOCRfREDiteration2Todo    : %s" % len(listDOCRfREDiteration2Todo)
###Number entries listDOCRfREDiteration1        : 545
###Number entries listCurrentEntries            : 2948
###Number entries listDOCRfREDiteration2        : 2404
###Number entries listDOCRfREDDone              : 1437
###Number entries listDOCRfREDiteration2Todo    : 1510
##
#### 6
##listDOCRfREDiteration2TodoTemp = getEntryListFromCsvFile(localConstants.urlLists + "/list_todo.csv")
##print "Number entries listDOCRfREDiteration2TodoTemp: %s" % len(listDOCRfREDiteration2TodoTemp)
##
#### 7
##listDOCRfREDWeekly = getEntryListFromCsvFile(localConstants.urlLists + "/list_weekly.csv")
##print "Number entries listDOCRfREDWeekly            : %s" % len(listDOCRfREDWeekly)

##for x in listDOCRfREDWeekly:
##    print x
