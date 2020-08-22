The files in this directory can be used as input to the Watos UserInterface
program. The extension .wcf waswas not in use acording to www.filext.com at the
time of creation. The most simple example is QuitBeforeBegun.wcf which does
nothing but start and stop right away. Execute like (on Windows or Linux):

wattos < QuitBeforeBegun.wcf

The input files are included in WATTOSROOT/data/test_data so change to that
directory before executing these examples. Output will be written to the
subdirectory tmp_dir which is included as an empty directory in the
distribution.

All files in directory              Use
------------------------------------+----------------------------------------------------
CalcDistance.wcf                    Look at close contacts excluding regular bonds. 
CalcDistConstrViolation.wcf         Calculates violations on distance constraints. 
CheckAssignment.wcf                 Stereo assignment swaps etc.
CheckCompleteness.wcf               Calculates the NOE distance restraint completeness.
CheckSurplus.wcf                    Checks the redundant and unuseful distances.
FilterSTAR.wcf                      Read N write with filtering in the future intended.
GetCoplanarBases.wcf                TODO check. Calculates bonds, hydrogen bonds, and coplanar bases of the large Ribosomal subunit (only 99996 are reported to be read).
GetHydrogenBonds.wcf                Calculates bonds and hydrogen bonds in a DNA triple helix.
QuitBeforeBegun.wcf                 Stop right away.
README.txt                          This file.
SetAtomNomenclatureToIUPAC.wcf      Reset nomenclature and add atoms from WHAT IF file.
ShowClassification.wcf              Lists the number of restraints in classes such as intra-residual, sequential, etc.
Sleep.wcf                           Just sleep. 
WriteEntryXplor.wcf                 TODO finish. 
WriteSQLDump.wcf                    TODO finish. Writes SQL commands and data for populating a MySQL database with the data in-memory with Wattos. 



