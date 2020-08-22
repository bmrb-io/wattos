#!/usr/bin/env python
#
#
import glob
import re
import os
import sys
import stat
import subprocess
import time
import signal
import errno
import shutil
import MySQLdb
from optparse import OptionParser

PDB = "/dumpzone/pdb/pdb/data/structures/all/pdb"
OUTDIR = "/raid/docr/mirroring/tmp" # NOTE: must be world-writable (thank mysql)
BFILES = "/raid/bfiles"
WATTOS = "wattos1"

RSYNC = [ "rsync", "-vaRz", "--delete" ]
MIRRORS = [ "rsync://swordfish.bmrb.wisc.edu:48888/", "rsync://moray.bmrb.wisc.edu:48888/" ]
WATTOS_MODULE = "wattos"
MOLGRAP = { "/raid/docr/molgrap/molgrap/molgrap" : "molgrap", \
            "/raid/docr/molgrap/molgrap/molgrapWhite" : "molgrap_white" }

def main() :
    global PDB
    global OUTDIR
    global BFILES
    global WATTOS
    global RSYNC
    global MIRRORS
    global MOLGRAP
    global WATTOS_MODULE

    options = []
    usage = "usage: %prog [options]"
    op = OptionParser( usage = usage )
    op.add_option( "-v", "--verbose", action = "store_true", dest = "verbose",
                 default = False, help = "print messages to stdout" )
    op.add_option( "--no-copy", action = "store_false", dest = "copy",
                 default = True, help = "don't copy bfiles" )
    op.add_option( "--no-dump", action = "store_false", dest = "dump",
                 default = True, help = "don't dump database tables (implies no-copy)" )
    op.add_option( "--no-rsync", action = "store_false", dest = "rsync",
                 default = True, help = "don't rsync to public servers" )
    (options, args) = op.parse_args()

    if not os.path.exists( OUTDIR ) :
        os.mkdir( OUTDIR )
# must be world-writable for mysql
    os.chmod( OUTDIR, stat.S_IRWXU | stat.S_IRWXG | stat.S_IRWXO )
    
    if options.dump :
        pdbids = []
        files = glob.glob( os.path.join( PDB, "*.gz" ) )
        pat = re.compile( "pdb([0-9a-z]{4}).ent.gz" )
        if options.verbose : print "Released PDB IDs:"
        for i in files :
            m = pat.search( i )
            if m :
                pdbids.append( m.group( 1 ) )
                if options.verbose : print m.group( 1 )
    
        db = MySQLdb.connect( user = WATTOS, passwd = "4I4KMS", db = WATTOS )
        c = db.cursor()

        pdbidstr = "'" + "','".join( pdbids ) + "'"
        del pdbids[:]

        entrycsv = "entry.csv"
        outfile = os.path.join( OUTDIR, entrycsv )
        sql = "select entry_id,bmrb_id,pdb_id into outfile '" \
            + outfile + "' fields terminated by ',' " \
            + "optionally enclosed by '\"' lines terminated by '\\n' " \
            + "from entry where pdb_id in (" + pdbidstr + ")"
        if os.path.exists( outfile ) : os.unlink( outfile )
        if options.verbose : print sql
        c.execute( sql )
        sql = "select entry_id from entry where pdb_id in (" + pdbidstr + ")"
        if options.verbose : print sql
        c.execute( sql )
        entryids = []
        while 1 :
            row = c.fetchone()
            if row == None :
                break
            entryids.append( row[0] )
    
        entryidstr = ",".join( str( i ) for i in entryids )
        del entryids[:]

        mrfilecsv = "mrfile.csv"
        outfile = os.path.join( OUTDIR, mrfilecsv )
        sql = "select mrfile_id,entry_id,detail,pdb_id,date_modified into outfile '" \
            + outfile + "' fields terminated by ',' optionally enclosed by '\"' " \
            + "lines terminated by '\\n' from mrfile where entry_id in (" \
            + entryidstr + ")"
        if os.path.exists( outfile ) : os.unlink( outfile )
        if options.verbose : print sql
        c.execute( sql )
        sql = "select mrfile_id from mrfile where entry_id in (" + entryidstr + ")"
        if options.verbose : print sql
        c.execute( sql )
        fileids = []
        while 1 :
            row = c.fetchone()
            if row == None :
                break
            fileids.append( row[0] )
    
        fileidstr = ",".join( str( i ) for i in fileids )
        del fileids[:]
    
        mrblockcsv = "mrblock.csv"
        outfile = os.path.join( OUTDIR, mrblockcsv )
        sql = "select mrblock_id,mrfile_id,position,program,type,subtype,format,text_type,"\
            + "byte_count,item_count,date_modified,other_prop,dbfs_id,file_name,md5_sum into outfile '" \
            + outfile + "' fields terminated by ',' optionally enclosed by '\"' " \
            + "lines terminated by '\\n' from mrblock where mrfile_id in (" \
            + fileidstr + ")"
        if os.path.exists( outfile ) : os.unlink( outfile )
        if options.verbose : print sql
        c.execute( sql )
        sql = "select dbfs_id from mrblock where mrfile_id in (" + fileidstr + ")"
        if options.verbose : print sql
        c.execute( sql )
        dbfsids = []
        while 1 :
            row = c.fetchone()
            if row == None :
                break
            dbfsids.append( row[0] )
    
        c.close()
        db.close()

        if options.copy : copy_bfiles( dbfsids )
        del dbfsids[:]

    if options.rsync :
