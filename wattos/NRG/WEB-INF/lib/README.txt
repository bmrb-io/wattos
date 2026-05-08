Jars bundled into the deployed NRG webapp.

File                              Description
--------------------------------- --------------------------------------------
CSVutils.jar                      CSV parsing
JFlex.jar                         Lexer generator runtime (used by STAR parser)
Jmol.jar                          Molecular imagery
caffeine-3.1.8.jar                In-process LRU cache for MRGridServlet responses
colt.jar                          Primitive-array / hash-table collections
jakarta-regexp.jar                Pre-Java-1.4 regex library (very old)
mysql-connector-j-8.4.0.jar       MySQL JDBC driver (LTS, 2024)
printf_hb15.jar                   C-style formatted printing
starlibj_with_source.jar          STAR file format library (Steve Mading)

Notes:
- servlet-api.jar is provided by Tomcat at runtime (not bundled).
- Tomcat 9 expects mysql-connector-j to be loaded via the webapp
  classloader; placing it here is correct.
- Several of these jars are pre-2010 and could be retired/replaced as
  part of a future modernisation pass.
