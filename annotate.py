#!/usr/bin/python

import sys,os,commands

# Must import storage before utils
import update_settings as settings
from storage import storage
storage.uri = settings.MONGODB_URI
storage.db_name = settings.MONGODB_DB_NAME
storage.connect()
storage.authenticate ('whynotadmin', 'waivuy8N')

from utils import entries_by_pdbid, get_unannotated_entries, get_missing_entries, read_http

from time import time
from sets import Set

# whynot comment files look like this:
# COMMENT: <text 1>
# <databank name>, <pdbid 1>
# <databank name>, <pdbid 2>
# <databank name>, <pdbid 3>
# COMMENT: <text 2>
# <databank name>, <pdbid 4>
# <databank name>, <pdbid 5>
# etc.

# Returns a list of triples: (comment, databank name, pdbid)
def parse_comments (lines):

    if len(lines) < 2:
        return {}

    d = []
    comment = None
    for line in lines:

        if line.startswith('COMMENT:'):
            comment = line[8:].strip()

        elif ',' in line:

            databank_name, pdbid = line.strip ().replace (' ','').split (',')
            d.append ((comment, databank_name, pdbid))

        elif len (line.strip ()) > 0:

            raise Exception ("not the right format: \"%s\"" % line )

    return d


# Searches for a specific entry in the comments.
# Returns the comment text for this entry.
def parse_comment(lines, entry):

    if len(lines) < 2:
        return ''

    if not lines[0].startswith('COMMENT:'):
        print lines[0], 'does not have comment'
        return ''

    comment = lines[0][8:].strip()

    for line in lines[1:]:

        line = line.replace (' ','').strip ()
        if line == '%s,%s' % (entry ['databank_name'], entry ['pdbid']):
            return comment

    return ''

def update_entry (entry):

    databank_name = entry ['databank_name']
    pdbid = entry ['pdbid']

    if storage.find_one ('entries', {'databank_name': databank_name, 'pdbid': pdbid}):

        storage.update ('entries', {'databank_name': databank_name, 'pdbid': pdbid}, entry)
    else:
        storage.insert ('entries', entry)

# This function gets all comment information from a whynot
# file and updates the corresponding entries with it.
def annotate_from_file (path):

    comments = parse_comments (open (path,'r').readlines ())

    for text, databank_name, pdbid in comments:

        entry = {'databank_name': databank_name, 'pdbid': pdbid,
                 'comment': text, 'mtime': time()}

        update_entry (entry)


# On the commandline, the user can give the filename of
# one or more whynot files as argument. This will make the
# script annotate only information from the files and skip
# all other missing entries.
if len (sys.argv) > 1:

    # just parse the given whynot files

    for path in sys.argv [1:]:

        print 'annotate', path
        annotate_from_file (path)

    sys.exit (0)

# else just check all other sources of information...



# Check the files in the whynot comments directory:

whynotdir = os.path.dirname (sys.argv [0])
commentsdir = os.path.join (whynotdir, 'comment')

if os.path.isdir (commentsdir):
    for filename in os.listdir (commentsdir):

        if filename.endswith ('.txt'):

            filepath = os.path.join (commentsdir, filename)

            annotate_from_file (filepath)

            os.rename (filepath, filepath + ".done")

# List the pdbids for pdb entries by category. For many missing entries,
# the category is the reason why they are missing. We base the comment on that.
#
# A pdb entry can have experimental methods: nmr, em, diffraction or other.
# Only nmr entries can have nmr-related data, only diffraction entries can have
# structure_factors data.
#
# A pdb entry can contain only carbohydrates or only nucleic acids, in
# which case no DSSP can be made.

pdbidscarbonly = Set ()
pdbidsnuconly = Set ()
pdbidsnmr = Set ()
pdbidsem = Set ()
pdbidsother = Set ()
pdbidsdiff = Set ()

# Parse wwpdb entry type record
for line in read_http('ftp://ftp.wwpdb.org/pub/pdb/derived_data/pdb_entry_type.txt').split('\n'):
    if len(line.strip()) <= 0:
        continue

    pdbid, content, method = line.split()

    if content=='nuc':
        pdbidsnuconly.add(pdbid)
    elif content=='carb':
        pdbidscarbonly.add(pdbid)

    if method=='diffraction':
        pdbidsdiff.add(pdbid)
    elif method=='NMR':
        pdbidsnmr.add(pdbid)
    elif method=='EM':
        pdbidsem.add(pdbid)
    elif method=='other':
        pdbidsother.add(pdbid)

# Generate comments for missing structure factors.
# Do this wherever the experimental method is not diffraction:
for entry in get_unannotated_entries('STRUCTUREFACTORS'):

    pdbid = entry['pdbid']
    if pdbid in pdbidsnmr:

        entry['comment'] = 'NMR experiment'
        entry['mtime'] = time()

    elif pdbid in pdbidsem:

        entry['comment'] = 'Electron microscopy experiment'
        entry['mtime'] = time()

    elif pdbid in pdbidsother:

        entry['comment'] = 'Not a Diffraction experiment'
        entry['mtime'] = time()

    if 'comment' in entry:
        update_entry (entry)


