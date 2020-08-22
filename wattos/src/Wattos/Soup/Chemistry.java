/*
 * Chemistry.java
 *
 * Created on November 11, 2002, 2:53 PM
 */

package Wattos.Soup;

import Wattos.Utils.Strings;
import Wattos.Database.Defs;
import Wattos.Utils.*;
import java.util.*;

/**
 *Definition on the atom elements.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Chemistry {

    /** What dimensionality of a world do we live in? */
    //public static final byte   SIZE_3D                         = 3;

    /** Thanks to Computer Chemistry Consultancy, Feldstr.20, 8488 Turbenthal, Switzerland, +41 52 3851745 or +41 76 3088403, info@CompChemCons.com
     *for typing this in. All the symbols are 2 characters long with a space padded at the back.
     */
    public static final String[] ELEMENT_SYMBOLS  = {"Qq",
       "H ","He","Li","Be","B ","C ","N ","O ","F ","Ne",
       "Na","Mg","Al","Si","P ","S ","Cl","Ar",
       "K ","Ca","Sc","Ti","V ","Cr","Mn","Fe","Co","Ni","Cu","Zn","Ga","Ge","As","Se","Br","Kr",
       "Rb","Sr","Y ","Zr","Nb","Mo","Tc","Ru","Rh","Pd","Ag","Cd","In","Sn","Sb","Te","I ","Xe",
       "Cs","Ba","La","Ce","Pr","Nd","Pm","Sm","Eu","Gd","Tb","Dy","Ho","Er","Tm","Yb","Lu",
       "Hf","Ta","W ","Re","Os","Ir","Pt","Au","Hg","Tl","Pb","Bi","Po","At","Rn",
       "Fr","Ra","Ac","Th","Pa","U ","Np","Pu","Am","Cm","Bk","Cf","Es","Fm","Md","No","Lw"
    };

    public static final String[] ELEMENT_NAMES  = {"Pseudo",
        "Hydrogen","Helium","Lithium","Berillium","Boron",
        "Carbon","Nitrogen","Oxygen","Fluorine","Neon",
        "Sodium","Magnesium","Aluminium","Silicon","Phosphorus","Sulfur","Chlorine","Argon",
        "Potassium","Calcium","Scandium","Titanium","Vanadium","Chromium","Manganese","Iron",
        "Cobalt","Nickel","Copper","Zinc","Gallium","Germanium","Arsenic","Selenium","Bromine","Krypton",
        "Rubidium","Strontium","Yttrium","Zirconium","Niobium","Molybdenum","Technetium","Ruthenium",
        "Rhodium","Palladium","Silver","Cadmium","Indium","Tin","Antimony","Tellurium","Iodine","Xenon",
        "Cesium","Barium","Lanthanum","Cerium","Praseodymium","Neodymium","Promethium","Samarium","Europium",
        "Gadolinum","Terbium","Dysprosium","Holmium","Erbium","Thulium","Ytterbium","Lutetium",
        "Hafnium","Tantalum","Wolfram","Rhenium","Osmium","Iridium","Platinum",
        "Gold","Mercury","Thallium","Pead","Bismuth","Polonium","Astatine","Radon",
        "Francium","Radium","Actinium","Thorium","Profactinium","Uranium","Neptunium","Plutonium","Americium",
        "Curium","Berkelium","Californium","Einsteinium","Fermium","Mendelevium","Nobelium","Lawrencium"
    };
    /** From http://www.chem.qmul.ac.uk/iupac/AtWt/ see their site for the footnotes. */
    public static final float[] ELEMENT_MASSES = {
        0.0f,
        1.00794f,
        4.002602f,
        6.941f,
        9.012182f,
        10.811f,
        12.0107f,
        14.0067f,
        15.9994f,
        18.9984032f,
        20.1797f,
        22.98976928f,
        24.3050f,
        26.9815386f,
        28.0855f,
        30.973762f,
        32.065f,
        35.453f,
        39.948f,
        39.0983f,
        40.078f,
        44.955912f,
        47.867f,
        50.9415f,
        51.9961f,
        54.938045f,
        55.845f,
        58.933195f,
        58.6934f,
        63.546f,
        65.409f,
        69.723f,
        72.64f,
        74.92160f,
        78.96f,
        79.904f,
        83.798f,
        85.4678f,
        87.62f,
        88.90585f,
        91.224f,
        92.90638f,
        95.94f,
        98f,
        101.07f,
        102.90550f,
        106.42f,
        107.8682f,
        112.411f,
        114.818f,
        118.710f,
        121.760f,
        127.60f,
        126.90447f,
        131.293f,
        132.9054519f,
        137.327f,
        138.90547f,
        140.116f,
        140.90765f,
        144.242f,
        145f,
        150.36f,
        151.964f,
        157.25f,
        158.92535f,
        162.500f,
        164.93032f,
        167.259f,
        168.93421f,
        173.04f,
        174.967f,
        178.49f,
        180.94788f,
        183.84f,
        186.207f,
        190.23f,
        192.217f,
        195.084f,
        196.966569f,
        200.59f,
        204.3833f,
        207.2f,
        208.98040f,
        209f,
        210f,
        222f,
        223f,
        226f,
        227f,
        232.03806f,
        231.03588f,
        238.02891f,
        237f,
        244f,
        243f,
        247f,
        247f,
        251f,
        252f,
        257f,
        258f,
        259f,
        262f,
        267f,
        268f,
        271f,
        272f,
        270f,
        276f,
        281f,
        280f,
        285f,
        284f,
        289f,
        288f,
        293f,
        294f
    };
    /** Left justified 2 character string set in all upper case characters. */
    public static final String[]        ELEMENT_SYMBOLS_UPPER_CASE;
    public static final String[]        ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED;
    public static final String[]        ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED; // Convention in pdb.
    /** A map for fast look up of the element id given the 2 letter right justified uppercase string.*/
    public static final StringIntMap    ELEMENT_SYMBOLS_UPPER_CASE_MAP;
    public static final StringIntMap    ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED_MAP;
    public static final StringIntMap    ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED_MAP;

    public static final String ELEMENT_SYMBOL_UPPER_CASE_RIGHT_JUSTIFIED_UNKNOWN_1 = " X";
    public static final String ELEMENT_SYMBOL_UPPER_CASE_RIGHT_JUSTIFIED_UNKNOWN_2 = " U"; // whatif doing

    /** Typed these myself. A little specific for NMR when talking about the isotope number.*/
    public static final int ELEMENT_ID_UNKNOWN  = Defs.NULL_INT;
    public static final int ELEMENT_ID_PSEUDO   = 0;
    public static final int ELEMENT_ID_HYDROGEN = 1;
    public static final int ELEMENT_ID_CARBON   = 6;
    public static final int ELEMENT_ID_NITROGEN = 7;
    public static final int ELEMENT_ID_OXYGEN   = 8;
    public static final int ELEMENT_ID_FLUOR    = 9;
    public static final int ELEMENT_ID_PHOSPOR  = 15;
    public static final int ELEMENT_ID_SULFUR   = 16;
    public static final int ELEMENT_ID_CALCIUM  = 20;
    public static final int ELEMENT_ID_ZINC     = 30;

    public static final int[] MAX_BONDS;
    /** More available at:
     *http://www.cmbi.kun.nl/cheminf/csd/cqdoc/ConQuest/PortableHTML/CQdocn16322.html#250776
<PRE>
They are taken from Bondi, J.Phys.Chem., 68, 441, 1964.
Ag 1.72
Ar 1.88
As 1.85
Au 1.66
Br 1.85
C 1.70
Cd 1.58
Cl 1.75
Cu 1.40
F 1.47
Ga 1.87
H 1.20    This seems to be high to me but is what all list. The HH bond is only 0.74 Ang long!
He 1.40
Hg 1.55
I 1.98
In 1,93
K 2.75
Kr 2.02
Li 1.82
Mg 1.73
N 1.55
Na 2.27
Ne 1.54
Ni 1.63
O 1.52
P 1.80
Pb 2.02
Pd 1.63
Pt 1.72
S 1.80
Se 1.90
Si 2.10
Sn 2.17
Te 2.06
Tl 1.96
U 1.86
Xe 2.16
Zn 1.39
</PRE>
     */
    /**
     * Taken from What If // Too generic for most purposes of course.
 C      1.80
 N      1.70
 O      1.40
 S      2.00
 P      2.00
 I      2.00
 Z      1.80
 A      1.80
 M      1.70
 H+D    1.00
 UNK    1.80
 HOH    1.40
 B      1.40
     */
    /** Taken from molmol setup/AtomRadius
     */
    public static float  ELEMENT_RADIUS_HYDROGEN = 0.4f;
    public static float  ELEMENT_RADIUS_CARBON   = 0.85f;
    public static float  ELEMENT_RADIUS_NITROGEN = 0.8f;
    public static float  ELEMENT_RADIUS_OXYGEN   = 0.7f;
    public static float  ELEMENT_RADIUS_SULFUR   = 1.3f;
    public static float  ELEMENT_RADIUS_PHOSPOR  = 1.2f;
    public static float  ELEMENT_RADIUS_FLUOR    = 0.8f;
    public static float  ELEMENT_RADIUS_ZINC     = 1.3f; // any metal?
    public static float  ELEMENT_RADIUS_CALCIUM  = ELEMENT_RADIUS_ZINC;

    public static final int elementCount = ELEMENT_SYMBOLS.length;
    public static final float[] radii;
    public static float sumRadiiProtonProton = ELEMENT_RADIUS_HYDROGEN + ELEMENT_RADIUS_HYDROGEN;
    /** CRC Handbook of physics and chemistry, 1st student edition */
    public static float smallestBondEver = 0.74611f;    // in H2 and are there smaller ones?

    private static StringArrayList ION_RESIDUE_NAME_LIST = new StringArrayList();
    public static BitSet METAL_ION_ELEMENT_ID_Set = new BitSet();

    // In case we want to be redundant -> see redundant
    public static final int ISOTOPE_ID_HYDROGEN    = 1;
    public static final int ISOTOPE_ID_DEUTERIUM   = 2;
    public static final int ISOTOPE_ID_TRITIUM     = 3;
    public static final int ISOTOPE_ID_CARBON_13   = 13;
    public static final int ISOTOPE_ID_NITROGEN_15 = 15;

    static {
        int[] sequentialArray = PrimitiveArray.createSequentialArray( ELEMENT_SYMBOLS.length, 1);
        ELEMENT_SYMBOLS_UPPER_CASE                      = Strings.toUpperCase(ELEMENT_SYMBOLS);
        ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED              = Strings.trim(ELEMENT_SYMBOLS_UPPER_CASE);
        ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED      = Strings.toRightAlign(ELEMENT_SYMBOLS_UPPER_CASE);
        ELEMENT_SYMBOLS_UPPER_CASE_MAP                  = new StringIntMap( ELEMENT_SYMBOLS_UPPER_CASE,                 sequentialArray );
        ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED_MAP          = new StringIntMap( ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED,         sequentialArray );
        ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED_MAP  = new StringIntMap( ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED, sequentialArray );

        //General.showDebug( "out is: " + Strings.toString( ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED ));
        radii = new float[ elementCount ];
        Arrays.fill(radii, 0, elementCount, Defs.NULL_FLOAT);
        // Fill with default values.
        radii[ ELEMENT_ID_HYDROGEN ]    = ELEMENT_RADIUS_HYDROGEN;
        radii[ ELEMENT_ID_CARBON ]      = ELEMENT_RADIUS_CARBON;
        radii[ ELEMENT_ID_NITROGEN ]    = ELEMENT_RADIUS_NITROGEN;
        radii[ ELEMENT_ID_OXYGEN ]      = ELEMENT_RADIUS_OXYGEN;
        radii[ ELEMENT_ID_PHOSPOR ]     = ELEMENT_RADIUS_PHOSPOR;
        radii[ ELEMENT_ID_FLUOR  ]      = ELEMENT_RADIUS_FLUOR;
        radii[ ELEMENT_ID_SULFUR ]      = ELEMENT_RADIUS_SULFUR;
        radii[ ELEMENT_ID_CALCIUM ]     = ELEMENT_RADIUS_CALCIUM;
        radii[ ELEMENT_ID_ZINC   ]      = ELEMENT_RADIUS_ZINC;
        MAX_BONDS = new int[ elementCount ];
        Arrays.fill(MAX_BONDS, 0, elementCount, 4); // vague rule.
        MAX_BONDS[ ELEMENT_ID_HYDROGEN ] = 1;
        MAX_BONDS[ ELEMENT_ID_OXYGEN ]   = 2;
        MAX_BONDS[ ELEMENT_ID_FLUOR ]    = 1;
        MAX_BONDS[ ELEMENT_ID_SULFUR ]   = 2;
        MAX_BONDS[ ELEMENT_ID_HYDROGEN ] = 1;
        // Extend as capabilities in xplor-nih extend.
        ION_RESIDUE_NAME_LIST.add("ZN");

        METAL_ION_ELEMENT_ID_Set.set(11,14); // Na, Mg, Al
        METAL_ION_ELEMENT_ID_Set.set(19,33); // K, thru Ge
        METAL_ION_ELEMENT_ID_Set.set(37,52); // Rb, thru Sb
        METAL_ION_ELEMENT_ID_Set.set(55,85); // Cs, thru Po
        METAL_ION_ELEMENT_ID_Set.set(87,107);// Fr, thru Unh
    }

    /** Creates new Chemistry */
    public Chemistry() {
    }

    /** Returns Defs.NULL_FLOAT unless both radii are known. */
    public static float getSumRadii( int atom_element_id_1, int atom_element_id_2 ) {
        if ( atom_element_id_1 == ELEMENT_ID_HYDROGEN && atom_element_id_2 == ELEMENT_ID_HYDROGEN ) {
            return sumRadiiProtonProton;
        }
        // Watch out for undefined arithmics.
        float sum = radii[ atom_element_id_1 ];
        if ( Defs.isNull( sum ) ) {
            return Defs.NULL_FLOAT;
        }
        if ( Defs.isNull( radii[ atom_element_id_2 ] ) ) {
            return Defs.NULL_FLOAT;
        }
        sum += radii[ atom_element_id_2 ];
        return sum;
    }

    /** Optimized to run on string array in which elements are non-redundant and
     *the == operator can be used to check equality.
     */
    public static boolean translateElementNameToIdInArrays( String[] in, int[] out,
        boolean useRightJustified,
        int startPosition, int endPosition ) {

        String  elementSymbol   = in[startPosition].toUpperCase(); // cache strings and ints for efficiency.
        String  elementSymbolNext = null;
        int     elementId       = Defs.NULL_INT;
        if ( useRightJustified ) {
            elementId       = ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED_MAP.getInt(    elementSymbol );
        } else {
            elementId       = ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED_MAP.getInt(            elementSymbol );
        }
        out[startPosition] = elementId;


        if ( useRightJustified ) {
            for (int i=startPosition+1; i<endPosition; i++) {
                elementSymbolNext = in[i].toUpperCase();
                if ( elementSymbolNext != elementSymbol ) {
                    elementSymbol = elementSymbolNext;
                    elementId = ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED_MAP.getInt( elementSymbol );
                }
                out[i] = elementId;
            }
        } else { // unroll loop for efficiency.
            for (int i=startPosition+1; i<endPosition; i++) {
                elementSymbolNext = in[i].toUpperCase();
                if ( elementSymbolNext != elementSymbol ) {
                    elementSymbol = elementSymbolNext;
                    elementId = ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED_MAP.getInt( elementSymbol );
                }
                out[i] = elementId;
            }
        }
        return true;
    }

    /** start is inclusive and end is exclusive.
     */
    public static boolean translateElementIdToNameInArrays( int[] in, String[] out,
        int startPosition, int endPosition ) {

        int     elementId;
//        String  elementSymbol;
        int     previousElementId = Defs.NULL_INT;
        String  previousElementName = Defs.NULL_STRING_NULL;

        /** Very fast routine in case they're all nill.*/
        for (int i=startPosition; i<endPosition; i++) {
            elementId = in[i];
            if ( elementId == previousElementId ) {
                out[i] = previousElementName;
            } else  {
                if ( Defs.isNull( elementId ) || (elementId < 0) ) {
                    out[i] = Defs.NULL_STRING_NULL;
                } else {
                    out[i] = ELEMENT_SYMBOLS_UPPER_CASE_TRIMMED[ elementId ];
                }
                previousElementName = out[i];
                previousElementId   = elementId;
            }
            //General.showDebug("Element: " +i + " has in: " + in[i] + " out: " + out[i] );
        }
        return true;
    }

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        int length = 5;
        String[] str = new String[length];
        int[] ints = new int[length];

        General.verbosity = General.verbosityDebug;

