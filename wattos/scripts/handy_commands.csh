# Use the linux join command instead of operate
alias operate 'java Wattos.Utils.StringArrayList'

set file_result_1     = r1
set file_result_2     = r2
set file_result_3     = r3
set file_result_4     = r4

# 1131 entries without nucleic acids or glycos with NMR constraints submitted
set file_only_protein = entries_only_protein.csv
#  825 entries with NOEs in CNS format
set file_NOEs_CNS     = entries_NOEs_CNS.csv
#   11 entries that have a corresponding entry with same data 
#              and should thus be removed
set file_red          = entries_redundant.txt
# 1036 entries where the/a? chain has at least 20 residues according to
		pdb searchfields
set file_20plus       = entries_20plusresidues_formatted.txt
#  374 entries with het groups other than ace/nh2
set file_bad_het      = entries_with_HET_exceptTER.txt

operate $file_only_protein      intersection    $file_NOEs_CNS      $file_result_1
operate $file_result_1          difference      $file_red           $file_result_2
operate $file_result_2          intersection    $file_20plus        $file_result_3
operate $file_result_3          difference      $file_bad_het       $file_result_4

#--------------------------------------------

# Get the entry codes from a csv listing of files (3rd column):
set base_name = entries_with_CNS
cut -d ',' -f 3 $base_name.csv > $base_name.txt
# manuall correct the list for end effects.


java -Xmx256M  Wattos.Episode_II.MRInterloop < EOD
l
S:\jurgen\CloneWars\DB
y
.
EOD