# Generate comments for missing nmr data.
# Do this wherever the experimental method is not nmr:
for entry in get_unannotated_entries('NMR'):

    pdbid = entry['pdbid']
    if pdbid in pdbidsdiff:

        entry['comment'] = 'Diffraction experiment'
        entry['mtime'] = time()

    elif pdbid in pdbidsem:

        entry['comment'] = 'Electron microscopy experiment'
        entry['mtime'] = time()

    elif pdbid in pdbidsother:

        entry['comment'] = 'Not an NMR experiment'
        entry['mtime'] = time()

    if 'comment' in entry:
        update_entry (entry)

# To find out why HSSP entries are missing, one must check the error output of
# mkhssp when it ran. It's been stored in a reserved directory:
for entry in get_unannotated_entries('HSSP'):

    pdbid = entry['pdbid']

    inputfile = '/srv/data/pdb/all/pdb%s.ent.gz' % pdbid
    if not os.path.isfile(inputfile):
        inputfile = '/srv/data/mmCIF/%s.cif.gz' % pdbid

    # Get hssp error from log file.
    errfile = '/srv/data/scratch/whynot2/hssp/%s.err' % pdbid
    if os.path.isfile (errfile):
        with open(errfile, 'r') as f:
            line = f.read()

            # We filter for a set of commonly ocurring errors:
            line = line.strip()
            if line in ['Not enough sequences in PDB file of length 25', 'multiple occurrences', 'No hits found', 'empty protein, or no valid complete residues']:
                entry ['comment'] = line
                entry ['mtime'] = time()
                update_entry (entry)

# DSSP files can be missing for multiple reasons:
# 1 the structure has no protein, carbohydrates/nucleic acids only
# 2 the structure hase no backbone, only alpha carbon atoms
#
# 1 can be found, using the predefined sets pdbidsnuconly and pdbidscarbonly.
# 2 can be found by reading the mkdssp standard error output.
for dbname in ['DSSP', 'DSSP_REDO']:
    for entry in get_missing_entries (dbname):

        pdbid = entry['pdbid']

        if pdbid in pdbidsnuconly:

            entry['comment'] = 'Nucleic acids only'
            entry['mtime'] = time()

        elif pdbid in pdbidscarbonly:

            entry['comment'] = 'Carbohydrates only'
            entry['mtime'] = time()

        else:

            # DSSP uses pdb files as input, DSSP_REDO uses pdb_redo files:
            if dbname == 'DSSP':
                inputfile = '/srv/data/pdb/all/pdb%s.ent.gz' % pdbid
                if not os.path.isfile(inputfile):
                    inputfile = '/srv/data/mmCIF/%s.cif.gz' % pdbid
            else:
                inputfile = '/srv/data/pdb_redo/%s/%s/%s_final.pdb' % (pdbid[1:3], pdbid, pdbid)
                if not os.path.isfile(inputfile):
                    continue

            # Read mkdssp stderr:
            errfile = '/srv/data/scratch/whynot2/%s/%s.err' % (dbname.lower(),
                                                               pdbid)
            if os.path.isfile(errfile):
                with open(errfile, 'r') as f:
                    lines = f.readlines()
                    if lines [-1].strip () == \
                            'empty protein, or no valid complete residues':

                        # for backwards compatibility:
                        entry['comment'] = 'No residues with complete backbone'
                        entry['mtime'] = time()

        if 'comment' in entry:
            update_entry (entry)

# BDB comments are simply stored in a file, generated by the bdb script.
for entry in get_missing_entries('BDB'):

    pdbid = entry['pdbid']
    part = pdbid[1:3]
    whynotfile = '/srv/data/bdb/%s/%s/%s.whynot' % (part, pdbid, pdbid)
    if not os.path.isfile(whynotfile):
        continue

    lines = open(whynotfile, 'r').readlines()
    comment = parse_comment(lines, entry)
    if len(comment) > 0:
        entry ['comment'] = comment
        entry ['mtime'] = time()
        update_entry (entry)

# WHATIF list comments are simply stored in a file, generated by the script.
for lis in ['acc', 'cal', 'cc1', 'cc2', 'cc3', 'chi', 'dsp', 'iod', 'sbh', 'sbr', 'ss1', 'ss2', 'tau', 'wat']:
    for src in ['pdb', 'redo']:
        dbname = 'WHATIF_%s_%s' % (src.upper(), lis)

        for entry in get_missing_entries (dbname):

            pdbid = entry['pdbid']
            whynotfile = '/srv/data/wi-lists/%s/%s/%s/%s.%s.whynot' % (src, lis, pdbid, pdbid, lis)
            if not os.path.isfile(whynotfile):
                continue

            lines = open(whynotfile, 'r').readlines()
            comment = parse_comment(lines, entry)
            if len(comment) > 0:
                entry['comment'] = comment
                entry['mtime'] = time()
                update_entry (entry)

# WHATIF scene comments are simply stored in a file, generated by the script.
for lis in ['iod', 'ss2']:
    for src in ['pdb', 'redo']:
        dbname = '%s_SCENES_%s' % (src.upper(), lis)

        for entry in get_missing_entries(dbname):

            pdbid = entry['pdbid']
            whynotfile = '/srv/data/wi-lists/%s/scenes/%s/%s/%s.%s.whynot' % (src, lis, pdbid, pdbid, lis)
            if not os.path.isfile(whynotfile):
                continue

            lines = open(whynotfile, 'r').readlines()
            comment = parse_comment(lines, entry)
            if len(comment) > 0:
                entry['comment'] = comment
                entry['mtime'] = time()
                update_entry (entry)
