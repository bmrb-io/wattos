DiscoverOut.class: DiscoverOut.java DiscoverParser.class LogicalNode.class ParserDoneException.class ErrorLine.class
	javac DiscoverOut.java

DiscoverParser.class: DiscoverParser.java
	javac DiscoverParser.java

DiscoverParser.java: DiscoverParser.jj
	javacc DiscoverParser.jj

LogicalNode.class: LogicalNode.java
	javac LogicalNode.java

ParserDoneException.class: ParserDoneException.java
	javac ParserDoneException.java

ErrorLine.class: ErrorLine.java
	javac ErrorLine.java

make clean:
	rm -f *.class
	rm -f ParseException.java SimpleCharStream.java Token.java TokenMgrError.java 
	rm -f DiscoverParser.java DiscoverParserConstants.java DiscoverParserTokenManager.java
