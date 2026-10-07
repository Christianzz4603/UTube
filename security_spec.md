# Security Specification: UTube Firestore Rules

## 1. Data Invariants
- Every document in `/user_watch_history/{historyId}` MUST belong to the authenticated user (`userId == request.auth.uid`), have a valid videoId, title, channelName, progressFraction between 0 and 1, and valid server timestamps (`createdAt`, `updatedAt`).
- Every document in `/sponsor_segments/{segmentId}` MUST be created by an authenticated user (`submittedByUid == request.auth.uid`), have a valid `videoId`, a valid `category` (`Sponsor`, `Intro`, `Outro`, `SelfPromo`, `Interaction`, `MusicOfftopic`), valid `startTimeSec >= 0` and `endTimeSec > startTimeSec`, and `votes >= 0`.

## 2. The "Dirty Dozen" Payloads
1. Unauthenticated read on `/user_watch_history` -> Reject
2. Cross-user read on `/user_watch_history/{id}` where `userId != request.auth.uid` -> Reject
3. Unfiltered list query on `/user_watch_history` without `where('userId', '==', uid)` -> Reject
4. Spoofed `userId` on create in `/user_watch_history` -> Reject
5. Shadow field injection (`"isAdmin": true`) on `/user_watch_history` -> Reject
6. Mutating immutable `createdAt` or `userId` on `/user_watch_history` update -> Reject
7. Oversized string (`title.size() > 300`) on `/user_watch_history` -> Reject
8. Invalid `progressFraction` (`< 0` or `> 1`) on `/user_watch_history` -> Reject
9. Unauthenticated create on `/sponsor_segments` -> Reject
10. Invalid SponsorBlock category (`"RandomCategory"`) on `/sponsor_segments` -> Reject
11. Invalid segment range (`endTimeSec <= startTimeSec`) on `/sponsor_segments` -> Reject
12. Deleting another user's SponsorBlock segment -> Reject
