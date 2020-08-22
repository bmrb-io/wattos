all: clean precompile 
	
clean:
	rm -f *.class
	rm -f ParseException.java SimpleCharStream.java Token.java TokenMgrError.java 
	rm -f XplorParser.java XplorParserConstants.java XplorParserTokenManager.java
        
precompile:
	java -classpath "C:\Program Files\javacc-3.2\bin\lib\javacc.jar"  javacc XplorParserAll.jj   

compile:
	javac *.java        