//        if ( false ) {
//            str[0] = "N";
//            str[1] = "S";
//            str[2] = "C";
//            str[4] = "H";
//            boolean useRightJustified = true;
//            boolean status = translateElementNameToIdInArrays( str, ints,
//                useRightJustified, 0, length );
//
//            General.showDebug("Status: " + status);
//            General.showDebug("in   str: " + Strings.toString( str ));
//            General.showDebug("out ints: " + Strings.toString( PrimitiveArray.asList(ints)));
//
//        }

        if ( true ) {
            ints[0] = 6;
            ints[1] = 6;
            ints[2] = Defs.NULL_INT;
            ints[3] = 0;
            ints[4] = 1;
            boolean status = translateElementIdToNameInArrays( ints, str,
                0, length );
            General.showDebug("Status: " + status);
            General.showDebug("in  ints: " + Strings.toString( PrimitiveArray.asList(ints)));
            General.showDebug("out  str: " + Strings.toString( str ));
        }
//        if ( false ) {
//            General.showOutput("Maximum number of bonds allowed:");
//            General.showOutput(PrimitiveArray.toString(MAX_BONDS));
//        }

    }

    /**
     *
     * @param sequence space separated
     * @return
     */
    public static boolean sequenceIsIonsOnly(String sequence) {
        String[] resList = sequence.split(" ");
        for (int i=0;i<resList.length;i++) {
            if ( ! isIonResName(resList[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean isIonResName(String resName) {
        return ION_RESIDUE_NAME_LIST.contains(resName);
    }

    public static int getElementId(String atomName) {
        char ch = atomName.charAt(0);
        switch (ch) {
            case 'H': return ELEMENT_ID_HYDROGEN;
            case 'C': return ELEMENT_ID_CARBON;
            case 'S': return ELEMENT_ID_SULFUR;
            case 'N': return ELEMENT_ID_NITROGEN;
            case 'O': return ELEMENT_ID_OXYGEN;
            case 'P': return ELEMENT_ID_PHOSPOR;
            case 'Q': return ELEMENT_ID_PSEUDO;
            default: return ELEMENT_ID_UNKNOWN;
        }
    }
}
