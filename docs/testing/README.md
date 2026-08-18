# Testing Strategy

## Unit Tests
- Protocol serialization and parsing
- TTL handling and hop counting
- Duplicate detection
- Identity generation
- Queue management
- State transitions

## Integration Tests
- BLE discovery and connection
- Handshake completion
- Encrypted message exchange
- Acknowledgement flow
- Relay forwarding

## Security Tests
- Replay rejection
- Tampered packet rejection
- Invalid identity handling
- Malformed payload handling
- Unauthorized peer rejection

## Performance Tests
- Message latency measurement
- Delivery ratio under load
- Multi-hop relay timing
- Queue recovery after disconnect

## Running Tests

```bash
# Unit tests
./gradlew test

# Android instrumentation tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```
