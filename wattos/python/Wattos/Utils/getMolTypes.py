#python -u $WATTOSROOT/python/Wattos/Utils/getMolTypes.py

from Wattos.Utils import PDBEntryLists
from Wattos.Utils.localConstants import starDir
from Wattos.Utils.localConstants import tmpDir
from cing.STAR.File import File
import os
import string

outputFile = '/Users/jd/moltypes.csv'

pdbList = PDBEntryLists.getBmrbNmrGridEntries()[0:200]
#pdbList = PDBEntryLists.getBmrbNmrGridEntries()
#pdbList=['1a03']
#pdbList=['1brv']
print "Read pdb entries from NMR Restraints Grid:", len( pdbList )
pdbList.sort()

molTypes = {}
seq_length = {}
for entry in pdbList:
    try:
        inputFN  = os.path.join(starDir,entry,entry+'_wattos.str')
        headFN = os.path.join(tmpDir,       entry+'_head.str')
        f = File()
        saveFrameRegExList = [r"^save_.*constraints", r"^save_conformer"]
        f.getHeader(saveFrameRegExList, inputFN, headFN)
        f.filename = headFN
        f.read()
        os.unlink( f.filename ) # removing temp file.
        molTypesPerEntry = {}
        molTypes[entry] = molTypesPerEntry
        seq_lengthPerEntry = {}
        seq_length[entry] = seq_lengthPerEntry
        sfList = f.getSaveFrames( category = 'entity')
        for node in sfList:
            tT = node.tagtables[0]
    #        print tT
            typeIdx = tT.tagnames.index('_Entity.Type')
    #        print typeIdx
            type = tT.tagvalues[typeIdx][0]
            poltype = ''
            if '_Entity.Polymer_type' in tT.tagnames:
                poltypeIdx = tT.tagnames.index('_Entity.Polymer_type')
        #        print poltypeIdx
                poltype = tT.tagvalues[poltypeIdx][0]

    #        print "type", type, ", and poltype", poltype
            key = type +'/' + poltype
            if molTypesPerEntry.has_key(key):
                molTypesPerEntry[key] += 1
            else:
                molTypesPerEntry[key] = 1

            lengthIdx = -1
            if '_Entity.Number_of_monomers' in tT.tagnames:
                lengthIdx = tT.tagnames.index('_Entity.Number_of_monomers')
            if lengthIdx>=0:
                length = string.atoi(tT.tagvalues[lengthIdx][0])
            else:
                length = 0

            if seq_lengthPerEntry.has_key(key):
                seq_lengthPerEntry[key] += length
            else:
                seq_lengthPerEntry[key] = length


        for key in molTypes[entry].keys():
            str = entry+","+key+','+`molTypes[entry][key]`+','+`seq_length[entry][key]`
            print str
    except KeyboardInterrupt:
        print "ERROR: Caught KeyboardInterrupt will exit(1)"
        os._exit(1)
    except Exception, info:
        print "Skipping entry: ", entry, info

print molTypes

if os.path.exists(outputFile ):
    os.unlink( outputFile )
output = open(outputFile,'w')
entryList = molTypes.keys()
entryList.sort()
for entry in entryList:
    molTypesEntryList = molTypes[entry].keys()
    molTypesEntryList.sort()
    for key in molTypesEntryList:
        str = entry+","+key+','+`molTypes[entry][key]`+','+`seq_length[entry][key]`+'\n'
        output.write(str)


