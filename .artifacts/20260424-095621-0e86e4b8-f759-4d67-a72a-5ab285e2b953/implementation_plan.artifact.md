# Fix Random Crash ("Voice rage game has stopped")

The "Voice rage game has stopped" crash is likely a `ConcurrentModificationException` caused by the `obstacles` list being modified in the `update()` method (on the `GameThread`) while simultaneously being accessed in the `draw()` method (also on the `GameThread`, but potentially asynchronously if not properly synchronized or if multiple threads are involved). Additionally, `SoundPool` interactions and `AudioEngine` lifecycle might contribute to instability.

## User Review Required

> [!IMPORTANT]
> I have identified a critical threading issue where the `obstacles` list is iterated over in `draw()` while being modified in `update()`. Although they currently run on the same thread in `GameThread.run()`, any future changes or unexpected OS behavior could trigger a crash. More importantly, I've noticed `selectedMode` logic in `onTouchEvent` has dead code that might cause the game to not start or crash if it expects a mode that isn't set.

## Proposed Changes

### Game Logic & Threading

#### [GameView.java](file:///C:/Users/shoun/AndroidStudioProjects/VoiceRageGame/app/src/main/java/com/shounak/voiceragegame/GameView.java)

- **Synchronize Obstacle Access**: Use a `synchronized` block or a thread-safe list (like `CopyOnWriteArrayList`) to prevent `ConcurrentModificationException`. I will choose `synchronized` for better performance in a game loop.
- **Fix Mode Selection Logic**: Clean up the `onTouchEvent` logic to correctly transition from "Mode Selection" to "Waiting for Sound" and then "Game Started".
- **Improve Sound Handling**: Ensure `soundPool` isn't used after being released.
- **Initialize Player Safely**: Ensure `player` is initialized before use in `update()`.

```java
// Example synchronization in update and draw
public void update() {
    synchronized(obstacles) {
        // ... update logic ...
    }
}

public void draw(Canvas canvas) {
    synchronized(obstacles) {
        for (Obstacle obs : obstacles) {
            // ... draw logic ...
        }
    }
}
```

#### [AudioEngine.java](file:///C:/Users/shoun/AndroidStudioProjects/VoiceRageGame/app/src/main/java/com/shounak/voiceragegame/AudioEngine.java)

- **Thread Safety for recording**: Ensure `isRecording` is volatile (already is) and handle thread termination more gracefully.

## Verification Plan

### Automated Tests
- I will run a build to ensure no regression in compilation.
- Command: `./gradlew :app:compileDebugJavaWithJavac`

### Manual Verification
- I will inspect the code for any remaining `ArrayList` iterations that happen outside of synchronization.
- I will verify the `onTouchEvent` logic by dry-running the state transitions.
- I will add `try-catch` blocks around `soundPool.play` as a defensive measure against "already released" errors.
