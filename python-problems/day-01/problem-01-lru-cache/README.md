# Problem 01: Thread-Safe LRU Cache Decorator for Asynchronous Functions

## Problem Statement

Implement a thread-safe LRU (Least Recently Used) cache decorator for asynchronous functions that supports an optional time-to-live (TTL) for each cache entry. The cache should:

- Be thread-safe for concurrent access
- Support both synchronous and asynchronous functions
- Allow configurable cache size and TTL
- Provide cache statistics and management features
- Handle cache eviction based on LRU policy
- Support per-entry TTL configuration

## Solution Overview

### Architecture

The solution implements a decorator-based caching system with the following components:

1. **AsyncLRUCache**: Main cache class with thread-safe operations
2. **CacheEntry**: Represents a cached item with metadata
3. **CacheStats**: Tracks cache performance metrics
4. **LRUCacheDecorator**: Decorator for easy function wrapping

### Key Features

- **Thread Safety**: Uses `asyncio.Lock` for async operations and `threading.Lock` for sync operations
- **LRU Eviction**: Implements doubly-linked list for O(1) eviction
- **TTL Support**: Automatic expiration with configurable per-entry TTL
- **Statistics**: Tracks hit/miss ratios, eviction counts, and performance metrics
- **Flexible Configuration**: Supports global and per-entry TTL settings

### Design Decisions

1. **Separate Sync/Async Locks**: Different locking mechanisms for optimal performance
2. **Lazy TTL Cleanup**: Periodic cleanup instead of per-access checks
3. **Statistics Tracking**: Comprehensive metrics for monitoring and optimization
4. **Memory Efficient**: Uses weak references where appropriate

## Implementation

### Core Classes

- `AsyncLRUCache`: Main cache implementation
- `CacheEntry`: Individual cache entry with metadata
- `CacheStats`: Performance tracking
- `lru_cache`: Decorator function

### Usage Examples

```python
# Basic usage
@lru_cache(maxsize=100, ttl=300)
async def expensive_api_call(user_id: int):
    # Simulate API call
    await asyncio.sleep(1)
    return f"user_data_{user_id}"

# With per-call TTL
@lru_cache(maxsize=50)
async def get_user_profile(user_id: int, ttl=60):
    # Cache for 60 seconds
    return await fetch_user_data(user_id)
```

## Performance Characteristics

- **Time Complexity**: O(1) for get/set operations
- **Space Complexity**: O(n) where n is maxsize
- **Thread Safety**: Full thread safety with minimal contention
- **Memory Usage**: Efficient with automatic cleanup

## Testing

The implementation includes comprehensive tests covering:
- Thread safety under concurrent access
- TTL expiration behavior
- LRU eviction policy
- Statistics accuracy
- Edge cases and error handling

## Dependencies

- `asyncio`: For async support
- `threading`: For thread safety
- `time`: For TTL calculations
- `weakref`: For memory efficiency
- `typing`: For type hints 