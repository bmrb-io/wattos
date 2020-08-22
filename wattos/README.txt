This directory is the root directory of the Wattos package. Wattos homepage can
be found at http://www.bmrb.wisc.edu/wattos.

Content files           Description
------------------------+-------------------------------------------------------
.classpath				Classpath specifics for Eclipse
.cvsignore				File with info for Eclipse CVS
.project                File for Eclipse to regenerate (not included). Eclipse doesn't 
						like this one present if I want to make a web module.
buildEclipse.xml        Ant makefile.
README.txt              This file.
ReadmeEclipse.txt       Specifics about working with Eclipse.
wattos.properties       Settings for Ant.

Content directories     Description
(not all present)
------------------------+-------------------------------------------------------
.settings				Eclipse settings
build                   Java classes and resources (not in CVS)
data                    Default settings and test data.
doc						Javadoc by Eclipse (not in CVS)
lib                     Java jar files used by Wattos and the Wattos.jar itself.
macros                  Wattos commands demonstrating functionality using test data.
scripts                 Scripts for settings and demonstration of functionality.
src                     Java source files and resources
test                    JUnit test Java source files.
tmp_dir                 Temporary files directory.
wattos_home             Homepage files accessible at http://www.bmrb.wisc.edu/wattos.
NRG                     NMR Restraint Grid servlet related files. For eclipse this needs to
						be in this location otherwise it will not be picked up. Also note
						that Eclipse setting: web project settings/Context Root -> WebModule


