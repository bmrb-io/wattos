all: clean precompile
	
clean:
	rm -f *.class
	rm -f ParseException.java SimpleCharStream.java Token.java TokenMgrError.java 
	rm -f AmberParser.java AmberParserConstants.java AmberParserTokenManager.java
        
precompile:
	java -classpath "C:\Program Files\javacc-3.2\bin\lib\Javacc.jar"  javacc AmberParserAll.jj   

compile:
	javac *.java        
        
	
#	
