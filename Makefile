JC =javac
.SUFFIXES:.java .class
.java.class:
	$(JC) $*.java

CLASSES = \
	MyDedup.java

default:CLASSES

classes:$(CLASSES:.java=.class)

clean:\
	$(RM) *.class