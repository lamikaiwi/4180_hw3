JC =/mnt/c/jdk1.8.0/bin/javac.exe
JAVA=/mnt/c/jdk1.8.0/bin/java.exe
JFLAGS =-encoding UTF-8
SRC_DIR = source
BIN_DIR = bin
TEST_DIR = test

SOURCES = $(wildcard $(SRC_DIR)/*.java)
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(BIN_DIR)/%.class)

all: $(CLASSES)

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	mkdir -p $(BIN_DIR)
	$(JC) -d $(BIN_DIR) $<

test_upload:
	$(JAVA) -cp $(BIN_DIR) $(SRC_DIR)/MyDedup upload 128 256 512 257 $(TEST_DIR)/test_33M.pdf debug

test_download:
	$(JAVA) -cp $(BIN_DIR) $(SRC_DIR)/MyDedup download $(TEST_DIR)/test_33M.pdf $(TEST_DIR)/test_pdf33M_download.pdf debug

clean:
	rm -rf $(BIN_DIR)/$(SRC_DIR)