Jars bundled into the deployed NRG webapp.

File                              Description
--------------------------------- --------------------------------------------
CSVutils.jar                      CSV parsing (com.Ostermiller, gnu.getopt)
JFlex.jar                         Lexer-generator runtime — referenced by the
                                  Ant `star_scanner` target (see buildEclipse.xml).
                                  The generated STARLexer.java is itself self-
                                  contained and does not import JFlex/java_cup
Jmol.jar                          Molecular imagery
caffeine-3.1.8.jar                In-process LRU cache for MRGridServlet responses
colt.jar                          Primitive-array / hash-table collections
mysql-connector-j-8.4.0.jar       MySQL JDBC driver (LTS, 2024)
printf_hb15.jar                   C-style formatted printing (com.braju.format)
starlibj_with_source.jar          STAR file format library (Steve Mading)

Notes:
- servlet-api.jar is provided by Tomcat at runtime (not bundled).
- Tomcat 11 expects mysql-connector-j to be loaded via the webapp
  classloader; placing it here is correct.
- jakarta-regexp.jar was retired: it had no callers anywhere in src/.
  Strings.java and the three Converters/{Discover,Xplor,Emboss}/Utils.java
  were migrated to java.util.regex (in the JDK since 1.4).
- printf_hb15 (com.braju.format) and colt (cern.colt) still have many
  callers across Utils/, Database/, Soup/, Episode_II/. Migrating them
  to java.util.Formatter / java.util collections is a separate refactor.
