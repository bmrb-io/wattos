@ECHO OFF
REM wattossetup: Script to define Wattos environment variables and aliases
REM ===========================================================
REM Jurgen F. Doreleijers
REM ===========================================================
REM Documented in unix versioned file: "wsetup"

set WATTOSMEM=512m

REM -------------------------------------------------
REM leave the rest untouched
REM -------------------------------------------------

set PATH=%PATH%;%WATTOSROOT%\scripts

set WATTOSSRCDIR=%WATTOSROOT%\src
set WATTOSCLASSDIR=%WATTOSROOT%\build\web\WEB-INF\classes
set WATTOSCLASSDIR=%WATTOSCLASSDIR%;%WATTOSROOT%\build\test\classes
set WATTOSJARSDIR=%WATTOSROOT%\lib
set WATTOSDOCDIR=%WATTOSROOT%\doc 
set WATTOSDATADIR=%WATTOSROOT%\data
set WATTOSBINDIR=%WATTOSROOT%\bin
set WATTOSSCRIPTSDIR=%WATTOSROOT%\scripts 

REM location of on-line manual for WATTOS:
set WATTOSHELPURL="file:///%WATTOSDOCDIR%/index.html"

REM Use the jar everywhere but on test computer named whelk.
REM There the development classes are used.
set CLASSPATH=%WATTOSJARSDIR%\Wattos.jar
if /I %computername% == WHELK  set CLASSPATH=%WATTOSCLASSDIR%
REM -------------------------------------------------
REM Use next line if need be to switch to production code.
REM -------------------------------------------------
set CLASSPATH=S:\wattos\Wattos\lib\Wattos.jar


echo Wattos initialized with CLASSPATH: %CLASSPATH%

REM # Then in alphabetical listing (capitals first)
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\ant-contrib.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\colt.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\CSVutils.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\jakarta-regexp.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\JavaCC.jar
#set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\jdbc20x.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\JFlex.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\mysql-connector-java-5.0.3-bin.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\printf_hb15.jar
REM set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\servlet.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\starlibj_with_source.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\swing-layout-1.0.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\jfreechart-1.0.1.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\jcommon-1.0.0.jar
set CLASSPATH=%CLASSPATH%;%WATTOSJARSDIR%\gnujaxp.jar
                                          
REM echo Classpath is set to: %CLASSPATH%