# rsync wattos
        end = time.time() + 3600 # 1 hour
        os.chdir( OUTDIR )
        errors = False
        for i in MIRRORS :
            cmd = []
            cmd.extend( RSYNC )
            cmd.append( "." )
            cmd.append( i + WATTOS_MODULE )
            if options.verbose : print cmd
            p = subprocess.Popen( cmd, stdout = subprocess.PIPE, stderr = subprocess.STDOUT )
            pid = p.pid
            while 1 :
                time.sleep( 10 )
                p.poll()
                out = p.stdout.readlines()
                if options.verbose : 
                    for line in out :
                        print line.rstrip()
                del out[:]
                if p.returncode != None : # rsync finished
                    break
                if time.time() > end :
                    os.kill( pid, signal.SIGKILL )
                    print cmd, " killed: timeout"
                    break

            if p.returncode != 0 :
                print "rsync", i, "returned", p.returncode
                errors = True

        os.chdir( ".." )
        if not errors : shutil.rmtree( OUTDIR )

# rsync molgrap
        end = time.time() + 3600 # 1 hour
        for key in MOLGRAP.keys() :
            os.chdir( key )
            for i in MIRRORS :
                cmd = []
                cmd.extend( RSYNC )
                cmd.append( "." )
                cmd.append( i + MOLGRAP[key] )
                if options.verbose : print cmd
                p = subprocess.Popen( cmd, stdout = subprocess.PIPE, stderr = subprocess.STDOUT )
                pid = p.pid
                while 1 :
                    time.sleep( 10 )
                    p.poll()
                    out = p.stdout.readlines()
                    if options.verbose : 
                        for line in out :
                            print line.rstrip()
                    del out[:]
                    if p.returncode != None : # rsync finished
                        break
                    if time.time() > end :
                        os.kill( pid, signal.SIGKILL )
                        print cmd, " killed: timeout"
                        break

                if p.returncode != 0 :
                    print "rsync", i, "returned", p.returncode

#
#
#
# 1.txt                      ->                   001.dat  -> /001.dat
# 12345.xml.gz               ->                012345.dat  -> /012/012345.dat
# 123456789.xml.gz           ->             123456789.dat  -> /123/456/123456789.dat
def copy_bfiles( idlist ) :
    global WATTOS
    global BFILES
    global OUTDIR

    bfiles = []
    for i in idlist :
        if len( str( i ) ) < 4 :
            bfiles.append( os.path.join( WATTOS, "%03d.dat" % i ) )
            continue
        if len( str( i ) ) < 7 :
            s = "%06d.dat" % i
            bfiles.append( os.path.join( WATTOS, s[0:3], s ) )
            continue
        s = "%09d.dat" % i
        bfiles.append( os.path.join( WATTOS, s[0:3], s[3:6], s ) )

    for i in bfiles :
        src = os.path.join( BFILES, i )
#FIXME
        dst = os.path.join( OUTDIR, "bfiles", i )
        dstdir = os.path.split( dst )[0]
        try:
            os.makedirs( dstdir )
        except OSError, exc :
            if exc.errno == errno.EEXIST : pass
            else : raise
        shutil.copyfile( src, dst )

    del bfiles[:]

#
#
#
if __name__ == "__main__" :
    main()

# eof
