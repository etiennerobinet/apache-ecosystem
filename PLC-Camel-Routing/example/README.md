# Camel-blueprint example

This is a little example showing the use of the camel-integration of PLC4X to read/write from/to a PLC

### s7-camel

- Connects to PLC with the given IP via TCP

- Fetches a WORD from the DB1 starting at Byte 0
- Prints the value into the log
- Writes the value to the Memento Byte 9 (to finally display it on outputs depending PLC programmation)

### ab-eth-camel

- Currently not implemented