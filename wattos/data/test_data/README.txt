This directory contains files that Wattos uses for testing itself using JUnit.
There is a test target in the build.xml file in the source directory for
doing this.

Filename              In or Out Wattos Test Class            Test (Only 1 will be listed) 
                              + Wattos."""X"""Test
                              + or .wcf in /macros
+-----------------------------+-+----------------------------+-----------------------                                
1ai0_small.cif                I ReadEntryMMCIF.wcf           TODO finish. 
1aj3_FRED_small.str.gz        I WriteEntryXplor.wcf          Contains real ORs.
1b4y_wattos.pdb.gz            I GetCoplanarBases.wcf         Calculates bonds and hydrogen bonds in a DNA triple helix.
1brv.pdb.gz                   I Soup.Atom                    Bond and distance calculation.
1brv_DOCR.noe                 O Star.NMRStar.File30          Reference data for Aqua based on 1brv_DOCR.str.gz
1brv_DOCR.str.gz              I Star.NMRStar.File30          Full version of 1brv_DOCR_small.str
1brv_DOCR_comp_str.comp       O Star.NMRStar.File30          Reference values from Aqua based on 1brv_DOCR.str.gz. Settings: no intra, <=4A, center averaging., no VAL 14 amides                                                        
1brv_DOCR_small.str.gz        I Star.NMRStar.File30          Surplus, completeness, violation, etc.
1brv_DOCR_small_assi_sum.str  I CheckAssignment.wcf          Stereo specific assignment check. 
1brv_DOCR_small_out.str       O Star.NMRStar.File30          Surplus, completeness, violation, etc.
1brv_DOCR_small_sumAssign.str O Star.NMRStar.File30          Stereo assignment swaps etc.
1brv_DOCR_small_sumCompl.str  O Star.NMRStar.File30          NOE distance restraint completeness
1brv_DOCR_small_sumCompl.txt  O Star.NMRStar.File30          NOE distance restraint completeness
1brv_DOCR_small_surplus.txt   O Star.NMRStar.File30          Distance restraint redundancy etc.
1brv_DOCR_small_viol0_1.txt   O Star.NMRStar.File30          Violation analysis
1brv_assigned.str             O CheckAssignment.wcf          Stereo specific assignment check.
1brv_dih.txt                  O Episode_II.MRSTARFile        Conversion to STAR from Discover
1brv_dist.txt                 O Episode_II.MRSTARFile        Conversion to STAR from Discover
1brv_small.pdb                I Soup.Atom                    Bond and distance calculation.
1brv_small_bond.txt           O Soup.Atom                    Bond and distance calculation.
1brv_small_dist.txt           O Soup.Atom                    Bond and distance calculation.
1cjg_noe_ambi_xplor.txt       I Episode_II.MRSTARFile        Conversion to STAR from Discover
1hue_DOCR.str.gz              I CheckCompleteness.wcf        Calculates the NOE distance restraint completeness.
1hue_DOCR_compl_sum.str       O CheckCompleteness.wcf        Calculates the NOE distance restraint completeness.
1hue_DOCR_compl_sum.txt       O CheckCompleteness.wcf        Calculates the NOE distance restraint completeness.
1hue_extra_small.pdb          O SetAtomNomenclatureToI.      Reset nomenclature and add atoms from WHAT IF file.
1hue_small.str                I SetAtomNomenclatureToI.      Reset nomenclature and add atoms from WHAT IF file.
1hue_wi_small.pdb             I SetAtomNomenclatureToI.      Reset nomenclature and add atoms from WHAT IF file.
1jj2_small_wattos.pdb.gz      I GetCoplanarBases.wcf         TODO check. Calculates bonds, hydrogen bonds, and coplanar bases of the large Ribosomal subunit (only 99996 are reported to be read).
1o5p_small.cif.gz	          I ReadEntryMMCIF.wcf           TODO finish.
1olg.cif.gz                   I ReadEntryMMCIF.wcf         
1olh_small.pdb.gz             I Soup.PdbFileReader           Reading PDB files
1olh_small_bond.txt           O Soup.Atom                    Bond and distance calculation.
1olh_small_dist.txt           O Soup.Atom                    Bond and distance calculation.
1wix_noe_small.txt            I Episode_II.MRSTARFile        Conversion to STAR from Discover
BDLB13.cif.gz		          I ReadEntryMMCIF.wcf           TODO finish. Example of mmCIF website but looks incomplete.
fileSetNMRSTAR.txt            I WriteSQLDump.wcf             TODO finish. Writes SQL commands and data for populating a MySQL database with the data in-memory with Wattos.
filter_rules.str	          I Star.STARFilter              Reformat and filter star files. Will be extended in future.
README.txt                    . .                            This file
restraint_map.str	          I .                            TODO finish. Just an idea for settings of mappings restraints to coordinates in star format.

                                                        