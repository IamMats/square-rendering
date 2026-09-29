# Validation

These results apply to Square Rendering `0.1.0+mc1.21.11` with the versions pinned in the README. Runtime tests use Java 21 on Linux, Xvfb, and Mesa llvmpipe (OpenGL 4.5). They validate real Minecraft clients, chunk packets, and shader compilation; they are not a GPU performance benchmark.

## Automated build checks

`./gradlew build` passes 22 JUnit tests and Google Java Style checks. The remapped installable artifact is the single JAR in `build/libs`. The runtime test mod is excluded from it.

| Check | Coverage |
| --- | --- |
| Square membership | Inclusive boundary, all four corners, radii 0/2/4/16/32, and exactly 1,089 visible columns at radius 16 |
| Coordinates | Negative coordinates, chunk boundary crossings, and integer-overflow-safe membership |
| Tracking differences | Randomized movement/resizing against independent set differences, teleports, and vanilla/square transitions |
| Meshing border | Exactly 1,225 delivered columns at radius 16; the outer ring is excluded from the 1,089 visible columns |
| Persistence | Default file creation, enabled/disabled round trips, missing keys, and malformed-file preservation |
| Fog source | Only ordinary distance changes; spherical environmental fog remains intact; disabled, missing, unrelated, and already-transformed sources are handled |

## Minecraft runtime harness

Run:

```sh
./gradlew -PgameTests runClientGameTest
./gradlew -PgameTests -PsodiumExtra runClientGameTest
```

Prefix either command with `xvfb-run -a env LIBGL_ALWAYS_SOFTWARE=1` on a headless Linux machine. The harness accepts Minecraft's EULA for disposable test servers. Fabric's in-harness dedicated server runs in the client JVM, so it does **not** establish server-only class loading by itself.

Verified by the harness:

- Singleplayer with the mod installed on the client, including its integrated server.
- All visible columns and the support border received at distance 16.
- Sodium settings registration and pending changes, Undo, Reset, and Apply bindings.
- Corner block updates received without moving or reloading chunks.
- First-person and third-person rendering screenshots.
- Disabling unloads missing vanilla corners; re-enabling restores them.
- Resource and shader reloads complete successfully when applying changes.
- Teleportation across negative chunk boundaries and render-distance changes.
- Nether/Overworld dimension transitions.
- Death and respawn retain the enabled preference and restore square tracking.
- Water, lava, blindness, and darkness are exercised with active in-game fog and screenshots.
- Dedicated-server networking and server-approved distance changes.
- Sodium Extra 0.8.3 with its default fog settings.
- Mixin audit with required injection targets resolving successfully.

Screenshots and game logs are generated under `build/run/clientGameTest/`. The tests check packet delivery and render-bound membership directly; screenshots provide visual evidence rather than cross-driver pixel-identical assertions.

## Independent client/server checks

A separate `./gradlew runServer --args nogui` process successfully starts with `Env=SERVER`, Fabric API, and Square Rendering. Its loaded-mod list contains no Sodium. It reaches Minecraft's `Done` startup message and shuts down cleanly using `stop`.

All three external connection checks passed:

| Client | Server | Observed behavior |
| --- | --- | --- |
| Square Rendering enabled | Square Rendering installed, no Sodium | Full square and support border delivered at the server's radius 8 limit |
| Square Rendering excluded by Fabric Loader | Square Rendering installed, no Sodium | Normal vanilla chunk delivery; diagonal corners remain absent |
| Square Rendering enabled | Square Rendering excluded by Fabric Loader | Join succeeds, server support is correctly detected as absent, normal chunk requests retained |

To reproduce optional-installation checks, configure a disposable local server in `run-server/server.properties` with `server-ip=127.0.0.1`, `server-port=25579`, `online-mode=false`, and `view-distance=8`. Accept the EULA for that test instance. Start the server, then use a second terminal:

```sh
# Both client and server have Square Rendering.
./gradlew -PgameTests -PexternalServer=127.0.0.1:25579 runClientGameTest

# Fabric Loader excludes Square Rendering from the test client's loaded mods.
./gradlew -PgameTests -PexternalServer=127.0.0.1:25579 -PunmodifiedClient runClientGameTest
```

For a server without Square Rendering, stop the server, restart it with `./gradlew -PwithoutSquareServer runServer --args nogui`, then run:

```sh
./gradlew -PgameTests -PexternalServer=127.0.0.1:25579 -PunsupportedServer runClientGameTest
```

These checks compare actual received chunks against the expected square or vanilla footprint and ensure the client's requested render distance stays at 16. The debug exclusion flags apply only to these development launches.

## Remaining manual coverage and limits

- Run a simultaneous multiplayer session with several independent modified and unmodified clients, including movement in different directions and dimensions. Sequential connection checks do not establish behavior under concurrent player load.
- Compare environmental fog visually on hardware GPUs and in varied biomes, lighting, and weather. Unit checks establish shader-source preservation; shader compilation alone cannot prove every visual effect.
- Exercise Sodium's tree fallback and immediate mesh-update paths during rapid building and occlusion changes. The shared collection hook covers these paths, but the harness does not force every internal traversal state.
- Check beacons at the support-border boundary and large modded block-entity models in a normal play session.
- No performance budget or multiplayer stress-test claim is made.
- Iris shader packs, custom fog overrides, and non-Fabric server implementations remain outside the compatibility target.
