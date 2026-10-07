const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user_watch_history", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("user_watch_history").get());
});

test("Authenticated user: cannot read another user's watch history", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("user_watch_history").doc("bob_hist_1").set({
      userId: BOB_UID,
      videoId: "vid_quantum_phone",
      title: "Transparent Phone Review",
      channelName: "Apex Tech Lab",
      thumbnailResName: "img_thumb_tech",
      durationSeconds: 15,
      progressFraction: 0.5,
      isLiked: true,
      isSavedToWatchLater: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("user_watch_history").doc("bob_hist_1").get());
});

test("Authenticated user: can create and query their own watch history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("user_watch_history").doc("alice_hist_1").set({
      userId: ALICE_UID,
      videoId: "vid_quantum_phone",
      title: "Transparent Phone Review",
      channelName: "Apex Tech Lab",
      thumbnailResName: "img_thumb_tech",
      durationSeconds: 15,
      progressFraction: 0.5,
      isLiked: true,
      isSavedToWatchLater: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("user_watch_history").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails query without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("user_watch_history").get());
});

test("Authenticated user: rejects shadow field injection on watch history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    aliceDb.collection("user_watch_history").doc("alice_hist_bad").set({
      userId: ALICE_UID,
      videoId: "vid_quantum_phone",
      title: "Transparent Phone Review",
      channelName: "Apex Tech Lab",
      thumbnailResName: "img_thumb_tech",
      durationSeconds: 15,
      progressFraction: 0.5,
      isLiked: true,
      isSavedToWatchLater: false,
      createdAt: now,
      updatedAt: now,
      isAdmin: true,
    })
  );
});

test("Authenticated user: can create and query SponsorBlock segments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("sponsor_segments").doc("seg_1").set({
      videoId: "vid_quantum_phone",
      submittedByUid: ALICE_UID,
      category: "Sponsor",
      startTimeSec: 2,
      endTimeSec: 6,
      votes: 1,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("sponsor_segments").where("videoId", "==", "vid_quantum_phone").get()
  );
});

test("Authenticated user: cannot delete another user's SponsorBlock segment", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("sponsor_segments").doc("seg_bob").set({
      videoId: "vid_quantum_phone",
      submittedByUid: BOB_UID,
      category: "Intro",
      startTimeSec: 0,
      endTimeSec: 3,
      votes: 5,
      createdAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("sponsor_segments").doc("seg_bob").delete());
});
