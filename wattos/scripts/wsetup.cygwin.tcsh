#!/bin/csh -f

# wsetup: Script to define Wattos environment variables and aliases 
# for use with cywin on my labtop
# ===========================================================
# Jurgen F. Doreleijers
# ===========================================================
# source the script before running Wattos.
# The variable 'WATTOSROOT' must exist and point to the Wattos top directory.
# The variable 'WATTOSHOME' must exist and point to a writable user-specific directory 
#	(not needed to exist yet) where Wattos can save settings and state.

echo "Doing the unix-like setup"
source $WATTOSROOT/wsetup

setenv CWCLASSPATH  `cygpath -wp $CLASSPATH`
#setenv CWWATTOSHOME `cygpath -wp $WATTOSHOME`
#setenv CWWATTOSROOT `cygpath -wp $WATTOSROOT`
setenv WATTOSROOT `cygpath -wp $WATTOSROOT`; 
#setenv WATTOSROOT `cygpath -u $WATTOSROOT`

alias wattos        'java -Xmx$WATTOSMEM -classpath $CWCLASSPATH  Wattos.CloneWars.UserInterface -at $*'
alias relate        'java -Xmx$WATTOSMEM -classpath $CWCLASSPATH  Wattos.Utils.Programs.Relate $*'
alias mrannotator   'java -Xmx$WATTOSMEM -classpath $CWCLASSPATH  Wattos.Episode_II.MRAnnotate'
alias getEpochTime  'java -Xmx$WATTOSMEM -classpath $CWCLASSPATH  Wattos.Utils.Programs.GetEpochTime'

