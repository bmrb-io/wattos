DyanaOut.class: DyanaOut.java DyanaParser.class LogicalNode.class ParserDoneException.class ErrorLine.class
	javac DyanaOut.java

DyanaParser.class: DyanaParser.java
	javac DyanaParser.java

DyanaParser.java: DyanaParser.jj
	javacc DyanaParser.jj

LogicalNode.class: LogicalNode.java
	javac LogicalNode.java

ParserDoneException.class: ParserDoneException.java
	javac ParserDoneException.java

ErrorLine.class: ErrorLine.java
	javac ErrorLine.java

make clean:
	rm -f *.class
	rm -f ParseException.java SimpleCharStream.java Token.java TokenMgrError.java 
	rm -f DyanaParser.java DyanaParserConstants.java DyanaParserTokenManager.java
