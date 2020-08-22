EmbossOut.class: EmbossOut.java EmbossParser.class LogicalNode.class ParserDoneException.class ErrorLine.class
	javac EmbossOut.java

EmbossParser.class: EmbossParser.java
	javac EmbossParser.java

EmbossParser.java: EmbossParser.jj
	javacc EmbossParser.jj

LogicalNode.class: LogicalNode.java
	javac LogicalNode.java

ParserDoneException.class: ParserDoneException.java
	javac ParserDoneException.java

ErrorLine.class: ErrorLine.java
	javac ErrorLine.java

make clean:
	rm -f *.class
	rm -f ParseException.java SimpleCharStream.java Token.java TokenMgrError.java 
	rm -f EmbossParser.java EmbossParserConstants.java EmbossParserTokenManager.java
